package com.example.adapt.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.adapt.data.model.TaskLog;

import java.util.List;

@Dao
public interface TaskLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TaskLog taskLog);

    @Query("SELECT * FROM task_logs WHERE taskId = :taskId")
    List<TaskLog> getLogsForTask(int taskId);

    @Query("SELECT * FROM task_logs")
    List<TaskLog> getAllLogs();
}
