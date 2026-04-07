package com.example.adapt.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.adapt.MainActivity;
import com.example.adapt.data.local.ActivityLogDao;
import com.example.adapt.data.local.AppDatabase;
import com.example.adapt.data.local.RoutineDao;
import com.example.adapt.data.model.ActivityLog;
import com.example.adapt.data.model.Routine;
import com.example.adapt.data.network.ApiService;
import com.example.adapt.data.network.NetworkClient;
import com.example.adapt.data.network.dto.AlertCreateRequest;
import com.example.adapt.data.network.dto.BackendAlert;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Response;

public class ReminderWorker extends Worker {

    private static final String REMINDER_CHANNEL_ID = "ADAPT_REMINDER_CHANNEL";
    private static final String ESCALATION_CHANNEL_ID = "ADAPT_TASK_GUARDIAN_ALERT_CHANNEL";

    private static final long MINUTE_MS = 60_000L;
    private static final long PRE_REMINDER_WINDOW_MS = 15L * MINUTE_MS;
    private static final long REMINDER_COOLDOWN_MS = 30L * MINUTE_MS;
    private static final long ESCALATION_THRESHOLD_MS = 90L * MINUTE_MS;
    private static final long ESCALATION_COOLDOWN_MS = 180L * MINUTE_MS;
    private static final long STALL_WINDOW_MS = 45L * MINUTE_MS;
    private static final long MAX_ROUTINE_WINDOW_MS = 12L * 60L * MINUTE_MS;

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        PrefManager prefManager = new PrefManager(context);
        if (!prefManager.isReminderEnabled()) {
            return Result.success();
        }

        AppDatabase database = AppDatabase.getInstance(context);
        RoutineDao routineDao = database.routineDao();
        ActivityLogDao activityLogDao = database.activityLogDao();
        ApiService apiService = NetworkClient.getRetrofit(context).create(ApiService.class);

        List<Routine> routines = routineDao.getAllRoutinesSync();
        if (routines == null || routines.isEmpty()) {
            return Result.success();
        }

        long nowMs = System.currentTimeMillis();
        for (Routine routine : routines) {
            evaluateRoutine(routine, nowMs, prefManager, activityLogDao, apiService);
        }

        return Result.success();
    }

    private void evaluateRoutine(
            Routine routine,
            long nowMs,
            PrefManager prefManager,
            ActivityLogDao activityLogDao,
            ApiService apiService
    ) {
        if (routine == null || routine.getId() <= 0) {
            return;
        }

        long scheduledAtMs = resolveTodayScheduleMs(routine.getScheduledTime(), nowMs);
        if (scheduledAtMs <= 0L) {
            return;
        }

        long sinceScheduledMs = nowMs - scheduledAtMs;
        if (sinceScheduledMs < -PRE_REMINDER_WINDOW_MS || sinceScheduledMs > MAX_ROUTINE_WINDOW_MS) {
            return;
        }

        int totalSteps = Math.max(1, routine.getTotalSteps());
        int completedSteps = Math.max(0, routine.getCompletedSteps());
        if (completedSteps >= totalSteps) {
            return;
        }

        if (sinceScheduledMs >= ESCALATION_THRESHOLD_MS && isRoutineStalled(routine, scheduledAtMs, nowMs)) {
            maybeSendEscalation(routine, nowMs, prefManager, activityLogDao, apiService);
            return;
        }

        maybeSendReminder(routine, nowMs, prefManager, activityLogDao);
    }

    private void maybeSendReminder(
            Routine routine,
            long nowMs,
            PrefManager prefManager,
            ActivityLogDao activityLogDao
    ) {
        long lastReminderTs = prefManager.getLastRoutineReminderTimestamp(routine.getId());
        if (lastReminderTs > 0 && nowMs - lastReminderTs < REMINDER_COOLDOWN_MS) {
            return;
        }

        sendReminderNotification(routine);
        prefManager.setLastRoutineReminderTimestamp(routine.getId(), nowMs);

        ActivityLog log = new ActivityLog(
                "Routine Reminder",
                "Reminder delivered for routine",
                nowMs,
                ActivityLog.Source.MOBILE,
                ActivityLog.Type.INFO,
                "Routine: " + safeRoutineTitle(routine) + "\nScheduled: " + safeScheduledTime(routine)
        );
        activityLogDao.insert(log);
    }

    private void maybeSendEscalation(
            Routine routine,
            long nowMs,
            PrefManager prefManager,
            ActivityLogDao activityLogDao,
            ApiService apiService
    ) {
        long lastEscalationTs = prefManager.getLastRoutineEscalationTimestamp(routine.getId());
        if (lastEscalationTs > 0 && nowMs - lastEscalationTs < ESCALATION_COOLDOWN_MS) {
            return;
        }

        sendEscalationNotification(routine);
        boolean cloudAlertSynced = postCloudAlert(routine, prefManager, apiService);

        prefManager.setLastRoutineEscalationTimestamp(routine.getId(), nowMs);
        prefManager.setLastRoutineReminderTimestamp(routine.getId(), nowMs);

        ActivityLog log = new ActivityLog(
                "Routine Escalated",
                "Routine appears stalled and requires caregiver follow-up",
                nowMs,
                ActivityLog.Source.MOBILE,
                ActivityLog.Type.WARNING,
                "Routine: " + safeRoutineTitle(routine)
                        + "\nCompleted: " + routine.getCompletedSteps() + "/" + Math.max(1, routine.getTotalSteps())
                        + "\nCloud Alert Synced: " + (cloudAlertSynced ? "YES" : "NO")
        );
        activityLogDao.insert(log);
    }

    private boolean postCloudAlert(Routine routine, PrefManager prefManager, ApiService apiService) {
        if (!prefManager.isLoggedIn()) {
            return false;
        }

        String patientId = prefManager.getActivePatientId();
        if (patientId == null || patientId.trim().isEmpty()) {
            return false;
        }

        AlertCreateRequest request = new AlertCreateRequest(
                patientId.trim(),
                safeRoutineTitle(routine),
                "incomplete",
                "HIGH",
                "TASK_NON_COMPLETION",
                "CAREGIVER_CALL"
        );

        try {
            Response<BackendAlert> response = apiService.createAlert(request).execute();
            return response.isSuccessful() && response.body() != null;
        } catch (IOException ignored) {
            return false;
        }
    }

    private boolean isRoutineStalled(Routine routine, long scheduledAtMs, long nowMs) {
        long lastActivityTs = routine.getLastActivityTimestamp();
        if (lastActivityTs <= 0L) {
            return true;
        }

        long referenceTs = Math.max(scheduledAtMs, lastActivityTs);
        return nowMs - referenceTs >= STALL_WINDOW_MS;
    }

    private long resolveTodayScheduleMs(String scheduledTime, long nowMs) {
        Date parsedTime = parseTimeOfDay(scheduledTime);
        if (parsedTime == null) {
            return -1L;
        }

        Calendar timeParts = Calendar.getInstance();
        timeParts.setTime(parsedTime);

        Calendar scheduledToday = Calendar.getInstance();
        scheduledToday.setTimeInMillis(nowMs);
        scheduledToday.set(Calendar.HOUR_OF_DAY, timeParts.get(Calendar.HOUR_OF_DAY));
        scheduledToday.set(Calendar.MINUTE, timeParts.get(Calendar.MINUTE));
        scheduledToday.set(Calendar.SECOND, 0);
        scheduledToday.set(Calendar.MILLISECOND, 0);

        return scheduledToday.getTimeInMillis();
    }

    private Date parseTimeOfDay(String scheduledTime) {
        if (scheduledTime == null || scheduledTime.trim().isEmpty()) {
            return null;
        }

        String normalized = scheduledTime.trim().toUpperCase(Locale.US);
        String[] patterns = new String[]{"hh:mm a", "h:mm a", "HH:mm", "H:mm"};

        for (String pattern : patterns) {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
            format.setLenient(false);
            try {
                return format.parse(normalized);
            } catch (ParseException ignored) {
                // Try the next accepted schedule format.
            }
        }

        return null;
    }

    private void sendReminderNotification(Routine routine) {
        NotificationManager notificationManager = getNotificationManager();
        if (notificationManager == null) {
            return;
        }

        ensureChannels(notificationManager);

        String title = safeRoutineTitle(routine);
        String time = safeScheduledTime(routine);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), REMINDER_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Routine Check-In")
                .setContentText(title + " is due at " + time)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Your routine \"" + title + "\" is due. Open ADAPT to continue guided steps."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(createOpenAppIntent(routine.getId(), false));

        int notificationId = 1_000 + Math.max(0, routine.getId());
        notificationManager.notify(notificationId, builder.build());
    }

    private void sendEscalationNotification(Routine routine) {
        NotificationManager notificationManager = getNotificationManager();
        if (notificationManager == null) {
            return;
        }

        ensureChannels(notificationManager);

        String title = safeRoutineTitle(routine);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), ESCALATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Task Guardian Escalation")
                .setContentText("Routine \"" + title + "\" appears stalled.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Routine \"" + title + "\" has not progressed for an extended period. Caregiver follow-up is recommended."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(createOpenAppIntent(routine.getId(), true));

        int notificationId = 3_000 + Math.max(0, routine.getId());
        notificationManager.notify(notificationId, builder.build());
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void ensureChannels(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel reminderChannel = new NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Routine Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            reminderChannel.setDescription("Routine check-ins and proactive reminders.");

            NotificationChannel escalationChannel = new NotificationChannel(
                    ESCALATION_CHANNEL_ID,
                    "Task Guardian Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            escalationChannel.setDescription("Escalations for stalled routines requiring follow-up.");

            notificationManager.createNotificationChannel(reminderChannel);
            notificationManager.createNotificationChannel(escalationChannel);
        }
    }

    private PendingIntent createOpenAppIntent(int routineId, boolean escalation) {
        Intent openIntent = new Intent(getApplicationContext(), MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openIntent.putExtra("ROUTINE_ID", routineId);
        openIntent.putExtra("TASK_GUARDIAN_ESCALATION", escalation);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        int requestCode = escalation ? 9_000 + routineId : 8_000 + routineId;
        return PendingIntent.getActivity(getApplicationContext(), requestCode, openIntent, flags);
    }

    private String safeRoutineTitle(Routine routine) {
        String title = routine.getTitle();
        return title == null || title.trim().isEmpty() ? "Routine" : title.trim();
    }

    private String safeScheduledTime(Routine routine) {
        String time = routine.getScheduledTime();
        return time == null || time.trim().isEmpty() ? "scheduled time" : time.trim();
    }
}
