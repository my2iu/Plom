//
//  PlomViewController.swift
//  plom-ios
//

import UIKit
import WebKit

private let PLOM_MIME_TYPE = "application/x.dev.plom";


class PlomViewController : UIViewController, WKURLSchemeHandler {
    
    // For passing in data to the view controller of which project to show
    var projectName: String!
    var projectUrl: URL!
    var bridge : PlomJsBridge!
    
    var oldNavigationBarHidden = false
    
    @IBOutlet weak var webViewHolder: UIView!
    weak var webView: WKWebView!
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
        navigationController?.setNavigationBarHidden(oldNavigationBarHidden, animated: false)
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        webView.evaluateJavaScript("plomPrepareToUnload()", completionHandler: nil)
    }
    @objc
    func keyboardWillShow(_ notification: NSNotification) {
        var keyboardFrame = (notification.userInfo![UIResponder.keyboardFrameEndUserInfoKey] as! NSValue).cgRectValue
        keyboardFrame = self.view.convert(keyboardFrame, from: self.view.window)
        self.bottomConstraint.constant = keyboardFrame.size.height
    }
    
    @objc
    func keyboardWillHide(_ notification: NSNotification) {
        self.bottomConstraint.constant = 0
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
        #if DEBUG
        config.preferences.setValue(true, forKey: "developerExtrasEnabled")
        #endif
        
        let wv = WKWebView(frame: webViewHolder.bounds, configuration:config)
        webView = wv
        webView.scrollView.bounces = false
        webView.allowsBackForwardNavigationGestures = false
        webView.contentMode = .scaleToFill
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight,.flexibleTopMargin, .flexibleLeftMargin, .flexibleRightMargin, .flexibleBottomMargin]
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
        if (url.host == "app") {
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
                let mime = extensionToMime(url.pathExtension)
                let data = try Data(contentsOf: NSURL.fileURL(withPath: htmlPath.appending(url.path)), options: .init())
                urlSchemeTask.didReceive(URLResponse(url: urlSchemeTask.request.url!, mimeType: mime, expectedContentLength: data.count, textEncodingName: "UTF-8"))
                urlSchemeTask.didReceive(data)
                urlSchemeTask.didFinish()
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
    
    init(_ view: PlomViewController, url: URL) {
        self.view = view
        self.projectUrl = url
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
    
    func readProjectFile(_ name: String) -> Data? {
        doProjectFileOperation( {() -> Data? in
            let srcDir = projectUrl.appendingPathComponent("src").appendingPathComponent(name)
            
            return try Data(contentsOf: srcDir)
        }, badReturn: nil);
    }
    
    func listProjectFiles() -> [String] {
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
    
    func callPostHandler(urlPath: String, data: Data?, params: [String:String]) throws -> PlomPostResponse {
        switch(urlPath) {
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

        case "listProjectFiles":
            let jsonEncoder = JSONEncoder()
            let toReturn: ProjectFilesList = ProjectFilesList(files: listProjectFiles())
            let toReturnJson = try jsonEncoder.encode(toReturn)
            return PlomPostResponse(mime: "application/json", data: toReturnJson)
            
        case "readProjectFile":
            let name = params["name"]!
            return PlomPostResponse(mime: PLOM_MIME_TYPE, data: readProjectFile(name) ?? Data(capacity: 0))

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
