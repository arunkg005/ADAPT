package com.example.adapt.model;

import com.google.gson.annotations.SerializedName;

public class CareTask {
    @SerializedName("id")
    private int id;

    @SerializedName("patient")
    private int patientId;

    @SerializedName("item_type")
    private String itemType;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("notes")
    private String notes;

    @SerializedName("priority")
    private String priority;

    @SerializedName("status")
    private String status;

    @SerializedName("due_at")
    private String dueAt;

    @SerializedName("recurrence_mode")
    private String recurrenceMode;

    @SerializedName("recurrence_weekdays")
    private String recurrenceWeekdays;

    @SerializedName("reminder_enabled")
    private boolean reminderEnabled;

    @SerializedName("created_at")
    private String createdAt;

    // Full constructor for creating new tasks
    public CareTask(int patientId, String itemType, String title, String description,
                    String priority, String status, String dueAt) {
        this.patientId = patientId;
        this.itemType = itemType;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.dueAt = dueAt;
    }

    // Simplified constructor (backward compatibility)
    public CareTask(int id, String title, String description, String priority,
                    String status, String dueAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.dueAt = dueAt;
    }

    public int getId() { return id; }
    public int getPatientId() { return patientId; }
    public String getItemType() { return itemType; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getNotes() { return notes; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public String getDueAt() { return dueAt; }
    public String getRecurrenceMode() { return recurrenceMode; }
    public String getRecurrenceWeekdays() { return recurrenceWeekdays; }
    public boolean isReminderEnabled() { return reminderEnabled; }
    public String getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
    public void setItemType(String itemType) { this.itemType = itemType; }
}
