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
	// Flag that we set when we're running inside the Plom interpreter. This is useful to differentiate
	// these two situations:
	// 1. when we have reentered or were reentrant when calling from Plom to Js and back to Plom 
	//   vs.
	// 2. when we've called into Plom, set an event handler, exited, and then called into Plom again
	var isInsidePlomCode = false;
	// Pass in some external JS code for working with async iterators since GWT can't handle them natively
	Main.setAsyncIteratorToArray(async (asyncFiles) => {
		var collectedFiles = [];
		for await (const val of asyncFiles) {
			collectedFiles.push(val);
		}
		return collectedFiles;
	});

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
		var errorLogger = main.createErrorLoggerForDiv(document.querySelector('.console'));

		// Run it
		var code = main.makeEntryPointCodeToInvokeMain();
		var terp = new org.programmingbasics.plom.core.interpreter.SimpleInterpreter(code);
		terp.setErrorLogger(errorLogger);
		isInsidePlomCode = true;
		try {
			terp.runNoReturn(function(scope, coreTypes) {
				StandardLibrary.createGlobals(terp, scope, coreTypes);
				scope.setParent(new org.programmingbasics.plom.core.RepositoryScope(main.repository, coreTypes, errorLogger));
  
				loadPlomStdlibPrimitivesIntoInterpreter(terp, coreTypes, CodeUnitLocation, Value);
			});
		}
		catch (err)
		{
			console.log(err);
			errorLogger.error(err);
		}
		finally
		{
			isInsidePlomCode = false;
		}
	}
	function runPlomStandalone(main)
	{
		var errorLogger;
		if (main.debuggerEnvironmentAvailableFlag) {
			var debuggerEnv = main.createDebuggerEnvironment();
			debuggerEnv.startConnection();
			errorLogger = debuggerEnv.getErrorLogger();
		} else {
			errorLogger = main.createErrorLoggerForConsole();
		}

		// Run it
		var code = main.makeEntryPointCodeToInvokeMain();
		var terp = new org.programmingbasics.plom.core.interpreter.SimpleInterpreter(code);
		terp.setErrorLogger(errorLogger);
		isInsidePlomCode = true;
		try {
			terp.runNoReturn(function(scope, coreTypes) {
				StandardLibrary.createGlobals(terp, scope, coreTypes);
				scope.setParent(new org.programmingbasics.plom.core.RepositoryScope(main.repository, coreTypes, errorLogger));
  
				loadPlomStdlibPrimitivesIntoInterpreter(terp, coreTypes, CodeUnitLocation, Value);
			});
		}
		catch (err)
		{
			console.log(err);
			errorLogger.error(err);
		}
		finally
		{
			isInsidePlomCode = false;
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
					var iframe = webViewDiv.querySelector('iframe');
					var consoleDiv =  webViewDiv.querySelector('.runWebViewConsoleLog');
					consoleDiv.innerHTML = '';
					var debugConnection = main.makeDebuggerConnection(iframe, consoleDiv,
						() => {
							document.querySelector('.runWebView').style.display = 'none';
						});
					debugConnection.startConnection(); 
					iframe.src = 'test' + localServerId + '/index.html';
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
			const decodedPath = decodeURIComponent(evt.data.path);
			if (main.repository.hasExtraFile('web/' + decodedPath)) {
				main.repository.getExtraFilesManager().getFileContentsTransferrable('web/' + decodedPath, (contents) => {
					if (contents) {
						let mime = 'text/html';
						if (evt.data.path.endsWith('.jpg') || evt.data.path.endsWith('.jpeg'))
							mime = 'image/jpeg';
						else if (evt.data.path.endsWith('.png'))
							mime = 'image/png';
							
						localServerServiceWorker.postMessage({
							type: 'GET',
							serverContext: evt.data.serverContext,
							path: evt.data.path,
							headers: {'Content-Type':mime},
							data: contents
						}, [contents]);
					} else
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
			} else if (evt.data.path == 'plomStdlibPrimitives.js') {
				fetch('plomStdlibPrimitives.js')
					.then((response) => response.arrayBuffer())
					.then((buf) => localServerServiceWorker.postMessage({
						type: 'GET',
						serverContext: evt.data.serverContext,
						path: evt.data.path,
						headers: {'Content-Type':'application/javascript'},
						data: buf
					}, [buf]));
			} else if (evt.data.path == 'main.plom.js') {
				main.getModuleAsJsonPString(true, window.location.origin).then((str) => {
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
			main.getModuleAsJsonPString(true, window.location.origin).then((str) => {
				// Insert BOM at the beginning to label it as UTF-8 
				str = "\ufeff" + str; 
				
				return codeTransfer(localServerId, str);
			}).then(() => {
				// Show the web view and point it to the running program
				var webViewDiv = document.querySelector('.runWebView');
				webViewDiv.style.display = 'flex';
				var iframe = webViewDiv.querySelector('iframe'); 
				var consoleDiv =  webViewDiv.querySelector('.runWebViewConsoleLog');
				consoleDiv.innerHTML = '';
				var debugConnection = main.makeDebuggerConnection(iframe, consoleDiv, 
					() => {
						document.querySelector('.runWebView').style.display = 'none';
					});
				debugConnection.startConnection(); 
				iframe.src = virtualServerAddr + 'test' + localServerId + '/index.html';
			});
			
			
			
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
		if (window.showDirectoryPicker) {
			var openDirEl = document.querySelector("a.openprojectdirbutton");
			openDirEl.style.display = null;
			openDirEl.addEventListener('click', function(evt) {
				evt.preventDefault();
				if (hamburgerMenuDiv) hamburgerMenuDiv.style.display = 'none';
				window.showDirectoryPicker({mode: 'readwrite'}).then((dirHandle) => {
					var repo = makeRepositoryWithStdLib();
					main.openFromProjectDir(dirHandle, repo);
				});
			});
		}
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