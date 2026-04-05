package com.example.adapt.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int routineId;
    private String title;
    private String stepDescription;
    private int order;

    public Task(int routineId, String title, String stepDescription, int order) {
        this.routineId = routineId;
        this.title = title;
        this.stepDescription = stepDescription;
        this.order = order;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRoutineId() { return routineId; }
    public void setRoutineId(int routineId) { this.routineId = routineId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStepDescription() { return stepDescription; }
    public void setStepDescription(String stepDescription) { this.stepDescription = stepDescription; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
}
