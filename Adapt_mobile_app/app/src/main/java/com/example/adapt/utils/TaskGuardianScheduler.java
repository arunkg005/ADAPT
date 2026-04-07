package com.example.adapt.utils;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class TaskGuardianScheduler {

    private static final String PERIODIC_WORK_NAME = "adapt_task_guardian_periodic";
    private static final String IMMEDIATE_WORK_NAME = "adapt_task_guardian_bootstrap";

    private TaskGuardianScheduler() {
    }

    public static void ensureScheduled(Context context) {
        Context appContext = context.getApplicationContext();
        WorkManager workManager = WorkManager.getInstance(appContext);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
                ReminderWorker.class,
                15,
                TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .addTag(PERIODIC_WORK_NAME)
                .build();

        workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
        );

        OneTimeWorkRequest bootstrapRequest = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setConstraints(constraints)
                .addTag(IMMEDIATE_WORK_NAME)
                .build();

        workManager.enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                bootstrapRequest
        );
    }

    public static void cancelScheduled(Context context) {
        Context appContext = context.getApplicationContext();
        WorkManager workManager = WorkManager.getInstance(appContext);
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME);
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME);
    }
}
