function setupPlomUi() {
	var CodeUnitLocation = org.programmingbasics.plom.core.interpreter.CodeUnitLocation;
	var Value = org.programmingbasics.plom.core.interpreter.Value; 
	
	function addPrimitives(interpreter, coreTypes)
	{
		// Stuff for @JS object
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "get:"),
				function(blockWait, machine) {
					var self = machine.currentScope().lookupThis();
					// We just store the JavaScript pointer as the object itself
					var toReturn = self.val[machine.currentScope().lookup("key").getStringValue()];
					var returnValue = Value.create(toReturn, machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object")));
					blockWait.unblockAndReturn(returnValue);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "set:to:"),
				function(blockWait, machine) {
					var index = machine.currentScope().lookup("key").getStringValue();
					var value = machine.currentScope().lookup("value").val;
					var self = machine.currentScope().lookupThis();
					self.val[index] = value;
					blockWait.unblockAndReturn(Value.createVoidValue(coreTypes));
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "at:"),
				function(blockWait, machine) {
					var self = machine.currentScope().lookupThis();
					// We just store the JavaScript pointer as the object itself
					var toReturn = self.val[machine.currentScope().lookup("index").getNumberValue()];
					var returnValue = Value.create(toReturn, machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object")));
					blockWait.unblockAndReturn(returnValue);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "at:set:"),
				function(blockWait, machine) {
					var index = machine.currentScope().lookup("index");
					var value = machine.currentScope().lookup("value").val;
					var self = machine.currentScope().lookupThis();
					self.val[index.getNumberValue()] = value;
					blockWait.unblockAndReturn(Value.createVoidValue(coreTypes));
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "as number"),
				function(blockWait, machine) {
					var self = machine.currentScope().lookupThis();
					if (self.val == null)
						blockWait.unblockAndReturn(machine.coreTypes().getNullValue());
					else
						blockWait.unblockAndReturn(Value.create(self.val, machine.coreTypes().getNumberType()));
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "as string"),
				function(blockWait, machine) {
					var self = machine.currentScope().lookupThis();
					if (self.val == null)
						blockWait.unblockAndReturn(machine.coreTypes().getNullValue());
					else
						blockWait.unblockAndReturn(Value.create(self.val, machine.coreTypes().getStringType()));
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "as boolean"),
				function(blockWait, machine) {
					var self = machine.currentScope().lookupThis();
					if (self.val == null)
						blockWait.unblockAndReturn(machine.coreTypes().getNullValue());
					else
						blockWait.unblockAndReturn(Value.create(self.val, machine.coreTypes().getBooleanType()));
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var self = machine.currentScope().lookupThis();
					var v = Value.create(self.val[method](), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var param1 = machine.currentScope().lookup("param1").val;
					var self = machine.currentScope().lookupThis();
					var v = Value.create(self.val[method](param1), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:and:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var param1 = machine.currentScope().lookup("param1").val;
					var param2 = machine.currentScope().lookup("param2").val;
					var self = machine.currentScope().lookupThis();
					var v = Value.create(self.val[method](param1, param2), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:and:and:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var param1 = machine.currentScope().lookup("param1").val;
					var param2 = machine.currentScope().lookup("param2").val;
					var param3 = machine.currentScope().lookup("param3").val;
					var self = machine.currentScope().lookupThis();
					var v = Value.create(self.val[method](param1, param2, param3), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "new:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var self = machine.currentScope().lookupThis();
					var v = Value.create(new self.val[method](), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "new:with:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var param1 = machine.currentScope().lookup("param1").val;
					var self = machine.currentScope().lookupThis();
					var v = Value.create(new self.val[method](param1), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "new:with:and:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var param1 = machine.currentScope().lookup("param1").val;
					var param2 = machine.currentScope().lookup("param2").val;
					var self = machine.currentScope().lookupThis();
					var v = Value.create(new self.val[method](param1, param2), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "new:with:and:and:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var param1 = machine.currentScope().lookup("param1").val;
					var param2 = machine.currentScope().lookup("param2").val;
					var param3 = machine.currentScope().lookup("param3").val;
					var self = machine.currentScope().lookupThis();
					var v = Value.create(new self.val[method](param1, param2, param3), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forStaticMethod("JS object", "globals"),
				function(blockWait, machine) {
					var v = Value.create(window, machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object")));
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forStaticMethod("JS object", "from number:"),
				function(blockWait, machine) {
					var val = machine.currentScope().lookup("value");
					if (val.type != machine.coreTypes().getNullType())
					{
						var v = Value.create(val.getNumberValue(), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object")));  
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
						var v = Value.create(val.getStringValue(), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object")));  
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
						var v = Value.create(val.getBooleanValue(), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object")));  
						blockWait.unblockAndReturn(v);
					}
					else
						blockWait.unblockAndReturn(val);
				});
	}
	
	function hookRun(main)
	{
		var runEl = document.querySelector('a.runbutton');
		runEl.addEventListener('click', function(evt) {
			evt.preventDefault();
			main.saveCodeToRepository();
	    	// Find code to run
	    	var fd = main.repository.getFunctionDescription("main");
	    	if (fd == null)
	    	{
				main.errorLogger.accept(new org.programmingbasics.plom.core.interpreter.RunException("No main function"));
				return;
			}
			var terp = new org.programmingbasics.plom.core.interpreter.SimpleInterpreter(fd.code);
			terp.setErrorLogger(main.errorLogger);
			try {
				terp.runNoReturn(function(scope, coreTypes) {
					org.programmingbasics.plom.core.interpreter.StandardLibrary.createGlobals(terp, scope, coreTypes);
					scope.setParent(new org.programmingbasics.plom.core.RepositoryScope(main.repository, coreTypes));
	  
					addPrimitives(terp, coreTypes);
				});
			}
			catch (err)
			{
				console.log(err);
			}
		});
	}
	window.hookRun = hookRun;
}