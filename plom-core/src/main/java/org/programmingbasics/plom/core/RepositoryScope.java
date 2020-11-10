package org.programmingbasics.plom.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.programmingbasics.plom.core.ModuleCodeRepository.ClassDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.ModuleCodeRepository.VariableDescription;
import org.programmingbasics.plom.core.ast.AstNode;
import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.interpreter.CodeUnitLocation;
import org.programmingbasics.plom.core.interpreter.CoreTypeLibrary;
import org.programmingbasics.plom.core.interpreter.ExecutableFunction;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.Value;
import org.programmingbasics.plom.core.interpreter.Value.LValue;
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
  
  public RepositoryScope(ModuleCodeRepository repository, CoreTypeLibrary coreTypes)
  {
    this.repository = repository;
    this.coreTypes = coreTypes;

    // Hash the different classes so that we can look them up more quickly later
    codeRepositoryClasses = new HashMap<>();
    for (ClassDescription cls: repository.classes)
    {
      codeRepositoryClasses.put(cls.name, cls);
    }
    if (repository.chainedRepository != null)
    {
      for (ClassDescription cls: repository.chainedRepository.classes)
      {
        codeRepositoryClasses.put(cls.name, cls);
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
  }
  
  @Override
  public Value lookup(String name) throws RunException
  {
    FunctionDescription func = repository.getFunctionDescription(name);
    if (func != null)
    {
      Value val = new Value();
      val.type = Type.makeFunctionType(coreTypes.getVoidType());
      
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
      for (VariableDescription var: cls.variables)
      {
        toReturn.addMemberVariable(var.name, typeFromToken(var.type));
      }
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
            ExecutableFunction execFn = ExecutableFunction.forCode(CodeUnitLocation.forStaticMethod(cls.name, fn.sig.getLookupName()), 
                code, fn.sig.argNames);
            toReturn.addStaticMethod(fn.sig.getLookupName(), execFn, 
                typeFromToken(fn.sig.returnType), args);
          }
          else if (fn.sig.isConstructor)
          {
            ExecutableFunction execFn = ExecutableFunction.forCode(CodeUnitLocation.forConstructorMethod(cls.name, fn.sig.getLookupName()), 
                code, fn.sig.argNames);
            toReturn.addStaticMethod(fn.sig.getLookupName(), execFn, 
                typeFromToken(fn.sig.returnType), args);
          }
          else
          {
            ExecutableFunction execFn = ExecutableFunction.forCode(CodeUnitLocation.forMethod(cls.name, fn.sig.getLookupName()), 
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

  @Override
  public List<Type> getAllKnownTypes()
  {
    List<Type> types = new ArrayList<>();
    // Create Type objects for all classes in the repository
    for (ClassDescription cls: codeRepositoryClasses.values())
    {
      try {
        types.add(typeFromToken(Token.ParameterToken.fromContents("@" + cls.name, Symbol.AtType)));
      }
      catch (RunException e)
      {
        // Eat all errors
      }
    }
    return types;
  }
  
  @Override
  public void lookupSuggestions(String val, List<String> suggestions)
  {
    suggestions.addAll(repository.getAllFunctionNamesSorted());
    for (VariableDescription v: repository.globalVars)
    {
      suggestions.add(v.name);
    }
    if (repository.chainedRepository != null)
    {
      for (VariableDescription v: repository.chainedRepository.globalVars)
      {
        suggestions.add(v.name);
      }
    }
  }
}
