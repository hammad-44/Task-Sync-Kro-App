package com.devhammad.tasksynckro.TaskFiles;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.devhammad.tasksynckro.Database.DatabaseClient;
import com.devhammad.tasksynckro.MainActivity;
import com.devhammad.tasksynckro.R;
import com.devhammad.tasksynckro.SyncedTask;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateTaskActivity extends AppCompatActivity {
    EditText taskTitle, taskDescription, taskDate, taskTime;
    Button updateTaskBtn;
    int taskId; // Store the ID of the task to be updated
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "hh:mm a";
    private Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_task);

        // Initialize views
        taskTitle = findViewById(R.id.addTaskTitle);
        taskDescription = findViewById(R.id.addTaskDescription);
        taskDate = findViewById(R.id.taskDate);
        taskTime = findViewById(R.id.taskTime);
        updateTaskBtn = findViewById(R.id.det_back);

        // Set up date picker for taskDate
        taskDate.setOnClickListener(v -> showDatePicker());

        // Set up time picker for taskTime
        taskTime.setOnClickListener(v -> showTimePicker());

        // Get task details from the intent
        Intent intent = getIntent();
        if (intent != null) {
            taskTitle.setText(intent.getStringExtra("title"));
            taskDescription.setText(intent.getStringExtra("desc"));
            taskDate.setText(intent.getStringExtra("date"));
            taskTime.setText(intent.getStringExtra("time"));
            taskId = intent.getIntExtra("taskId", -1); // Retrieve Task ID
        }

        updateTaskBtn.setOnClickListener(view -> {
            // Gather input data
            String title = taskTitle.getText().toString();
            String description = taskDescription.getText().toString();
            String date = taskDate.getText().toString();
            String time = taskTime.getText().toString();
            taskId = intent.getIntExtra("taskId", -1); // Retrieve Task ID for Room
            String firebaseTaskId = intent.getStringExtra("firebaseTaskId");
            // Basic validation
            if (title.isEmpty() || description.isEmpty() || date.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            } else if (!isValidDate(date)) {
                Toast.makeText(this, "Date must be in yyyy-MM-dd format!", Toast.LENGTH_SHORT).show();
            } else if (!isValidTime(time)) {
                Toast.makeText(this, "Time must be in hh:mm AM/PM format!", Toast.LENGTH_SHORT).show();
            } else {
                // Update the Task object
                Task updatedTask = new Task();
                updatedTask.setTaskId(taskId);
                updatedTask.setFirebaseTaskId(firebaseTaskId);
                updatedTask.setTaskId(taskId);
                updatedTask.setTaskTitle(title);
                updatedTask.setTaskDescrption(description);
                updatedTask.setDate(date);
                updatedTask.setFirstAlarmTime(time);

                // Update task in Room database
                updateTask(updatedTask);
            }
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel();
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this, (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            updateTimeLabel();
        },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        taskDate.setText(sdf.format(calendar.getTime()));
    }

    private void updateTimeLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
        taskTime.setText(sdf.format(calendar.getTime()));
    }

    private boolean isValidDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        sdf.setLenient(false);
        try {
            Date parsedDate = sdf.parse(date);
            return parsedDate != null;
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean isValidTime(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
        sdf.setLenient(false);
        try {
            Date parsedTime = sdf.parse(time);
            return parsedTime != null;
        } catch (ParseException e) {
            return false;
        }
    }
    private void updateTaskInFirebase(Task task) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String firebaseTaskId = task.getFirebaseTaskId();

        // Debugging the Firebase Task ID
        if (firebaseTaskId == null || firebaseTaskId.isEmpty()) {
            Toast.makeText(this, "Invalid Firebase Task ID", Toast.LENGTH_SHORT).show();
            return;  // Exit early if Firebase Task ID is invalid
        }

        // Reference to the user's task in Firebase
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users")
                .child(userId).child("tasks").child(firebaseTaskId);

        // Create a map to hold the updated fields
        Map<String, Object> taskUpdates = new HashMap<>();
        taskUpdates.put("taskTitle", task.getTaskTitle());
        taskUpdates.put("taskDescrption", task.getTaskDescrption());
        taskUpdates.put("date", task.getDate());
        taskUpdates.put("firstAlarmTime", task.getFirstAlarmTime());

        // Update the task in Firebase
        databaseReference.updateChildren(taskUpdates)
                .addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(UpdateTaskActivity.this, "Task updated in Firebase", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(UpdateTaskActivity.this, SyncedTask.class);
                        startActivity(intent);
                    } else {
                        String errorMessage = task1.getException() != null ? task1.getException().getMessage() : "Unknown error";
                        Toast.makeText(UpdateTaskActivity.this, "Error updating task in Firebase: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateTaskInRoomDatabase(final Task task) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                DatabaseClient.getInstance(UpdateTaskActivity.this)
                        .getAppDatabase()
                        .dataBaseAction()
                        .updateAnExistingRow(task.getTaskId(), task.getTaskTitle(), task.getTaskDescrption(), task.getDate(), task.getFirstAlarmTime());

                runOnUiThread(() -> {
                    Toast.makeText(UpdateTaskActivity.this, "Task updated ", Toast.LENGTH_SHORT).show();
                    Intent intent= new Intent(UpdateTaskActivity.this,MainActivity.class);
                    startActivity(intent);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(UpdateTaskActivity.this, "Error updating task . Please try again.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

        private void updateTask(final Task task) {
            // If the task has a Firebase Task ID, update it in Firebase
            if (task.getFirebaseTaskId() != null && !task.getFirebaseTaskId().isEmpty()) {
                updateTaskInFirebase(task);
            } else {
                // Otherwise, update it in the Room database (local)
                updateTaskInRoomDatabase(task);
            }
        }
}
