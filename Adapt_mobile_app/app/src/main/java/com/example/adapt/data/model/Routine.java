package com.example.adapt.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "routines")
public class Routine {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String description;
    private String scheduledTime;
    private int totalSteps;
    private int completedSteps;
    private long lastActivityTimestamp;

    public Routine(String title, String description, String scheduledTime) {
        this.title = title;
        this.description = description;
        this.scheduledTime = scheduledTime;
        this.totalSteps = 5; // Default for MVP
        this.completedSteps = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }

    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }

    public int getCompletedSteps() { return completedSteps; }
    public void setCompletedSteps(int completedSteps) { this.completedSteps = completedSteps; }

    public long getLastActivityTimestamp() { return lastActivityTimestamp; }
    public void setLastActivityTimestamp(long lastActivityTimestamp) { this.lastActivityTimestamp = lastActivityTimestamp; }
}
