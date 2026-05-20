package com.example.adapt.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.adapt.repository.MockCareRepository;

public class ProgressViewModel extends ViewModel {
    private final MutableLiveData<int[]> trend =
            new MutableLiveData<>(new MockCareRepository().getCompletionTrend());

    public LiveData<int[]> getTrend() {
        return trend;
    }
}
