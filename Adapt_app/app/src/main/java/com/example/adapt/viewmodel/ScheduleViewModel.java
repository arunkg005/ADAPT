package com.example.adapt.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.adapt.model.CareTask;
import com.example.adapt.repository.CareRepository;

import java.util.List;

public class ScheduleViewModel extends ViewModel {
    private final CareRepository repository = new CareRepository();
    private final MutableLiveData<Integer> selectedPatientId = new MutableLiveData<>(1); // Default to patient 1 for demo
    private final MutableLiveData<String> selectedDay = new MutableLiveData<>("WED 14");
    private final LiveData<List<CareTask>> schedule = Transformations.switchMap(selectedPatientId, repository::getCareItems);

    public LiveData<List<CareTask>> getSchedule() {
        return schedule;
    }

    public LiveData<String> getSelectedDay() {
        return selectedDay;
    }

    public void selectDay(String day) {
        selectedDay.setValue(day);
    }
    
    public void setPatientId(int patientId) {
        selectedPatientId.setValue(patientId);
    }
}
