package com.example.adapt.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.adapt.api.ApiService;
import com.example.adapt.api.RetrofitClient;
import com.example.adapt.model.Patient;
import com.example.adapt.model.CareTask;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CareRepository {
    private final ApiService apiService;

    public CareRepository() {
        this.apiService = RetrofitClient.getApiService();
    }

    // ── Patients ─────────────────────────────────────────

    public LiveData<List<Patient>> getPatients() {
        MutableLiveData<List<Patient>> data = new MutableLiveData<>();
        apiService.getPatients().enqueue(new Callback<List<Patient>>() {
            @Override
            public void onResponse(Call<List<Patient>> call, Response<List<Patient>> response) {
                if (response.isSuccessful()) {
                    data.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Patient>> call, Throwable t) {
                data.setValue(null);
            }
        });
        return data;
    }

    public LiveData<Patient> addPatient(Patient patient) {
        MutableLiveData<Patient> data = new MutableLiveData<>();
        apiService.addPatient(patient).enqueue(new Callback<Patient>() {
            @Override
            public void onResponse(Call<Patient> call, Response<Patient> response) {
                data.setValue(response.isSuccessful() ? response.body() : null);
            }

            @Override
            public void onFailure(Call<Patient> call, Throwable t) {
                data.setValue(null);
            }
        });
        return data;
    }

    public void deletePatient(int patientId) {
        apiService.deletePatient(patientId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {}

            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    // ── Care Items ───────────────────────────────────────

    public LiveData<List<CareTask>> getCareItems(int patientId) {
        MutableLiveData<List<CareTask>> data = new MutableLiveData<>();
        apiService.getCareItems(patientId).enqueue(new Callback<List<CareTask>>() {
            @Override
            public void onResponse(Call<List<CareTask>> call, Response<List<CareTask>> response) {
                if (response.isSuccessful()) {
                    data.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<CareTask>> call, Throwable t) {
                data.setValue(null);
            }
        });
        return data;
    }

    public LiveData<List<CareTask>> getAllCareItems() {
        MutableLiveData<List<CareTask>> data = new MutableLiveData<>();
        apiService.getAllCareItems().enqueue(new Callback<List<CareTask>>() {
            @Override
            public void onResponse(Call<List<CareTask>> call, Response<List<CareTask>> response) {
                if (response.isSuccessful()) {
                    data.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<CareTask>> call, Throwable t) {
                data.setValue(null);
            }
        });
        return data;
    }

    public void addTask(CareTask task) {
        apiService.addCareTask(task).enqueue(new Callback<CareTask>() {
            @Override
            public void onResponse(Call<CareTask> call, Response<CareTask> response) {
                // Handle success
            }

            @Override
            public void onFailure(Call<CareTask> call, Throwable t) {
                // Handle failure
            }
        });
    }

    public void updateTaskStatus(int taskId, String newStatus) {
        java.util.HashMap<String, Object> fields = new java.util.HashMap<>();
        fields.put("status", newStatus);
        apiService.patchCareTask(taskId, fields).enqueue(new Callback<CareTask>() {
            @Override
            public void onResponse(Call<CareTask> call, Response<CareTask> response) {}

            @Override
            public void onFailure(Call<CareTask> call, Throwable t) {}
        });
    }

    public void deleteTask(int taskId) {
        apiService.deleteCareTask(taskId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {}

            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }
}
