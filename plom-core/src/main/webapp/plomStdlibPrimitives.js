function loadPlomStdlibPrimitivesIntoInterpreter(interpreter, coreTypes, CodeUnitLocation, Value, UnboundType, SimpleInterpreter, isInsidePlomCode)
{
	// Used to store whether a Plom object has a JS equivalent/proxy
	const toJS = Symbol();
	// Used to store whether a JS object has a Plom equivalent/proxy
	const toPlom = Symbol();

	// Stuff for @JS object
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "get:"),
			function(blockWait, machine) {
				var selfVal = machine.currentScope().lookupThis();
				// We just store the JavaScript pointer as the object itself
				var toReturn = selfVal.val[machine.currentScope().lookup("key").getStringValue()];
				var returnValue = Value.create(toReturn, machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object")));
				blockWait.unblockAndReturn(returnValue);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "set:to:"),
			function(blockWait, machine) {
				var index = machine.currentScope().lookup("key").getStringValue();
				var value = machine.currentScope().lookup("value").val;
				var selfVal = machine.currentScope().lookupThis();
				selfVal.val[index] = value;
				blockWait.unblockAndReturn(Value.createVoidValue(coreTypes));
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "at:"),
			function(blockWait, machine) {
				var selfVal = machine.currentScope().lookupThis();
				// We just store the JavaScript pointer as the object itself
				var toReturn = selfVal.val[machine.currentScope().lookup("index").getNumberValue()];
				var returnValue = Value.create(toReturn, machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object")));
				blockWait.unblockAndReturn(returnValue);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "at:set:"),
			function(blockWait, machine) {
				var index = machine.currentScope().lookup("index");
				var value = machine.currentScope().lookup("value").val;
				var selfVal = machine.currentScope().lookupThis();
				selfVal.val[index.getNumberValue()] = value;
				blockWait.unblockAndReturn(Value.createVoidValue(coreTypes));
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "as number"),
			function(blockWait, machine) {
				var selfVal = machine.currentScope().lookupThis();
				if (selfVal.val == null)
					blockWait.unblockAndReturn(machine.coreTypes().getNullValue());
				else
					blockWait.unblockAndReturn(Value.create(selfVal.val, machine.coreTypes().getNumberType()));
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "as string"),
			function(blockWait, machine) {
				var selfVal = machine.currentScope().lookupThis();
				if (selfVal.val == null)
					blockWait.unblockAndReturn(machine.coreTypes().getNullValue());
				else
					blockWait.unblockAndReturn(Value.create(selfVal.val, machine.coreTypes().getStringType()));
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "as boolean"),
			function(blockWait, machine) {
				var selfVal = machine.currentScope().lookupThis();
				if (selfVal.val == null)
					blockWait.unblockAndReturn(machine.coreTypes().getNullValue());
				else
					blockWait.unblockAndReturn(Value.create(selfVal.val, machine.coreTypes().getBooleanType()));
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "as function"),
			function(blockWait, machine) {
				var selfVal = machine.currentScope().lookupThis();
				if (selfVal.val == null)
					blockWait.unblockAndReturn(machine.coreTypes().getNullValue());
				else if (selfVal.val[toPlom])
					return selfVal.val[toPlom];
				else
					// Simply return the function as a JS object, which can be invoked as a normal function using apply()
					blockWait.unblockAndReturn(Value.create(selfVal.val, machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))));
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "passthrough to Plom"),
			function(blockWait, machine) {
				var selfVal = machine.currentScope().lookupThis();
				if (selfVal.val == null)
					blockWait.unblockAndReturn(machine.coreTypes().getNullValue());
				else
					blockWait.unblockAndReturn(selfVal.val);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(selfVal.val[method](), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var param1 = machine.currentScope().lookup("param1").val;
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(selfVal.val[method](param1), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:and:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var param1 = machine.currentScope().lookup("param1").val;
				var param2 = machine.currentScope().lookup("param2").val;
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(selfVal.val[method](param1, param2), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:and:and:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var param1 = machine.currentScope().lookup("param1").val;
				var param2 = machine.currentScope().lookup("param2").val;
				var param3 = machine.currentScope().lookup("param3").val;
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(selfVal.val[method](param1, param2, param3), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:and:and:and:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var param1 = machine.currentScope().lookup("param1").val;
				var param2 = machine.currentScope().lookup("param2").val;
				var param3 = machine.currentScope().lookup("param3").val;
				var param4 = machine.currentScope().lookup("param4").val;
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(selfVal.val[method](param1, param2, param3, param4), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:and:and:and:and:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var param1 = machine.currentScope().lookup("param1").val;
				var param2 = machine.currentScope().lookup("param2").val;
				var param3 = machine.currentScope().lookup("param3").val;
				var param4 = machine.currentScope().lookup("param4").val;
				var param5 = machine.currentScope().lookup("param5").val;
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(selfVal.val[method](param1, param2, param3, param4, param5), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:and:and:and:and:and:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var param1 = machine.currentScope().lookup("param1").val;
				var param2 = machine.currentScope().lookup("param2").val;
				var param3 = machine.currentScope().lookup("param3").val;
				var param4 = machine.currentScope().lookup("param4").val;
				var param5 = machine.currentScope().lookup("param5").val;
				var param6 = machine.currentScope().lookup("param6").val;
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(selfVal.val[method](param1, param2, param3, param4, param5, param6), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:and:and:and:and:and:and:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var param1 = machine.currentScope().lookup("param1").val;
				var param2 = machine.currentScope().lookup("param2").val;
				var param3 = machine.currentScope().lookup("param3").val;
				var param4 = machine.currentScope().lookup("param4").val;
				var param5 = machine.currentScope().lookup("param5").val;
				var param6 = machine.currentScope().lookup("param6").val;
				var param7 = machine.currentScope().lookup("param7").val;
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(selfVal.val[method](param1, param2, param3, param4, param5, param6, param7), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:and:and:and:and:and:and:and:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var param1 = machine.currentScope().lookup("param1").val;
				var param2 = machine.currentScope().lookup("param2").val;
				var param3 = machine.currentScope().lookup("param3").val;
				var param4 = machine.currentScope().lookup("param4").val;
				var param5 = machine.currentScope().lookup("param5").val;
				var param6 = machine.currentScope().lookup("param6").val;
				var param7 = machine.currentScope().lookup("param7").val;
				var param8 = machine.currentScope().lookup("param8").val;
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(selfVal.val[method](param1, param2, param3, param4, param5, param6, param7, param8), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:and:and:and:and:and:and:and:and:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var param1 = machine.currentScope().lookup("param1").val;
				var param2 = machine.currentScope().lookup("param2").val;
				var param3 = machine.currentScope().lookup("param3").val;
				var param4 = machine.currentScope().lookup("param4").val;
				var param5 = machine.currentScope().lookup("param5").val;
				var param6 = machine.currentScope().lookup("param6").val;
				var param7 = machine.currentScope().lookup("param7").val;
				var param8 = machine.currentScope().lookup("param8").val;
				var param9 = machine.currentScope().lookup("param9").val;
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(selfVal.val[method](param1, param2, param3, param4, param5, param6, param7, param8, param9), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "new:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(new selfVal.val[method](), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "new:with:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var param1 = machine.currentScope().lookup("param1").val;
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(new selfVal.val[method](param1), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "new:with:and:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var param1 = machine.currentScope().lookup("param1").val;
				var param2 = machine.currentScope().lookup("param2").val;
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(new selfVal.val[method](param1, param2), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "new:with:and:and:"),
			function(blockWait, machine) {
				var method = machine.currentScope().lookup("method").getStringValue();
				var param1 = machine.currentScope().lookup("param1").val;
				var param2 = machine.currentScope().lookup("param2").val;
				var param3 = machine.currentScope().lookup("param3").val;
				var selfVal = machine.currentScope().lookupThis();
				var v = Value.create(new selfVal.val[method](param1, param2, param3), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))); 
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forStaticMethod("JS object", "globals"),
			function(blockWait, machine) {
				var v = Value.create(window, machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object")));
				blockWait.unblockAndReturn(v);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forStaticMethod("JS object", "from number:"),
			function(blockWait, machine) {
				var val = machine.currentScope().lookup("value");
				if (val.type != machine.coreTypes().getNullType())
				{
					var v = Value.create(val.getNumberValue(), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object")));  
					blockWait.unblockAndReturn(v);
				}
				else
					blockWait.unblockAndReturn(val);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forStaticMethod("JS object", "from string:"),
			function(blockWait, machine) {
				var val = machine.currentScope().lookup("value");
				if (val.type != machine.coreTypes().getNullType())
				{
					var v = Value.create(val.getStringValue(), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object")));  
					blockWait.unblockAndReturn(v);
				}
				else
					blockWait.unblockAndReturn(val);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forStaticMethod("JS object", "from boolean:"),
			function(blockWait, machine) {
				var val = machine.currentScope().lookup("value");
				if (val.type != machine.coreTypes().getNullType())
				{
					var v = Value.create(val.getBooleanValue(), machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object")));  
					blockWait.unblockAndReturn(v);
				}
				else
					blockWait.unblockAndReturn(val);
			});
	coreTypes.addPrimitive(CodeUnitLocation.forStaticMethod("JS object", "from function:"),
			function(blockWait, machine) {
				var val = machine.currentScope().lookup("value");
				if (val.type == machine.coreTypes().getNullType())
				{
					blockWait.unblockAndReturn(val);
				}
				else if (val.val[toJS])
				{
					blockWait.unblockAndReturn(val.val[toJS]);
				}
				else
				{
					const JsObjectType = machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"));
					const jsProxy = function() {
						var plomArgVals = [];
						for (var n = 0; n < arguments.length; n++)
							plomArgVals.push(Value.create(arguments[n], JsObjectType));
						var isTopLevel = !isInsidePlomCode.inside;
						try {
							isInsidePlomCode.inside = true;
							var returnVal = SimpleInterpreter.callPlomLambdaFromJs(machine, val.val, plomArgVals);
							if (returnVal == null || returnVal.isNull()) 
								return null;
							else 
								return returnVal.val;
						} 
						catch (err)
						{
							// We only need to log errors if we're top-level. Otherwise the error
							// can just be propagated to the top-level
							if (isTopLevel)
							{
								var errorLogger = machine.getErrorLogger();
								errorLogger.error(err);
							}
							throw(err);
						}
						finally
						{
							isInsidePlomCode.inside = !isTopLevel;
						}
					};
					jsProxy[toPlom] = val;
					val[toJS] = jsProxy;
					var v = Value.create(jsProxy, JsObjectType);
					blockWait.unblockAndReturn(v);
				}
			});
	coreTypes.addPrimitive(CodeUnitLocation.forStaticMethod("JS object", "passthrough to JS:"),
			function(blockWait, machine) {
				const JsObjectType = machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"));
				var val = Value.createCopy(machine.currentScope().lookup("value"));
				var v = Value.create(val, JsObjectType);
				blockWait.unblockAndReturn(v);
			});
			
}
