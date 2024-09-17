package com.devhammad.tasksynckro.TaskFiles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class addTask extends AppCompatActivity {
    EditText taskTitle, taskDescription, taskDate, taskTime;
    Button addTaskBtn;
    DatabaseClient taskDatabase;
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "hh:mm a";
    private Calendar calendar = Calendar.getInstance();
    private String sourceActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        // Initialize views
        taskTitle = findViewById(R.id.addTaskTitle);
        taskDescription = findViewById(R.id.addTaskDescription);
        taskDate = findViewById(R.id.taskDate);
        taskTime = findViewById(R.id.taskTime);
        addTaskBtn = findViewById(R.id.det_back);

        // Initialize database
        taskDatabase = DatabaseClient.getInstance(this);

        // Get the source activity from intent
        sourceActivity = getIntent().getStringExtra("SOURCE_ACTIVITY");
        // Set up date picker
        taskDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerFragment().show(getSupportFragmentManager(), "datePicker");
            }
        });

        // Set up time picker
        taskTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerFragment().show(getSupportFragmentManager(), "timePicker");
            }
        });

        addTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Gather input data
                String title = taskTitle.getText().toString();
                String description = taskDescription.getText().toString();
                String date = taskDate.getText().toString();
                String time = taskTime.getText().toString();

                // Basic validation
                if (title.isEmpty() || description.isEmpty() || date.isEmpty() || time.isEmpty()) {
                    Toast.makeText(addTask.this, "All fields are required!", Toast.LENGTH_SHORT).show();
                } else if (!isValidDate(date)) {
                    Toast.makeText(addTask.this, "Date must be in yyyy-MM-dd format!", Toast.LENGTH_SHORT).show();
                } else if (!isValidTime(time)) {
                    Toast.makeText(addTask.this, "Time must be in hh:mm AM/PM format!", Toast.LENGTH_SHORT).show();
                } else {
                    // Create a Task object
                    Task newTask = new Task();
                    newTask.setTaskTitle(title);
                    newTask.setTaskDescrption(description);
                    newTask.setDate(date);
                    newTask.setFirstAlarmTime(time); // You can modify this if needed
                    newTask.setComplete(false); // Task is incomplete initially

                    if ("MainActivity".equals(sourceActivity)) {
                        // Insert task into Room database
                        insertTask(newTask);
                    } else if ("SyncedTask".equals(sourceActivity)) {
                        // Insert task into Firebase Realtime Database
                        insertTaskIntoFirebase(newTask);
                    }
                }
            }
        });
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

    private void insertTask(final Task task) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    DatabaseClient.getInstance(addTask.this).getAppDatabase().dataBaseAction().insertDataIntoTaskList(task);

                    // Notify user on success and refresh task list
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(addTask.this, "Task added successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(addTask.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish(); // Optional: close the activity after adding the task
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(addTask.this, "Error adding task. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }


//    private void insertTaskIntoFirebase(Task task) {
//        // Get current user ID
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        // Add task to Firebase Realtime Database under user's node
//        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users")
//                .child(userId).child("tasks"); // Adjust userId as needed
//
//// Convert taskId to String if necessary
//        String taskId = String.valueOf(task.getTaskId());
//        databaseReference.child(taskId).setValue(task)
//                .addOnCompleteListener(task1 -> {
//                    if (task1.isSuccessful()) {
//                        Toast.makeText(addTask.this, "Task added to Firebase", Toast.LENGTH_SHORT).show();
//                        Intent intent = new Intent(addTask.this,SyncedTask.class);
//                        startActivity(intent);
//                    } else {
//                        String errorMessage = task1.getException() != null ? task1.getException().getMessage() : "Unknown error";
//                        Toast.makeText(addTask.this, "Error adding task to Firebase: " + errorMessage, Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
private void insertTaskIntoFirebase(Task task) {
    // Get current user ID
    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    // Get a reference to the "tasks" node for the current user
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users")
            .child(userId).child("tasks");

    // Generate a new ID for the task using Firebase's push() method
    String firebaseTaskId = databaseReference.push().getKey(); // This returns a string ID

    if (firebaseTaskId != null) {
        // Set the generated Firebase ID in the Task object
        task.setFirebaseTaskId(firebaseTaskId);

        // Store the task in Firebase using the generated Firebase ID
        databaseReference.child(firebaseTaskId).setValue(task)
                .addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(addTask.this, "Task added", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(addTask.this, SyncedTask.class);
                        startActivity(intent);
                    } else {
                        String errorMessage = task1.getException() != null ? task1.getException().getMessage() : "Unknown error";
                        Toast.makeText(addTask.this, "Error adding task to Server: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    } else {
        Toast.makeText(addTask.this, "Error generating task ID", Toast.LENGTH_SHORT).show();
    }
}

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create and return a new instance of DatePickerDialog
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            // Get the activity and set the selected date to the corresponding EditText
            addTask activity = (addTask) getActivity();
            activity.taskDate.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day));
        }
    }

    public static class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default time in the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create and return a new instance of TimePickerDialog
            return new TimePickerDialog(getActivity(), this, hour, minute, true);
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Get the activity and format the selected time
            addTask activity = (addTask) getActivity();
            Calendar timeCalendar = Calendar.getInstance();
            timeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            timeCalendar.set(Calendar.MINUTE, minute);
            String formattedTime = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault()).format(timeCalendar.getTime());
            activity.taskTime.setText(formattedTime);
        }
    }
}
