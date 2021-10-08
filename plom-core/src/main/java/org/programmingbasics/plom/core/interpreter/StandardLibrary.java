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
import jsinterop.annotations.JsType;

@JsType
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
    public String returnType;
    public List<String> argNames; 
    public List<String> argTypes;
    public PrimitivePassthrough primitive;
    public boolean isStatic = false;
    public boolean isConstructor = false;
    public static StdLibMethod code(String className, String methodName, PrimitivePassthrough primitive, String returnType, List<String> argNames, List<String> argTypes, StatementContainer code)
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
    public static StdLibMethod constructor(String className, String methodName, PrimitivePassthrough primitive, String returnType, List<String> argNames, List<String> argTypes, StatementContainer code)
    {
      StdLibMethod toReturn = StdLibMethod.code(className, methodName, primitive, returnType, argNames, argTypes, code);
      toReturn.isConstructor = true;
      return toReturn;
    }
    public static StdLibMethod primitive(String className, String methodName, String comment, String returnType, List<String> argNames, List<String> argTypes, PrimitivePassthrough primitive)
    {
      return code(className, methodName, primitive,
          returnType, argNames, argTypes, 
          new StatementContainer(
              new TokenContainer(
                  new Token.WideToken("// " + comment, Symbol.DUMMY_COMMENT),
                  new Token.SimpleToken("primitive", Symbol.PrimitivePassthrough)
                  ))
          );
    }
    // Some convenience methods providing easier ways to make some common primitives
    public static interface NoArgPrimitive
    {
      public void execute(MachineContext.PrimitiveBlockingFunctionReturn blockWait, MachineContext machine, Value thisVal) throws RunException;
    }
    public static StdLibMethod primitiveNoArg(String className, String methodName, String comment, String returnType, NoArgPrimitive primitive)
    {
      return primitive(className, methodName, comment, returnType, Collections.emptyList(), Collections.emptyList(),
          (blockWait, machine) -> {
            primitive.execute(blockWait, machine, machine.currentScope().lookupThis());
          });
    }
    public static interface OneArgPrimitive
    {
      public void execute(MachineContext.PrimitiveBlockingFunctionReturn blockWait, MachineContext machine, Value thisVal, Value val) throws RunException;
    }
    public static StdLibMethod primitiveOneArg(String className, String methodName, String comment, String returnType, String argType, OneArgPrimitive primitive)
    {
      return primitive(className, methodName, comment, returnType, Arrays.asList("val"), Arrays.asList(argType),
          (blockWait, machine) -> {
            primitive.execute(blockWait, machine, machine.currentScope().lookupThis(), machine.currentScope().lookup("val"));
          });
    }
    public static StdLibMethod primitiveStaticOneArg(String className, String methodName, String comment, String returnType, String argType, OneArgPrimitive primitive)
    {
      StdLibMethod m = primitive(className, methodName, comment, returnType, Arrays.asList("val"), Arrays.asList(argType),
          (blockWait, machine) -> {
            primitive.execute(blockWait, machine, null, machine.currentScope().lookup("val"));
          });
      m.isStatic = true;
      return m;
    }
  }
  
  public static List<StdLibClass> stdLibClasses = Arrays.asList(
      new StdLibClass("object", null),
      new StdLibClass("number", "object"),
      new StdLibClass("string", "object"),
      new StdLibClass("boolean", "object"),
      new StdLibClass("null", "object"),
      new StdLibClass("object array", "object")
  );
  
  public static List<StdLibMethod> stdLibMethods = Arrays.asList(
      // some object methods
      StdLibMethod.primitive("object", "to string", "Comment", "string", Collections.emptyList(), Collections.emptyList(),
          (blockWait, machine) -> {
            blockWait.unblockAndReturn(Value.createStringValue(machine.coreTypes(), machine.currentScope().lookupThis().type.name));
          }),
      StdLibMethod.primitive("object", "=:", "Comment", "boolean", Arrays.asList("val"), Arrays.asList("object"),
          (blockWait, machine) -> {
            blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), machine.currentScope().lookupThis().val == machine.currentScope().lookup("val").val));
          }),
      StdLibMethod.code("object", "!=:", null, "boolean", Arrays.asList("val"), Arrays.asList("object"),
          new StatementContainer(
              new TokenContainer(
                  new Token.SimpleToken("return", Symbol.Return),
                  new Token.SimpleToken("(", Symbol.OpenParenthesis),
                  new Token.SimpleToken("this", Symbol.This),
                  new Token.SimpleToken("=", Symbol.Eq),
                  Token.ParameterToken.fromContents(".val", Symbol.DotVariable),
                  new Token.SimpleToken(")", Symbol.ClosedParenthesis),
                  Token.ParameterToken.fromContents(".not", Symbol.DotVariable)
                  ))),
      StdLibMethod.constructor("object", "new", null, "void", Collections.emptyList(), Collections.emptyList(),
          new StatementContainer()),
      
      // some number methods
      StdLibMethod.primitiveNoArg("number", "abs", "Comment", "number", 
          (blockWait, machine, thisVal) -> {
            blockWait.unblockAndReturn(Value.createNumberValue(machine.coreTypes(), Math.abs(thisVal.getNumberValue())));
          }),
      StdLibMethod.primitiveNoArg("number", "floor", "Comment", "number", 
          (blockWait, machine, thisVal) -> {
            blockWait.unblockAndReturn(Value.createNumberValue(machine.coreTypes(), Math.floor(thisVal.getNumberValue())));
          }),
      StdLibMethod.primitiveNoArg("number", "ceiling", "Comment", "number", 
          (blockWait, machine, thisVal) -> {
            blockWait.unblockAndReturn(Value.createNumberValue(machine.coreTypes(), Math.ceil(thisVal.getNumberValue())));
          }),
      StdLibMethod.primitiveNoArg("number", "round", "Comment", "number", 
          (blockWait, machine, thisVal) -> {
            blockWait.unblockAndReturn(Value.createNumberValue(machine.coreTypes(), Math.round(thisVal.getNumberValue())));
          }),
      StdLibMethod.primitiveNoArg("number", "to string", "Comment", "string", 
          (blockWait, machine, self) -> {
            blockWait.unblockAndReturn(Value.createStringValue(machine.coreTypes(), Double.toString(self.getNumberValue())));
          }),
      StdLibMethod.primitiveOneArg("number", "+:", "Comment", "number", "number", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getNumberType().equals(val.type))
              throw new RunException();
            blockWait.unblockAndReturn(Value.createNumberValue(machine.coreTypes(), self.getNumberValue() + val.getNumberValue()));
          }),
      StdLibMethod.primitiveOneArg("number", "-:", "Comment", "number", "number", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getNumberType().equals(val.type))
              throw new RunException();
            blockWait.unblockAndReturn(Value.createNumberValue(machine.coreTypes(), self.getNumberValue() - val.getNumberValue()));
          }),
      StdLibMethod.primitiveOneArg("number", "*:", "Comment", "number", "number", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getNumberType().equals(val.type))
              throw new RunException();
            blockWait.unblockAndReturn(Value.createNumberValue(machine.coreTypes(), self.getNumberValue() * val.getNumberValue()));
          }),
      StdLibMethod.primitiveOneArg("number", "/:", "Comment", "number", "number", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getNumberType().equals(val.type))
              throw new RunException();
            blockWait.unblockAndReturn(Value.createNumberValue(machine.coreTypes(), self.getNumberValue() / val.getNumberValue()));
          }),
      StdLibMethod.primitiveOneArg("number", ">:", "Comment", "boolean", "number", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getNumberType().equals(val.type))
              throw new RunException();
            blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), self.getNumberValue() > val.getNumberValue()));
          }),
      StdLibMethod.primitiveOneArg("number", ">=:", "Comment", "boolean", "number", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getNumberType().equals(val.type))
              throw new RunException();
            blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), self.getNumberValue() >= val.getNumberValue()));
          }),
      StdLibMethod.primitiveOneArg("number", "<:", "Comment", "boolean", "number", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getNumberType().equals(val.type))
              throw new RunException();
            blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), self.getNumberValue() < val.getNumberValue()));
          }),
      StdLibMethod.primitiveOneArg("number", "<=:", "Comment", "boolean", "number", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getNumberType().equals(val.type))
              throw new RunException();
            blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), self.getNumberValue() <= val.getNumberValue()));
          }),
      StdLibMethod.primitiveOneArg("number", "=:", "Comment", "boolean", "object", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getNumberType().equals(val.type))
              blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), false));
            else
              blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), self.getNumberValue() == val.getNumberValue()));
          }),
      StdLibMethod.primitiveStaticOneArg("number", "parse US number:", "Comment", "number", "string", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getStringType().equals(val.type))
              throw new RunException();
            Value toReturn;
            try {
              toReturn = Value.createNumberValue(machine.coreTypes(), Double.parseDouble(val.getStringValue()));
            } 
            catch (NumberFormatException e)
            {
              toReturn = machine.coreTypes().getNullValue();
            }
            blockWait.unblockAndReturn(toReturn);
          }),

      // some string methods
      StdLibMethod.primitiveNoArg("string", "to string", "Comment", "string", 
          (blockWait, machine, self) -> {
            blockWait.unblockAndReturn(Value.createStringValue(machine.coreTypes(), self.getStringValue()));
          }),
      StdLibMethod.primitiveOneArg("string", "+:", "Comment", "boolean", "string", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getStringType().equals(val.type))
              throw new RunException();
            blockWait.unblockAndReturn(Value.createStringValue(machine.coreTypes(), self.getStringValue() + val.getStringValue()));
          }),
      StdLibMethod.primitiveOneArg("string", "=:", "Comment", "boolean", "object", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getStringType().equals(val.type))
              blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), false));
            else
              blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), self.getStringValue().equals(val.getStringValue())));
          }),
      StdLibMethod.primitive("string", "substring from:to:", "Comment", "string", Arrays.asList("from", "to"), Arrays.asList("number", "number"), 
          (blockWait, machine) -> {
            blockWait.unblockAndReturn(Value.createStringValue(machine.coreTypes(), machine.currentScope().lookupThis().getStringValue().substring((int)machine.currentScope().lookup("from").getNumberValue(), (int)machine.currentScope().lookup("to").getNumberValue())));
          }),
      
      // some boolean methods
      StdLibMethod.primitiveOneArg("boolean", "=:", "Comment", "boolean", "object", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getBooleanType().equals(val.type))
              blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), false));
            else
              blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), self.getBooleanValue() == val.getBooleanValue()));
          }),
      StdLibMethod.primitiveNoArg("boolean", "not", "Comment", "boolean", 
          (blockWait, machine, self) -> {
            blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), !self.getBooleanValue()));
          }),
      
      // some Null methods
      StdLibMethod.primitiveOneArg("null", "=:", "Comment", "boolean", "object", 
          (blockWait, machine, self, val) -> {
            if (!machine.coreTypes().getNullType().equals(val.type))
              blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), false));
            else
              blockWait.unblockAndReturn(Value.createBooleanValue(machine.coreTypes(), true));
          }),

      // some object array methods
      StdLibMethod.constructor("object array", "new", 
          (blockWait, machine) -> {
            Value array = Value.createEmptyArray(machine.coreTypes(), machine.coreTypes().getObjectArrayType());
            machine.currentScope().overwriteThis(array);
            blockWait.unblockAndReturn(array);
          },
          "void", Collections.emptyList(), Collections.emptyList(), 
          new StatementContainer(
              new TokenContainer(
                  new Token.SimpleToken("primitive", Symbol.PrimitivePassthrough))))
      );
  
  public static void createCoreTypes(CoreTypeLibrary coreTypes)
  {
    // Create the initial core types
    coreTypes.objectType = new Type("object");
    coreTypes.booleanType = new Type("boolean", coreTypes.objectType);
    coreTypes.nullType = new Type("null", coreTypes.objectType);
    coreTypes.numberType = new Type("number", coreTypes.objectType);
    coreTypes.stringType = new Type("string", coreTypes.objectType);
    coreTypes.objectArrayType = new Type("object array", coreTypes.objectType);
    coreTypes.voidType = new Type("void");

    // Create some literals
    coreTypes.nullVal = Value.create(null, coreTypes.nullType);
    coreTypes.trueVal = Value.create(true, coreTypes.booleanType);
    coreTypes.falseVal = Value.create(false, coreTypes.booleanType);
    
    // Load up code for all the primitive objects
    try {
      for (StdLibMethod m: stdLibMethods)
      {
        Type [] argTypes = new Type[m.argTypes.size()];
        for (int n = 0; n < m.argTypes.size(); n++)
          argTypes[n] = coreTypeFromString(m.argTypes.get(n), coreTypes);
 
        if (m.isStatic)
        {
          ExecutableFunction execFn = ExecutableFunction.forCode(
              CodeUnitLocation.forStaticMethod(m.className, m.methodName), 
              ParseToAst.parseStatementContainer(m.code),
              m.argNames); 
          coreTypeFromString(m.className, coreTypes).addStaticMethod(m.methodName,
              execFn, coreTypeFromString(m.returnType, coreTypes), argTypes);
        }
        else if (m.isConstructor)
        {
          ExecutableFunction execFn = ExecutableFunction.forCode(
              CodeUnitLocation.forConstructorMethod(m.className, m.methodName), 
              ParseToAst.parseStatementContainer(m.code),
              m.argNames); 
          coreTypeFromString(m.className, coreTypes).addStaticMethod(m.methodName,
              execFn, coreTypeFromString(m.returnType, coreTypes), argTypes);
        }
        else
        {
          ExecutableFunction execFn = ExecutableFunction.forCode(
              CodeUnitLocation.forMethod(m.className, m.methodName), 
              ParseToAst.parseStatementContainer(m.code),
              m.argNames); 
          coreTypeFromString(m.className, coreTypes).addMethod(m.methodName,
              execFn, coreTypeFromString(m.returnType, coreTypes), argTypes);
        }
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
      if (m.isStatic)
        coreTypes.addPrimitive(CodeUnitLocation.forStaticMethod(m.className, m.methodName), m.primitive);
      else if (m.isConstructor)
        coreTypes.addPrimitive(CodeUnitLocation.forConstructorMethod(m.className, m.methodName), m.primitive);
      else
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
      case "object array": return coreTypes.getObjectArrayType();
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
