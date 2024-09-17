package com.devhammad.tasksynckro;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.devhammad.tasksynckro.Database.DatabaseClient;
import com.devhammad.tasksynckro.SuggesTasks.SuggestTask;
import com.devhammad.tasksynckro.TaskFiles.Task;
import com.devhammad.tasksynckro.TaskFiles.addTask;
import com.devhammad.tasksynckro.TaskFiles.TaskAdapter;
import com.devhammad.tasksynckro.TaskFiles.TaskDetails;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class MainActivity extends AppCompatActivity {


    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    TextView addTask, modeltext, suggestbtn;
    private ExecutorService executorService;
    private Handler handler;
    private Runnable taskChecker;
    ImageView userImage;
    private FirebaseAuth mAuth;

    // Use this format for time with AM/PM
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.taskRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskList = new ArrayList<>();

        userImage = findViewById(R.id.UserImage);

        suggestbtn = findViewById(R.id.suggest_task);
        suggestbtn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SuggestTask.class);
            startActivity(intent);
        });

        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAuth.getCurrentUser() != null) {
                    // User is logged in, navigate to SyncedTasksActivity
                    Intent intent = new Intent(MainActivity.this, SyncedTask.class);
                    startActivity(intent);
                } else {
                    // User is not logged in, navigate to LoginActivity
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            }
        });

        taskAdapter = new TaskAdapter(this, taskList, task -> {
            // Handle item click
            Intent intent = new Intent(MainActivity.this, TaskDetails.class);
            intent.putExtra("TASK_TITLE", task.getTaskTitle());
            intent.putExtra("TASK_DESCRIPTION", task.getTaskDescrption());
            intent.putExtra("TASK_DATE", task.getDate());
            intent.putExtra("TASK_TIME", task.getFirstAlarmTime());
            intent.putExtra("TASK_COMPLETE", task.isComplete());
            startActivity(intent);
        });
        recyclerView.setAdapter(taskAdapter);

        addTask = findViewById(R.id.det_back);
        addTask.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, addTask.class);
            intent.putExtra("SOURCE_ACTIVITY", "MainActivity");
            startActivity(intent);
        });

        // Initialize ExecutorService
        executorService = Executors.newSingleThreadExecutor();

        // Load tasks from database
        loadTasksFromDatabase();

        // Initialize the handler for periodic task checking
        handler = new Handler();
        taskChecker = new Runnable() {
            @Override
            public void run() {
                checkForTasksWithFiveMinutesLeft();
                handler.postDelayed(this, 20000); // Check every 1 minute
            }
        };

        // Start checking tasks
        handler.post(taskChecker);
    }

    private void loadTasksFromDatabase() {
        executorService.execute(() -> {
            List<Task> tasks = DatabaseClient.getInstance(MainActivity.this)
                    .getAppDatabase()
                    .dataBaseAction()
                    .getAllTasksList();

            // Update RecyclerView on UI thread
            runOnUiThread(() -> {
                taskList.clear();
                taskList.addAll(tasks);
                taskAdapter.notifyDataSetChanged();
            });
        });
    }

    private void checkForTasksWithFiveMinutesLeft() {
        Date currentDate = new Date(); // Get current date and time

        for (Task task : taskList) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            try {
                // Parse task time (e.g., "03:47 AM")
                Date taskTime = timeFormat.parse(task.getFirstAlarmTime());

                // Get current time
                Date currentTime = new Date();
                String currentTimeString = timeFormat.format(currentTime);  // Convert current time to string
                currentDate = timeFormat.parse(currentTimeString);     // Parse current time as hh:mm a

                // Convert task time and current time to minutes from midnight
                long taskMinutes = (taskTime.getHours() * 60) + taskTime.getMinutes();
                long currentMinutes = (currentDate.getHours() * 60) + currentDate.getMinutes();

                long timeDifferenceInMinutes = taskMinutes - currentMinutes;


                // Check if the task is 5 minutes away
                if (timeDifferenceInMinutes == 5) {
                    // Trigger the alarm here by starting AlarmActivity
                    Intent intent = new Intent(MainActivity.this, AlarmActivity.class);
                    intent.putExtra("TITLE", task.getTaskTitle());
                    intent.putExtra("DESC", task.getTaskDescrption());
                    intent.putExtra("DATE", task.getDate());
                    intent.putExtra("TIME", task.getFirstAlarmTime());

                    startActivity(intent);
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }

        }
    }

    private void triggerAlarmActivity(Task task) {
        Intent intent = new Intent(MainActivity.this, AlarmActivity.class);
        intent.putExtra("TITLE", task.getTaskTitle());
        intent.putExtra("DESC", task.getTaskDescrption());
        intent.putExtra("DATE",task.getDate());
        intent.putExtra("TIME",task.getFirstAlarmTime());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        // Stop the handler when activity is destroyed
        if (handler != null) {
            handler.removeCallbacks(taskChecker);
        }
    }
}
