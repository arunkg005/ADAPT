package com.example.adapt.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.adapt.data.local.ActivityLogDao;
import com.example.adapt.data.local.AppDatabase;
import com.example.adapt.data.local.RoutineDao;
import com.example.adapt.data.local.TaskDao;
import com.example.adapt.data.local.TaskLogDao;
import com.example.adapt.data.model.ActivityLog;
import com.example.adapt.data.model.Routine;
import com.example.adapt.data.model.Task;
import com.example.adapt.data.model.TaskLog;
import com.example.adapt.data.network.ApiService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AppRepository {
    private final RoutineDao routineDao;
    private final TaskDao taskDao;
    private final TaskLogDao taskLogDao;
    private final ActivityLogDao activityLogDao;
    private final ApiService apiService;
    private final ExecutorService executorService;

    public AppRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        routineDao = db.routineDao();
        taskDao = db.taskDao();
        taskLogDao = db.taskLogDao();
        activityLogDao = db.activityLogDao();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.example.com/") // Mock base URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
        
        executorService = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<Routine>> getAllRoutines() {
        return routineDao.getAllRoutines();
    }

    public void insertRoutine(Routine routine) {
        executorService.execute(() -> routineDao.insert(routine));
    }

    public void updateRoutine(Routine routine) {
        executorService.execute(() -> routineDao.update(routine));
    }

    public LiveData<List<Task>> getTasksForRoutine(int routineId) {
        return taskDao.getTasksForRoutine(routineId);
    }

    public void insertTask(Task task) {
        executorService.execute(() -> taskDao.insert(task));
    }

    public void logTaskCompletion(TaskLog taskLog) {
        executorService.execute(() -> {
            taskLogDao.insert(taskLog);
        });
    }

    public void updateRoutineProgress(int routineId, int completedSteps) {
        executorService.execute(() -> {
            Routine routine = routineDao.getRoutineByIdSync(routineId);
            if (routine != null) {
                routine.setCompletedSteps(completedSteps);
                routine.setLastActivityTimestamp(System.currentTimeMillis());
                routineDao.update(routine);
                
                // Also create an ActivityLog for this progress
                ActivityLog log = new ActivityLog(
                        "Progress Update: " + routine.getTitle(),
                        "Completed " + completedSteps + " steps.",
                        System.currentTimeMillis(),
                        ActivityLog.Source.MOBILE,
                        ActivityLog.Type.SUCCESS,
                        "Automatic log: Patient is moving through their routine."
                );
                activityLogDao.insert(log);
            }
        });
    }

    public LiveData<List<ActivityLog>> getAllLogs() {
        return activityLogDao.getAllLogs();
    }

    public void insertActivityLog(ActivityLog log) {
        executorService.execute(() -> activityLogDao.insert(log));
    }
}
