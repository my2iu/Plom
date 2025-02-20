/** JavaScript that's common to the Android and iOS app versions of Plom */

function setupPlomMain(virtualServerAddr, runHtmlCodeTransferHandler, exportZipAsBase64, saveOutBlobHandler)
{
	setupPlomUi();
	setOverrideLanguageServerWorkerUrl('plom/languageServerWorker.js');
	var main = new org.programmingbasics.plom.core.Main();
	//initRepository(main);
	//main.go();
	hookRun(main);
	hookWebRunWithBridge(main, virtualServerAddr, runHtmlCodeTransferHandler);
	// hookLoadSave(main);
    hookExportZip(main, 'plom/', exportZipAsBase64, saveOutBlobHandler);
	main.hookSubject();
	window.addEventListener('resize', function() {
		main.updateAfterResize();
	});

	return main;
}

function createPlomRepositoryWithProjectCode(main, bridgeUrl, writeFileHandler)
{
	var repo = makeRepositoryWithStdLib();
	repo.setExtraFilesManager(new org.programmingbasics.plom.core.ExtraFilesManagerBridge(bridgeUrl, writeFileHandler));
    fetch(bridgeUrl + 'listProjectFiles')
        .then(response => response.json())
        .then((json) =>
            Promise.all(json.files.map(filename =>
                fetch(bridgeUrl + 'readProjectFile?name=' + encodeURIComponent(filename))
                    .then((contents) => contents.text())
                    .then((code) => {
            if (filename == 'program.plom')
                return loadCodeStringIntoRepository(code, repo);
            else
                return loadClassCodeStringIntoRepository(code, repo);
            }))))
        .then((done) => {
//            if (repo.isNoStdLibFlag)
//                repo.setChainedRepository(null);
            main.setRepository(repo);

			main.getRepository().refreshExtraFiles(() => {
            	//main.loadFunctionCodeView("main");
            	main.loadGlobalsView();
        	});
        });
}