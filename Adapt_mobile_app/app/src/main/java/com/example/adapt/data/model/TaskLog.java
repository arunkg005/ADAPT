package com.example.adapt.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "task_logs")
public class TaskLog {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int taskId;
    private String userId;
    private boolean completed;
    private long timestamp;

    public TaskLog(int taskId, String userId, boolean completed, long timestamp) {
        this.taskId = taskId;
        this.userId = userId;
        this.completed = completed;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
