package org.programmingbasics.plom.core.interpreter;

/**
 * Provides easy access to the core types needed for basic operations
 */
public class CoreTypeLibrary
{
  public Type getNumberType() { return numberType; }
  public Type getStringType() { return stringType; }
  public Type getBooleanType() { return booleanType; }
  public Type getVoidType() { return voidType; }
  public Type getNullType() { return nullType; }
  protected Type numberType; 
  protected Type stringType; 
  protected Type booleanType; 
  protected Type voidType; 
  protected Type nullType;
  
  public Value getNullValue() { return nullVal; }
  public Value getTrueValue() { return trueVal; }
  public Value getFalseValue() { return falseVal; }
  protected Value nullVal;
  protected Value trueVal;
  protected Value falseVal;
  
  public static CoreTypeLibrary createTestLibrary()
  {
    CoreTypeLibrary coreTypes = new CoreTypeLibrary();
    StandardLibrary.createCoreTypes(coreTypes);
    return coreTypes;
  }
}
