package com.example.adapt.model;

public class RoutineTask {
    private final String title;
    private final String detail;
    private boolean enabled;

    public RoutineTask(String title, String detail, boolean enabled) {
        this.title = title;
        this.detail = detail;
        this.enabled = enabled;
    }

    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
