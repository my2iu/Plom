function setupPlomUi() {
	var Main = org.programmingbasics.plom.core.Main;
	var ModuleCodeRepository = org.programmingbasics.plom.core.ModuleCodeRepository;
	var PlomTextReader = org.programmingbasics.plom.core.ast.PlomTextReader;
	var CodeUnitLocation = org.programmingbasics.plom.core.interpreter.CodeUnitLocation;
	var Value = org.programmingbasics.plom.core.interpreter.Value; 
	var StandardLibrary = org.programmingbasics.plom.core.interpreter.StandardLibrary;
	var RunException = org.programmingbasics.plom.core.interpreter.RunException;
	
	function addPrimitives(interpreter, coreTypes)
	{
		// Stuff for @JS object
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "get:"),
				function(blockWait, machine) {
					var selfVal = machine.currentScope().lookupThis();
					// We just store the JavaScript pointer as the object itself
					var toReturn = selfVal.val[machine.currentScope().lookup("key").getStringValue()];
					var returnValue = Value.create(toReturn, machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object")));
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
					var returnValue = Value.create(toReturn, machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object")));
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
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var selfVal = machine.currentScope().lookupThis();
					var v = Value.create(selfVal.val[method](), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var param1 = machine.currentScope().lookup("param1").val;
					var selfVal = machine.currentScope().lookupThis();
					var v = Value.create(selfVal.val[method](param1), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:and:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var param1 = machine.currentScope().lookup("param1").val;
					var param2 = machine.currentScope().lookup("param2").val;
					var selfVal = machine.currentScope().lookupThis();
					var v = Value.create(selfVal.val[method](param1, param2), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "call:with:and:and:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var param1 = machine.currentScope().lookup("param1").val;
					var param2 = machine.currentScope().lookup("param2").val;
					var param3 = machine.currentScope().lookup("param3").val;
					var selfVal = machine.currentScope().lookupThis();
					var v = Value.create(selfVal.val[method](param1, param2, param3), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "new:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var selfVal = machine.currentScope().lookupThis();
					var v = Value.create(new selfVal.val[method](), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "new:with:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var param1 = machine.currentScope().lookup("param1").val;
					var selfVal = machine.currentScope().lookupThis();
					var v = Value.create(new selfVal.val[method](param1), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "new:with:and:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var param1 = machine.currentScope().lookup("param1").val;
					var param2 = machine.currentScope().lookup("param2").val;
					var selfVal = machine.currentScope().lookupThis();
					var v = Value.create(new selfVal.val[method](param1, param2), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
					blockWait.unblockAndReturn(v);
				});
		coreTypes.addPrimitive(CodeUnitLocation.forMethod("JS object", "new:with:and:and:"),
				function(blockWait, machine) {
					var method = machine.currentScope().lookup("method").getStringValue();
					var param1 = machine.currentScope().lookup("param1").val;
					var param2 = machine.currentScope().lookup("param2").val;
					var param3 = machine.currentScope().lookup("param3").val;
					var selfVal = machine.currentScope().lookupThis();
					var v = Value.create(new selfVal.val[method](param1, param2, param3), machine.currentScope().typeFromToken(machine.quickTypeToken("@JS object"))); 
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
	function makeStdLibRepository()
	{
		var newRepository = new ModuleCodeRepository();
		newRepository.loadBuiltInPrimitives(StandardLibrary.stdLibClasses, StandardLibrary.stdLibMethods);
		try {
			loadCodeStringIntoRepository(Main.getStdLibCodeText(), newRepository);
		}
		catch (e)
		{
			console.error(e);
		}
		newRepository.markAsImported();
		return newRepository;
	}
	function doRun(main)
	{
		main.saveCodeToRepository();
    	// Find code to run
    	var fd = main.repository.getFunctionDescription("main");
    	if (fd == null)
    	{
			main.errorLogger.accept(new RunException("No main function"));
			return;
		}
		var terp = new org.programmingbasics.plom.core.interpreter.SimpleInterpreter(fd.code);
		terp.setErrorLogger(main.errorLogger);
		try {
			terp.runNoReturn(function(scope, coreTypes) {
				StandardLibrary.createGlobals(terp, scope, coreTypes);
				scope.setParent(new org.programmingbasics.plom.core.RepositoryScope(main.repository, coreTypes));
  
				addPrimitives(terp, coreTypes);
			});
		}
		catch (err)
		{
			console.log(err);
		}
	}
	function loadCodeStringIntoRepository(code, repository)
	{
		var inStream = new PlomTextReader.StringTextReader(code);
		var lexer = new PlomTextReader.PlomTextScanner(inStream);
		repository.loadModule(lexer);
	}
	function doLoad(main)
	{
		var fileInput = document.createElement('input');
		fileInput.type = "file";
		fileInput.style.display = 'none';
		document.body.appendChild(fileInput);
		fileInput.accept = ".plom";
		fileInput.addEventListener('change', function(e) {
			e.preventDefault();
			if (fileInput.files.length != 0)
			{
				var reader = new FileReader();
				reader.onload = function(loadedEvt) {
					var readStr = reader.result;
					try {
						main.loadGlobalsView();  // Close the existing code view to force a save of the current code before it is overwritten
					    var newRepository = new ModuleCodeRepository();
					    newRepository.setChainedRepository(makeStdLibRepository());
					    loadCodeStringIntoRepository(readStr, newRepository);
						main.repository = newRepository;
						main.loadGlobalsView();
					}
					catch (err)
					{
						console.error(err);
					}
				};
				reader.readAsText(fileInput.files[0]);
			}
			document.body.removeChild(fileInput);
		});
		fileInput.click();
	}
	function doSave(main)
	{
		var saveLink = document.createElement("a");
		try
		{
			var blob = new Blob([main.getModuleAsString()], {type:"text/plain"});
			saveLink.href = URL.createObjectURL(blob);
			saveLink.download = "program.plom";
			saveLink.click();
		}
		catch (e)
		{
			console.error(e);
		}
	}
	function hookRun(main)
	{
		var runEl = document.querySelector('a.runbutton');
		runEl.addEventListener('click', function(evt) {
			evt.preventDefault();
			doRun(main);
		});
	}
	function hookLoadSave(main)
	{
		var loadEl = document.querySelector("a.loadbutton");
		loadEl.addEventListener('click', function(evt) {
			evt.preventDefault();
			doLoad(main);
		});
		var saveEl = document.querySelector("a.savebutton");
		saveEl.addEventListener('click', function(evt) {
			evt.preventDefault();
			doSave(main);
		});
	}
	function initRepository(main)
	{
	    // Load in the built-in primitives of the interpreter into the 
	    // code repository so that they can be browsed in the UI
	    main.repository = new ModuleCodeRepository();
	    main.repository.setChainedRepository(makeStdLibRepository());
	    // Create a basic main function that can be filled in
	    var sampleCode = `module .{program} {
				function .{main} @{void} {
					var .{a} : @{string}
					.{a} := .{input: {"Guess a number between 1 and 10"} }
					if { .{a} = "8" } {
						.{print: {"You guessed correctly"} }
					} else {
						.{print: {"Incorrect"} }
					}
				}	
			}`;
		try {
		    loadCodeStringIntoRepository(sampleCode, main.repository);
		}
		catch (err)
		{
			console.error(err);
		}

	}
	window.hookRun = hookRun;
	window.hookLoadSave = hookLoadSave;
	window.initRepository = initRepository;
}