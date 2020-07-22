package org.programmingbasics.plom.core;

import java.util.List;

import org.programmingbasics.plom.core.ModuleCodeRepository.FunctionDescription;
import org.programmingbasics.plom.core.interpreter.CoreTypeLibrary;
import org.programmingbasics.plom.core.interpreter.RunException;
import org.programmingbasics.plom.core.interpreter.Type;
import org.programmingbasics.plom.core.interpreter.Value;
import org.programmingbasics.plom.core.interpreter.VariableScope;
import org.programmingbasics.plom.core.interpreter.Value.LValue;

/**
 * Maps a code repository so that it can be accessed in the scope stack
 * so that it can be accessed in the SimpleInterpreter 
 */
public class RepositoryScope extends VariableScope
{
  private ModuleCodeRepository repository;
  private CoreTypeLibrary coreTypes;
  public RepositoryScope(ModuleCodeRepository repository, CoreTypeLibrary coreTypes)
  {
    this.repository = repository;
    this.coreTypes = coreTypes;
  }
  
  @Override
  public Value lookup(String name) throws RunException
  {
    FunctionDescription func = repository.getFunctionDescription(name);
    if (func != null)
    {
      Value val = new Value();
      val.val = func;
      val.type = new Type.TypeSignature(name, coreTypes.getVoidType());
      return val;
    }
    return super.lookup(name);
  }

  @Override
  public LValue lookupLValue(String name) throws RunException
  {
    return super.lookupLValue(name);
  }

  @Override
  public void lookupSuggestions(String val, List<String> suggestions)
  {
    suggestions.addAll(repository.getAllFunctions());
  }
}
