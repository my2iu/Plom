function setupPlomUi() {
	const Main = org.programmingbasics.plom.core.Main;
	const ModuleCodeRepository = org.programmingbasics.plom.core.ModuleCodeRepository;
	const ExtraFilesManagerWebInMemory = org.programmingbasics.plom.core.ExtraFilesManagerWebInMemory;
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
						// Simply return the function as a JS object, which can be invoked as a normal function using apply()
						blockWait.unblockAndReturn(Value.create(selfVal.val, machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"))));
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
						JsObjectType = machine.currentScope().typeFromUnboundTypeFromScope(UnboundType.forClassLookupName("JS object"));
						const jsProxy = function() {
							var plomArgVals = [];
							for (var n = 0; n < arguments.length; n++)
								plomArgVals.push(Value.create(arguments[n], JsObjectType));
							var returnVal = SimpleInterpreter.callPlomLambdaFromJs(machine, val.val, plomArgVals);
							if (returnVal == null || returnVal.isNull()) 
								return null;
							else 
								return returnVal.val; 
						};
						jsProxy[toPlom] = val;
						val[toJS] = jsProxy;
						var v = Value.create(jsProxy, JsObjectType);
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
	function runPlomStandalone(main)
	{
		var errorLogger = (err) => {console.log(err);} // Ignore the error
		//var errorLogger = main.createErrorLoggerForConsole(document.querySelector('.console'));
		//main.saveCodeToRepository();
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
		return repository.loadModule(lexer);
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
	function doLoadWeb(main)
	{
		Main.jsShowFileChooser('.plom', true, function(name, readStr) {
			try {
			    var newRepository = makeRepositoryWithStdLib(main);
			    newRepository.setExtraFilesManager(new ExtraFilesManagerWebInMemory());
			    var extraFilesPromise = loadCodeStringIntoRepository(readStr, newRepository);
				main.repository = newRepository;
			    extraFilesPromise.then(() => {
			    	newRepository.refreshExtraFiles(() => {
						main.closeCodePanelWithoutSavingIfOpen();  // Usually code panel will save over just loaded code when you switch view
						main.loadGlobalsView();
			    	});
			    });
			}
			catch (err)
			{
				console.error(err);
			}
		});
	}
	function doSaveWeb(main)
	{
		var saveLink = document.createElement("a");
		try {
			main.getModuleWithFilesAsString().then((str) => {
			//main.getModuleAsJsonPString().then((str) => { str = "\ufeff" + str; // Insert BOM at the beginning to label it as UTF-8
				var blob = new Blob([str], {type:"text/plain"});
				saveLink.href = URL.createObjectURL(blob);
				saveLink.download = "program.plom";
				saveLink.click();
			});
//			var blob = new Blob([main.getModuleAsString()], {type:"text/plain"});
//			saveLink.href = URL.createObjectURL(blob);
//			saveLink.download = "program.plom";
//			saveLink.click();
		}
		catch (e)
		{
			console.error(e);
		}
	}

	var hamburgerMenuDiv;   // saves a reference to the div showing the popup menu of hamburger options
	function hookRun(main)
	{
		var runEl = document.querySelector('a.runbutton');
		runEl.addEventListener('click', function(evt) {
			evt.preventDefault();
			if (hamburgerMenuDiv) hamburgerMenuDiv.style.display = 'none';
			doRun(main);
		});
	}
	function hookWebRun(main)
	{
		// Run things as a web page in a mini-sandbox
		var localServerServiceWorker;  // Stores a reference to the active service worker after it has started
		var localServerId;
		var runHtmlEl = document.querySelector('a.runhtmlbutton');
		runHtmlEl.addEventListener('click', function(evt) {
			evt.preventDefault();
			if (hamburgerMenuDiv) hamburgerMenuDiv.style.display = 'none';
			// Register a service worker that can serve data as if
			// it were a web page sent from a server (even though it's
			// actually all done locally)
			if (!('serviceWorker' in navigator)) {
				console.error('Service workers not supported');
				return;
			}
			navigator.serviceWorker.register('localServerServiceWorker.js')
			.then((registration) => {
				// Wait until the service worker becomes active
				navigator.serviceWorker.ready.then((registration) => {
					localServerServiceWorker = registration.active;
					// Generate a random id for the url that we'll serve data from
					localServerId = Math.floor(Math.random() * 1000000000).toString();
					// Show the web view and point it to the running program
					var webViewDiv = document.querySelector('.runWebView');
					webViewDiv.style.display = 'flex';
					webViewDiv.querySelector('iframe').src = 'test' + localServerId + '/index.html';
				});
			})
			.catch((e) => {
    			console.error(e);
			});
		});
		
		var serviceWorkerBroadcastChannel = new BroadcastChannel('Plom virtual web server service worker request resource');
		serviceWorkerBroadcastChannel.addEventListener('message', (evt) => {
			// Service worker (i.e. virtual web server) has relayed a 
			// request for a resource to us. See if we can fulfill that
			// request.
			//  
			// Expected message format: {
			//   type:'GET',
			//   serverContext: 'identifying string for server',
			//   path: 'index.html' 
			// }
			if (evt.data.type != 'GET') return;
			if (!localServerServiceWorker) return;
			if (event.data.serverContext != localServerId) return;
			// Check if the file exists in the code repository
			if (main.repository.hasExtraFile('web/' + evt.data.path)) {
				main.repository.getExtraFilesManager().getFileContentsTransferrable('web/' + evt.data.path, (contents) => {
					if (contents)
						localServerServiceWorker.postMessage({
							type: 'GET',
							serverContext: evt.data.serverContext,
							path: evt.data.path,
							headers: {'Content-Type':'text/html'},
							data: contents
						}, [contents]);
					else
						localServerServiceWorker.postMessage({
							type: 'GET',
							serverContext: evt.data.serverContext,
							path: evt.data.path,
							data: null
						});
				});
			} else if (evt.data.path == 'index.html') {
				fetch('plomweb.html')
					.then((response) => response.arrayBuffer())
					.then((buf) => localServerServiceWorker.postMessage({
						type: 'GET',
						serverContext: evt.data.serverContext,
						path: evt.data.path,
						headers: {'Content-Type':'text/html'},
						data: buf
					}, [buf]));
//				var dataToSend = new TextEncoder().encode('hi world');
//				localServerServiceWorker.postMessage({
//					type: 'GET',
//					serverContext: evt.data.serverContext,
//					path: evt.data.path,
//					headers: {'Content-Type':'text/html'},
//					data: dataToSend.buffer
//				}, [dataToSend.buffer]);
			} else if (evt.data.path == 'plomdirect.js') {
				var plomdirectLoc;
				if (!!document.querySelector('iframe#plomcore')) {
					plomdirectLoc = document.querySelector('iframe#plomcore').contentDocument.querySelector('script').src;
				} else {
					plomdirectLoc = 'plomdirect.js';
				}
				fetch(plomdirectLoc)
					.then((response) => response.arrayBuffer())
					.then((buf) => localServerServiceWorker.postMessage({
						type: 'GET',
						serverContext: evt.data.serverContext,
						path: evt.data.path,
						headers: {'Content-Type':'application/javascript'},
						data: buf
					}, [buf]));
			} else if (evt.data.path == 'plomUi.js') {
				fetch('plomUi.js')
					.then((response) => response.arrayBuffer())
					.then((buf) => localServerServiceWorker.postMessage({
						type: 'GET',
						serverContext: evt.data.serverContext,
						path: evt.data.path,
						headers: {'Content-Type':'application/javascript'},
						data: buf
					}, [buf]));
			} else if (evt.data.path == 'main.plom.js') {
				main.getModuleAsJsonPString().then((str) => {
					// Insert BOM at the beginning to label it as UTF-8 
					str = "\ufeff" + str; 
					localServerServiceWorker.postMessage({
							type: 'GET',
							serverContext: evt.data.serverContext,
							path: evt.data.path,
							headers: {'Content-Type':'application/javascript'},
							data: str
						})
					});
					
			} else {
				// Could not find any data to send, so send back null
				// (it will be treated as a 404 by the service worker)
				localServerServiceWorker.postMessage({
					type: 'GET',
					serverContext: evt.data.serverContext,
					path: evt.data.path,
					data: null
				});
			}
		});

		// Button to show the running code output (just shows the web page)
		var showHtmlEl = document.querySelector('a.showhtmlbutton');
		showHtmlEl.addEventListener('click', function(evt) {
			evt.preventDefault();
			if (hamburgerMenuDiv) hamburgerMenuDiv.style.display = 'none';
			var webViewDiv = document.querySelector('.runWebView');
			webViewDiv.style.display = 'flex';
		});
		
		// Close button on the web view showing the output of the running code
		document.querySelector('.runWebView .runWebViewMenuBar a').addEventListener('click', (evt) => {
			evt.preventDefault();
			document.querySelector('.runWebView').style.display = 'none';
		});
		
	}
	function hookWebRunWithBridge(main, virtualServerAddr, codeTransfer)
	{
		// Run things as a web page in a mini-sandbox
		var runHtmlEl = document.querySelector('a.runhtmlbutton');
		runHtmlEl.addEventListener('click', function(evt) {
			evt.preventDefault();
			if (hamburgerMenuDiv) hamburgerMenuDiv.style.display = 'none';

			// Generate a random id for the url that we'll serve data from
			localServerId = Math.floor(Math.random() * 1000000000).toString();
			
			// Group all the Plom code together into a big blob that can
			// be sent over to the web view (this could be done in native
			// code since the native code has access to all the code files,
			// but we'll do it here in JavaScript for convenience for now,
			// though this may be slow on Android with the need to push all
			// the code from JS to native)
			main.getModuleAsJsonPString().then((str) => {
				// Insert BOM at the beginning to label it as UTF-8 
				str = "\ufeff" + str; 
				
				return codeTransfer(localServerId, str);
			}).then(() => {
				// Show the web view and point it to the running program
				var webViewDiv = document.querySelector('.runWebView');
				webViewDiv.style.display = 'flex';
				webViewDiv.querySelector('iframe').src = virtualServerAddr + 'test' + localServerId + '/index.html';
			});
			
			
			
			// Register a service worker that can serve data as if
			// it were a web page sent from a server (even though it's
			// actually all done locally)
			/*
			if (!('serviceWorker' in navigator)) {
				console.error('Service workers not supported');
				return;
			}
			navigator.serviceWorker.register('localServerServiceWorker.js')
			.then((registration) => {
				// Wait until the service worker becomes active
				navigator.serviceWorker.ready.then((registration) => {
					localServerServiceWorker = registration.active;
					// Generate a random id for the url that we'll serve data from
					localServerId = Math.floor(Math.random() * 1000000000).toString();
					// Show the web view and point it to the running program
					var webViewDiv = document.querySelector('.runWebView');
					webViewDiv.style.display = 'flex';
					webViewDiv.querySelector('iframe').src = 'test' + localServerId + '/index.html';
				});
			})
			.catch((e) => {
    			console.error(e);
			});
		});
		
		var serviceWorkerBroadcastChannel = new BroadcastChannel('Plom virtual web server service worker request resource');
		serviceWorkerBroadcastChannel.addEventListener('message', (evt) => {
			// Service worker (i.e. virtual web server) has relayed a 
			// request for a resource to us. See if we can fulfill that
			// request.
			//  
			// Expected message format: {
			//   type:'GET',
			//   serverContext: 'identifying string for server',
			//   path: 'index.html' 
			// }
			if (evt.data.type != 'GET') return;
			if (!localServerServiceWorker) return;
			if (event.data.serverContext != localServerId) return;
			// Check if the file exists in the code repository
			if (main.repository.hasExtraFile('web/' + evt.data.path)) {
				main.repository.getExtraFilesManager().getFileContentsTransferrable('web/' + evt.data.path, (contents) => {
					if (contents)
						localServerServiceWorker.postMessage({
							type: 'GET',
							serverContext: evt.data.serverContext,
							path: evt.data.path,
							headers: {'Content-Type':'text/html'},
							data: contents
						}, [contents]);
					else
						localServerServiceWorker.postMessage({
							type: 'GET',
							serverContext: evt.data.serverContext,
							path: evt.data.path,
							data: null
						});
				});
			} else if (evt.data.path == 'index.html') {
				fetch('plomweb.html')
					.then((response) => response.arrayBuffer())
					.then((buf) => localServerServiceWorker.postMessage({
						type: 'GET',
						serverContext: evt.data.serverContext,
						path: evt.data.path,
						headers: {'Content-Type':'text/html'},
						data: buf
					}, [buf]));
//				var dataToSend = new TextEncoder().encode('hi world');
//				localServerServiceWorker.postMessage({
//					type: 'GET',
//					serverContext: evt.data.serverContext,
//					path: evt.data.path,
//					headers: {'Content-Type':'text/html'},
//					data: dataToSend.buffer
//				}, [dataToSend.buffer]);
			} else if (evt.data.path == 'plomdirect.js') {
				var plomdirectLoc;
				if (!!document.querySelector('iframe#plomcore')) {
					plomdirectLoc = document.querySelector('iframe#plomcore').contentDocument.querySelector('script').src;
				} else {
					plomdirectLoc = 'plomdirect.js';
				}
				fetch(plomdirectLoc)
					.then((response) => response.arrayBuffer())
					.then((buf) => localServerServiceWorker.postMessage({
						type: 'GET',
						serverContext: evt.data.serverContext,
						path: evt.data.path,
						headers: {'Content-Type':'application/javascript'},
						data: buf
					}, [buf]));
			} else if (evt.data.path == 'plomUi.js') {
				fetch('plomUi.js')
					.then((response) => response.arrayBuffer())
					.then((buf) => localServerServiceWorker.postMessage({
						type: 'GET',
						serverContext: evt.data.serverContext,
						path: evt.data.path,
						headers: {'Content-Type':'application/javascript'},
						data: buf
					}, [buf]));
			} else if (evt.data.path == 'main.plom.js') {
				main.getModuleAsJsonPString().then((str) => {
					// Insert BOM at the beginning to label it as UTF-8 
					str = "\ufeff" + str; 
					localServerServiceWorker.postMessage({
							type: 'GET',
							serverContext: evt.data.serverContext,
							path: evt.data.path,
							headers: {'Content-Type':'application/javascript'},
							data: str
						})
					});
					
			} else {
				// Could not find any data to send, so send back null
				// (it will be treated as a 404 by the service worker)
				localServerServiceWorker.postMessage({
					type: 'GET',
					serverContext: evt.data.serverContext,
					path: evt.data.path,
					data: null
				});
			}*/
		});

		// Button to show the running code output (just shows the web page)
		var showHtmlEl = document.querySelector('a.showhtmlbutton');
		showHtmlEl.addEventListener('click', function(evt) {
			evt.preventDefault();
			if (hamburgerMenuDiv) hamburgerMenuDiv.style.display = 'none';
			var webViewDiv = document.querySelector('.runWebView');
			webViewDiv.style.display = 'flex';
		});
		
		// Close button on the web view showing the output of the running code
		document.querySelector('.runWebView .runWebViewMenuBar a').addEventListener('click', (evt) => {
			evt.preventDefault();
			document.querySelector('.runWebView').style.display = 'none';
		});
		
	}
	
	function hookLoadSave(main)
	{
		var loadEl = document.querySelector("a.loadbutton");
		loadEl.addEventListener('click', function(evt) {
			evt.preventDefault();
			if (hamburgerMenuDiv) hamburgerMenuDiv.style.display = 'none';
			doLoadWeb(main);
		});
		var saveEl = document.querySelector("a.savebutton");
		saveEl.addEventListener('click', function(evt) {
			evt.preventDefault();
			if (hamburgerMenuDiv) hamburgerMenuDiv.style.display = 'none';
			doSaveWeb(main);
		});
	}
	function hookExportZip(main, plomSystemFilePrefix, exportZipAsBase64, saveOutBlobHandler)
	{
		var exportEl = document.querySelector("a.exportzipbutton");
		exportEl.addEventListener('click', function(evt) {
			evt.preventDefault();
			if (hamburgerMenuDiv) hamburgerMenuDiv.style.display = 'none';
			main.exportAsZip(new JSZip(), plomSystemFilePrefix, exportZipAsBase64).then((data) => {
				saveOutBlobHandler('plom.zip', data);
			});
		});
	}
	function initRepository(main)
	{
	    // Load in the built-in primitives of the interpreter into the 
	    // code repository so that they can be browsed in the UI
	    main.repository = makeRepositoryWithStdLib(main);
	    main.repository.setExtraFilesManager(new ExtraFilesManagerWebInMemory());
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
		hamburgerMenuDiv = menuDiv;
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
	window.hookWebRun = hookWebRun;
	window.hookWebRunWithBridge = hookWebRunWithBridge;
	window.hookLoadSave = hookLoadSave;
	window.hookExportZip = hookExportZip;
	window.initRepository = initRepository;
	window.makeRepositoryWithStdLib = makeRepositoryWithStdLib;
	window.loadCodeStringIntoRepository = loadCodeStringIntoRepository;
	window.loadClassCodeStringIntoRepository = loadClassCodeStringIntoRepository;
	window.hookSimpleHamburgerMenu = hookSimpleHamburgerMenu;
	window.runPlomStandalone = runPlomStandalone;
}