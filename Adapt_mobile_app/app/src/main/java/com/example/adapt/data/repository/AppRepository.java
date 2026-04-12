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
import com.example.adapt.data.network.NetworkClient;
import com.example.adapt.data.network.dto.AiChatRequest;
import com.example.adapt.data.network.dto.AiChatResponse;
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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;

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
        
        apiService = NetworkClient.getRetrofit(application).create(ApiService.class);
        
        executorService = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<Routine>> getAllRoutines() {
        return routineDao.getAllRoutines();
    }

    public void insertRoutine(Routine routine) {
        executorService.execute(() -> routineDao.insert(routine));
    }

    public void createRoutinePlan(String title, String description, String scheduledTime) {
        executorService.execute(() -> {
            Routine routine = new Routine(title, description, scheduledTime);
            long insertedId = routineDao.insert(routine);
            int routineId = (int) insertedId;

            taskDao.insert(new Task(routineId, "Prepare", "Get ready and clear your task space.", 1));
            taskDao.insert(new Task(routineId, "Follow Main Step", "Complete the key action slowly and carefully.", 2));
            taskDao.insert(new Task(routineId, "Confirm Completion", "Check off the action and review if needed.", 3));

            Routine savedRoutine = routineDao.getRoutineByIdSync(routineId);
            if (savedRoutine != null) {
                savedRoutine.setTotalSteps(3);
                savedRoutine.setCompletedSteps(0);
                savedRoutine.setLastActivityTimestamp(System.currentTimeMillis());
                routineDao.update(savedRoutine);
            }

            ActivityLog log = new ActivityLog(
                    "Routine Created",
                    "New routine plan added",
                    System.currentTimeMillis(),
                    ActivityLog.Source.MOBILE,
                    ActivityLog.Type.INFO,
                    "Routine '" + title + "' scheduled at " + scheduledTime + " with guided starter tasks."
            );
            activityLogDao.insert(log);
        });
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
                int totalSteps = taskDao.countTasksForRoutine(routineId);
                if (totalSteps > 0) {
                    routine.setTotalSteps(totalSteps);
                }

                int safeCompletedSteps = Math.min(completedSteps, Math.max(1, routine.getTotalSteps()));
                routine.setCompletedSteps(safeCompletedSteps);
                routine.setLastActivityTimestamp(System.currentTimeMillis());
                routineDao.update(routine);
                
                // Also create an ActivityLog for this progress
                ActivityLog log = new ActivityLog(
                        "Progress Update: " + routine.getTitle(),
                        "Completed " + safeCompletedSteps + " steps.",
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

    public Call<ApiListResponse<BackendPatient>> fetchPatients(int limit, int offset) {
        return apiService.getPatients(limit, offset);
    }

    public Call<BackendPatient> createPatient(PatientCreateRequest request) {
        return apiService.createPatient(request);
    }

    public Call<ApiListResponse<BackendAlert>> fetchAlerts(int limit, int offset) {
        return apiService.getAlerts(limit, offset);
    }

    public Call<BackendAlert> acknowledgeAlert(String alertId) {
        return apiService.acknowledgeAlert(alertId);
    }

    public Call<ApiListResponse<BackendDevice>> fetchDevices(int limit, int offset) {
        return apiService.getDevices(limit, offset);
    }

    public Call<BackendDevice> createDevice(DeviceCreateRequest request) {
        return apiService.createDevice(request);
    }

    public Call<TelemetryIngestResponse> submitTelemetry(TelemetryIngestRequest request) {
        return apiService.submitTelemetry(request);
    }

    public Call<List<BackendTelemetry>> fetchTelemetryForPatient(
            String patientId,
            String signalType,
            Long limitMs
    ) {
        return apiService.getTelemetryByPatient(patientId, signalType, limitMs);
    }

    public Call<EvaluateTelemetryResponse> evaluatePatientTelemetry(
            String patientId,
            EvaluateTelemetryRequest request
    ) {
        return apiService.evaluateTelemetry(patientId, request);
    }

    public Call<AiChatResponse> chatWithAssistant(AiChatRequest request) {
        return apiService.chatWithAssistant(request);
    }
}
