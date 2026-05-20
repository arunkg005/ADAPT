package com.example.adapt.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<String> title = new MutableLiveData<>("ADAPT");

    public LiveData<String> getTitle() {
        return title;
    }
}
