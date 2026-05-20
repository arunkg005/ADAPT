package com.example.adapt.model;

import java.util.List;

public class RoutineGroup {
    private final String title;
    private final String subtitle;
    private final List<RoutineTask> tasks;
    private boolean expanded;

    public RoutineGroup(String title, String subtitle, List<RoutineTask> tasks, boolean expanded) {
        this.title = title;
        this.subtitle = subtitle;
        this.tasks = tasks;
        this.expanded = expanded;
    }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public List<RoutineTask> getTasks() { return tasks; }
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }
}
