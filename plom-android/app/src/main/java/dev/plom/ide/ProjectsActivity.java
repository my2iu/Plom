package dev.plom.ide;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static dev.plom.ide.PlomActivity.PLOM_MIME_TYPE;

public class ProjectsActivity extends AppCompatActivity {

    // For activity results
    static final int ACTIVITY_RESULT_NEW_PROJECT = 1000;

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_RESULT_NEW_PROJECT && resultCode == Activity.RESULT_OK) {
            if (data == null) return;
            String projectName = data.getStringExtra("name");
            Uri dir = (Uri)data.getParcelableExtra("externalDir");
            String templateDir = data.getStringExtra("template");
            // Check if we already have a project with the same name
            for (ProjectDescription proj: projects)
            {
                if (projectName.equals(proj.name))
                {
                    new AlertDialog.Builder(this)
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
            fillDirWithTemplate(dir, templateDir);
            // Start an activity for the new project
            Intent intent = new Intent(this, PlomActivity.class);
            intent.putExtra("name", projectName);
            intent.putExtra("uri", dir);
            startActivity(intent);
        }
    }

    public void onNewProjectClicked(View button)
    {
        Intent intent = new Intent(this, NewProjectActivity.class);
        startActivityForResult(intent, ACTIVITY_RESULT_NEW_PROJECT);
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
                byte [] data = new byte[8192];
                try (InputStream in = getAssets().open(src);
                     OutputStream out = getContentResolver().openOutputStream(destFile.getUri()))
                {
                    int len = in.read(data);
                    while (len >= 0)
                    {
                        out.write(data, 0, len);
                        len = in.read(data);
                    }
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