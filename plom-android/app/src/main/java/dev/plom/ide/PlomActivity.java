package dev.plom.ide;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewFeature;

import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlomActivity extends AppCompatActivity {

    String projectName;
    Uri projectUri;
    /** When showing a file picker activity to import an extra file into a project, we need
     * to remember the path for the file when the activity returns with the file to import. */
    String extraFileActivityPath = "";

    /** When showing the Create Document activity to save out a .zip file of the project,
     * we need to remember the name of the temporary zip file that is being copied out */
    String exportedZipFilePath;

    // Plom code to run in the virtual web server
    String plomCodeJsToRun;

    public static final String STATE_BUNDLE_KEY_PROJECTNAME = "dev.plom.projectname";
    public static final String STATE_BUNDLE_KEY_PROJECTURI = "dev.plom.projecturi";
    public static final String STATE_BUNDLE_KEY_EXTRAFILEACTIVITY_PATH = "dev.plom.extrafileactivity.path";
    public static final String STATE_BUNDLE_KEY_EXPORT_ZIPFILE_PATH = "dev.plom.exportzipfile.path";

    public static final String PLOM_MIME_TYPE = "application/x.dev.plom";

    ActivityResultLauncher<String> saveOutZipFileActivity = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/zip"),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri dest) {
                    try {
                        File f = new File(exportedZipFilePath);
                        try (FileInputStream in = new FileInputStream(f);
                             ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(dest, "w");
                             FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor())) {
                            FileManagement.copyStreams(in, fileOutputStream);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

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
            extraFileActivityPath = savedInstanceState.getString(STATE_BUNDLE_KEY_EXTRAFILEACTIVITY_PATH);
            exportedZipFilePath = savedInstanceState.getString(STATE_BUNDLE_KEY_EXPORT_ZIPFILE_PATH);
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
        WebViewAssetLoader localFileServer = new WebViewAssetLoader.Builder()
                .setDomain("androidwebview.plom.dev")
                .setHttpAllowed(true)
                .addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if ("http".equals(request.getUrl().getScheme())
                    && "webviewbridge.plom.dev".equals(request.getUrl().getHost())) {
                    if (request.getUrl().getPath().startsWith("/bridge/")) {
                        String endpoint = request.getUrl().getPath().substring("/bridge/".length());
                        Map<String, String> params = new HashMap<>();
                        for (String key : request.getUrl().getQueryParameterNames())
                            params.put(key, request.getUrl().getQueryParameter(key));
                        WebResourceResponse response = handleBridgeRequest(endpoint, params);
                        if (response != null)
                        {
                            if (response.getResponseHeaders() == null)
                                response.setResponseHeaders(new HashMap<>());
                            response.getResponseHeaders().put("Access-Control-Allow-Origin", "*");
                        }
                        return response;
                    }
                    else if (request.getUrl().getPath().startsWith("/plomweb/")) {
                        String endpoint = request.getUrl().getPath().substring("/plomweb/".length());
                        Map<String, String> params = new HashMap<>();
                        for (String key : request.getUrl().getQueryParameterNames())
                            params.put(key, request.getUrl().getQueryParameter(key));
                        return handleVirtualWebServerRequest(endpoint, params);
                    }
                }
                else if ("http".equals(request.getUrl().getScheme())
                    && "androidwebview.plom.dev".equals(request.getUrl().getHost()))
                {
                    try {
                        return localFileServer.shouldInterceptRequest(request.getUrl());
                    } catch (Throwable e) {
                        // Android's lousy local assets server can't handle "file not found"
                        // and doesn't even catch the exception properly, so we need to grab it here
                        // with a generic exception handler
                        return new WebResourceResponse("text/plain", "utf-8", 404, "Could not read file", null, new ByteArrayInputStream(new byte[0]));
                    }

                }
                return super.shouldInterceptRequest(view, request);
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            webView.setSafeBrowsingWhitelist(Arrays.asList("webviewbridge.plom.dev"), null);
        }
//        webView.loadUrl("file:///android_asset/www/androidplom.html");
        webView.loadUrl("http://androidwebview.plom.dev/www/androidplom.html");

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
        outState.putString(STATE_BUNDLE_KEY_EXTRAFILEACTIVITY_PATH, extraFileActivityPath);
        outState.putString(STATE_BUNDLE_KEY_EXPORT_ZIPFILE_PATH, exportedZipFilePath);
    }

    ActivityResultLauncher<String> importExtraFile = registerForActivityResult(
            new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    // Get the name of the file
                    String fileName = null;
                    try (Cursor c = getContentResolver().query(result, new String[] {OpenableColumns.DISPLAY_NAME},
                            null, null, null)) {
                        if (c != null && c.moveToFirst()) {
                            fileName = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                        }
                    }
                    if (fileName == null)
                        fileName = result.getLastPathSegment();
                    if (fileName == null)
                        fileName = "file";
                    // Copy in the file contents into the project
                    try (InputStream in = getContentResolver().openInputStream(result)) {
                        writeProjectFile(extraFileActivityPath, fileName, in);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    final WebView webView = (WebView) findViewById(R.id.webview);
                    webView.evaluateJavascript("window.plomUpdateExtraFileList()", null);
                }
            }
    );

    WebResourceResponse handleBridgeRequest(String endpoint, Map<String, String> params)
    {
        switch (endpoint)
        {
            case "test":
                return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("received ".getBytes(StandardCharsets.UTF_8)));
            case "listProjectFiles":
                // Only list Plom files (doesn't actually use the listProjectFiles() method)
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
                break;
            case "readProjectFile":
                try {
                    DocumentFile file = readSourceFile(params.get("name"));
                    return new WebResourceResponse("text/plain", "utf-8", getContentResolver().openInputStream(file.getUri()));
                }
                catch (IOException e)
                {
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                }
            case "listFiles":
                // List any files (not just Plom files) that are in the project
                // TODO: Pass in a list of base paths and rejection filters
                try {
                    JSONArray filesJson = new JSONArray();
                    for (String name : listProjectFiles("", Arrays.asList("^src/$")))
                        filesJson.put(name);
                    JSONObject json = new JSONObject();
                    json.put("files", filesJson);
                    return new WebResourceResponse("application/json", "utf-8", new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)));
                }
                catch (JSONException e)
                {
                    // Eat the error
                }
                break;
            case "newFileUi":
            {
                // Show a UI allowing the user to add new extra files to the project
                extraFileActivityPath = params.get("pathPrefix");
                importExtraFile.launch("*/*");

                // Instead of synchronizing with multiple threads and an external activity (which can't
                // be reliably done anyways since the current activity can die while the external
                // activity is running), we'll just send back an empty response now with an ambiguous
                // status code. We'll manually inform the JS after the content is loaded in.
                return new WebResourceResponse("application/json", "utf-8",
                        202, "Starting file chooser activity", Collections.emptyMap(),
                        new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
            }
            case "getFile":
            {
                // See if the file being requested is in the web/ files folder
                String path = params.get("path");
                DocumentFile f = getProjectDocumentFile(path);
                if (f != null && f.isFile())
                {
                    try {
                        String guessedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path));
                        return new WebResourceResponse(guessedMimeType, null, getContentResolver().openInputStream(f.getUri()));
                    }
                    catch (IOException e)
                    {
                        // Fall through
                    }
                }
                return new WebResourceResponse("text/plain", "utf-8", 404, "Could not read file", null, new ByteArrayInputStream(new byte[0]));
            }
            case "exit":
                finish();
        }
        return null;

    }

    /** When running Plom code, we take project files and create a fake web server to serve those
     * files to run in JS.
     */
    WebResourceResponse handleVirtualWebServerRequest(String endpoint, Map<String, String> params)
    {
        // See if we fit the pattern of having a separate directory for each running instance
        Matcher m = Pattern.compile("^(test[^//]*)/(.*)$").matcher(endpoint);
        if (m.matches())
        {
            // We'll ignore the serverId since we'll only run one Plom program at a time
            String serverId = m.group(1);
            String path = m.group(2);
            // Check if we're being asked for a special file
            if ("plomUi.js".equals(path))
            {
                try {
                    return new WebResourceResponse("application/javascript", null, getAssets().open("www/plom/plomUi.js"));
                }
                catch (IOException e)
                {
                    return new WebResourceResponse("text/plain", "utf-8", 404, "Could not read file", null, new ByteArrayInputStream(new byte[0]));
                }
            }
            else if ("plomdirect.js".equals(path))
            {
                try {
                    return new WebResourceResponse("application/javascript", null, getAssets().open("www/plom/plomcore/plomdirect.js"));
                }
                catch (IOException e)
                {
                    return new WebResourceResponse("text/plain", "utf-8", 404, "Could not read file", null, new ByteArrayInputStream(new byte[0]));
                }
            }
            else if ("main.plom.js".equals(path))
            {
                return new WebResourceResponse("application/javascript", null, new ByteArrayInputStream(plomCodeJsToRun.getBytes(StandardCharsets.UTF_8)));
            }
            else
            {
                // See if the file being requested is in the web/ files folder
                DocumentFile f = getProjectDocumentFile("web/" + path);
                if (f != null && f.isFile())
                {
                    try {
                        String guessedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path));
                        return new WebResourceResponse(guessedMimeType, null, getContentResolver().openInputStream(f.getUri()));
                    }
                    catch (IOException e)
                    {
                        return new WebResourceResponse("text/plain", "utf-8", 404, "Could not read file", null, new ByteArrayInputStream(new byte[0]));
                    }
                }
            }

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
                String filePath = basePath + f.getName();
                for (String rejectPattern : rejectList)
                {
                    if (filePath.matches(rejectPattern))
                    {
                        reject = true;
                        break;
                    }
                }
                if (!reject)
                    results.add(filePath);
            }
        }
    }

    public void writeProjectFile(String dir, String fileName, InputStream in) throws IOException
    {
        // Create subdirectories if needed
        DocumentFile base = getProjectDocumentFile(dir);
        DocumentFile newFile = base.createFile(PLOM_MIME_TYPE, fileName);
        if (newFile == null)
            newFile = base.findFile(fileName);
        try (OutputStream out = getContentResolver().openOutputStream(newFile.getUri())) {
            byte [] buffer = new byte[8192];
            int len = in.read(buffer);
            while (len >= 0)
            {
                out.write(buffer, 0, len);
                len = in.read(buffer);
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
            // Ignore the serverId, and save the plom code to be served later from the virtual web server
            plomCodeJsToRun = code;
        }

        @JavascriptInterface
        public void saveOutZipFile(String name, String fileBase64)
        {
            File cacheDir = getExternalCacheDir();
            File shareDir = new File(cacheDir, "share");
            shareDir.mkdir();
            try {
                File f = new File(shareDir, name);
                OutputStream out = new FileOutputStream(f, false);
                out.write(Base64.decode(fileBase64, Base64.DEFAULT));
                out.close();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            exportedZipFilePath = f.getCanonicalPath();
                            saveOutZipFileActivity.launch(f.getName());
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}