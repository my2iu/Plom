// Some of the code completions take a slight pause to compute, and we
// don't want to have the UI feel sluggish during the pause, so I've moved
// the code completion (and future incremental compilation stuff) into 
// a separate web worker. This web worker will act as a language server
// where the UI can make requests about code completions.
//
// This design also prepares Plom for future mobile versions where the
// compilation might actually be done using native code. But the JS to 
// native code communication is asynchronous on some platforms, requiring
// a server-like design for the language server.

importScripts('plomcore/plomcore.webworker.nocache.js');

var languageServer = new org.programmingbasics.plom.core.languageserver.LanguageServerWorker();
languageServer.start();
