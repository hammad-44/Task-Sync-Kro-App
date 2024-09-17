package com.devhammad.tasksynckro.SuggesTasks;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.devhammad.tasksynckro.MainActivity;
import com.devhammad.tasksynckro.R;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class SuggestTask extends AppCompatActivity {
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private FusedLocationProviderClient fusedLocationClient;
    private TaskAdapter taskAdapter;
    private List<TaskModel> taskList;
    private String userLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggest_task);

        recyclerView = findViewById(R.id.taskRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        progressBar = findViewById(R.id.progressBar);
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, this);
        recyclerView.setAdapter(taskAdapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check permissions and request if needed
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            userLocation = String.format(Locale.getDefault(), "%f, %f", location.getLatitude(), location.getLongitude());
                        } else {
                            userLocation = "Unknown Location"; // Set location as unknown if not available
                        }
                        fetchSuggestedTasks(); // Fetch tasks regardless of location success
                    }
                });
    }

    public String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Fetch current date and extract year, month, day of week, and day of month
    public String[] getCurrentDateComponents() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());  // Day of the week, e.g., Mon
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault()); // Month name, e.g., Sep
        SimpleDateFormat dayOfMonthFormat = new SimpleDateFormat("dd", Locale.getDefault()); // Day of the month, e.g., 08

        Date date = new Date();  // Use the current date

        String dayOfWeek = dayFormat.format(date);
        String month = monthFormat.format(date);
        String dayOfMonth = dayOfMonthFormat.format(date);

        return new String[]{currentDate, dayOfWeek, month, dayOfMonth};
    }
    //GET YOUR API KEY FROM GOOGLE GEMINI
    // Method to fetch suggested tasks based on location status
    private void fetchSuggestedTasks() {
        progressBar.setVisibility(View.VISIBLE);
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", "YOUR_API_KEY");
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String prompt = createPrompt(); // Generate prompt based on location

        Log.d("Prompt", prompt);
        Content content = new Content.Builder().addText(prompt).build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(
                response,
                new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        progressBar.setVisibility(View.GONE);
                        String responseText = result.getText();
                        Log.d("responseText", responseText);
                        if (isValidResponseFormat(responseText)) {
                            parseAndDisplayTasks(responseText);
                        } else {
                            Toast.makeText(SuggestTask.this, "Invalid response format! Try Again Later", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SuggestTask.this,MainActivity.class);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        t.printStackTrace();
                    }
                },
                getMainExecutor()
        );
    }
    private boolean isValidResponseFormat(String responseText) {
        String[] tasks = responseText.split("\\n\\n");  // Split tasks by double newline
        for (String task : tasks) {
            String[] details = task.split("\\n");  // Split task details by newlines
            if (details.length < 4) {
                return false;  // Each task must have at least 4 lines (Title, Description, Date, Time)
            }
        }
        return true;  // If all tasks have the correct structure, the format is valid
    }

    // Create the prompt based on location availability
    private String createPrompt() {
        String prompt;
        // Get the current date components (day, month, etc.)
        String[] currentDateComponents = getCurrentDateComponents();
        String currentDate = currentDateComponents[0];
        String dayOfWeek = currentDateComponents[1];
        String month = currentDateComponents[2];
        String dayOfMonth = currentDateComponents[3];
        if ("Unknown Location".equals(userLocation)) {

            prompt = String.format(
                    "Suggest me some tasks to do at %s on %s in Lahore, Pakistan. Write each task in the following format:\n" +
                            "Task Title\n" +
                            "Task Description\n" +
                            "Task Date\n" +
                            "Task Time\n" +
                            "\nNext Task\n" +
                            "USE THE CURRENT TIME TO SUGGEST TASKS. Minimum 5 tasks.  DO NOT USE BOLD WORDS AND DO NOT ADD ANY OTHER WORDS.JUST WRITE like this : task title\ndescription\ndate\ntime.",
                    getCurrentTime(),dayOfWeek
            );
        } else {
            prompt = String.format(
                    "Suggest me some tasks to do at %s in %s on %s . Write each task in the following format:\n" +
                            "Task Title\n" +
                            "Task Description(minimum 40 words)\n" +
                            "Task Date\n" +
                            "Task Time\n" +
                            "\nNext Task\n" +
                            "USE THE CURRENT LOCATION AND TIME TO SUGGEST TASKS. Take this location to Google Maps to think of some tasks. Minimum 5 tasks. DO NOT USE BOLD WORDS AND DO NOT ADD ANY OTHER WORDS..JUST WRITE like this : task title\ndescription\ndate\ntime.",
                    getCurrentTime(), userLocation,dayOfWeek
            );
        }
        return prompt;
    }

    private void parseAndDisplayTasks(String responseText) {
        // Split the response by tasks based on the pattern of two consecutive newlines
        String[] tasks = responseText.split("\\n\\n");

        // Get the current date components (day, month, etc.)
        String[] currentDateComponents = getCurrentDateComponents();
        String currentDate = currentDateComponents[0];
        String dayOfWeek = currentDateComponents[1];
        String month = currentDateComponents[2];
        String dayOfMonth = currentDateComponents[3];

        for (String task : tasks) {
            // Split each task's information by newline
            String[] details = task.split("\\n");

            if (details.length >= 4) {
                String title = details[0].trim();               // Task title
                String description = details[1].trim();         // Task description
                String time = details[3].trim();                // Task time

                // Add the task to the list, using current date for day, month, and dayOfWeek
                taskList.add(new TaskModel(title, description, time, dayOfWeek, dayOfMonth, month));
            }
        }

        // Notify the adapter that the data set has changed
        taskAdapter.notifyDataSetChanged();
    }
}
