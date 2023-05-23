/** JavaScript that's common to the Android and iOS app versions of Plom */

function setupPlomMain(virtualServerAddr, runHtmlCodeTransferHandler, saveOutBlobHandler)
{
	setupPlomUi();
	var main = new org.programmingbasics.plom.core.Main();
	//initRepository(main);
	//main.go();
	hookRun(main);
	hookWebRunWithBridge(main, virtualServerAddr, runHtmlCodeTransferHandler);
	// hookLoadSave(main);
    hookExportZip(main, 'plom/', saveOutBlobHandler);
	main.hookSubject();
	window.addEventListener('resize', function() {
		main.updateAfterResize();
	});

	return main;
}

function createPlomRepositoryWithProjectCode(main, bridgeUrl)
{
	var repo = makeRepositoryWithStdLib();
	repo.setExtraFilesManager(new org.programmingbasics.plom.core.ExtraFilesManagerBridge(bridgeUrl));
    fetch(bridgeUrl + 'listProjectFiles')
        .then(response => response.json())
        .then((json) =>
            Promise.all(json.files.map(filename =>
                fetch(bridgeUrl + 'readProjectFile?name=' + encodeURIComponent(filename))
                    .then((contents) => contents.text())
                    .then((code) => {
            if (filename == 'program.plom')
                loadCodeStringIntoRepository(code, repo);
            else
                loadClassCodeStringIntoRepository(code, repo);
            }))))
        .then((done) => {
            if (repo.isNoStdLibFlag)
                repo.setChainedRepository(null);
            main.repository = repo;

			main.repository.refreshExtraFiles(() => {
            	//main.loadFunctionCodeView("main");
            	main.loadGlobalsView();
        	});
        });
}