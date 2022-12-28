function setupPlomUi() {
	const Main = org.programmingbasics.plom.core.Main;
	const ModuleCodeRepository = org.programmingbasics.plom.core.ModuleCodeRepository;
	const PlomTextReader = org.programmingbasics.plom.core.ast.PlomTextReader;
	const CodeUnitLocation = org.programmingbasics.plom.core.interpreter.CodeUnitLocation;
	const Value = org.programmingbasics.plom.core.interpreter.Value; 
	const StandardLibrary = org.programmingbasics.plom.core.interpreter.StandardLibrary;
	const SimpleInterpreter = org.programmingbasics.plom.core.interpreter.SimpleInterpreter;
	const RunException = org.programmingbasics.plom.core.interpreter.RunException;
	const UnboundType = org.programmingbasics.plom.core.interpreter.UnboundType;
	// Used to store whether a Plom object has a JS equivalent/proxy
	const toJS = Symbol();
	// Used to store whether a JS object has a Plom equivalent/proxy
	const toPlom = Symbol();

	// Code for the custom auto-resizing DOM input element	
	class AutoResizingInputElement extends HTMLElement {
		constructor() {
			super();
			var shadowRoot = this.attachShadow({mode: 'open'});
			shadowRoot.innerHTML = org.programmingbasics.plom.core.Main.getAutoResizingInputHtmlText();
			this.shadowRoot.querySelector('input').addEventListener('change', function(evt) {
				if (!evt.composed) 
				{
					// Redispatch the event if it won't bubble out of the shadow dom
					shadowRoot.dispatchEvent(new CustomEvent('change', {
						  bubbles: true,
						  composed: true
						}));
				}
			});
		}
		get value() {
			return this.shadowRoot.querySelector('input').value;
		}
		set value(val) {
			this.shadowRoot.querySelector('input').value = val;
			this.shadowRoot.querySelector('div').textContent = val;
		}
		focus() {
			this.shadowRoot.querySelector('input').focus();
		}
		select() {
			this.shadowRoot.querySelector('input').select();
		}
	}
	customElements.define('plom-autoresizing-input', AutoResizingInputElement);
	
	function addPrimitives(interpreter, coreTypes)
	{
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
						blockWait.unblockAndReturn(Value.create(selfVal.val, machine.coreTypes().getBooleanType()));
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
						const jsProxy = function() {
							SimpleInterpreter.callPlomLambdaFromJs(machine, val.val);
						};
						jsProxy[toPlom] = val;
						val[toJS] = jsProxy;
						var v = Value.create(jsProxy, machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object")));
						blockWait.unblockAndReturn(v);
					}
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
		var errorLogger = main.createErrorLoggerForConsole(document.querySelector('.console'));
		main.saveCodeToRepository();
    	// Find code to run
    	var fd = main.repository.getFunctionDescription("main");
    	if (fd == null)
    	{
			errorLogger(new RunException("No main function"));
			return;
		}
		var terp = new org.programmingbasics.plom.core.interpreter.SimpleInterpreter(fd.code);
		terp.setErrorLogger(errorLogger);
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
			errorLogger(err);
		}
	}
	function loadCodeStringIntoRepository(code, repository)
	{
		var inStream = new PlomTextReader.StringTextReader(code);
		var lexer = new PlomTextReader.PlomTextScanner(inStream);
		repository.loadModule(lexer);
	}
	function loadClassCodeStringIntoRepository(code, repository)
	{
		var inStream = new PlomTextReader.StringTextReader(code);
		var lexer = new PlomTextReader.PlomTextScanner(inStream);
		repository.loadClassIntoModule(lexer);
	}
	function makeRepositoryWithStdLib(main)
    {
        var repo = new ModuleCodeRepository();
        repo.setChainedRepository(makeStdLibRepository());
        return repo;
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
					    var newRepository = makeRepositoryWithStdLib(main);
					    loadCodeStringIntoRepository(readStr, newRepository);
						main.repository = newRepository;
						main.closeCodePanelWithoutSavingIfOpen();  // Usually code panel will save over just loaded code when you switch view
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
	    main.repository = makeRepositoryWithStdLib(main);
	    // Create a basic main function that can be filled in
	    var sampleCode = `module .{program} {
				function .{main} {@{void}} {} {
					var .{a} @{string}
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
	function hookSimpleHamburgerMenu(menuButtonAnchor, menuDiv)
		{
			menuButtonAnchor.onclick = function(evt) {
				evt.preventDefault();
				if (menuDiv.style.display == 'none') {
					menuDiv.style.display = 'flex';
					// Position the menu under the button
					var bounds = menuButtonAnchor.getBoundingClientRect();
					menuDiv.style.top = bounds.bottom + 'px';
				}
				else
					menuDiv.style.display = 'none';
			};
		}
	window.hookRun = hookRun;
	window.hookLoadSave = hookLoadSave;
	window.initRepository = initRepository;
	window.makeRepositoryWithStdLib = makeRepositoryWithStdLib;
	window.loadCodeStringIntoRepository = loadCodeStringIntoRepository;
	window.loadClassCodeStringIntoRepository = loadClassCodeStringIntoRepository;
	window.hookSimpleHamburgerMenu = hookSimpleHamburgerMenu;
}