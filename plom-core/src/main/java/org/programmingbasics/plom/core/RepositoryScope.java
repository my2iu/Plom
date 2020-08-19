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
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.gen.Symbol;
import org.programmingbasics.plom.core.interpreter.CodeUnitLocation;
import org.programmingbasics.plom.core.interpreter.CoreTypeLibrary;
import org.programmingbasics.plom.core.interpreter.ExecutableFunction;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.Value;
import org.programmingbasics.plom.core.interpreter.Value.LValue;
import org.programmingbasics.plom.core.interpreter.VariableScope;

/**
 * Maps a code repository so that it can be accessed in the scope stack
 * so that it can be accessed in the SimpleInterpreter 
 */
public class RepositoryScope extends VariableScope
{
  // TODO: Add caching to this
  private ModuleCodeRepository repository;
  private CoreTypeLibrary coreTypes;

  public RepositoryScope(ModuleCodeRepository repository, CoreTypeLibrary coreTypes)
  {
    this.repository = repository;
    this.coreTypes = coreTypes;
    
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
    
    // Create all methods for all classes
    for (ClassDescription cls: repository.classes)
    {
      try {
        Type t = typeFromToken(Token.ParameterToken.fromContents("@" + cls.name, Symbol.AtType));
        for (FunctionDescription fn: cls.methods)
        {
          Type[] args = new Type[fn.sig.argTypes.size()];
          for (int n = 0; n < fn.sig.argTypes.size(); n++)
            args[n] = typeFromToken(fn.sig.argTypes.get(n));
          AstNode code = ParseToAst.parseStatementContainer(fn.code);
          ExecutableFunction execFn = ExecutableFunction.forCode(CodeUnitLocation.forMethod(cls.name, fn.sig.getLookupName()), 
              code, fn.sig.argNames);
          t.addMethod(fn.sig.getLookupName(), execFn, 
              typeFromToken(fn.sig.returnType), args);
        }
      }
      catch (RunException e)
      {
        // Ignore errors when setting up methods on classes
      }
      catch (ParseException e)
      {
        // Ignore errors when setting up methods on classes
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
      default: toReturn = new Type(name); break;
    }
    typeLookupCache.put(name, toReturn);
    return toReturn;
  }
  
  @Override
  public void lookupSuggestions(String val, List<String> suggestions)
  {
    suggestions.addAll(repository.getAllFunctions());
    for (VariableDescription v: repository.globalVars)
    {
      suggestions.add(v.name);
    }
  }
}
