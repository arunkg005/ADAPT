package com.example.adapt.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.adapt.data.model.Task;

import java.util.List;

@Dao
public interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Task task);

    @Query("SELECT * FROM tasks WHERE routineId = :routineId ORDER BY `order` ASC")
    LiveData<List<Task>> getTasksForRoutine(int routineId);
}
