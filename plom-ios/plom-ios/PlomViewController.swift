//
//  PlomViewController.swift
//  plom-ios
//

import UIKit
import WebKit


class PlomViewController : UIViewController {
    // For passing in data to the view controller of which project to show
    var projectName: String!
    var projectUrl: URL!
    
    @IBOutlet weak var webViewHolder: UIView!
    weak var webView: WKWebView!
    
    override func viewDidLoad() {
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
//        navigationController?.isNavigationBarHidden = true
        
        let wv = WKWebView(frame: webViewHolder.bounds)
        webView = wv
        webView.scrollView.bounces = false
        webView.allowsBackForwardNavigationGestures = false
        webView.contentMode = .scaleToFill
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight,.flexibleTopMargin, .flexibleLeftMargin, .flexibleRightMargin, .flexibleBottomMargin]
        webViewHolder.addSubview(webView)
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
//        navigationController?.isNavigationBarHidden = false
    }
}
