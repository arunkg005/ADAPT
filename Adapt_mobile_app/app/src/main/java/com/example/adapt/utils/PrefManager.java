package com.example.adapt.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    private static final String PREF_NAME = "adapt_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_ROUTINE_SEEDED = "routine_seeded";
    private static final String KEY_ACTIVITY_LOG_SEEDED = "activity_log_seeded";
    private static final String KEY_TASK_SEEDED_PREFIX = "task_seeded_";
    private static final String KEY_DARK_MODE_ENABLED = "dark_mode_enabled";
    private static final String KEY_AI_ASSISTANT_ENABLED = "ai_assistant_enabled";
    private static final String KEY_ACTIVE_PATIENT_ID = "active_patient_id";
    private static final String KEY_ACTIVE_PATIENT_NAME = "active_patient_name";
    private static final String KEY_ACTIVE_PATIENT_RISK = "active_patient_risk";
    private static final String KEY_CONNECTED_DEVICES_COUNT = "connected_devices_count";
    private static final String KEY_LATEST_DIAGNOSTIC_SUMMARY = "latest_diagnostic_summary";
    private static final String KEY_LATEST_DIAGNOSTIC_SEVERITY = "latest_diagnostic_severity";
    private static final String KEY_LATEST_DIAGNOSTIC_TIMESTAMP = "latest_diagnostic_timestamp";
    private static final String KEY_ROUTINE_LAST_REMINDER_PREFIX = "routine_last_reminder_";
    private static final String KEY_ROUTINE_LAST_ESCALATION_PREFIX = "routine_last_escalation_";
    private static final String KEY_TASK_LAB_DRAFT_TITLE = "task_lab_draft_title";
    private static final String KEY_TASK_LAB_DRAFT_DESCRIPTION = "task_lab_draft_description";
    private static final String KEY_TASK_LAB_DRAFT_TIME = "task_lab_draft_time";
    private static final String KEY_TASK_LAB_DRAFT_TASK_TYPE = "task_lab_draft_task_type";
    private static final String KEY_TASK_LAB_DRAFT_RISK_LEVEL = "task_lab_draft_risk_level";
    private static final String KEY_TASK_LAB_DRAFT_COMPLEXITY = "task_lab_draft_complexity";
    private static final String KEY_TASK_LAB_DRAFT_TEMPLATE_KEY = "task_lab_draft_template_key";
    private static final String KEY_TASK_LAB_DRAFT_STEPS_JSON = "task_lab_draft_steps_json";
    private static final String KEY_ASSIST_VOICE_GUIDANCE_ENABLED = "assist_voice_guidance_enabled";
    private static final String KEY_ASSIST_LARGE_TEXT_ENABLED = "assist_large_text_enabled";
    private static final String KEY_ASSIST_DOUBLE_CONFIRM_ENABLED = "assist_double_confirm_enabled";
    private static final String KEY_ASSIST_ADAPTIVE_PROMPTS_ENABLED = "assist_adaptive_prompts_enabled";
    private static final String KEY_ROUTINE_RESTRICTIONS_ENABLED_PREFIX = "assist_routine_restrictions_enabled_";
    private static final String KEY_ROUTINE_DOUBLE_CONFIRM_PREFIX = "assist_routine_double_confirm_";
    private static final String KEY_ROUTINE_ALLOW_REPEAT_PREFIX = "assist_routine_allow_repeat_";
    private static final String KEY_ROUTINE_ALLOW_HELP_PREFIX = "assist_routine_allow_help_";

    private final SharedPreferences pref;

    public PrefManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, String role, String name, String email) {
        pref.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .apply();
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public void saveToken(String token) {
        pref.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public void saveRole(String role) {
        pref.edit().putString(KEY_USER_ROLE, role).apply();
    }

    public String getRole() {
        return pref.getString(KEY_USER_ROLE, "patient");
    }

    public void saveUserProfile(String name, String email) {
        pref.edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .apply();
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "ADAPT User");
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "No email linked");
    }

    public void setReminderEnabled(boolean enabled) {
        pref.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply();
    }

    public boolean isReminderEnabled() {
        return pref.getBoolean(KEY_REMINDER_ENABLED, true);
    }

    public void setRoutineSeeded(boolean seeded) {
        pref.edit().putBoolean(KEY_ROUTINE_SEEDED, seeded).apply();
    }

    public boolean isRoutineSeeded() {
        return pref.getBoolean(KEY_ROUTINE_SEEDED, false);
    }

    public void setActivityLogSeeded(boolean seeded) {
        pref.edit().putBoolean(KEY_ACTIVITY_LOG_SEEDED, seeded).apply();
    }

    public boolean isActivityLogSeeded() {
        return pref.getBoolean(KEY_ACTIVITY_LOG_SEEDED, false);
    }

    public void setTaskSeeded(int routineId, boolean seeded) {
        pref.edit().putBoolean(getTaskSeededKey(routineId), seeded).apply();
    }

    public boolean isTaskSeeded(int routineId) {
        return pref.getBoolean(getTaskSeededKey(routineId), false);
    }

    private String getTaskSeededKey(int routineId) {
        return KEY_TASK_SEEDED_PREFIX + routineId;
    }

    public void setDarkModeEnabled(boolean enabled) {
        pref.edit().putBoolean(KEY_DARK_MODE_ENABLED, enabled).apply();
    }

    public boolean isDarkModeEnabled() {
        return pref.getBoolean(KEY_DARK_MODE_ENABLED, false);
    }

    public void setAiAssistantEnabled(boolean enabled) {
        pref.edit().putBoolean(KEY_AI_ASSISTANT_ENABLED, enabled).apply();
    }

    public boolean isAiAssistantEnabled() {
        return pref.getBoolean(KEY_AI_ASSISTANT_ENABLED, true);
    }

    public void setActivePatient(String patientId, String patientName, String patientRisk) {
        pref.edit()
                .putString(KEY_ACTIVE_PATIENT_ID, patientId)
                .putString(KEY_ACTIVE_PATIENT_NAME, patientName)
                .putString(KEY_ACTIVE_PATIENT_RISK, patientRisk)
                .apply();
    }

    public String getActivePatientId() {
        return pref.getString(KEY_ACTIVE_PATIENT_ID, "");
    }

    public String getActivePatientName() {
        return pref.getString(KEY_ACTIVE_PATIENT_NAME, "Patient");
    }

    public String getActivePatientRisk() {
        return pref.getString(KEY_ACTIVE_PATIENT_RISK, "MEDIUM");
    }

    public void setConnectedDevicesCount(int count) {
        pref.edit().putInt(KEY_CONNECTED_DEVICES_COUNT, Math.max(0, count)).apply();
    }

    public int getConnectedDevicesCount() {
        return pref.getInt(KEY_CONNECTED_DEVICES_COUNT, 0);
    }

    public void setLatestDiagnostic(String summary, String severity, long timestampMs) {
        pref.edit()
                .putString(KEY_LATEST_DIAGNOSTIC_SUMMARY, summary)
                .putString(KEY_LATEST_DIAGNOSTIC_SEVERITY, severity)
                .putLong(KEY_LATEST_DIAGNOSTIC_TIMESTAMP, timestampMs)
                .apply();
    }

    public String getLatestDiagnosticSummary() {
        return pref.getString(KEY_LATEST_DIAGNOSTIC_SUMMARY, "No diagnostic data yet.");
    }

    public String getLatestDiagnosticSeverity() {
        return pref.getString(KEY_LATEST_DIAGNOSTIC_SEVERITY, "NONE");
    }

    public long getLatestDiagnosticTimestamp() {
        return pref.getLong(KEY_LATEST_DIAGNOSTIC_TIMESTAMP, 0L);
    }

    public void setLastRoutineReminderTimestamp(int routineId, long timestampMs) {
        pref.edit().putLong(KEY_ROUTINE_LAST_REMINDER_PREFIX + routineId, timestampMs).apply();
    }

    public long getLastRoutineReminderTimestamp(int routineId) {
        return pref.getLong(KEY_ROUTINE_LAST_REMINDER_PREFIX + routineId, 0L);
    }

    public void setLastRoutineEscalationTimestamp(int routineId, long timestampMs) {
        pref.edit().putLong(KEY_ROUTINE_LAST_ESCALATION_PREFIX + routineId, timestampMs).apply();
    }

    public long getLastRoutineEscalationTimestamp(int routineId) {
        return pref.getLong(KEY_ROUTINE_LAST_ESCALATION_PREFIX + routineId, 0L);
    }

    public void saveTaskLabDraft(String title, String description, String scheduledTime) {
        saveTaskLabDraft(
            title,
            description,
            scheduledTime,
            "OTHER",
            "MEDIUM",
            "MEDIUM",
            null,
            ""
        );
        }

        public void saveTaskLabDraft(
            String title,
            String description,
            String scheduledTime,
            String taskType,
            String riskLevel,
            String complexity,
            String templateKey,
            String stepsJson
        ) {
        pref.edit()
                .putString(KEY_TASK_LAB_DRAFT_TITLE, title)
                .putString(KEY_TASK_LAB_DRAFT_DESCRIPTION, description)
                .putString(KEY_TASK_LAB_DRAFT_TIME, scheduledTime)
            .putString(KEY_TASK_LAB_DRAFT_TASK_TYPE, taskType)
            .putString(KEY_TASK_LAB_DRAFT_RISK_LEVEL, riskLevel)
            .putString(KEY_TASK_LAB_DRAFT_COMPLEXITY, complexity)
            .putString(KEY_TASK_LAB_DRAFT_TEMPLATE_KEY, templateKey)
            .putString(KEY_TASK_LAB_DRAFT_STEPS_JSON, stepsJson)
                .apply();
    }

    public boolean hasTaskLabDraft() {
        String title = getTaskLabDraftTitle();
        return title != null && !title.trim().isEmpty();
    }

    public String getTaskLabDraftTitle() {
        return pref.getString(KEY_TASK_LAB_DRAFT_TITLE, "");
    }

    public String getTaskLabDraftDescription() {
        return pref.getString(KEY_TASK_LAB_DRAFT_DESCRIPTION, "");
    }

    public String getTaskLabDraftTime() {
        return pref.getString(KEY_TASK_LAB_DRAFT_TIME, "09:00 AM");
    }

    public String getTaskLabDraftTaskType() {
        return pref.getString(KEY_TASK_LAB_DRAFT_TASK_TYPE, "OTHER");
    }

    public String getTaskLabDraftRiskLevel() {
        return pref.getString(KEY_TASK_LAB_DRAFT_RISK_LEVEL, "MEDIUM");
    }

    public String getTaskLabDraftComplexity() {
        return pref.getString(KEY_TASK_LAB_DRAFT_COMPLEXITY, "MEDIUM");
    }

    public String getTaskLabDraftTemplateKey() {
        return pref.getString(KEY_TASK_LAB_DRAFT_TEMPLATE_KEY, null);
    }

    public String getTaskLabDraftStepsJson() {
        return pref.getString(KEY_TASK_LAB_DRAFT_STEPS_JSON, "");
    }

    public void clearTaskLabDraft() {
        pref.edit()
                .remove(KEY_TASK_LAB_DRAFT_TITLE)
                .remove(KEY_TASK_LAB_DRAFT_DESCRIPTION)
                .remove(KEY_TASK_LAB_DRAFT_TIME)
                .remove(KEY_TASK_LAB_DRAFT_TASK_TYPE)
                .remove(KEY_TASK_LAB_DRAFT_RISK_LEVEL)
                .remove(KEY_TASK_LAB_DRAFT_COMPLEXITY)
                .remove(KEY_TASK_LAB_DRAFT_TEMPLATE_KEY)
                .remove(KEY_TASK_LAB_DRAFT_STEPS_JSON)
                .apply();
    }

    public void setAssistVoiceGuidanceEnabled(boolean enabled) {
        pref.edit().putBoolean(KEY_ASSIST_VOICE_GUIDANCE_ENABLED, enabled).apply();
    }

    public boolean isAssistVoiceGuidanceEnabled() {
        return pref.getBoolean(KEY_ASSIST_VOICE_GUIDANCE_ENABLED, true);
    }

    public void setAssistLargeTextEnabled(boolean enabled) {
        pref.edit().putBoolean(KEY_ASSIST_LARGE_TEXT_ENABLED, enabled).apply();
    }

    public boolean isAssistLargeTextEnabled() {
        return pref.getBoolean(KEY_ASSIST_LARGE_TEXT_ENABLED, true);
    }

    public void setAssistDoubleConfirmEnabled(boolean enabled) {
        pref.edit().putBoolean(KEY_ASSIST_DOUBLE_CONFIRM_ENABLED, enabled).apply();
    }

    public boolean isAssistDoubleConfirmEnabled() {
        return pref.getBoolean(KEY_ASSIST_DOUBLE_CONFIRM_ENABLED, true);
    }

    public void setAssistAdaptivePromptsEnabled(boolean enabled) {
        pref.edit().putBoolean(KEY_ASSIST_ADAPTIVE_PROMPTS_ENABLED, enabled).apply();
    }

    public boolean isAssistAdaptivePromptsEnabled() {
        return pref.getBoolean(KEY_ASSIST_ADAPTIVE_PROMPTS_ENABLED, true);
    }

    public void setRoutineRestrictions(int routineId, boolean requireDoubleConfirm, boolean allowRepeat, boolean allowHelp) {
        pref.edit()
                .putBoolean(KEY_ROUTINE_RESTRICTIONS_ENABLED_PREFIX + routineId, true)
                .putBoolean(KEY_ROUTINE_DOUBLE_CONFIRM_PREFIX + routineId, requireDoubleConfirm)
                .putBoolean(KEY_ROUTINE_ALLOW_REPEAT_PREFIX + routineId, allowRepeat)
                .putBoolean(KEY_ROUTINE_ALLOW_HELP_PREFIX + routineId, allowHelp)
                .apply();
    }

    public boolean hasRoutineRestrictions(int routineId) {
        return pref.getBoolean(KEY_ROUTINE_RESTRICTIONS_ENABLED_PREFIX + routineId, false);
    }

    public boolean getRoutineDoubleConfirmRequired(int routineId) {
        if (hasRoutineRestrictions(routineId)) {
            return pref.getBoolean(KEY_ROUTINE_DOUBLE_CONFIRM_PREFIX + routineId, isAssistDoubleConfirmEnabled());
        }

        return isAssistDoubleConfirmEnabled();
    }

    public boolean isRoutineRepeatAllowed(int routineId) {
        if (hasRoutineRestrictions(routineId)) {
            return pref.getBoolean(KEY_ROUTINE_ALLOW_REPEAT_PREFIX + routineId, true);
        }

        return true;
    }

    public boolean isRoutineHelpAllowed(int routineId) {
        if (hasRoutineRestrictions(routineId)) {
            return pref.getBoolean(KEY_ROUTINE_ALLOW_HELP_PREFIX + routineId, true);
        }

        return true;
    }

    public void clearRoutineRestrictions(int routineId) {
        pref.edit()
                .remove(KEY_ROUTINE_RESTRICTIONS_ENABLED_PREFIX + routineId)
                .remove(KEY_ROUTINE_DOUBLE_CONFIRM_PREFIX + routineId)
                .remove(KEY_ROUTINE_ALLOW_REPEAT_PREFIX + routineId)
                .remove(KEY_ROUTINE_ALLOW_HELP_PREFIX + routineId)
                .apply();
    }

    public void clear() {
        pref.edit().clear().apply();
    }
}
