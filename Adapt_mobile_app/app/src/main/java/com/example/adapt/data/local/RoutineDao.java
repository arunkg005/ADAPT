package com.example.adapt.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.adapt.data.model.Routine;

import java.util.List;

@Dao
public interface RoutineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Routine routine);

    @Update
    void update(Routine routine);

    @Query("SELECT * FROM routines")
    LiveData<List<Routine>> getAllRoutines();

    @Query("SELECT * FROM routines WHERE id = :routineId")
    LiveData<Routine> getRoutineById(int routineId);
    
    @Query("SELECT * FROM routines WHERE id = :routineId")
    Routine getRoutineByIdSync(int routineId);
}
