package com.example.adapt.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.adapt.data.model.Routine;
import com.example.adapt.data.model.Task;
import com.example.adapt.data.model.TaskLog;
import com.example.adapt.data.repository.AppRepository;

import java.util.List;

public class RoutineViewModel extends AndroidViewModel {
    private final AppRepository repository;
    private final LiveData<List<Routine>> allRoutines;

    public RoutineViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        allRoutines = repository.getAllRoutines();
    }

    public LiveData<List<Routine>> getAllRoutines() {
        return allRoutines;
    }

    public void insertRoutine(Routine routine) {
        repository.insertRoutine(routine);
    }

    public void updateRoutine(Routine routine) {
        repository.updateRoutine(routine);
    }

    public LiveData<List<Task>> getTasksForRoutine(int routineId) {
        return repository.getTasksForRoutine(routineId);
    }

    public void insertTask(Task task) {
        repository.insertTask(task);
    }

    public void logTaskCompletion(TaskLog taskLog) {
        repository.logTaskCompletion(taskLog);
    }
}
