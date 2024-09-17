package com.devhammad.tasksynckro.TaskFiles;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.devhammad.tasksynckro.Database.DatabaseClient;
import com.devhammad.tasksynckro.MainActivity;
import com.devhammad.tasksynckro.R;
import com.devhammad.tasksynckro.SyncedTask;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private Context context;
    private List<Task> taskList;
    private OnItemClickListener onItemClickListener;

    public TaskAdapter(Context context, List<Task> taskList, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.taskList = taskList;
        this.onItemClickListener = onItemClickListener;
    }

    private void handleDeleteAction(Task task) {
    // Create an AlertDialog to confirm the action
    new AlertDialog.Builder(context)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Yes", (dialog, which) -> {
                // User clicked "Yes", proceed with the delete
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(() -> {
                    if (context instanceof SyncedTask) {
                        deleteTaskFromFirebase(task);
                    } else {
                        // Delete from Room database
                        DatabaseClient.getInstance(context)
                                .getAppDatabase()
                                .dataBaseAction()
                                .deleteTaskFromId(task.getTaskId());

                        // Remove the task from the RecyclerView list
                        ((MainActivity) context).runOnUiThread(() -> {
                            taskList.remove(task);
                            notifyDataSetChanged(); // Refresh RecyclerView
                            Toast.makeText(context, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            })
            .setNegativeButton("No", null) // User clicked "No", do nothing
            .show();
}
    private void deleteTaskFromFirebase(Task task) {
        // Get current user ID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get firebaseTaskId from the Task object
        String firebaseTaskId = task.getFirebaseTaskId(); // Assuming you've added this field in your Task model

        // Check if firebaseTaskId is not null or empty
        if (firebaseTaskId != null && !firebaseTaskId.isEmpty()) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users")
                    .child(userId).child("tasks").child(firebaseTaskId);

            databaseReference.removeValue()
                    .addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            Toast.makeText(context, "Task deleted from Firebase", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Error deleting task from Firebase", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(context, "Invalid Firebase Task ID", Toast.LENGTH_SHORT).show();
        }
    }


    private void handleUpdateAction(Task task) {
        // Pass the task to the UpdateTaskActivity
        Intent intent = new Intent(context, UpdateTaskActivity.class);
        intent.putExtra("taskId", task.getTaskId());
        intent.putExtra("firebaseTaskId", task.getFirebaseTaskId());
        intent.putExtra("title",task.getTaskTitle());
        intent.putExtra("desc",task.getTaskDescrption());
        intent.putExtra("date",task.getDate());
        intent.putExtra("time",task.getFirstAlarmTime());
        intent.putExtra("taskId",task.getTaskId());

        context.startActivity(intent);
    }

    private void handleCompleteAction(Task task) {
        // Create an AlertDialog to confirm the action
        new AlertDialog.Builder(context)
                .setTitle("Complete Task")
                .setMessage("Are you sure you want to mark this task as complete? Task Will Be Removed After 15 Seconds.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // User clicked "Yes", proceed with marking as complete
                    task.setComplete(true); // Mark the task as complete

                    // Update the task status in ROOM or Firebase based on its origin
                    if (context instanceof SyncedTask) {
                        markTaskAsCompleteInFirebase(task);
                    }

                    // Optionally remove the task from the RecyclerView list after a delay
                    new Handler().postDelayed(() -> {
                        taskList.remove(task);
                        notifyDataSetChanged(); // Refresh RecyclerView
                    }, 15000); // 5 seconds delay
                })
                .setNegativeButton("No", null) // User clicked "No", do nothing
                .show();
    }

    private void markTaskAsCompleteInFirebase(Task task) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String firebaseTaskId = task.getFirebaseTaskId();

        // Reference to the user's task in Firebase
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users")
                .child(userId).child("tasks").child(firebaseTaskId);

        // Create a map to hold the updated fields
        Map<String, Object> taskUpdates = new HashMap<>();
        taskUpdates.put("complete", true); // Assuming 'completed' is the field to mark task as completed

        // Update the task status in Firebase
        databaseReference.updateChildren(taskUpdates)
                .addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(context, "Task marked as completed", Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMessage = task1.getException() != null ? task1.getException().getMessage() : "Unknown error";
                        Toast.makeText(context, "Error updating task status" + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout (item_task.xml)
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        // Get the current task
        Task task = taskList.get(position);

        // Set data into the views
        holder.title.setText(task.getTaskTitle());
        holder.description.setText(task.getTaskDescrption());
        holder.time.setText(task.getFirstAlarmTime());
        holder.date.setText(task.getDate().split("-")[2]); // Assuming format yyyy-mm-dd
        holder.month.setText(task.getDate().split("-")[1]);
        holder.day.setText(task.getDate().split("-")[0]);

        // Set status
        holder.status.setText(task.isComplete() ? "COMPLETED" : "PENDING");

        // Set icon for more options
        holder.options.setImageResource(R.drawable.more); // Assuming a drawable

        // Set up item click listener
        holder.itemView.setOnClickListener(view -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(task);
            }
        });

        holder.options.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.options);
            popupMenu.inflate(R.menu.menu); // Inflate your menu.xml

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();

                if (id == R.id.menuDelete) {
                    handleDeleteAction(task);
                    return true;
                } else if (id == R.id.menuUpdate) {
                    handleUpdateAction(task);
                    return true;
                } else if (id == R.id.menuComplete) {
                    handleCompleteAction(task);
                    return true;
                } else {
                    return false;
                }
            });


            popupMenu.show();
        });


    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public interface OnItemClickListener {
        void onItemClick(Task task);
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, time, date, month, day, status;
        ImageView options;
        CardView cardView;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            description = itemView.findViewById(R.id.description);
            time = itemView.findViewById(R.id.time);
            date = itemView.findViewById(R.id.date);
            month = itemView.findViewById(R.id.month);
            day = itemView.findViewById(R.id.day);
            status = itemView.findViewById(R.id.status);
            options = itemView.findViewById(R.id.options);
        }
    }
}
