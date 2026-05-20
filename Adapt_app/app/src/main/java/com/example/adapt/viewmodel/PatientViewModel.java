package com.example.adapt.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.adapt.model.Patient;
import com.example.adapt.repository.CareRepository;

import java.util.ArrayList;
import java.util.List;

public class PatientViewModel extends ViewModel {
    private final CareRepository repository = new CareRepository();
    private final LiveData<List<Patient>> apiPatients = repository.getPatients();
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MediatorLiveData<List<Patient>> filteredPatients = new MediatorLiveData<>();

    public PatientViewModel() {
        filteredPatients.addSource(apiPatients, patients -> filter());
        filteredPatients.addSource(searchQuery, query -> filter());
    }

    public LiveData<List<Patient>> getPatients() {
        return filteredPatients;
    }

    public void search(String query) {
        searchQuery.setValue(query);
    }

    private void filter() {
        List<Patient> all = apiPatients.getValue();
        String query = searchQuery.getValue();
        
        if (all == null) {
            filteredPatients.setValue(null);
            return;
        }
        
        if (query == null || query.trim().isEmpty()) {
            filteredPatients.setValue(all);
            return;
        }

        String normalized = query.toLowerCase();
        List<Patient> filtered = new ArrayList<>();
        for (Patient patient : all) {
            if (patient.getName().toLowerCase().contains(normalized)
                    || patient.getAllergies().toLowerCase().contains(normalized)) {
                filtered.add(patient);
            }
        }
        filteredPatients.setValue(filtered);
    }
}
