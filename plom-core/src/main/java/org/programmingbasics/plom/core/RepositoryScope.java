package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.programmingbasics.plom.core.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.VariableDescription;
import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ErrorList;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.interpreter.CodeUnitLocation;
import org.programmingbasics.plom.core.interpreter.CoreTypeLibrary;
import org.programmingbasics.plom.core.interpreter.ExecutableFunction;
import org.programmingbasics.plom.core.interpreter.GatheredSuggestions;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.Type;
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
  
  public RepositoryScope(ModuleCodeRepository repository, CoreTypeLibrary coreTypes)
  {
    this.repository = repository;
    this.coreTypes = coreTypes;

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
      typeFromToken(Token.ParameterToken.fromContents("@object", Symbol.AtType));
      typeFromToken(Token.ParameterToken.fromContents("@number", Symbol.AtType));
      typeFromToken(Token.ParameterToken.fromContents("@boolean", Symbol.AtType));
      typeFromToken(Token.ParameterToken.fromContents("@string", Symbol.AtType));
      typeFromToken(Token.ParameterToken.fromContents("@null", Symbol.AtType));
      typeFromToken(Token.ParameterToken.fromContents("@void", Symbol.AtType));
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
        (name, type) -> {
          if (type == null) type = coreTypes.getVoidType();
          addVariable(name, type, coreTypes.getNullValue());
          globalVariableSuggestions.add(name);
        };
    VariableDeclarationInterpreter.TypeLookup typeLookup =
        token -> {
          try {
            return typeFromToken(token);
          } catch (RunException e) {
            // skip any errors
            return null;
          }
        };
    VariableDeclarationInterpreter.fromStatements(repository.getImportedVariableDeclarationCode(),
        variableDeclarer, typeLookup, errors);
    VariableDeclarationInterpreter.fromStatements(repository.getVariableDeclarationCode(),
        variableDeclarer, typeLookup, errors);
    globalVariableSuggestions.sort(null);
  }
  
  private Type makeFunctionType(FunctionDescription func) throws RunException
  {
    Type [] argTypes = new Type[func.sig.argTypes.size()];
    for (int n = 0; n < func.sig.argTypes.size(); n++)
      argTypes[n] = typeFromToken(func.sig.argTypes.get(n));
    return Type.makeFunctionType(typeFromToken(func.sig.returnType), argTypes);    
  }
  
  @Override
  public Value lookup(String name) throws RunException
  {
    FunctionDescription func = repository.getFunctionDescription(name);
    if (func != null)
    {
      Value val = new Value();
      val.type = makeFunctionType(func);
      
      AstNode code;
      try {
        code = ParseToAst.parseStatementContainer(func.code);
      } 
      catch (ParseException e)
      {
        // TODO: Augment parse info with function name etc.
        throw new RunException(e);
      }
      List<String> argPosToName = new ArrayList<>();
      for (int n = 0; n < func.sig.argNames.size(); n++)
      {
        argPosToName.add(func.sig.argNames.get(n));
      }
      val.val = ExecutableFunction.forCode(CodeUnitLocation.forFunction(name), code, argPosToName);
      return val;
    }
    return super.lookup(name);
  }

  @Override
  public Type lookupType(String name)
  {
    FunctionDescription func = repository.getFunctionDescription(name);
    if (func != null)
    {
      try {
        return makeFunctionType(func);
      }
      catch (RunException e)
      {
        // Fall through
      }
    }
    return super.lookupType(name);
  }

  @Override
  public LValue lookupLValue(String name) throws RunException
  {
    return super.lookupLValue(name);
  }

  // TODO: String not good enough for generics
  private Map<String, Type> typeLookupCache = new HashMap<>(); 

  @Override
  public Type typeFromToken(Token typeToken) throws RunException
  {
    String name = ((Token.ParameterToken)typeToken).getLookupName();
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
          throw new RunException("Unknown class");
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
        toReturn.parent = typeFromToken(cls.parent); 
      }
      addMemberVarsFromClass(toReturn, cls);
      for (FunctionDescription fn: cls.methods)
      {
        if (fn.sig.isBuiltIn) continue;
        Type[] args = new Type[fn.sig.argTypes.size()];
        for (int n = 0; n < fn.sig.argTypes.size(); n++)
          args[n] = typeFromToken(fn.sig.argTypes.get(n));
        try {
          AstNode code = ParseToAst.parseStatementContainer(fn.code);
          if (fn.sig.isStatic)
          {
            ExecutableFunction execFn = ExecutableFunction.forCode(CodeUnitLocation.forStaticMethod(cls.getName(), fn.sig.getLookupName()), 
                code, fn.sig.argNames);
            toReturn.addStaticMethod(fn.sig.getLookupName(), execFn, 
                typeFromToken(fn.sig.returnType), args);
          }
          else if (fn.sig.isConstructor)
          {
            ExecutableFunction execFn = ExecutableFunction.forCode(CodeUnitLocation.forConstructorMethod(cls.getName(), fn.sig.getLookupName()), 
                code, fn.sig.argNames);
            // Use @void as the return type for constructors
            toReturn.addStaticMethod(fn.sig.getLookupName(), execFn, 
                coreTypes.getVoidType(), args);
          }
          else
          {
            ExecutableFunction execFn = ExecutableFunction.forCode(CodeUnitLocation.forMethod(cls.getName(), fn.sig.getLookupName()), 
                code, fn.sig.argNames);
            toReturn.addMethod(fn.sig.getLookupName(), execFn, 
                typeFromToken(fn.sig.returnType), args);
          }
        } 
        catch (ParseException e)
        {
          // Ignore parse exceptions for now (allowing us to run
          // code that might have errors in unrelated methods)
        }
        catch (RunException e)
        {
          // RunException might be generated if functions use bad types
          // in their signatures
        }
      }
    }
    
    return toReturn;
  }

  private void addMemberVarsFromClass(Type toReturn, ClassDescription cls) throws RunException
  {
    if (cls.parent != null && !cls.getName().equals("object"))
    {
      String name = cls.parent.getLookupName();
      ClassDescription parentCls = codeRepositoryClasses.get(name);
      addMemberVarsFromClass(toReturn, parentCls);
    }
    for (VariableDescription var: cls.variables)
    {
      toReturn.addMemberVariable(var.name, typeFromToken(var.type));
    }
    // Also run the variable declaration code to get members from there
    ErrorList errors = new ErrorList();
    VariableDeclarationInterpreter.VariableDeclarer variableDeclarer =
        (name, type) -> {
          if (type == null) type = coreTypes.getVoidType();
          toReturn.addMemberVariable(name, type);
        };
    VariableDeclarationInterpreter.TypeLookup typeLookup =
        token -> {
          try {
            return typeFromToken(token);
          } catch (RunException e) {
            // skip any errors
            return null;
          }
        };
    VariableDeclarationInterpreter.fromStatements(
        cls.getVariableDeclarationCode(),
        variableDeclarer, typeLookup, errors);
  }
  
  @Override
  public List<Type> getAllKnownTypes()
  {
    List<Type> types = new ArrayList<>();
    // Create Type objects for all classes in the repository
    for (ClassDescription cls: codeRepositoryClasses.values())
    {
      try {
        types.add(typeFromToken(Token.ParameterToken.fromContents("@" + cls.getName(), Symbol.AtType)));
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
