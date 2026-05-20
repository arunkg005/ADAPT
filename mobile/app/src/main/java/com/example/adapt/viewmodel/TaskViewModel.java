package com.example.adapt.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.adapt.model.CareTask;
import com.example.adapt.repository.CareRepository;

import java.util.List;

public class TaskViewModel extends ViewModel {
    private final CareRepository repository = new CareRepository();
    private final MutableLiveData<Integer> selectedPatientId = new MutableLiveData<>();
    private final LiveData<List<CareTask>> tasks = Transformations.switchMap(selectedPatientId, repository::getCareItems);

    public LiveData<List<CareTask>> getTasks() {
        return tasks;
    }

    public void setPatientId(int patientId) {
        selectedPatientId.setValue(patientId);
    }

    public void addTask(CareTask task) {
        repository.addTask(task);
        // Refresh tasks if needed, or the repository could update a LiveData
        Integer currentId = selectedPatientId.getValue();
        if (currentId != null) {
            selectedPatientId.setValue(currentId);
        }
    }
}
