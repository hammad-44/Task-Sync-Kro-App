package com.devhammad.tasksynckro.SuggesTasks;

public class TaskModel {
    private String title;
    private String description;
    private String time;
    private String day;
    private String date;
    private String month;

    public TaskModel(String title, String description, String time, String day, String date, String month) {
        this.title = title;
        this.description = description;
        this.time = time;
        this.day = day;
        this.date = date;
        this.month = month;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTime() {
        return time;
    }

    public String getDay() {
        return day;
    }

    public String getDate() {
        return date;
    }
    public String getFullDate()
    {
        return date+" "+month+", 2024";
    }

    public String getMonth() {
        return month;
    }
}
