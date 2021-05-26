package dev.plom.ide;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

public class NewProjectActivity extends AppCompatActivity {

    EditText projectNameEdit;
    CheckBox useExistingDirCheckBox;
    TextView useExistingDirFolderName;
    View useExistingDirFolderPane;

    Uri externalDir = null;

    // For activity results
    static final int ACTIVITY_RESULT_OPEN_DOCUMENT_TREE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_project);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("New project");

        projectNameEdit = findViewById(R.id.projectName);
        useExistingDirCheckBox = findViewById(R.id.useExistingDirCheckBox);
        useExistingDirFolderName = findViewById(R.id.useExistingDirFolderName);
        useExistingDirFolderPane = findViewById(R.id.useExistingDirFolderPane);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_RESULT_OPEN_DOCUMENT_TREE && resultCode == Activity.RESULT_OK) {
            if (data == null) return;
            externalDir = data.getData();
            useExistingDirCheckBox.setChecked(true);
            useExistingDirFolderPane.setVisibility(View.VISIBLE);
            useExistingDirFolderName.setText(DocumentFile.fromTreeUri(this, externalDir).getName());
        }
    }

    public void onCreateProjectClicked(View button) {
        // Validate input
        String projectName = projectNameEdit.getText().toString();
        if (projectName.trim().isEmpty())
        {
            new AlertDialog.Builder(this)
                    .setTitle("Missing Project Name")
                    .setMessage("Please provide a name for the project")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        Intent dataToReturn = new Intent();
        dataToReturn.putExtra("name", projectName);
        setResult(RESULT_OK, dataToReturn);
        finish();
    }

    public void onUseExistingDirClicked(View button) {
        if (useExistingDirCheckBox.isChecked())
        {
            try {
                useExistingDirCheckBox.setChecked(false);
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, ACTIVITY_RESULT_OPEN_DOCUMENT_TREE);
            } catch (RuntimeException e) {
                new AlertDialog.Builder(this)
                        .setTitle("Not supported")
                        .setMessage("Your device does not support sharing directories")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }
        }
        else
        {
            externalDir = null;
            useExistingDirFolderPane.setVisibility(View.GONE);
        }
    }
}