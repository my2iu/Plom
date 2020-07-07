package org.programmingbasics.plom.core.interpreter;

import java.util.List;

import org.programmingbasics.plom.core.interpreter.MachineContext.PrimitiveBlockingFunctionReturn;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.html.DivElement;
import elemental.html.InputElement;

public class StandardLibrary
{
  public static void createCoreTypes(CoreTypeLibrary coreTypes)
  {
    // Create the initial core types
    coreTypes.objectType = new Type("object");
    coreTypes.booleanType = new Type("boolean");
    coreTypes.nullType = new Type("null");
    coreTypes.numberType = new Type("number");
    coreTypes.stringType = new Type("string");
    coreTypes.voidType = new Type("void");
    
    // Add some object methods
    coreTypes.getObjectType().addPrimitiveMethod("to string", (self, args) -> {
      return Value.createStringValue(coreTypes, self.type.name);
    }, coreTypes.getStringType());
    
    // Add some number methods
    coreTypes.getNumberType().addPrimitiveMethod("abs", (self, args) -> {
      return Value.createNumberValue(coreTypes, Math.abs(self.getNumberValue()));
    }, coreTypes.getNumberType());
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
  }
  
  public static void createGlobals(SimpleInterpreter interpreter, VariableScope scope, CoreTypeLibrary coreTypes)
  {
    createCoreTypes(coreTypes);
    
    // Create the other global variables and stuff
    Value printFun = new Value();
    if (interpreter != null)
    {
      printFun.type = Type.makePrimitiveBlockingFunctionType(coreTypes.getNumberType(), coreTypes.getStringType());
      printFun.val = new PrimitiveFunction.PrimitiveBlockingFunction() {
        @Override
        public void call(PrimitiveBlockingFunctionReturn blockWait, List<Value> args)
        {
          Document doc = Browser.getDocument();
          DivElement container = doc.createDivElement();
          container.setAttribute("style", "position: fixed; left: 50%; top: 50%; transform: translate(-50%, -50%); background-color: white; padding: 0.5em; border: 1px solid black; text-align: center;");
          container.setInnerHTML("<div></div><div><a href=\"#\">Ok</a></div>");
          container.querySelectorAll("div").item(0).setTextContent(args.get(0).getStringValue());
          container.querySelector("a").setOnclick((e) -> {
            e.preventDefault();
            doc.getBody().removeChild(container);
            blockWait.unblockAndReturn(Value.createNumberValue(coreTypes, 0));
            interpreter.continueRun();
          });
          doc.getBody().appendChild(container);
          container.querySelector("a").focus();
        }
      };
    }
    scope.addVariable("print:", printFun.type, printFun);
    
    Value inputFun = new Value();
    if (interpreter != null)
    {
      inputFun.type = Type.makePrimitiveBlockingFunctionType(coreTypes.getStringType(), coreTypes.getStringType());
      inputFun.val = new PrimitiveFunction.PrimitiveBlockingFunction() {
        @Override
        public void call(PrimitiveBlockingFunctionReturn blockWait, List<Value> args)
        {
          Document doc = Browser.getDocument();
          DivElement container = doc.createDivElement();
          container.setAttribute("style", "position: fixed; left: 50%; top: 50%; transform: translate(-50%, -50%); background-color: white; padding: 0.5em; border: 1px solid black; text-align: center;");
          container.setInnerHTML("<div></div><div><form><input type=\"text\"></form></div><div><a href=\"#\">Ok</a></div>");
          container.querySelectorAll("div").item(0).setTextContent(args.get(0).getStringValue());
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
        }
      };
    }
    scope.addVariable("input:", inputFun.type, inputFun);
  }

}
