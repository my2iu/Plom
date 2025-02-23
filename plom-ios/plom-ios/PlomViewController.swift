//
//  PlomViewController.swift
//  plom-ios
//

import UIKit
import MobileCoreServices
import WebKit

private let PLOM_MIME_TYPE = "application/x.dev.plom";


class PlomViewController : UIViewController, WKURLSchemeHandler {
    
    // For passing in data to the view controller of which project to show
    var projectName: String!
    var projectUrl: URL!
    var bridge : PlomJsBridge!
    
    // Plom code to run in the virtual web server
    var plomCodeJsToRun : String = ""
    
    var oldNavigationBarHidden = false
    
    @IBOutlet weak var webViewHolder: UIView!
    var webView: WKWebView!
    // For adjusting the webview when the soft keyboard is shown
    @IBOutlet weak var bottomConstraint: NSLayoutConstraint!

    let htmlPath = Bundle.main.resourcePath!.appending("/html/")
    
    override func viewDidLoad() {
        bridge = PlomJsBridge(self, url: projectUrl)
        
        // Handle shrinking the webkit viewport when the soft keyboard is shown
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillShow(_:)), name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillHide(_:)), name: UIResponder.keyboardWillHideNotification, object: nil)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        oldNavigationBarHidden = navigationController?.isNavigationBarHidden ?? false
        navigationController?.setNavigationBarHidden(true, animated: false)
        startWebView()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        // We want to code fast, so we'll disable all animations (including, notably, the keyboard animation)
        UIView.setAnimationsEnabled(false)
    }

    override func viewWillDisappear(_ animated: Bool) {
        UIView.setAnimationsEnabled(true)
        super.viewWillDisappear(animated)
        webView.evaluateJavaScript("plomPrepareToUnload()", completionHandler: nil)
        navigationController?.setNavigationBarHidden(oldNavigationBarHidden, animated: false)
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        webView = nil
    }
    @objc
    func keyboardWillShow(_ notification: NSNotification) {
        var keyboardFrame = (notification.userInfo![UIResponder.keyboardFrameEndUserInfoKey] as! NSValue).cgRectValue
        keyboardFrame = self.view.convert(keyboardFrame, from: self.view.window)
        self.bottomConstraint.constant = keyboardFrame.size.height
        self.view.layoutIfNeeded()
    }
    
    @objc
    func keyboardWillHide(_ notification: NSNotification) {
        self.bottomConstraint.constant = 0
        self.view.layoutIfNeeded()
    }
    
    func callWebViewFunction(_ name: String, json: Any, completionHandler: ((Any?, Error?) -> Void)? = nil) {
        let runCallback = String(format: "(%@)(%@[0])", name, self.jsonEncode([json]))
        webView.evaluateJavaScript(runCallback, completionHandler: completionHandler)
    }
 
    func jsonEncode(_ obj:Any) -> String {
        do {
            let data = try JSONSerialization.data(withJSONObject: obj, options: .init())
            return String(data: data, encoding: .utf8)!
        } catch {
            return "";
        }
    }
    
    func startWebView() {
        let contentController = WKUserContentController.init()
        let config = WKWebViewConfiguration()
        config.userContentController = contentController
        config.setURLSchemeHandler(self, forURLScheme: "plombridge")
        config.setURLSchemeHandler(self, forURLScheme: "plomrun")
        #if DEBUG
        config.preferences.setValue(true, forKey: "developerExtrasEnabled")
        #endif
        
        let wv = WKWebView(frame: webViewHolder.bounds, configuration:config)
        webView = wv
        webView.scrollView.bounces = false
        webView.allowsBackForwardNavigationGestures = false
        webView.contentMode = .scaleToFill
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight,.flexibleTopMargin, .flexibleLeftMargin, .flexibleRightMargin, .flexibleBottomMargin]
        #if DEBUG
        if #available(iOS 16.4, *) {
            webView.isInspectable = true
        }
        #endif
        webViewHolder.addSubview(webView)
        
        self.webView.load(URLRequest(url: URL(string: "plombridge://app/iosplom.html")!))
    }
    
    func extensionToMime(_ ending:String) -> String {
        var mime = "text/plain"
        switch(ending) {
        case "html":
            mime = "text/html"
        case "png":
            mime = "image/png"
        case "gif":
            mime = "image/gif"
        case "jpeg":
            mime = "image/jpeg"
        case "css":
            mime = "text/css"
        case "js":
            mime = "application/x-javascript"
        case "svg":
            mime = "image/svg+xml"
        case "json":
            mime = "application/json"
        case "wasm":
            mime = "application/wasm"
        default:
            mime = "application/octet-stream"
        }
        return mime
    }
    
    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        let url = urlSchemeTask.request.url!
        if url.scheme == "plomrun" && url.host == "virtualserver" {
            do {
                if url.path.hasPrefix("/plomweb/") {
                    let urlPath = String(url.path.suffix(from: "/plomweb/".endIndex))
                    // See if we fit the pattern of having a separate directory for each running instance
                    let pattern = try! NSRegularExpression(pattern: "^(test[^/]*)/(.*)$")
                    if let m = pattern.firstMatch(in:urlPath,range:NSRange(location: 0, length: urlPath.utf16.count)) {
                        // We'll ignore the serverId since we'll only run one Plom program at a time
                        let serverId = urlPath[Range(m.range(at: 1), in: urlPath)!]
                        let path = urlPath[Range(m.range(at: 2), in: urlPath)!]
                        // Check if we're being asked for a special file
                        switch (path) {
                        case "plomUi.js":
                            try serveAppFileForUrlScheme(file: htmlPath.appending("plom/plomUi.js"), urlScheme: urlSchemeTask)
                        case "plomStdlibPrimitives.js":
                            try serveAppFileForUrlScheme(file: htmlPath.appending("plom/plomStdlibPrimitives.js"), urlScheme: urlSchemeTask)
                        case "plomdirect.js":
                            try serveAppFileForUrlScheme(file: htmlPath.appending("plom/plomcore/plomdirect.js"), urlScheme: urlSchemeTask)
                        case "main.plom.js":
                            let data = plomCodeJsToRun.data(using: .utf8)!
                            urlSchemeTask.didReceive(URLResponse(url: urlSchemeTask.request.url!, mimeType: "application/javascript", expectedContentLength: data.count, textEncodingName: "UTF-8"))
                            urlSchemeTask.didReceive(data)
                            urlSchemeTask.didFinish()
                        default:
                            // See if the file being requested is in the web/ files folder
//                            let extraWebFilePath = projectUrl.appendingPathComponent("web/" + path)
                            if let data = bridge.readProjectFile("web/" + path) {
                                let mime = extensionToMime(urlSchemeTask.request.url!.pathExtension)
                                urlSchemeTask.didReceive(URLResponse(url: urlSchemeTask.request.url!, mimeType: mime, expectedContentLength: data.count, textEncodingName: "UTF-8"))
                                urlSchemeTask.didReceive(data)
                                urlSchemeTask.didFinish()
                            } else {
                                urlSchemeTask.didFailWithError(NSError(domain: NSCocoaErrorDomain, code: NSFileReadUnknownError, userInfo: nil))
                            }
                        }

                    }

                }
            } catch {
                urlSchemeTask.didFailWithError(NSError(domain: NSCocoaErrorDomain, code: NSFileReadUnknownError, userInfo: nil))
            }
        } else if url.scheme == "plombridge" && url.host == "app" {
            do {
                if (url.path.hasPrefix("/bridge/")) {
                    // Special handling of /bridge/ paths
                    let urlComponents = URLComponents(url:url, resolvingAgainstBaseURL: false)
                    var queryParams : [String:String] = [:]
                    if let items = urlComponents?.queryItems {
                        for item in items {
                            queryParams[item.name] = item.value
                        }
                    }
                    let urlPath = String(url.path.suffix(from: "/bridge/".endIndex))
                    let response = try bridge.callPostHandler(urlPath: urlPath, data: urlSchemeTask.request.httpBody, params:queryParams)
                    var data: Data
                    if let text = response.text {
                        data = text.data(using: .utf8)!
                    } else {
                        data = response.data!
                    }
                    urlSchemeTask.didReceive(URLResponse(url: urlSchemeTask.request.url!, mimeType: response.mime, expectedContentLength: data.count, textEncodingName: "UTF-8"))
                    urlSchemeTask.didReceive(data)
                    urlSchemeTask.didFinish()
                    return;
                }
                try serveAppFileForUrlScheme(file: htmlPath.appending(url.path), urlScheme: urlSchemeTask)
            } catch {
                print ("File could not be opened " + url.path)
                urlSchemeTask.didFailWithError(NSError(domain: NSCocoaErrorDomain, code: NSFileReadUnknownError, userInfo: nil))
            }
        } else {
            urlSchemeTask.didFailWithError(NSError(domain: NSCocoaErrorDomain, code: NSFileReadUnknownError, userInfo: nil))
        }
    }
    
    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {
        
    }
    
    // Takes one of the app's files and serve it in response to a url scheme
    func serveAppFileForUrlScheme(file path: String, urlScheme urlSchemeTask: WKURLSchemeTask) throws {
        let mime = extensionToMime(urlSchemeTask.request.url!.pathExtension)
        let data = try Data(contentsOf: NSURL.fileURL(withPath: path), options: .init())
        urlSchemeTask.didReceive(URLResponse(url: urlSchemeTask.request.url!, mimeType: mime, expectedContentLength: data.count, textEncodingName: "UTF-8"))
        urlSchemeTask.didReceive(data)
        urlSchemeTask.didFinish()
    }
    
    // Called from the scene delegate when going into the background so that the current project can be saved
    func prepareForBackground() {
        // This may be redundant because I also hook viewDidDisappear, but I'll also do a save here just in case viewDidDisappear isn't called or we aren't given enough time there to save
        webView.evaluateJavaScript("plomPrepareToUnload()", completionHandler: nil)
    }

}

struct PlomPostResponse {
    init(mime: String, data: Data) {
        self.mime = mime
        self.data = data
    }
    init(mime: String, string: String) {
        self.mime = mime
        self.text = string
    }
    var mime: String
    var data: Data?
    var text: String?
}

enum BridgeError : Error {
    case badArguments
}

// Code that interfaces with the Plom JS code
class PlomJsBridge {
    weak var view: PlomViewController?
    var projectUrl: URL
    
    class PickerDelegate : NSObject, UIDocumentPickerDelegate {
        weak var bridge: PlomJsBridge?
        var extraFilePath = ""
        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            var url = urls[0]
            do {
                try FileManager.default.copyItem(at: url, to: bridge!.projectUrl.appendingPathComponent(extraFilePath).appendingPathComponent(url.lastPathComponent))
                bridge?.view?.webView.evaluateJavaScript("plomUpdateExtraFileList()", completionHandler: nil)
            } catch {
                // Ignore errors
            }
        }
    }
    var pickerDelegate = PickerDelegate()

    
    init(_ view: PlomViewController, url: URL) {
        self.view = view
        self.projectUrl = url
        self.pickerDelegate.bridge = self
    }
    
    func doProjectFileOperation<T>(_ op: () throws -> T, badReturn:T) -> T {
        do {
            _ = projectUrl.startAccessingSecurityScopedResource()
            
            defer { projectUrl.stopAccessingSecurityScopedResource() }
            
            return try op()
        } catch {
            return badReturn
        }
    }
    
    func readProjectSrcFile(_ name: String) -> Data? {
        return readProjectFile("src/" + name)
    }
    
    func listProjectSrcFiles() -> [String] {
        doProjectFileOperation( {() -> [String] in
            let srcDir = projectUrl.appendingPathComponent("src")
            
            let urlList = try FileManager.default.contentsOfDirectory(at: srcDir, includingPropertiesForKeys: nil, options: [.skipsHiddenFiles])
            
            var toReturn: [String] = []
            for url in urlList {
                toReturn.append(url.lastPathComponent)
            }
            
            return toReturn

        }, badReturn: []);
    }
    
    func readProjectFile(_ name: String) -> Data? {
        doProjectFileOperation( {() -> Data? in
            let srcDir = projectUrl.appendingPathComponent(name)
            
            return try Data(contentsOf: srcDir)
        }, badReturn: nil);
    }
    
    func writeProjectFile(_ name: String, data: Data) {
        doProjectFileOperation( {() -> Void in
            let path = projectUrl.appendingPathComponent(name)
            try FileManager.default.createDirectory(at: path.deletingLastPathComponent(), withIntermediateDirectories: true, attributes: nil)
            try data.write(to: path, options: .atomic)
        }, badReturn: nil);
    }
    
    func listProjectFiles(base: String, rejectList: [String]) -> [String] {
        doProjectFileOperation( {() -> [String] in
            // Start searching for files
            var results : [String] = []
            var fixedBase = base
            if !fixedBase.hasSuffix("/") && !fixedBase.isEmpty {
                fixedBase += "/"
            }
            try PlomJsBridge.listProjectFiles(projectUrl: projectUrl, basePath: fixedBase, rejectList: rejectList, results: &results)
            return results;
        }, badReturn: []);
    }
    
    private static func listProjectFiles(projectUrl: URL, basePath: String, rejectList: [String], results: inout [String]) throws {
        for rejectPattern in rejectList {
            if basePath.range(of: rejectPattern, options: .regularExpression) != nil {
                return
            }
        }
        let dir = projectUrl.appendingPathComponent(basePath)
        let files = try FileManager.default.contentsOfDirectory(at: dir, includingPropertiesForKeys: [.isDirectoryKey], options: [.skipsHiddenFiles])
        
        for f in files {
            if f.hasDirectoryPath {
                try listProjectFiles(projectUrl: projectUrl, basePath: basePath + f.lastPathComponent + "/", rejectList: rejectList, results: &results)
            } else {
                var reject = false
                let filePath = basePath + f.lastPathComponent
                for rejectPattern in rejectList {
                    if filePath.range(of: rejectPattern, options: .regularExpression) != nil {
                        reject = true
                        break
                    }
                }
                if !reject {
                    results.append(filePath)
                }
            }
        }
    }

    func writeStringToSrcDir(fileName: String, contents: String)
    {
        doProjectFileOperation( {() -> Void in
            let srcDir = projectUrl.appendingPathComponent("src")
            
            try FileManager.default.createDirectory(at: srcDir, withIntermediateDirectories: true, attributes: nil)
            
            // Write file with the class contents
            let classFile = srcDir.appendingPathComponent(fileName)
            try contents.write(to: classFile, atomically: true, encoding: .utf8)

        }, badReturn: nil);
    }
    
    func saveModule(contents: String) {
        writeStringToSrcDir(fileName:"program.plom", contents: contents)
    }
    
    func saveClass(name: String, contents: String) {
        writeStringToSrcDir(fileName: "@" + name + ".plom", contents: contents)
    }
    
    func deleteClass(name:String) {
        doProjectFileOperation( {() -> Void in
            let srcDir = projectUrl.appendingPathComponent("src").appendingPathComponent("@" + name + ".plom")
            
            try FileManager.default.removeItem(at: srcDir)
        }, badReturn: nil);
    }
    
    func callPostHandler(urlPath: String, data: Data?, params: [String:String]) throws -> PlomPostResponse {
        switch(urlPath) {
        case "preparedToUnload":
            if let wv = view?.webView {
                wv.removeFromSuperview()
            }
            return PlomPostResponse(mime: "text/plain", string: "")

        case "test":
            let received = String(data: data!, encoding: .utf8)
            return PlomPostResponse(mime: "text/plain", string: received!.appending(" received"))
        
        case "saveModule":
            saveModule(contents: String(data: data!, encoding: .utf8)!)
            return PlomPostResponse(mime: "text/plain", string: "")

        case "saveClass":
            let name = params["name"]
            saveClass(name:name!, contents: String(data: data!, encoding: .utf8)!)
            return PlomPostResponse(mime: "text/plain", string: "")

        case "deleteClass":
            let name = params["name"]
            deleteClass(name:name!)
            return PlomPostResponse(mime: "text/plain", string: "")

        case "listProjectFiles":
            let jsonEncoder = JSONEncoder()
            let toReturn: ProjectFilesList = ProjectFilesList(files: listProjectSrcFiles())
            let toReturnJson = try jsonEncoder.encode(toReturn)
            return PlomPostResponse(mime: "application/json", data: toReturnJson)
            
        case "readProjectFile":
            let name = params["name"]!
            return PlomPostResponse(mime: PLOM_MIME_TYPE, data: readProjectSrcFile(name) ?? Data(capacity: 0))

        case "listFiles":
            let jsonEncoder = JSONEncoder()
            let toReturn: ProjectFilesList = ProjectFilesList(files: listProjectFiles(base: "", rejectList: ["^src/$"]))
            let toReturnJson = try jsonEncoder.encode(toReturn)
            return PlomPostResponse(mime: "application/json", data: toReturnJson)
            
        case "newFileUi":
            // Show a UI allowing the user to add new extra files to the project
            let documentPicker =
                UIDocumentPickerViewController(documentTypes: [kUTTypeContent as String], in: .import)

            pickerDelegate.extraFilePath = params["pathPrefix"] ?? ""
            documentPicker.delegate = pickerDelegate
            documentPicker.modalPresentationStyle = .popover
            documentPicker.popoverPresentationController?.sourceView = view?.webView
            view?.present(documentPicker, animated: true, completion: nil)
            
            // Instead of synchronizing with multiple threads and an external activity (which can't
            // be reliably done anyways since the current activity can die while the external
            // activity is running), we'll just send back an empty response now with an ambiguous
            // status code. We'll manually inform the JS after the content is loaded in.
            return PlomPostResponse(mime:"application/json", string: "")
                    //202, "Starting file chooser activity", Collections.emptyMap(),
                    //new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));

        case "getFile":
            if let path = params["path"], let data = readProjectFile(path) {
                let mime = view!.extensionToMime(URL(fileURLWithPath: path).pathExtension)
                return PlomPostResponse(mime: mime, data: data)
            } else {
                throw BridgeError.badArguments
            }

        case "startVirtualWebServer":
            // Ignore the serverId, and save the plom code to be served later from the virtual web server
            let serverId = params["serverId"]
            view?.plomCodeJsToRun = String(data: data!, encoding: .utf8)!
            return PlomPostResponse(mime: "text/plain", string: "done")
            
        case "saveOutFile":
            let name = params["name"] ?? "file"
            var files : [URL] = []
            let tempDir = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
            do {
                guard let passedData = data else { throw BridgeError.badArguments }
                try FileManager.default.createDirectory(at: tempDir, withIntermediateDirectories: false, attributes: nil)
                try passedData.write(to: tempDir.appendingPathComponent(name))
                files.append(tempDir.appendingPathComponent(name))
            } catch {
                throw BridgeError.badArguments
            }
            let activity = UIActivityViewController(activityItems: files, applicationActivities: nil)
            // The popup is from a random location on the screen, so we'll just have it drop down
            // from the top
            if let popoverPresentationController = activity.popoverPresentationController {
                popoverPresentationController.sourceView = self.view!.view
                popoverPresentationController.sourceRect = CGRect(x: self.view!.view.bounds.midX, y: 0, width: 0, height: 0)
            }
            self.view!.present(activity, animated: true)
            return PlomPostResponse(mime: "text/plain", string: "done")

        case "writeFile":
            if let name = params["name"], let passedData = data {
                writeProjectFile(name, data: passedData)
            }
            return PlomPostResponse(mime: "text/plain", string: "done")

        case "exit":
            view?.navigationController?.popViewController(animated: true)
            return PlomPostResponse(mime: "text/plain", string: "")

        default:
            throw BridgeError.badArguments
        }
    }
}

struct ProjectFilesList : Codable {
    var files: [String]
}
