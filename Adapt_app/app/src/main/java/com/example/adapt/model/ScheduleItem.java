package com.example.adapt.model;

public class ScheduleItem {
    private String time;
    private String title;
    private String patientName;
    private String type;
    private boolean current;

    public ScheduleItem(String time, String title, String patientName, String type, boolean current) {
        this.time = time;
        this.title = title;
        this.patientName = patientName;
        this.type = type;
        this.current = current;
    }

    public String getTime() { return time; }
    public String getTitle() { return title; }
    public String getPatientName() { return patientName; }
    public String getType() { return type; }
    public boolean isCurrent() { return current; }

    public void setTime(String time) { this.time = time; }
    public void setTitle(String title) { this.title = title; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public void setType(String type) { this.type = type; }
    public void setCurrent(boolean current) { this.current = current; }
}
