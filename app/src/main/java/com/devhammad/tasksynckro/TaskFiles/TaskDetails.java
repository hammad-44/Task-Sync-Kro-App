package com.devhammad.tasksynckro.TaskFiles;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.devhammad.tasksynckro.R;

public class TaskDetails extends AppCompatActivity {
    TextView title, date, time, desc;
    Button goBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        // Initialize views
        title = findViewById(R.id.det_title);
        date = findViewById(R.id.det_date);
        time = findViewById(R.id.det_time);
        desc = findViewById(R.id.det_desc);
        goBack = findViewById(R.id.det_back);
        // Get the Intent that started this activity and extract the data
        if (getIntent() != null) {
            String taskTitle = getIntent().getStringExtra("TASK_TITLE");
            String taskDate = getIntent().getStringExtra("TASK_DATE");
            String taskTime = getIntent().getStringExtra("TASK_TIME");
            String taskDescription = getIntent().getStringExtra("TASK_DESCRIPTION");
            boolean isComplete = getIntent().getBooleanExtra("TASK_COMPLETE", false);

            // Set the data to the views
            title.setText(taskTitle);
            date.setText(taskDate);
            time.setText(taskTime);
            desc.setText(taskDescription);

        }
        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close the activity and return to the previous one
            }
        });
    }
}
