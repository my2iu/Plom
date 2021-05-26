package dev.plom.ide;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class NewProjectActivity extends AppCompatActivity {

    EditText projectNameEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_project);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("New project");

        projectNameEdit = findViewById(R.id.projectName);
    }

    public void onCreateProjectClicked(View button) {
        Intent dataToReturn = new Intent();
        dataToReturn.putExtra("name", projectNameEdit.getText());
        setResult(RESULT_OK, dataToReturn);
        finish();
    }
}