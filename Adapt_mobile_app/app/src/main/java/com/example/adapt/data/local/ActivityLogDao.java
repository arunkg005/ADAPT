package com.example.adapt.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.adapt.data.model.ActivityLog;

import java.util.List;

@Dao
public interface ActivityLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ActivityLog log);

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    LiveData<List<ActivityLog>> getAllLogs();

    @Query("SELECT * FROM activity_logs WHERE source = :source ORDER BY timestamp DESC")
    LiveData<List<ActivityLog>> getLogsBySource(ActivityLog.Source source);
}
