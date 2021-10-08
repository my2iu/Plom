package org.programmingbasics.plom.core.interpreter;

import java.util.HashMap;
import java.util.Map;

import jsinterop.annotations.JsType;

/**
 * Provides easy access to the core types needed for basic operations
 */
@JsType
public class CoreTypeLibrary
{
  public Type getObjectType() { return objectType; }
  public Type getNumberType() { return numberType; }
  public Type getStringType() { return stringType; }
  public Type getBooleanType() { return booleanType; }
  public Type getVoidType() { return voidType; }
  public Type getNullType() { return nullType; }
  public Type getObjectArrayType() { return objectArrayType; }
  protected Type objectType; 
  protected Type numberType; 
  protected Type stringType; 
  protected Type booleanType; 
  protected Type voidType; 
  protected Type nullType;
  protected Type objectArrayType;
  
  public Value getNullValue() { return nullVal; }
  public Value getTrueValue() { return trueVal; }
  public Value getFalseValue() { return falseVal; }
  protected Value nullVal;
  protected Value trueVal;
  protected Value falseVal;
  
  protected Map<CodeUnitLocation, PrimitivePassthrough> primitives = new HashMap<>();
  
  public CoreTypeLibrary addPrimitive(CodeUnitLocation codeUnit, PrimitivePassthrough func)
  {
    primitives.put(codeUnit, func);
    return this;
  }
  
  public PrimitivePassthrough lookupPrimitive(CodeUnitLocation codeUnit)
  {
    return primitives.get(codeUnit);
  }
  
  public static CoreTypeLibrary createTestLibrary()
  {
    CoreTypeLibrary coreTypes = new CoreTypeLibrary();
    StandardLibrary.createCoreTypes(coreTypes);
    return coreTypes;
  }
}
