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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

public class NewProjectActivity extends AppCompatActivity {

    EditText projectNameEdit;
    Spinner templateSpinner;
    CheckBox useExistingDirCheckBox;
    TextView useExistingDirFolderName;
    View useExistingDirFolderPane;

    Uri externalDir = null;

    CodeTemplate[] templateOptions = new CodeTemplate[] {
            new CodeTemplate("Empty Project", ""),
            new CodeTemplate("Simple Main", "simple"),
            new CodeTemplate("Standard Library", "stdlib")
    };
    int selectedTemplateIndex = 1;

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
        templateSpinner = findViewById(R.id.templateSpinner);
        ArrayAdapter<CodeTemplate> templateAdapter = new ArrayAdapter(this,
                R.layout.support_simple_spinner_dropdown_item, templateOptions);
        templateSpinner.setAdapter(templateAdapter);
        templateSpinner.setSelection(selectedTemplateIndex);
        templateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTemplateIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTemplateIndex = 0;
            }
        });
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
        if (externalDir != null) {
            dataToReturn.putExtra("externalDir", externalDir);
        }
        dataToReturn.putExtra("template", templateOptions[selectedTemplateIndex].sourceDir);
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

    static class CodeTemplate
    {
        String name;
        String sourceDir;

        CodeTemplate(String name, String sourceDir)
        {
            this.name = name;
            this.sourceDir = sourceDir;
        }

        public String toString() { return name; }
    }
}