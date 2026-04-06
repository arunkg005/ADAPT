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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private final List<ActivityLog> allLogs;
    private final List<ActivityLog> filteredLogs;
    private String activeCategory;

    public LogAdapter(List<ActivityLog> logs) {
        this.allLogs = new ArrayList<>();
        this.filteredLogs = new ArrayList<>();
        this.activeCategory = "All Logs";
        setLogs(logs);
    }

    public void setLogs(List<ActivityLog> logs) {
        allLogs.clear();
        if (logs != null) {
            allLogs.addAll(logs);
        }
        applyFilter(activeCategory);
    }

    public void filter(String category) {
        activeCategory = category;
        applyFilter(category);
    }

    private void applyFilter(String category) {
        filteredLogs.clear();
        if (category.equals("All Logs")) {
            filteredLogs.addAll(allLogs);
        } else {
            for (ActivityLog log : allLogs) {
                if (category.equals("IoT Sensors") && log.getSource() == ActivityLog.Source.IOT) {
                    filteredLogs.add(log);
                } else if (category.equals("Mobile Actions") && log.getSource() == ActivityLog.Source.MOBILE) {
                    filteredLogs.add(log);
                } else if (category.equals("Alerts") && log.getType() == ActivityLog.Type.WARNING) {
                    filteredLogs.add(log);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        ActivityLog log = filteredLogs.get(position);
        holder.bind(log);
    }

    @Override
    public int getItemCount() {
        return filteredLogs.size();
    }

    class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvTime;
        ImageView ivIcon;
        View vStatusIndicator;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvLogTitle);
            tvDescription = itemView.findViewById(R.id.tvLogDescription);
            tvTime = itemView.findViewById(R.id.tvLogTime);
            ivIcon = itemView.findViewById(R.id.ivLogIcon);
            vStatusIndicator = itemView.findViewById(R.id.vStatusIndicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= filteredLogs.size()) {
                    return;
                }
                showDetailsDialog(filteredLogs.get(position));
            });
        }

        void bind(ActivityLog log) {
            tvTitle.setText(log.getTitle());
            tvDescription.setText(log.getDescription());
            
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
