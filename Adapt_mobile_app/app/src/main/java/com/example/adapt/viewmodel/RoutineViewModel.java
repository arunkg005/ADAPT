package com.example.adapt.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.adapt.data.model.ActivityLog;
import com.example.adapt.data.model.Routine;
import com.example.adapt.data.model.Task;
import com.example.adapt.data.model.TaskLog;
import com.example.adapt.data.network.dto.ApiListResponse;
import com.example.adapt.data.network.dto.BackendAlert;
import com.example.adapt.data.network.dto.BackendDevice;
import com.example.adapt.data.network.dto.BackendPatient;
import com.example.adapt.data.network.dto.BackendTelemetry;
import com.example.adapt.data.network.dto.DeviceCreateRequest;
import com.example.adapt.data.network.dto.EvaluateTelemetryRequest;
import com.example.adapt.data.network.dto.EvaluateTelemetryResponse;
import com.example.adapt.data.network.dto.PatientCreateRequest;
import com.example.adapt.data.network.dto.TelemetryIngestRequest;
import com.example.adapt.data.network.dto.TelemetryIngestResponse;
import com.example.adapt.data.repository.AppRepository;

import java.util.List;

import retrofit2.Call;

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

    public void createRoutinePlan(String title, String description, String scheduledTime) {
        repository.createRoutinePlan(title, description, scheduledTime);
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

    public void updateRoutineProgress(int routineId, int completedSteps) {
        repository.updateRoutineProgress(routineId, completedSteps);
    }

    public LiveData<List<ActivityLog>> getAllLogs() {
        return repository.getAllLogs();
    }

    public void insertActivityLog(ActivityLog log) {
        repository.insertActivityLog(log);
    }

    public Call<ApiListResponse<BackendPatient>> fetchPatients(int limit, int offset) {
        return repository.fetchPatients(limit, offset);
    }

    public Call<BackendPatient> createPatient(PatientCreateRequest request) {
        return repository.createPatient(request);
    }

    public Call<ApiListResponse<BackendAlert>> fetchAlerts(int limit, int offset) {
        return repository.fetchAlerts(limit, offset);
    }

    public Call<BackendAlert> acknowledgeAlert(String alertId) {
        return repository.acknowledgeAlert(alertId);
    }

    public Call<ApiListResponse<BackendDevice>> fetchDevices(int limit, int offset) {
        return repository.fetchDevices(limit, offset);
    }

    public Call<BackendDevice> createDevice(DeviceCreateRequest request) {
        return repository.createDevice(request);
    }

    public Call<TelemetryIngestResponse> submitTelemetry(TelemetryIngestRequest request) {
        return repository.submitTelemetry(request);
    }

    public Call<List<BackendTelemetry>> fetchTelemetryForPatient(String patientId, String signalType, Long limitMs) {
        return repository.fetchTelemetryForPatient(patientId, signalType, limitMs);
    }

    public Call<EvaluateTelemetryResponse> evaluatePatientTelemetry(String patientId, EvaluateTelemetryRequest request) {
        return repository.evaluatePatientTelemetry(patientId, request);
    }
}
