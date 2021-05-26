package dev.plom.ide;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class PlomActivity extends AppCompatActivity {

    String projectName;
    Uri projectUri;

    public static final String STATE_BUNDLE_KEY_PROJECTNAME = "com.wobastic.omber.projectname";
    public static final String STATE_BUNDLE_KEY_PROJECTURI = "com.wobastic.omber.projecturi";

    public static final String PLOM_MIME_TYPE = "application/x.dev.plom";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plom);

        projectName = getIntent().getStringExtra("name");
        projectUri = getIntent().getParcelableExtra("uri");
        if (savedInstanceState != null)
        {
            projectUri = savedInstanceState.getParcelable(STATE_BUNDLE_KEY_PROJECTURI);
            projectName = savedInstanceState.getString(STATE_BUNDLE_KEY_PROJECTNAME);
        }

        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
            WebView.setWebContentsDebuggingEnabled(true);
        final WebView webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);  // To allow my omberbridge: XHR requests for sending data
        webView.getSettings().setDomStorageEnabled(true);
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)
                && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY))
        {
            WebSettingsCompat.setForceDarkStrategy(webView.getSettings(), WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY);
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES)
                WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
            else
                WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
        }
        webView.addJavascriptInterface(new PlomJsBridge(), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return null;
            }
        }) ;
        webView.loadUrl("file:///android_asset/www/androidplom.html");

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_BUNDLE_KEY_PROJECTURI, projectUri);
        outState.putString(STATE_BUNDLE_KEY_PROJECTNAME, projectName);
    }

    void writeSourceFile(String name, String contents)
    {
        DocumentFile projectFile;
        if ("file".equals(projectUri.getScheme())) {
            new File(new File(projectUri.getPath()), "src").mkdirs();
            projectFile = DocumentFile.fromFile(new File(new File(projectUri.getPath()), "src"));
        } else {
            projectFile = DocumentFile.fromTreeUri(this, projectUri).createDirectory("src");
            if (projectFile == null)
                projectFile = DocumentFile.fromTreeUri(this, projectUri).findFile("src");
        }

        DocumentFile newFile = projectFile.createFile(PLOM_MIME_TYPE, name);
        if (newFile == null)
            newFile = projectFile.findFile(name);
        try (OutputStream out = getContentResolver().openOutputStream(newFile.getUri());
            Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.write(contents);
        } catch (IOException e) {
            // Ignore errors
        }
    }

    class PlomJsBridge {
        @JavascriptInterface
        public void saveModule(String contents) {
            writeSourceFile("program.plom", contents);
        }

        @JavascriptInterface
        public void saveClass(String name, String contents) {
            writeSourceFile("@" + name + ".plom", contents);
        }

    }
}