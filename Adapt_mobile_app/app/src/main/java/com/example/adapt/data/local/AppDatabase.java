package com.example.adapt.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.adapt.data.model.ActivityLog;
import com.example.adapt.data.model.Routine;
import com.example.adapt.data.model.Task;
import com.example.adapt.data.model.TaskLog;

@Database(entities = {Routine.class, Task.class, TaskLog.class, ActivityLog.class}, version = 2, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract RoutineDao routineDao();
    public abstract TaskDao taskDao();
    public abstract TaskLogDao taskLogDao();
    public abstract ActivityLogDao activityLogDao();

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "adapt_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
