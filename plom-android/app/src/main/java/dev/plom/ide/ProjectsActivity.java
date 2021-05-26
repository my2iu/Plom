package dev.plom.ide;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;

public class ProjectsActivity extends AppCompatActivity {

    // For activity results
    static final int ACTIVITY_RESULT_NEW_PROJECT = 1000;

    RecyclerView listWidget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);

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
                };            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                TextView textView = holder.itemView.findViewById(R.id.project_row_item_text);
                textView.setText("Project");
                final ImageButton moreButton = holder.itemView.findViewById(R.id.project_row_item_more);
                moreButton.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(ProjectsActivity.this, moreButton);
                        popup.inflate(R.menu.projects_row_context_menu);
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch(item.getItemId())
                                {
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
                return 1;
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_RESULT_NEW_PROJECT && resultCode == Activity.RESULT_OK) {
            if (data == null) return;
            String projectName = data.getStringExtra("name");
        }
    }

    public void onNewProjectClicked(View button)
    {
        Intent intent = new Intent(this, NewProjectActivity.class);
        startActivityForResult(intent, ACTIVITY_RESULT_NEW_PROJECT);
    }
}