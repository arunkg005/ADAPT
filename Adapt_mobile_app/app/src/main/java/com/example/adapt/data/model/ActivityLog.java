package com.example.adapt.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "activity_logs")
public class ActivityLog {
    public enum Source { IOT, MOBILE }
    public enum Type { SUCCESS, WARNING, INFO }

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String description;
    private long timestamp;
    private Source source;
    private Type type;
    private String detailInfo;

    public ActivityLog(String title, String description, long timestamp, Source source, Type type, String detailInfo) {
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.source = source;
        this.type = type;
        this.detailInfo = detailInfo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getDetailInfo() { return detailInfo; }
    public void setDetailInfo(String detailInfo) { this.detailInfo = detailInfo; }
}
