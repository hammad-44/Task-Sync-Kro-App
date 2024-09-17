package com.devhammad.tasksynckro;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.devhammad.tasksynckro.TaskFiles.Task;
import com.devhammad.tasksynckro.TaskFiles.TaskAdapter;
import com.devhammad.tasksynckro.TaskFiles.TaskDetails;
import com.devhammad.tasksynckro.TaskFiles.addTask;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SyncedTask extends AppCompatActivity {
    TextView logout, addtask;
    RecyclerView taskRecycler;
    private TaskAdapter taskAdapter;
    private List<Task> taskList = new ArrayList<>();

    private DatabaseReference tasksRef;
    private ProgressBar loadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_synced_task);

        logout = findViewById(R.id.logout);
        addtask = findViewById(R.id.addtask);
        loadingIndicator = findViewById(R.id.progressBar);
        taskRecycler = findViewById(R.id.taskRecycler);
        taskRecycler.setLayoutManager(new LinearLayoutManager(this));

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        tasksRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("tasks");

        // Set up RecyclerView adapter
        taskAdapter = new TaskAdapter(this, taskList, task -> {
            // Handle item click
            Intent intent = new Intent(SyncedTask.this, TaskDetails.class);
            intent.putExtra("TASK_TITLE", task.getTaskTitle());
            intent.putExtra("TASK_DESCRIPTION", task.getTaskDescrption());
            intent.putExtra("TASK_DATE", task.getDate());
            intent.putExtra("TASK_TIME", task.getFirstAlarmTime());
            intent.putExtra("TASK_COMPLETE", task.isComplete());
            startActivity(intent);
        });
        taskRecycler.setAdapter(taskAdapter);

        // Fetch tasks
        fetchTasks();

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                // Optionally, redirect to the login activity or another appropriate activity
                Intent intent = new Intent(SyncedTask.this, LoginActivity.class);
                Toast.makeText(SyncedTask.this, "Logout Successfully", Toast.LENGTH_SHORT).show();
                startActivity(intent);
                finish();
            }
        });

        addtask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SyncedTask.this, addTask.class);
                intent.putExtra("SOURCE_ACTIVITY", "SyncedTask");
                startActivity(intent);
            }
        });


    }
    @Override
    public void onBackPressed() {
        // Create an intent to start MainActivity
        Intent intent = new Intent(SyncedTask.this, MainActivity.class);

        // Add flags to clear the activity stack
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Start MainActivity
        startActivity(intent);

        // Optionally finish the current activity
        finish();
    }

    private void fetchTasks() {
        showLoadingIndicator();
        tasksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                taskList.clear(); // Clear the list before adding new data
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Task task = snapshot.getValue(Task.class);
                    if (task != null) {
                        taskList.add(task);
                    }
                }
                taskAdapter.notifyDataSetChanged();
                hideLoadingIndicator();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SyncedTask.this, "Error fetching tasks. Please try again.", Toast.LENGTH_SHORT).show();
                hideLoadingIndicator();
            }
        });

    }
    private void showLoadingIndicator() {
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        loadingIndicator.setVisibility(View.GONE);
    }
}