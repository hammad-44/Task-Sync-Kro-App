package com.devhammad.tasksynckro;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class AlarmActivity extends AppCompatActivity {
    TextView taskTitle, taskDescription, timeAndData;
    ImageView imageView;
    Button back_btn;
    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        taskTitle = findViewById(R.id.title);
        taskDescription = findViewById(R.id.description);
        back_btn = findViewById(R.id.closeButton);
        timeAndData = findViewById(R.id.timeAndData);
        imageView = findViewById(R.id.imageView);

        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.notification);
        mediaPlayer.start();

        if (getIntent().getExtras() != null) {
            taskTitle.setText(getIntent().getStringExtra("TITLE"));
            taskDescription.setText(getIntent().getStringExtra("DESC"));
            timeAndData.setText(getIntent().getStringExtra("DATE") + ", " + getIntent().getStringExtra("TIME"));
        }

        Glide.with(getApplicationContext()).load(R.drawable.alert).into(imageView);

        back_btn.setOnClickListener(view -> finish());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
    }
}