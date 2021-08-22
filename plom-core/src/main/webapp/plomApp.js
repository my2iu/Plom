/** JavaScript that's common to the Android and iOS app versions of Plom */

function setupPlomMain()
{
	setupPlomUi();
	var main = new org.programmingbasics.plom.core.Main();
	//initRepository(main);
	//main.go();
	hookRun(main);
	// hookLoadSave(main);
	main.hookSubject();
	window.addEventListener('resize', function() {
		main.updateAfterResize();
	});

	return main;
}

function createPlomRepositoryWithProjectCode(main, bridgeUrl)
{
	var repo = makeRepositoryWithStdLib();
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

            //main.loadFunctionCodeView("main");
            main.loadGlobalsView();
        });
}