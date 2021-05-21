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
    
    @IBOutlet weak var webViewHolder: UIView!
    weak var webView: WKWebView!
    
    let htmlPath = Bundle.main.resourcePath!.appending("/html/")
    
    override func viewDidLoad() {
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
    
    func startWebView() {
        let contentController = WKUserContentController.init()
        let config = WKWebViewConfiguration()
        config.userContentController = contentController
        config.setURLSchemeHandler(self, forURLScheme: "plombridge")
        
        let wv = WKWebView(frame: webViewHolder.bounds, configuration:config)
        webView = wv
        webView.scrollView.bounces = false
        webView.allowsBackForwardNavigationGestures = false
        webView.contentMode = .scaleToFill
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight,.flexibleTopMargin, .flexibleLeftMargin, .flexibleRightMargin, .flexibleBottomMargin]
        webViewHolder.addSubview(webView)
        
        self.webView.load(URLRequest(url: URL(string: "plombridge://app/index.html")!))
    }
    
    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        let url = urlSchemeTask.request.url!
        if (url.host == "app") {
            if (url.path.hasPrefix("/bridge")) {
                // Special handling of paths
            }
            var mime = "text/plain"
            switch(url.pathExtension) {
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
                mime = "application/octet-streamSection"
            }
            do {
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
