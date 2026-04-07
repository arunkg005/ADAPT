package com.example.adapt.ui.dashboard;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapt.R;
import com.example.adapt.data.model.ActivityLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_LOG = 1;

    private final List<ActivityLog> allLogs;
    private final List<Object> displayItems;
    private String activeCategory;
    private String searchQuery;

    public LogAdapter(List<ActivityLog> logs) {
        this.allLogs = new ArrayList<>();
        this.displayItems = new ArrayList<>();
        this.activeCategory = "All Logs";
        this.searchQuery = "";
        setLogs(logs);
    }

    public void setLogs(List<ActivityLog> logs) {
        allLogs.clear();
        if (logs != null) {
            allLogs.addAll(logs);
        }
        applyFilterAndSearch();
    }

    public void filter(String category) {
        activeCategory = category == null ? "All Logs" : category;
        applyFilterAndSearch();
    }

    public void search(String query) {
        searchQuery = query == null ? "" : query.trim();
        applyFilterAndSearch();
    }

    private void applyFilterAndSearch() {
        List<ActivityLog> filteredLogs = new ArrayList<>();
        for (ActivityLog log : allLogs) {
            if (!matchesCategory(log, activeCategory)) {
                continue;
            }

            if (!matchesSearch(log, searchQuery)) {
                continue;
            }

            filteredLogs.add(log);
        }

        Collections.sort(filteredLogs, (left, right) -> {
            String leftPatient = extractPatientLabel(left);
            String rightPatient = extractPatientLabel(right);

            int patientCompare = leftPatient.compareToIgnoreCase(rightPatient);
            if (patientCompare != 0) {
                return patientCompare;
            }

            return Long.compare(right.getTimestamp(), left.getTimestamp());
        });

        displayItems.clear();
        String previousPatient = null;
        for (ActivityLog log : filteredLogs) {
            String patient = extractPatientLabel(log);
            if (previousPatient == null || !previousPatient.equalsIgnoreCase(patient)) {
                displayItems.add("Patient: " + patient);
                previousPatient = patient;
            }

            displayItems.add(log);
        }

        notifyDataSetChanged();
    }

    private boolean matchesCategory(ActivityLog log, String category) {
        if (log == null || category == null || category.equals("All Logs")) {
            return true;
        }

        String normalizedCategory = category.trim();
        if (normalizedCategory.equals("IoT/Sensor") || normalizedCategory.equals("IoT Sensors")) {
            return log.getSource() == ActivityLog.Source.IOT;
        }

        if (normalizedCategory.equals("Task/Routine") || normalizedCategory.equals("Mobile Actions")) {
            String haystack = buildHaystack(log);
            return haystack.contains("task")
                    || haystack.contains("routine")
                    || haystack.contains("medication")
                    || haystack.contains("step")
                    || haystack.contains("assist")
                    || log.getSource() == ActivityLog.Source.MOBILE;
        }

        if (normalizedCategory.equals("Alerts")) {
            return log.getType() == ActivityLog.Type.WARNING || buildHaystack(log).contains("alert");
        }

        return true;
    }

    private boolean matchesSearch(ActivityLog log, String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase(Locale.US);
        return buildHaystack(log).contains(normalizedQuery)
                || extractPatientLabel(log).toLowerCase(Locale.US).contains(normalizedQuery);
    }

    private String buildHaystack(ActivityLog log) {
        return (safe(log.getTitle()) + " " + safe(log.getDescription()) + " " + safe(log.getDetailInfo()))
                .toLowerCase(Locale.US);
    }

    private String extractPatientLabel(ActivityLog log) {
        String detail = safe(log.getDetailInfo());
        if (!detail.isEmpty()) {
            String[] lines = detail.split("\\n");
            for (String line : lines) {
                if (line == null) {
                    continue;
                }

                String trimmed = line.trim();
                if (trimmed.toLowerCase(Locale.US).startsWith("patient:")) {
                    String candidate = trimmed.substring("patient:".length()).trim();
                    if (!candidate.isEmpty()) {
                        return candidate;
                    }
                }
            }
        }

        return "General";
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = displayItems.get(position);
        return item instanceof ActivityLog ? VIEW_TYPE_LOG : VIEW_TYPE_HEADER;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log_patient_header, parent, false);
            return new HeaderViewHolder(view);
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_log, parent, false);
        return new LogItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) displayItems.get(position));
            return;
        }

        ActivityLog log = (ActivityLog) displayItems.get(position);
        ((LogItemViewHolder) holder).bind(log);
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    abstract static class LogViewHolder extends RecyclerView.ViewHolder {

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    class HeaderViewHolder extends LogViewHolder {
        private final TextView tvPatientHeader;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientHeader = itemView.findViewById(R.id.tvPatientHeader);
        }

        void bind(String text) {
            tvPatientHeader.setText(text);
        }
    }

    class LogItemViewHolder extends LogViewHolder {
        TextView tvTitle;
        TextView tvDescription;
        TextView tvTime;
        TextView tvPatient;
        ImageView ivIcon;
        View vStatusIndicator;

        LogItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvLogTitle);
            tvDescription = itemView.findViewById(R.id.tvLogDescription);
            tvTime = itemView.findViewById(R.id.tvLogTime);
            tvPatient = itemView.findViewById(R.id.tvLogPatient);
            ivIcon = itemView.findViewById(R.id.ivLogIcon);
            vStatusIndicator = itemView.findViewById(R.id.vStatusIndicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= displayItems.size()) {
                    return;
                }

                Object item = displayItems.get(position);
                if (!(item instanceof ActivityLog)) {
                    return;
                }
                showDetailsDialog((ActivityLog) item);
            });
        }

        void bind(ActivityLog log) {
            tvTitle.setText(log.getTitle());
            tvDescription.setText(log.getDescription());
            tvPatient.setText("Patient: " + extractPatientLabel(log));
            
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            tvTime.setText(sdf.format(new Date(log.getTimestamp())));

            if (log.getSource() == ActivityLog.Source.IOT) {
                ivIcon.setImageResource(android.R.drawable.ic_menu_compass);
            } else {
                ivIcon.setImageResource(android.R.drawable.ic_menu_edit);
            }

            // Industry standard: Color indicators for different log types
            int statusColor;
            switch (log.getType()) {
                case SUCCESS:
                    statusColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_light);
                    break;
                case WARNING:
                    statusColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_orange_light);
                    break;
                default:
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.primaryBlue);
                    break;
            }
            vStatusIndicator.setBackgroundColor(statusColor);
        }

        private void showDetailsDialog(ActivityLog log) {
            Dialog dialog = new Dialog(itemView.getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_log_details);
            
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            TextView title = dialog.findViewById(R.id.tvDetailTitle);
            TextView source = dialog.findViewById(R.id.tvDetailSource);
            TextView time = dialog.findViewById(R.id.tvDetailTime);
            TextView status = dialog.findViewById(R.id.tvDetailStatus);
            TextView desc = dialog.findViewById(R.id.tvDetailDescription);
            Button close = dialog.findViewById(R.id.btnCloseDialog);

            title.setText(log.getTitle());
            source.setText(log.getSource() == ActivityLog.Source.IOT ? "IoT Sensor (Surveillance/Wearable)" : "Mobile Device Action");
            
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM dd - hh:mm a", Locale.getDefault());
            time.setText(sdf.format(new Date(log.getTimestamp())));
            
            status.setText(log.getType().name());
            desc.setText(log.getDetailInfo());

            close.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }
    }
}
