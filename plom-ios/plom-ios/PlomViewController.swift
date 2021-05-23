//
//  PlomViewController.swift
//  plom-ios
//

import UIKit
import WebKit


class PlomViewController : UIViewController, WKURLSchemeHandler {
    
    // For passing in data to the view controller of which project to show
    var projectName: String!
    var projectUrl: URL!
    var bridge : PlomJsBridge!
    
    @IBOutlet weak var webViewHolder: UIView!
    weak var webView: WKWebView!
    
    let htmlPath = Bundle.main.resourcePath!.appending("/html/")
    
    override func viewDidLoad() {
        bridge = PlomJsBridge(self)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
//        navigationController?.isNavigationBarHidden = true
        
        startWebView()
 
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
//        navigationController?.isNavigationBarHidden = false
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
    weak var view: PlomViewController?;
    
    init(_ view: PlomViewController) {
        self.view = view
    }
    
    func callPostHandler(urlPath: String, data: Data?, params: [String:String]) throws -> PlomPostResponse {
        switch(urlPath) {
        case "test":
            let received = String(data: data!, encoding: .utf8)
            return PlomPostResponse(mime: "text/plain", string: received!.appending(" received"))
            
        case "exit":
            view?.navigationController?.popViewController(animated: true)
            return PlomPostResponse(mime: "text/plain", string: "")

        default:
            throw BridgeError.badArguments
        }
    }
}
