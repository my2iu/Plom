package dev.plom.ide;

import static dev.plom.ide.PlomActivity.PLOM_MIME_TYPE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ProjectsActivity extends AppCompatActivity {

    // For activity results
//    static final int ACTIVITY_RESULT_NEW_PROJECT = 1000;

    RecyclerView listWidget;

    List<ProjectDescription> projects = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);

        loadProjectList();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Plom projects");

        listWidget = findViewById(R.id.rvProjectList);
        listWidget.setLayoutManager(new LinearLayoutManager(this));
        listWidget.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.activity_projects_row, parent, false);
                    return new RecyclerView.ViewHolder(v) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                TextView textView = holder.itemView.findViewById(R.id.project_row_item_text);
                textView.setText(projects.get(position).name);
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        int position = holder.getAbsoluteAdapterPosition();
                        ProjectDescription proj = projects.remove(position);
                        projects.add(0, proj);
                        listWidget.getAdapter().notifyDataSetChanged();
                        saveProjectList();
                        // Start an activity for the project
                        Intent intent = new Intent(ProjectsActivity.this, PlomActivity.class);
                        intent.putExtra("name", proj.name);
                        intent.putExtra("uri", proj.uri);
                        startActivity(intent);
                    }
                });
                final ImageButton moreButton = holder.itemView.findViewById(R.id.project_row_item_more);
                moreButton.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        int position = holder.getAbsoluteAdapterPosition();
                        ProjectDescription proj = projects.get(position);
                        PopupMenu popup = new PopupMenu(ProjectsActivity.this, moreButton);
                        popup.inflate(R.menu.projects_row_context_menu);
                        if (proj.isManagedByPlom)
                            popup.getMenu().findItem(R.id.menuDelete).setVisible(false);
                        else
                            popup.getMenu().findItem(R.id.menuRemove).setVisible(false);
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                int position = holder.getAbsoluteAdapterPosition();
                                ProjectDescription proj = projects.get(position);
                                switch(item.getItemId())
                                {
                                    case R.id.menuDelete:
                                        if ("file".equals(proj.uri.getScheme()))
                                            deleteFileRecursively(new File(proj.uri.getPath()));
                                        projects.remove(position);
                                        saveProjectList();
                                        listWidget.getAdapter().notifyDataSetChanged();
                                        break;
                                    case R.id.menuRemove:
                                        projects.remove(position);
                                        saveProjectList();
                                        listWidget.getAdapter().notifyDataSetChanged();
                                        break;
                                }
                                return false;
                            }
                        });
                        popup.show();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return projects.size();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.floatingActionButton).getRootView(), (v, windowInsets) -> {
            // Handle insets and cutouts
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.floatingActionButton), (v, windowInsets) -> {
            // Push the floating action button away from bottom
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = insets.left;
            mlp.bottomMargin = insets.bottom;
            mlp.rightMargin = insets.right;
            v.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    static class NewProjectInfo
    {
        String projectName;
        Uri dir;
        String templateDir;
    }

    ActivityResultLauncher<Void> newProjectActivity = registerForActivityResult(
            new ActivityResultContract<Void, NewProjectInfo>() {
                @NonNull @Override public Intent createIntent(@NonNull Context context, Void input) {
                    Intent intent = new Intent(ProjectsActivity.this, NewProjectActivity.class);
                    return intent;
                }
                @Override public NewProjectInfo parseResult(int resultCode, @Nullable Intent data) {
                    if (resultCode == Activity.RESULT_OK) {
                        if (data == null) return null;
                        NewProjectInfo toReturn = new NewProjectInfo();
                        toReturn.projectName = data.getStringExtra("name");
                        toReturn.dir = (Uri) data.getParcelableExtra("externalDir");
                        toReturn.templateDir = data.getStringExtra("template");
                        return toReturn;
                    }
                    return null;
                }
            }, new ActivityResultCallback<NewProjectInfo>() {
                @Override
                public void onActivityResult(NewProjectInfo data) {
                    if (data == null) return;
                    String projectName = data.projectName;
                    Uri dir = data.dir;
                    String templateDir = data.templateDir;
                    // Check if we already have a project with the same name
                    for (ProjectDescription proj: projects)
                    {
                        if (projectName.equals(proj.name))
                        {
                            new AlertDialog.Builder(ProjectsActivity.this)
                                    .setTitle("Project already exists")
                                    .setMessage("A project with the same name already exists")
                                    .setPositiveButton("OK", null)
                                    .show();
                            return;
                        }
                    }
                    // Create necessary directories and permissions for the project
                    boolean isManagedByPlom;
                    if (dir == null)
                    {
                        File projectDir = new File(getExternalFilesDir(null), "projects/" + projectName);
                        projectDir.mkdirs();
                        dir = Uri.fromFile(projectDir);
                        isManagedByPlom = false;
                    }
                    else
                    {
                        getContentResolver().takePersistableUriPermission(dir, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        isManagedByPlom = true;
                    }
                    // Add the new project to the list of projects
                    projects.add(0, new ProjectDescription(projectName, dir, isManagedByPlom));
                    listWidget.getAdapter().notifyDataSetChanged();
                    saveProjectList();
                    // Fill the project with template contents
                    if (templateDir != null && !templateDir.isEmpty())
                        fillDirWithTemplate(dir, templateDir);
                    // Start an activity for the new project
                    Intent intent = new Intent(ProjectsActivity.this, PlomActivity.class);
                    intent.putExtra("name", projectName);
                    intent.putExtra("uri", dir);
                    startActivity(intent);
                }
            }
    );

    public void onNewProjectClicked(View button)
    {
        newProjectActivity.launch(null);
    }

    static class ProjectDescription
    {
        ProjectDescription(String name, Uri uri, boolean isManagedByPlom)
        {
            this.name = name;
            this.uri = uri;
            this.isManagedByPlom = isManagedByPlom;
        }
        String name;
        Uri uri;
        boolean isManagedByPlom;
    }

    void saveProjectList()
    {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        JSONArray json = new JSONArray();
        for (ProjectDescription proj: projects)
        {
            try {
                JSONObject projJson = new JSONObject();
                projJson.put("name", proj.name);
                projJson.put("uri", proj.uri.toString());
                projJson.put("managed", proj.isManagedByPlom);
                json.put(projJson);
            }
            catch (JSONException e)
            {
                // Ignore errors
            }
        }
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString("projects", json.toString());
        prefsEditor.apply();
    }

    void loadProjectList()
    {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        List<ProjectDescription> newProjects = new ArrayList<>();
        try {
            JSONArray json = new JSONArray(prefs.getString("projects", "[]"));
            for (int n = 0; n < json.length(); n++) {
                JSONObject projJson = json.getJSONObject(n);
                newProjects.add(new ProjectDescription(
                        projJson.getString("name"),
                        Uri.parse(projJson.getString("uri")),
                        projJson.getBoolean("managed")
                ));
            }
            projects = newProjects;
        }
        catch (JSONException e)
        {
            // Ignore errors
        }
    }

    void deleteFileRecursively(File dir)
    {
        if (dir.isDirectory())
        {
            for (File f: dir.listFiles())
            {
                deleteFileRecursively(f);
            }
        }
        dir.delete();
    }

    void copyFromTemplate(DocumentFile dest, String src, String srcName)
    {
        if ((src).isEmpty()) return;
        try {
            String[] fileList = getAssets().list(src);
            if (fileList == null || fileList.length == 0)
            {
                DocumentFile destFile = dest.findFile(srcName);
                if (destFile != null) return;
                destFile = dest.createFile(PLOM_MIME_TYPE, srcName);
                // It might be a file
                try (InputStream in = getAssets().open(src);
                     OutputStream out = getContentResolver().openOutputStream(destFile.getUri()))
                {
                    FileManagement.copyStreams(in, out);
                }
            }
            else
            {
                DocumentFile subdir = dest;
                if (!srcName.isEmpty())
                {
                    subdir = dest.createDirectory(srcName);
                    if (subdir == null)
                        subdir = dest.findFile(srcName);
                }
                // Recurse into directory
                for (String f: fileList)
                {
                    copyFromTemplate(subdir, src + "/" + f, f);
                }
            }
        }
        catch (IOException e)
        {
            // Stop trying to fill dir with template on error
        }

    }

    void fillDirWithTemplate(Uri projectUri, String templateDir)
    {
        if ("file".equals(projectUri.getScheme())) {
            copyFromTemplate(DocumentFile.fromFile(new File(projectUri.getPath())), "templates/" + templateDir, "");
        } else {
            copyFromTemplate(DocumentFile.fromTreeUri(this, projectUri), "templates/" + templateDir, "");
        }
    }
}