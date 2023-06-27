package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.programmingbasics.plom.core.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionSignature;
import org.programmingbasics.plom.core.ModuleCodeRepository.VariableDescription;
import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.CodePosition;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.FindToken;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.CodeUnitLocation;
import org.programmingbasics.plom.core.interpreter.CoreTypeLibrary;
import org.programmingbasics.plom.core.interpreter.ExecutableFunction;
import org.programmingbasics.plom.core.interpreter.GatheredSuggestions;
import org.programmingbasics.plom.core.interpreter.ProgramCodeLocation;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.SimpleInterpreter.ErrorLogger;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.UnboundType;
import org.programmingbasics.plom.core.interpreter.Value;
import org.programmingbasics.plom.core.interpreter.Value.LValue;

import org.programmingbasics.plom.core.interpreter.VariableDeclarationInterpreter;
import org.programmingbasics.plom.core.interpreter.VariableScope;

import jsinterop.annotations.JsType;

/**
 * Maps a code repository so that it can be accessed in the scope stack
 * so that it can be accessed in the SimpleInterpreter 
 */
@JsType
public class RepositoryScope extends VariableScope
{
  // TODO: Add caching to this
  private ModuleCodeRepository repository;
  private CoreTypeLibrary coreTypes;
  private Map<String, ClassDescription> codeRepositoryClasses;
  private List<String> globalVariableSuggestions = new ArrayList<>();
  
  /** Since all code in the repository will go through this object when
   * the program is run run, we can also do error logging for code errors 
   * in this object
   */
  private ErrorLogger errLogger;
  
  public RepositoryScope(ModuleCodeRepository repository, CoreTypeLibrary coreTypes, ErrorLogger errLogger)
  {
    this.repository = repository;
    this.coreTypes = coreTypes;
    this.errLogger = errLogger;

    // Hash the different classes so that we can look them up more quickly later
    codeRepositoryClasses = new HashMap<>();
    for (ClassDescription cls: repository.classes)
    {
      codeRepositoryClasses.put(cls.getName(), cls);
    }
    if (repository.chainedRepository != null)
    {
      for (ClassDescription cls: repository.chainedRepository.classes)
      {
        codeRepositoryClasses.put(cls.getName(), cls);
      }
    }
    
    // Force a load of the standard built-in types (the interpreter
    // has direct access to these types, but the repository may have
    // extra methods defined on them)
    try {
      typeFromUnboundType(UnboundType.forClassLookupName("object"), this);
      typeFromUnboundType(UnboundType.forClassLookupName("number"), this);
      typeFromUnboundType(UnboundType.forClassLookupName("boolean"), this);
      typeFromUnboundType(UnboundType.forClassLookupName("string"), this);
      typeFromUnboundType(UnboundType.forClassLookupName("null"), this);
      typeFromUnboundType(UnboundType.forClassLookupName("void"), this);
    }
    catch (RunException e)
    {
      throw new IllegalArgumentException(e);
    }

    // Create variables for all the global variables in the module
    for (VariableDescription v: repository.globalVars)
    {
      try {
        addVariable(v.name, typeFromToken(v.type), coreTypes.getNullValue());
      }
      catch (RunException e)
      {
        // Ignore errors when registering variables
      }
    }
    if (repository.chainedRepository != null)
    {
      for (VariableDescription v: repository.chainedRepository.globalVars)
      {
        // This code is obsolete since VariableDescription is being phased out of the UI
        try {
          addVariable(v.name, typeFromToken(v.type), coreTypes.getNullValue());
        }
        catch (RunException e)
        {
          // Ignore errors when registering variables
        }
      }
    }
    ErrorList errors = new ErrorList();
    VariableDeclarationInterpreter.VariableDeclarer variableDeclarer =
        (name, unboundType) -> {
          Type type;
          try {
            type = typeFromUnboundType(unboundType, this);
          } catch (RunException e) {
            // skip any errors
            if (errLogger != null)
              errLogger.warn(new RunException("Could not determine type for global variable " + name, e));
            type = null;
          }
          if (type == null) type = coreTypes.getVoidType();
          addVariable(name, type, coreTypes.getNullValue());
          globalVariableSuggestions.add(name);
        };
//    VariableDeclarationInterpreter.TypeLookup<Type> typeLookup =
//        unboundType -> {
//          try {
//            return typeFromUnboundType(unboundType);
//          } catch (RunException e) {
//            // skip any errors
//            return null;
//          }
//        };
    VariableDeclarationInterpreter.fromStatements(repository.getImportedVariableDeclarationCode(),
        variableDeclarer, errors);
    VariableDeclarationInterpreter.fromStatements(repository.getVariableDeclarationCode(),
        variableDeclarer, errors);
    if (errLogger != null)
    {
      for (Exception e: errors.getCodeErrors())
      {
        errLogger.warn(new RunException("Problem with variable declarations", e));
      }
    }
    globalVariableSuggestions.sort(null);
  }
  
  private Type makeFunctionType(FunctionDescription func) throws RunException
  {
    Type [] argTypes = new Type[func.sig.getNumArgs()];
    for (int n = 0; n < func.sig.getNumArgs(); n++)
    {
      try {
        argTypes[n] = typeFromUnboundType(func.sig.getArgType(n), this);
      }
      catch (RunException e)
      {
        throw new RunException("Problem with function argument " + (n + 1), e);
      }
    }
    try {
      return Type.makeFunctionType(typeFromUnboundType(func.sig.getReturnType(), this), argTypes);
    }
    catch (RunException e)
    {
      throw new RunException("Problem with return type", e);
    }
  }
  
  private ParsedFunction parseFunction(String name, FunctionDescription func) throws RunException
  {
    ParsedFunction funCache = new ParsedFunction();
    funCache.sig = func.sig;
    List<Throwable> signatureWarnings = new ArrayList<>();
    funCache.sig.gatherSignatureParseWarnings(signatureWarnings);
    if (!signatureWarnings.isEmpty())
      throw new RunException("Function signature is invalid", signatureWarnings.get(0), ProgramCodeLocation.forFunction(name));
    
    try {
      funCache.type = makeFunctionType(func);
    } 
    catch (RunException e)
    {
      throw new RunException("Function signature is invalid", e, ProgramCodeLocation.forFunction(name));
    }
    
    try {
      funCache.sourceLookup = Optional.of(func.code);
      funCache.code = ParseToAst.parseStatementContainer(func.code);
    } 
    catch (ParseException e)
    {
      CodePosition pos = null;
      if (e.token != null)
        pos = FindToken.tokenToPosition(e.token, func.code);
      throw new RunException("Syntax error in function code", e, ProgramCodeLocation.forFunction(name, pos));
    }
    return funCache;

  }
  
  @Override
  public Value lookup(String name) throws RunException
  {
    ParsedFunction funCache = functionLookupCache.get(name);
    if (funCache == null)
    {
      FunctionDescription func = repository.getFunctionDescription(name);
      if (func != null)
      {
        funCache = parseFunction(name, func);
        functionLookupCache.put(name, funCache);
      }
    }
    if (funCache != null)
    {
      Value val = new Value();
      val.type = funCache.type;
      
      List<String> argPosToName = new ArrayList<>();
      for (int n = 0; n < funCache.sig.getNumArgs(); n++)
      {
        argPosToName.add(funCache.sig.getArgName(n));
      }
      val.val = ExecutableFunction.forCode(CodeUnitLocation.forFunction(name), funCache.code, funCache.sourceLookup, argPosToName);
      return val;
    }
    return super.lookup(name);
  }

  @Override
  public Type lookupType(String name)
  {
    ParsedFunction funCache = functionLookupCache.get(name);
    if (funCache == null)
    {
      FunctionDescription func = repository.getFunctionDescription(name);
      if (func != null)
      {
        try {
          funCache = parseFunction(name, func);
        }
        catch (RunException e)
        {
          // Fall through
        }
        functionLookupCache.put(name, funCache);
      }
    }
    
    if (funCache != null) 
      return funCache.type;
    return super.lookupType(name);
  }

  @Override
  public LValue lookupLValue(String name) throws RunException
  {
    return super.lookupLValue(name);
  }

  // TODO: String not good enough for generics
  private Map<String, Type> typeLookupCache = new HashMap<>();
  
  static class ParsedFunction
  {
    FunctionSignature sig;
    Type type;
    AstNode code;
    Optional<StatementContainer> sourceLookup;
  }
  private Map<String, ParsedFunction> functionLookupCache = new HashMap<>(); 
  
  @Override
  public Type typeFromUnboundType(UnboundType unboundType, VariableScope subTypeCreator) throws RunException
  {
    if (unboundType == null) throw new RunException();
    if (unboundType.mainToken.type == Symbol.FunctionTypeName) 
    {
      return helperFunctionTypeFromUnboundType(unboundType, subTypeCreator);
    }
    String name = unboundType.mainToken.getLookupName();
    Type toReturn = typeLookupCache.get(name);
    if (toReturn != null) return toReturn;
    switch (name)
    {
      case "number": toReturn = coreTypes.getNumberType(); break;
      case "string": toReturn = coreTypes.getStringType(); break;
      case "boolean": toReturn = coreTypes.getBooleanType(); break;
      case "object": toReturn = coreTypes.getObjectType(); break;
      case "null": toReturn = coreTypes.getNullType(); break;
      case "void": toReturn = coreTypes.getVoidType(); break;
      case "object array": toReturn = coreTypes.getObjectArrayType(); break;
      default: 
      {
        if (codeRepositoryClasses.containsKey(name))
          toReturn = new Type(name); 
        else
          throw new RunException("Unknown class: @" + name);
        break;
      }
    }
    typeLookupCache.put(name, toReturn);
    
    // Load any methods into it as needed
    if (codeRepositoryClasses.containsKey(name))
    {
      ClassDescription cls = codeRepositoryClasses.get(name);
      if (cls.parent != null)
      {
        toReturn.parent = typeFromUnboundType(cls.parent, subTypeCreator); 
      }
      addMemberVarsFromClass(toReturn, cls);
      for (FunctionDescription fn: cls.methods)
      {
        if (fn.sig.isBuiltIn) continue;
        try {
          List<Throwable> signatureWarnings = new ArrayList<>();
          fn.sig.gatherSignatureParseWarnings(signatureWarnings);
          if (!signatureWarnings.isEmpty())
            throw new RunException("Syntax error in method signature", signatureWarnings.get(0), ProgramCodeLocation.forFunction(name));
          Type[] args = new Type[fn.sig.getNumArgs()];
          for (int n = 0; n < fn.sig.getNumArgs(); n++)
          {
            try {
              args[n] = typeFromUnboundType(fn.sig.getArgType(n), subTypeCreator);
            }
            catch (RunException e)
            {
              throw new RunException("Problem with function argument " + (n + 1), e);
            }
          }
          AstNode code = ParseToAst.parseStatementContainer(fn.code);
          if (fn.sig.isStatic)
          {
            ExecutableFunction execFn = ExecutableFunction.forCode(CodeUnitLocation.forStaticMethod(cls.getName(), fn.sig.getLookupName()), 
                code, Optional.of(fn.code), fn.sig.argNames);
            Type returnType;
            try {
              returnType = typeFromUnboundType(fn.sig.getReturnType(), subTypeCreator); 
            }
            catch (RunException e)
            {
              throw new RunException("Problem with return type", e);
            }
            toReturn.addStaticMethod(fn.sig.getLookupName(), execFn, 
                returnType, args);
          }
          else if (fn.sig.isConstructor)
          {
            ExecutableFunction execFn = ExecutableFunction.forCode(CodeUnitLocation.forConstructorMethod(cls.getName(), fn.sig.getLookupName()), 
                code, Optional.of(fn.code), fn.sig.argNames);
            // Use @void as the return type for constructors
            toReturn.addStaticMethod(fn.sig.getLookupName(), execFn, 
                coreTypes.getVoidType(), args);
          }
          else
          {
            ExecutableFunction execFn = ExecutableFunction.forCode(CodeUnitLocation.forMethod(cls.getName(), fn.sig.getLookupName()), 
                code, Optional.of(fn.code), fn.sig.argNames);
            Type returnType;
            try {
              returnType = typeFromUnboundType(fn.sig.getReturnType(), subTypeCreator); 
            }
            catch (RunException e)
            {
              throw new RunException("Problem with return type", e);
            }
            toReturn.addMethod(fn.sig.getLookupName(), execFn, 
                returnType, args);
          }
        } 
        catch (ParseException e)
        {
          // Ignore parse exceptions for now (allowing us to run
          // code that might have errors in unrelated methods)
          if (errLogger != null)
          {
            CodePosition pos = null;
            if (e.token != null)
              pos = FindToken.tokenToPosition(e.token, fn.code);
            errLogger.warn(new RunException("Method signature is invalid", e, 
                ProgramCodeLocation.forMethod(cls.getName(), fn.sig.getLookupName(), fn.sig.isStatic || fn.sig.isConstructor, pos)));
            
          }
        }
        catch (RunException e)
        {
          // RunException might be generated if functions use bad types
          // in their signatures
          if (errLogger != null)
            errLogger.warn(new RunException("Method signature is invalid", e, 
                ProgramCodeLocation.forMethod(cls.getName(), fn.sig.getLookupName(), fn.sig.isStatic || fn.sig.isConstructor)));
        }
      }
    }
    
    return toReturn;
  }

  private void addMemberVarsFromClass(Type toReturn, ClassDescription cls) throws RunException
  {
    if (cls.parent != null && !cls.getName().equals("object"))
    {
      String name = cls.parent.mainToken.getLookupName();
      ClassDescription parentCls = codeRepositoryClasses.get(name);
      addMemberVarsFromClass(toReturn, parentCls);
    }
    for (VariableDescription var: cls.variables)
    {
      // This code is obsolete since variable declarations are being phased out of the UI
      toReturn.addMemberVariable(var.name, typeFromToken(var.type));
    }
    // Also run the variable declaration code to get members from there
    ErrorList errors = new ErrorList();
    VariableDeclarationInterpreter.VariableDeclarer variableDeclarer =
        (name, unboundType) -> {
          Type type;
          try {
            type = typeFromUnboundType(unboundType, this);
          } catch (RunException e) {
            // skip any errors
            type = null;
            if (errLogger != null)
              errLogger.warn(new RunException("Could not determine type for member variable " + name, e, ProgramCodeLocation.forClass(cls.getName())));
          }
          if (type == null) type = coreTypes.getVoidType();
          toReturn.addMemberVariable(name, type);
        };
//    VariableDeclarationInterpreter.TypeLookup<Type> typeLookup =
//        unboundType -> {
//          try {
//            return typeFromUnboundType(unboundType);
//          } catch (RunException e) {
//            // skip any errors
//            return null;
//          }
//        };
    VariableDeclarationInterpreter.fromStatements(
        cls.getVariableDeclarationCode(),
        variableDeclarer, errors);
    if (errLogger != null)
    {
      for (Exception e: errors.getCodeErrors())
      {
        errLogger.warn(new RunException("Problem with member variable declarations", e, ProgramCodeLocation.forClass(cls.getName())));
      }
    }

  }
  
  @Override
  public List<Type> getAllKnownTypes()
  {
    List<Type> types = new ArrayList<>();
    // Create Type objects for all classes in the repository
    for (ClassDescription cls: codeRepositoryClasses.values())
    {
      try {
        types.add(typeFromUnboundType(UnboundType.forClassLookupName(cls.getName()), this));
      }
      catch (RunException e)
      {
        // Eat all errors
      }
    }
    return types;
  }
  
  @Override
  public void lookupSuggestions(GatheredSuggestions suggestions)
  {
    suggestions.addAllSuggestions(repository.getAllFunctionNamesSorted());
    for (VariableDescription v: repository.globalVars)
    {
      suggestions.addSuggestion(v.name);
    }
    if (repository.chainedRepository != null)
    {
      for (VariableDescription v: repository.chainedRepository.globalVars)
      {
        suggestions.addSuggestion(v.name);
      }
    }
    for (String name: globalVariableSuggestions)
      suggestions.addSuggestion(name);
  }
}
