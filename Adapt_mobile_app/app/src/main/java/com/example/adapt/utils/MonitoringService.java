package com.example.adapt.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.adapt.data.model.ActivityLog;
import com.example.adapt.data.network.dto.ApiListResponse;
import com.example.adapt.data.network.dto.BackendDevice;
import com.example.adapt.data.network.dto.BackendPatient;
import com.example.adapt.data.network.dto.EvaluateTelemetryRequest;
import com.example.adapt.data.network.dto.EvaluateTelemetryResponse;
import com.example.adapt.data.network.dto.TelemetryIngestRequest;
import com.example.adapt.data.network.dto.TelemetryIngestResponse;
import com.example.adapt.data.repository.AppRepository;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MonitoringService extends Service {

    private static final String CHANNEL_ID = "MonitoringServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long LOOP_INTERVAL_MS = 60_000L;
    private static final int EVALUATE_EVERY_CYCLES = 3;
    private static final int REFRESH_PATIENT_EVERY_CYCLES = 5;
    private static final long TELEMETRY_LOOKBACK_MS = 15 * 60_000L;

    private HandlerThread workerThread;
    private Handler workerHandler;
    private Runnable monitorLoop;

    private AppRepository repository;
    private PrefManager prefManager;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor lightSensor;

    private float lastAccelMagnitude = 0f;
    private float lastAmbientLux = 0f;

    private int cycle = 0;

    private String activePatientId = "";
    private String activePatientName = "Patient";
    private String activePatientRisk = "MEDIUM";

    private boolean patientLookupInFlight = false;
    private boolean devicesLookupInFlight = false;
    private boolean evaluationInFlight = false;

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event == null || event.sensor == null || event.values == null) {
                return;
            }

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && event.values.length >= 3) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                lastAccelMagnitude = (float) Math.sqrt(x * x + y * y + z * z);
            }

            if (event.sensor.getType() == Sensor.TYPE_LIGHT && event.values.length > 0) {
                lastAmbientLux = event.values[0];
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // No-op.
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        repository = new AppRepository(getApplication());
        prefManager = new PrefManager(this);
        restoreActivePatientFromPrefs();

        workerThread = new HandlerThread("adapt-monitoring-worker");
        workerThread.start();
        workerHandler = new Handler(workerThread.getLooper());

        initSensors();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = buildNotification("Initializing cloud diagnostics...");

        startForeground(NOTIFICATION_ID, notification);

        startMonitorLoop();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (workerHandler != null && monitorLoop != null) {
            workerHandler.removeCallbacks(monitorLoop);
        }

        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }

        if (workerThread != null) {
            workerThread.quitSafely();
            workerThread = null;
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startMonitorLoop() {
        if (workerHandler == null || monitorLoop != null) {
            return;
        }

        monitorLoop = new Runnable() {
            @Override
            public void run() {
                executeMonitoringCycle();
                if (workerHandler != null) {
                    workerHandler.postDelayed(this, LOOP_INTERVAL_MS);
                }
            }
        };

        workerHandler.post(monitorLoop);
    }

    private void executeMonitoringCycle() {
        cycle++;

        if (cycle == 1 || cycle % REFRESH_PATIENT_EVERY_CYCLES == 0) {
            ensureActivePatientFromCloud();
        }

        if (activePatientId == null || activePatientId.trim().isEmpty()) {
            restoreActivePatientFromPrefs();
        }

        if (activePatientId == null || activePatientId.trim().isEmpty()) {
            prefManager.setLatestDiagnostic("Waiting for patient assignment from cloud.", "NONE", System.currentTimeMillis());
            updateForegroundStatus("Awaiting patient assignment");
            return;
        }

        refreshConnectedDeviceCount();
        publishTelemetryBatch();

        if (cycle % EVALUATE_EVERY_CYCLES == 0) {
            triggerCognitiveEvaluation();
        }

        updateForegroundStatus(String.format(
                Locale.US,
                "Patient %s | devices %d | cycle %d",
                activePatientName,
                prefManager.getConnectedDevicesCount(),
                cycle
        ));
    }

    private void ensureActivePatientFromCloud() {
        if (patientLookupInFlight) {
            return;
        }

        patientLookupInFlight = true;
        repository.fetchPatients(100, 0).enqueue(new Callback<ApiListResponse<BackendPatient>>() {
            @Override
            public void onResponse(Call<ApiListResponse<BackendPatient>> call, Response<ApiListResponse<BackendPatient>> response) {
                patientLookupInFlight = false;

                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) {
                    return;
                }

                List<BackendPatient> patients = response.body().getData();
                if (patients.isEmpty()) {
                    return;
                }

                BackendPatient selected = patients.get(0);
                activePatientId = safeString(selected.getId(), "");
                activePatientName = formatPatientName(selected);
                activePatientRisk = normalizeRisk(selected.getRiskLevel());

                if (!activePatientId.isEmpty()) {
                    prefManager.setActivePatient(activePatientId, activePatientName, activePatientRisk);
                }
            }

            @Override
            public void onFailure(Call<ApiListResponse<BackendPatient>> call, Throwable t) {
                patientLookupInFlight = false;
            }
        });
    }

    private void refreshConnectedDeviceCount() {
        if (devicesLookupInFlight) {
            return;
        }

        devicesLookupInFlight = true;
        repository.fetchDevices(200, 0).enqueue(new Callback<ApiListResponse<BackendDevice>>() {
            @Override
            public void onResponse(Call<ApiListResponse<BackendDevice>> call, Response<ApiListResponse<BackendDevice>> response) {
                devicesLookupInFlight = false;

                int connected = 0;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    for (BackendDevice device : response.body().getData()) {
                        if (device == null) {
                            continue;
                        }

                        if (device.isOnline() && activePatientId.equals(safeString(device.getPatientId(), ""))) {
                            connected++;
                        }
                    }
                }

                prefManager.setConnectedDevicesCount(connected);
            }

            @Override
            public void onFailure(Call<ApiListResponse<BackendDevice>> call, Throwable t) {
                devicesLookupInFlight = false;
            }
        });
    }

    private void publishTelemetryBatch() {
        long now = System.currentTimeMillis();
        int connectedDevices = prefManager.getConnectedDevicesCount();
        long noResponseDurationMs = lastAccelMagnitude < 1.2f ? LOOP_INTERVAL_MS : LOOP_INTERVAL_MS / 3;

        Map<String, Object> passiveSignals = new HashMap<>();
        passiveSignals.put("reminderDelivered", true);
        passiveSignals.put("motionDetectedAfterPrompt", lastAccelMagnitude >= 1.5f);
        passiveSignals.put("noResponseDurationMs", noResponseDurationMs);
        passiveSignals.put("taskWindowElapsedMs", cycle * LOOP_INTERVAL_MS);

        Map<String, Object> interactionSignals = new HashMap<>();
        interactionSignals.put("singleTapConfirm", false);
        interactionSignals.put("helpRequests", 0);
        interactionSignals.put("instructionReplay", lastAccelMagnitude < 1.0f ? 1 : 0);

        Map<String, Object> progressSignals = new HashMap<>();
        progressSignals.put("currentStepIndex", cycle % 4);
        progressSignals.put("stepCompleted", lastAccelMagnitude >= 1.3f);
        progressSignals.put("idleTimeMs", noResponseDurationMs);

        Map<String, Object> historySignals = new HashMap<>();
        historySignals.put("baselineResponseTimeMs", 15000);
        historySignals.put("recentFailureCount", 0);
        historySignals.put("commonDifficultyType", "NONE");
        historySignals.put("recentHelpFrequency", 0);

        sendTelemetry("passive", passiveSignals, now);
        sendTelemetry("interaction", interactionSignals, now);
        sendTelemetry("progress", progressSignals, now);
        sendTelemetry("history", historySignals, now);
        sendTelemetry("sensor.accelerationMagnitude", lastAccelMagnitude, now);
        sendTelemetry("sensor.ambientLux", lastAmbientLux, now);
        sendTelemetry("iot.connectedDevices", connectedDevices, now);
    }

    private void sendTelemetry(String signalType, Object signalValue, long timestampMs) {
        if (activePatientId == null || activePatientId.trim().isEmpty()) {
            return;
        }

        TelemetryIngestRequest request = new TelemetryIngestRequest(
                activePatientId,
                null,
                signalType,
                signalValue,
                timestampMs
        );

        repository.submitTelemetry(request).enqueue(new Callback<TelemetryIngestResponse>() {
            @Override
            public void onResponse(Call<TelemetryIngestResponse> call, Response<TelemetryIngestResponse> response) {
                // No-op: best effort stream.
            }

            @Override
            public void onFailure(Call<TelemetryIngestResponse> call, Throwable t) {
                // No-op: service keeps running and retries on next cycle.
            }
        });
    }

    private void triggerCognitiveEvaluation() {
        if (evaluationInFlight || activePatientId == null || activePatientId.trim().isEmpty()) {
            return;
        }

        evaluationInFlight = true;
        EvaluateTelemetryRequest.TaskContext taskContext = new EvaluateTelemetryRequest.TaskContext(
                "monitoring-cycle-" + cycle,
                "Background Monitoring Diagnostics",
                "OTHER",
                System.currentTimeMillis() - LOOP_INTERVAL_MS,
                LOOP_INTERVAL_MS * 3,
                1,
                normalizeRisk(activePatientRisk),
                "LOW"
        );

        EvaluateTelemetryRequest request = new EvaluateTelemetryRequest(taskContext, TELEMETRY_LOOKBACK_MS);
        repository.evaluatePatientTelemetry(activePatientId, request).enqueue(new Callback<EvaluateTelemetryResponse>() {
            @Override
            public void onResponse(Call<EvaluateTelemetryResponse> call, Response<EvaluateTelemetryResponse> response) {
                evaluationInFlight = false;

                if (!response.isSuccessful() || response.body() == null) {
                    prefManager.setLatestDiagnostic(
                            "Cloud evaluation failed for this cycle. Monitoring continues.",
                            "UNKNOWN",
                            System.currentTimeMillis()
                    );
                    return;
                }

                EvaluateTelemetryResponse payload = response.body();
                String severity = extractSeverity(payload);
                String summary = String.format(
                        Locale.US,
                        "Severity %s | telemetry %d | alert %s",
                        severity,
                        payload.getTelemetryCount(),
                        payload.isAlertCreated() ? "created" : "not required"
                );

                prefManager.setLatestDiagnostic(summary, severity, System.currentTimeMillis());

                if (payload.isAlertCreated()) {
                    repository.insertActivityLog(new ActivityLog(
                            "Cloud Diagnostic Alert",
                            "Monitoring service triggered an engine alert",
                            System.currentTimeMillis(),
                            ActivityLog.Source.IOT,
                            ActivityLog.Type.WARNING,
                            "Patient: " + activePatientName + "\nSummary: " + summary
                    ));
                }
            }

            @Override
            public void onFailure(Call<EvaluateTelemetryResponse> call, Throwable t) {
                evaluationInFlight = false;
                prefManager.setLatestDiagnostic(
                        "Engine unavailable. Cloud monitoring will retry automatically.",
                        "UNAVAILABLE",
                        System.currentTimeMillis()
                );
            }
        });
    }

    private String extractSeverity(EvaluateTelemetryResponse response) {
        if (response == null) {
            return "NONE";
        }

        JsonElement evaluation = response.getEvaluation();
        if (evaluation != null && evaluation.isJsonObject()) {
            JsonObject object = evaluation.getAsJsonObject();
            if (object.has("severity") && !object.get("severity").isJsonNull()) {
                return safeString(object.get("severity").getAsString(), "NONE").toUpperCase(Locale.US);
            }
        }

        if (response.getAlert() != null && response.getAlert().getSeverity() != null) {
            return safeString(response.getAlert().getSeverity(), "NONE").toUpperCase(Locale.US);
        }

        return "NONE";
    }

    private void restoreActivePatientFromPrefs() {
        activePatientId = safeString(prefManager.getActivePatientId(), "");
        activePatientName = safeString(prefManager.getActivePatientName(), "Patient");
        activePatientRisk = normalizeRisk(prefManager.getActivePatientRisk());
    }

    private String normalizeRisk(String risk) {
        String value = safeString(risk, "MEDIUM").toUpperCase(Locale.US);
        if ("LOW".equals(value) || "MEDIUM".equals(value) || "HIGH".equals(value)) {
            return value;
        }
        return "MEDIUM";
    }

    private String formatPatientName(BackendPatient patient) {
        if (patient == null) {
            return "Patient";
        }

        String first = safeString(patient.getFirstName(), "");
        String last = safeString(patient.getLastName(), "");
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "Patient" : full;
    }

    private String safeString(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private void updateForegroundStatus(String contentText) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        manager.notify(NOTIFICATION_ID, buildNotification(contentText));
    }

    private Notification buildNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ADAPT Monitoring Active")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .build();
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            return;
        }

        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (accelerometerSensor != null) {
            sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (lightSensor != null) {
            sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Monitoring Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
