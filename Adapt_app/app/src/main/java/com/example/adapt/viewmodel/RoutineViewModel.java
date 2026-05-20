package com.example.adapt.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.adapt.model.RoutineGroup;
import com.example.adapt.repository.MockCareRepository;

import java.util.List;

public class RoutineViewModel extends ViewModel {
    private final MutableLiveData<List<RoutineGroup>> routines =
            new MutableLiveData<>(new MockCareRepository().getRoutineGroups());

    public LiveData<List<RoutineGroup>> getRoutines() {
        return routines;
    }

    public void toggleExpanded(RoutineGroup group) {
        group.setExpanded(!group.isExpanded());
        routines.setValue(routines.getValue());
    }
}
