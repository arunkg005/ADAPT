package com.example.adapt.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CrashRecoveryManager {

    private static final String PREF_NAME = "adapt_prefs";
    private static final String KEY_PENDING_CRASH_REPORT = "pending_crash_report";
    private static final String KEY_PENDING_CRASH_TIMESTAMP = "pending_crash_timestamp";
    private static final int MAX_STACKTRACE_LENGTH = 2800;

    private static boolean installed = false;

    private CrashRecoveryManager() {
    }

    public static synchronized void install(Context context) {
        if (installed) {
            return;
        }

        final Context appContext = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            persistCrash(appContext, thread, throwable);

            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable);
            } else {
                System.exit(2);
            }
        });

        installed = true;
    }

    public static String consumePendingCrashReport(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String payload = prefs.getString(KEY_PENDING_CRASH_REPORT, null);
        long timestamp = prefs.getLong(KEY_PENDING_CRASH_TIMESTAMP, 0L);

        if (TextUtils.isEmpty(payload)) {
            return null;
        }

        prefs.edit()
                .remove(KEY_PENDING_CRASH_REPORT)
                .remove(KEY_PENDING_CRASH_TIMESTAMP)
                .apply();

        String formattedTime = formatTimestamp(timestamp);
        return "Crash time: " + formattedTime + "\n" + payload;
    }

    private static void persistCrash(Context context, Thread thread, Throwable throwable) {
        String threadName = thread == null ? "unknown-thread" : thread.getName();
        String type = throwable == null ? "UnknownError" : throwable.getClass().getSimpleName();
        String message = throwable == null || throwable.getMessage() == null
                ? "No message"
                : throwable.getMessage().trim();

        String stacktrace = throwable == null ? "" : Log.getStackTraceString(throwable);
        if (stacktrace.length() > MAX_STACKTRACE_LENGTH) {
            stacktrace = stacktrace.substring(0, MAX_STACKTRACE_LENGTH) + "...";
        }

        String payload = "Thread: " + threadName
                + "\nType: " + type
                + "\nMessage: " + message
                + "\nStacktrace:\n" + stacktrace;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_PENDING_CRASH_REPORT, payload)
                .putLong(KEY_PENDING_CRASH_TIMESTAMP, System.currentTimeMillis())
                .apply();
    }

    private static String formatTimestamp(long timestamp) {
        if (timestamp <= 0L) {
            return "Unknown";
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return format.format(new Date(timestamp));
    }
}
