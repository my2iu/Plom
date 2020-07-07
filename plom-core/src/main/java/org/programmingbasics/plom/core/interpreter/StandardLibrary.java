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
    coreTypes.booleanType = new Type("boolean");
    coreTypes.nullType = new Type("null");
    coreTypes.numberType = new Type("number");
    coreTypes.stringType = new Type("string");
    coreTypes.voidType = new Type("void");
    
    // Add some methods
    coreTypes.getNumberType().addPrimitiveMethod("abs", (self, args) -> {
      return Value.createNumberValue(coreTypes, Math.abs(self.getNumberValue()));
    }, coreTypes.getNumberType());
    
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
