package dev.plom.ide;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);  // To allow my plombridge: XHR requests for sending data
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
                if ("http".equals(request.getUrl().getScheme())
                    && "webviewbridge.plom.dev".equals(request.getUrl().getHost())
                    && request.getUrl().getPath().startsWith("/bridge/"))
                {
                    String endpoint = request.getUrl().getPath().substring("/bridge/".length());
                    Map<String, String> params = new HashMap<>();
                    for (String key: request.getUrl().getQueryParameterNames())
                        params.put(key, request.getUrl().getQueryParameter(key));
                    return handleBridgeRequest(endpoint, params);
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            webView.setSafeBrowsingWhitelist(Arrays.asList("webviewbridge.plom.dev"), null);
        }
//        webView.loadUrl("file:///android_asset/www/androidplom.html");
        webView.loadUrl("file:///android_asset/www/androidplom.html");

    }

    @Override
    protected void onStop() {
        super.onStop();
        final WebView webView = (WebView) findViewById(R.id.webview);
        webView.evaluateJavascript("window.plomPrepareToUnload()", null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_BUNDLE_KEY_PROJECTURI, projectUri);
        outState.putString(STATE_BUNDLE_KEY_PROJECTNAME, projectName);
    }

    WebResourceResponse handleBridgeRequest(String endpoint, Map<String, String> params)
    {
        if ("test".equals(endpoint))
            return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("received ".getBytes(StandardCharsets.UTF_8)));
        else if ("listProjectFiles".equals(endpoint))
        {
            try {
                JSONArray filesJson = new JSONArray();
                for (String name : listSourceFiles())
                    filesJson.put(name);
                JSONObject json = new JSONObject();
                json.put("files", filesJson);
                return new WebResourceResponse("application/json", "utf-8", new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)));
            }
            catch (JSONException e)
            {
                // Eat the error
            }
        }
        else if ("readProjectFile".equals(endpoint))
        {
            DocumentFile file = readSourceFile(params.get("name"));
            try {
                return new WebResourceResponse("text/plain", "utf-8", getContentResolver().openInputStream(file.getUri()));
            }
            catch (IOException e)
            {
                return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
            }
        }
        else if ("listFiles".equals(endpoint))
        {
            // TODO: Pass in a list of base paths and rejection filters
            JSONArray filesJson = new JSONArray();
            for (String name : listProjectFiles("", Arrays.asList("^src/$")))
                filesJson.put(name);
            return new WebResourceResponse("application/json", "utf-8", new ByteArrayInputStream(filesJson.toString().getBytes(StandardCharsets.UTF_8)));
        }
        else if ("exit".equals(endpoint))
        {
            finish();
        }
        return null;

    }

    @Override
    public void onBackPressed() {
        // Intercept the back button and let the JavaScript handle it. The JS can call
        // back into Java if it wants to actually exit
        final WebView webView = (WebView) findViewById(R.id.webview);
        webView.evaluateJavascript("window.plomOnAndroidBackPressed()", null);
//        super.onBackPressed();
    }

    DocumentFile getSourceDirectory()
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
        return projectFile;
    }

    List<String> listSourceFiles()
    {
        DocumentFile projectFile = getSourceDirectory();
        List<String> toReturn = new ArrayList<>();
        for (DocumentFile file: projectFile.listFiles())
        {
            toReturn.add(file.getName());
        }
        return toReturn;
    }

    DocumentFile readSourceFile(String name)
    {
        DocumentFile projectFile = getSourceDirectory();
        DocumentFile file = projectFile.findFile(name);
        return file;
    }

    void writeSourceFile(String name, String contents)
    {
        DocumentFile projectFile = getSourceDirectory();
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

    void deleteSourceFile(String name)
    {
        DocumentFile projectFile = getSourceDirectory();
        DocumentFile file = projectFile.findFile(name);
        if (file != null)
            file.delete();
    }

    /** Gets the DocumentFile of the document/direcctory that represents a certain path
     * inside a project. Returns null if the path could not be traversed.
     */
    DocumentFile getProjectDocumentFile(String path)
    {
        DocumentFile projectFile;
        if ("file".equals(projectUri.getScheme())) {
            projectFile = DocumentFile.fromFile(new File(projectUri.getPath()));
        } else {
            projectFile = DocumentFile.fromTreeUri(this, projectUri);
        }
        String base = path;
        // Find the base from which we want to gather files
        String baseNextDir = base;
        if (base.indexOf("/") >= 0)
            baseNextDir = baseNextDir.substring(0, base.indexOf("/"));
        while (!baseNextDir.isEmpty())
        {
            projectFile = projectFile.findFile(baseNextDir);
            if (projectFile == null)
                return null;
            base = base.substring(Math.min(baseNextDir.length() + 1, base.length()));
            baseNextDir = base;
            if (base.indexOf("/") >= 0)
                baseNextDir = baseNextDir.substring(0, base.indexOf("/"));
        }
        return projectFile;
    }

    List<String> listProjectFiles(String base, List<String> rejectList)
    {
        DocumentFile projectFile = getProjectDocumentFile(base);
        // Start searching for files
        List<String> results = new ArrayList<>();
        if (!base.endsWith("/") && !base.isEmpty())
            base = base + "/";
        listProjectFiles(projectFile, base, rejectList, results);
        return results;
    }

    private void listProjectFiles(DocumentFile projectFile, String basePath, List<String> rejectList, List<String> results)
    {
        for (String rejectPattern : rejectList)
        {
            if (basePath.matches(rejectPattern))
                return;
        }
        DocumentFile[] files = projectFile.listFiles();
        for (DocumentFile f: files)
        {
            if (f.isDirectory())
            {
                listProjectFiles(f, basePath + f.getName() + "/", rejectList, results);
            }
            else if (f.isFile())
            {
                boolean reject = false;
                for (String rejectPattern : rejectList)
                {
                    if (basePath.matches(rejectPattern))
                    {
                        reject = true;
                        break;
                    }
                }
                if (!reject)
                    results.add(basePath + f.getName());
            }
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

        @JavascriptInterface
        public void deleteClass(String name) {
            deleteSourceFile("@" + name + ".plom");
        }

        @JavascriptInterface
        public void startVirtualWebServer(String serverId, String code)
        {
            // Do nothing stub for now
        }
    }
}