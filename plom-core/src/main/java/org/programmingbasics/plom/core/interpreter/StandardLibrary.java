package org.programmingbasics.plom.core.interpreter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.programmingbasics.plom.core.ast.ParseToAst;
import org.programmingbasics.plom.core.ast.ParseToAst.ParseException;
import org.programmingbasics.plom.core.ast.StatementContainer;
import org.programmingbasics.plom.core.ast.Token;
import org.programmingbasics.plom.core.ast.TokenContainer;
import org.programmingbasics.plom.core.ast.gen.Symbol;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.html.DivElement;
import elemental.html.InputElement;

public class StandardLibrary
{
  public static class StdLibClass
  {
    public StdLibClass(String name, String parent)
    {
      this.name = name;
      this.parent = parent;
    }
    public String name;
    public String parent;
  }
  public static class StdLibMethod
  {
    public String className;
    public String methodName;
    public StatementContainer code;
    String returnType;
    public List<String> argNames; 
    public List<String> argTypes;
    public PrimitivePassthrough primitive;
    public static StdLibMethod code(String className, String methodName, String comment, StatementContainer code, PrimitivePassthrough primitive, String returnType, List<String> argNames, List<String> argTypes)
    {
      StdLibMethod toReturn = new StdLibMethod();
      toReturn.className = className;
      toReturn.methodName = methodName;
      toReturn.code = code;
      toReturn.returnType = returnType;
      toReturn.argNames = argNames;
      toReturn.argTypes = argTypes;
      toReturn.primitive = primitive;
      return toReturn;
    }
    public static StdLibMethod primitive(String className, String methodName, String comment, String returnType, List<String> argNames, List<String> argTypes, PrimitivePassthrough primitive)
    {
      return code(className, methodName, comment,
          new StatementContainer(
              new TokenContainer(
                  new Token.SimpleToken("primitive", Symbol.PrimitivePassthrough)
                  )),
          primitive,
          returnType, argNames, argTypes);
    }
  }
  
  public static List<StdLibClass> stdLibClasses = Arrays.asList(
      new StdLibClass("object", null));
  
  public static List<StdLibMethod> stdLibMethods = Arrays.asList(
      StdLibMethod.primitive("object", "to string", "Comment", "string", Collections.emptyList(), Collections.emptyList(),
          (blockWait, machine) -> {
            blockWait.unblockAndReturn(Value.createStringValue(machine.coreTypes(), machine.currentScope().lookupThis().type.name));
          }),
      StdLibMethod.primitive("number", "abs", "Comment", "number", Collections.emptyList(), Collections.emptyList(),
          (blockWait, machine) -> {
            blockWait.unblockAndReturn(Value.createNumberValue(machine.coreTypes(), Math.abs(machine.currentScope().lookupThis().getNumberValue())));
          })
      );
  
  public static void createCoreTypes(CoreTypeLibrary coreTypes)
  {
    // Create the initial core types
    coreTypes.objectType = new Type("object");
    coreTypes.booleanType = new Type("boolean", coreTypes.objectType);
    coreTypes.nullType = new Type("null", coreTypes.objectType);
    coreTypes.numberType = new Type("number", coreTypes.objectType);
    coreTypes.stringType = new Type("string", coreTypes.objectType);
    coreTypes.voidType = new Type("void");

    // Add some object methods
    coreTypes.getObjectType().addPrimitiveMethod("=:", (self, args) -> {
      return Value.createBooleanValue(coreTypes, self.val == args.get(0).val);
    }, coreTypes.getBooleanType(), coreTypes.getObjectType());
    coreTypes.getObjectType().addPrimitiveMethod("!=:", (self, args) -> {
      return Value.createBooleanValue(coreTypes,
          !self.type.lookupPrimitiveMethod("=:").call(self, args).getBooleanValue());
    }, coreTypes.getBooleanType(), coreTypes.getObjectType());
    
    // Add some number methods
    coreTypes.getNumberType().addPrimitiveMethod("floor", (self, args) -> {
      return Value.createNumberValue(coreTypes, Math.floor(self.getNumberValue()));
    }, coreTypes.getNumberType());
    coreTypes.getNumberType().addPrimitiveMethod("ceiling", (self, args) -> {
      return Value.createNumberValue(coreTypes, Math.ceil(self.getNumberValue()));
    }, coreTypes.getNumberType());
    coreTypes.getNumberType().addPrimitiveMethod("round", (self, args) -> {
      return Value.createNumberValue(coreTypes, Math.round(self.getNumberValue()));
    }, coreTypes.getNumberType());
    coreTypes.getNumberType().addPrimitiveMethod("to string", (self, args) -> {
      return Value.createStringValue(coreTypes, Double.toString(self.getNumberValue()));
    }, coreTypes.getStringType());
    coreTypes.getNumberType().addPrimitiveMethod("+:", (self, args) -> {
      if (!coreTypes.getNumberType().equals(args.get(0).type))
        throw new RunException();
      return Value.createNumberValue(coreTypes, self.getNumberValue() + args.get(0).getNumberValue());
    }, coreTypes.getNumberType(), coreTypes.getNumberType());
    coreTypes.getNumberType().addPrimitiveMethod("-:", (self, args) -> {
      if (!coreTypes.getNumberType().equals(args.get(0).type))
        throw new RunException();
      return Value.createNumberValue(coreTypes, self.getNumberValue() - args.get(0).getNumberValue());
    }, coreTypes.getNumberType(), coreTypes.getNumberType());
    coreTypes.getNumberType().addPrimitiveMethod("*:", (self, args) -> {
      if (!coreTypes.getNumberType().equals(args.get(0).type))
        throw new RunException();
      return Value.createNumberValue(coreTypes, self.getNumberValue() * args.get(0).getNumberValue());
    }, coreTypes.getNumberType(), coreTypes.getNumberType());
    coreTypes.getNumberType().addPrimitiveMethod("/:", (self, args) -> {
      if (!coreTypes.getNumberType().equals(args.get(0).type))
        throw new RunException();
      return Value.createNumberValue(coreTypes, self.getNumberValue() / args.get(0).getNumberValue());
    }, coreTypes.getNumberType(), coreTypes.getNumberType());
    coreTypes.getNumberType().addPrimitiveMethod(">:", (self, args) -> {
      if (!coreTypes.getNumberType().equals(args.get(0).type))
        throw new RunException();
      return Value.createBooleanValue(coreTypes, self.getNumberValue() > args.get(0).getNumberValue());
    }, coreTypes.getBooleanType(), coreTypes.getNumberType());
    coreTypes.getNumberType().addPrimitiveMethod(">=:", (self, args) -> {
      if (!coreTypes.getNumberType().equals(args.get(0).type))
        throw new RunException();
      return Value.createBooleanValue(coreTypes, self.getNumberValue() >= args.get(0).getNumberValue());
    }, coreTypes.getBooleanType(), coreTypes.getNumberType());
    coreTypes.getNumberType().addPrimitiveMethod("<:", (self, args) -> {
      if (!coreTypes.getNumberType().equals(args.get(0).type))
        throw new RunException();
      return Value.createBooleanValue(coreTypes, self.getNumberValue() < args.get(0).getNumberValue());
    }, coreTypes.getBooleanType(), coreTypes.getNumberType());
    coreTypes.getNumberType().addPrimitiveMethod("<=:", (self, args) -> {
      if (!coreTypes.getNumberType().equals(args.get(0).type))
        throw new RunException();
      return Value.createBooleanValue(coreTypes, self.getNumberValue() <= args.get(0).getNumberValue());
    }, coreTypes.getBooleanType(), coreTypes.getNumberType());
    coreTypes.getNumberType().addPrimitiveMethod("=:", (self, args) -> {
      if (!coreTypes.getNumberType().equals(args.get(0).type))
        return Value.createBooleanValue(coreTypes, false);
      return Value.createBooleanValue(coreTypes, self.getNumberValue() == args.get(0).getNumberValue());
    }, coreTypes.getBooleanType(), coreTypes.getObjectType());
    
    // Add some string methods
    coreTypes.getStringType().addPrimitiveMethod("to string", (self, args) -> {
      return Value.createStringValue(coreTypes, self.getStringValue());
    }, coreTypes.getStringType());
    coreTypes.getStringType().addPrimitiveMethod("substring from:to:", (self, args) -> {
      return Value.createStringValue(coreTypes, self.getStringValue().substring((int)args.get(0).getNumberValue(), (int)args.get(1).getNumberValue()));
    }, coreTypes.getStringType(), coreTypes.getNumberType(), coreTypes.getNumberType());
    coreTypes.getStringType().addPrimitiveMethod("+:", (self, args) -> {
      if (!coreTypes.getStringType().equals(args.get(0).type))
        throw new RunException();
      return Value.createStringValue(coreTypes, self.getStringValue() + args.get(0).getStringValue());
    }, coreTypes.getStringType(), coreTypes.getStringType());
    coreTypes.getStringType().addPrimitiveMethod("=:", (self, args) -> {
      if (!coreTypes.getStringType().equals(args.get(0).type))
        return Value.createBooleanValue(coreTypes, false);
      return Value.createBooleanValue(coreTypes, self.getStringValue().equals(args.get(0).getStringValue()));
    }, coreTypes.getBooleanType(), coreTypes.getObjectType());

    // Add some boolean methods
    coreTypes.getBooleanType().addPrimitiveMethod("=:", (self, args) -> {
      if (!coreTypes.getBooleanType().equals(args.get(0).type))
        return Value.createBooleanValue(coreTypes, false);
      return Value.createBooleanValue(coreTypes, self.getBooleanValue() == args.get(0).getBooleanValue());
    }, coreTypes.getBooleanType(), coreTypes.getObjectType());

    // Add some Null methods
    coreTypes.getNullType().addPrimitiveMethod("=:", (self, args) -> {
      if (!coreTypes.getNullType().equals(args.get(0).type))
        return Value.createBooleanValue(coreTypes, false);
      return Value.createBooleanValue(coreTypes, true);
    }, coreTypes.getBooleanType(), coreTypes.getObjectType());

    // Create some literals
    coreTypes.nullVal = new Value();
    coreTypes.trueVal = new Value();
    coreTypes.falseVal = new Value();
    coreTypes.nullVal.type = coreTypes.nullType;
    coreTypes.nullVal.val = null;
    coreTypes.trueVal.type = coreTypes.booleanType;
    coreTypes.trueVal.val = true;
    coreTypes.falseVal.type = coreTypes.booleanType;
    coreTypes.falseVal.val = false;
    
    // Load up code for all the objects
    try {
      for (StdLibMethod m: stdLibMethods)
      {
        Type [] argTypes = new Type[m.argTypes.size()];
        for (int n = 0; n < m.argTypes.size(); n++)
          argTypes[n] = coreTypeFromString(m.argTypes.get(n), coreTypes);
        coreTypeFromString(m.className, coreTypes).addMethod(m.methodName,
            ExecutableFunction.forCode(
                CodeUnitLocation.forMethod(m.className, m.methodName), 
                ParseToAst.parseStatementContainer(m.code),
                m.argNames), 
            coreTypeFromString(m.returnType, coreTypes),
            argTypes);
      }
    }
    catch (ParseException e)
    {
      throw new IllegalArgumentException(e);
    }
    
    // Add in the primitive passthrough methods
    for (StdLibMethod m: stdLibMethods)
    {
      if (m.primitive == null) continue;
      coreTypes.addPrimitive(CodeUnitLocation.forMethod(m.className, m.methodName), m.primitive);
    }
  }
  
  private static Type coreTypeFromString(String className, CoreTypeLibrary coreTypes)
  {
    switch (className)
    {
      case "object": return coreTypes.getObjectType(); 
      case "string": return coreTypes.getStringType();
      case "number": return coreTypes.getNumberType();
      case "boolean": return coreTypes.getBooleanType();
      case "null": return coreTypes.getNullType();
    }
    return null;
  }
  
  public static void createGlobals(SimpleInterpreter interpreter, VariableScope scope, CoreTypeLibrary coreTypes)
  {
    createCoreTypes(coreTypes);
    
    if (interpreter != null)
    {
      // Create primitive functions
      coreTypes.addPrimitive(CodeUnitLocation.forFunction("print string:"), 
          (blockWait, machine) -> {
            Document doc = Browser.getDocument();
            DivElement container = doc.createDivElement();
            container.setAttribute("style", "position: fixed; left: 50%; top: 50%; transform: translate(-50%, -50%); background-color: white; padding: 0.5em; border: 1px solid black; text-align: center;");
            container.setInnerHTML("<div></div><div><a href=\"#\">Ok</a></div>");
            container.querySelectorAll("div").item(0).setTextContent(machine.currentScope().lookup("value").getStringValue());
            container.querySelector("a").setOnclick((e) -> {
              e.preventDefault();
              doc.getBody().removeChild(container);
              blockWait.unblockAndReturn(Value.createNumberValue(coreTypes, 0));
              interpreter.continueRun();
            });
            doc.getBody().appendChild(container);
            container.querySelector("a").focus();
          });
      
      coreTypes.addPrimitive(CodeUnitLocation.forFunction("input:"), 
          (blockWait, machine) -> {
            Document doc = Browser.getDocument();
            DivElement container = doc.createDivElement();
            container.setAttribute("style", "position: fixed; left: 50%; top: 50%; transform: translate(-50%, -50%); background-color: white; padding: 0.5em; border: 1px solid black; text-align: center;");
            container.setInnerHTML("<div></div><div><form><input type=\"text\"></form></div><div><a href=\"#\">Ok</a></div>");
            container.querySelectorAll("div").item(0).setTextContent(machine.currentScope().lookup("prompt").getStringValue());
            container.querySelector("form").setOnsubmit((e) -> {
              e.preventDefault();
              doc.getBody().removeChild(container);
              blockWait.unblockAndReturn(Value.createStringValue(coreTypes, ((InputElement)container.querySelector("input")).getValue()));
              interpreter.continueRun();
            });
            container.querySelector("a").setOnclick((e) -> {
              e.preventDefault();
              doc.getBody().removeChild(container);
              blockWait.unblockAndReturn(Value.createStringValue(coreTypes, ((InputElement)container.querySelector("input")).getValue()));
              interpreter.continueRun();
            });
            doc.getBody().appendChild(container);
            container.querySelector("input").focus();
          });
    }
  }

}
