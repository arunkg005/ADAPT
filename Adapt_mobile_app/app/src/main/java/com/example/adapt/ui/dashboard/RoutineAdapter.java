package com.example.adapt.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapt.R;
import com.example.adapt.data.model.Routine;
import com.example.adapt.utils.PrefManager;

import java.util.ArrayList;
import java.util.List;

public class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder> {

    private List<Routine> routines = new ArrayList<>();
    private final OnRoutineClickListener listener;
    private String userRole;

    public interface OnRoutineClickListener {
        void onStartClick(Routine routine);
    }

    public RoutineAdapter(OnRoutineClickListener listener) {
        this.listener = listener;
    }

    public void setRoutines(List<Routine> routines) {
        this.routines = routines;
        notifyDataSetChanged();
    }

    public void setUserRole(String role) {
        this.userRole = role;
    }

    @NonNull
    @Override
    public RoutineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_routine, parent, false);
        
        // Initialize PrefManager here if not passed
        if (userRole == null) {
            PrefManager prefManager = new PrefManager(parent.getContext());
            userRole = prefManager.getRole();
        }
        
        return new RoutineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoutineViewHolder holder, int position) {
        Routine routine = routines.get(position);
        holder.bind(routine, userRole, listener);
    }

    @Override
    public int getItemCount() {
        return routines.size();
    }

    static class RoutineViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvStatus, tvLast;
        Button btnStart;
        ProgressBar miniProgress;
        View caretakerLayout;

        public RoutineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvRoutineTitle);
            tvTime = itemView.findViewById(R.id.tvRoutineTime);
            tvStatus = itemView.findViewById(R.id.tvRoutineStatus);
            tvLast = itemView.findViewById(R.id.tvLastActivity);
            btnStart = itemView.findViewById(R.id.btnStartRoutine);
            miniProgress = itemView.findViewById(R.id.routineMiniProgress);
            caretakerLayout = itemView.findViewById(R.id.caretakerInfoLayout);
        }

        void bind(Routine routine, String role, OnRoutineClickListener listener) {
            tvTitle.setText(routine.getTitle());
            tvTime.setText(routine.getScheduledTime());
            btnStart.setOnClickListener(v -> listener.onStartClick(routine));

            // Support for Executive Function & Generalization (Visual Progress)
            // Show caretaker tracking info for caregiver/admin users.
            if ("caregiver".equals(role) || "admin".equals(role)) {
                caretakerLayout.setVisibility(View.VISIBLE);
                String statusText = "Status: " + routine.getCompletedSteps() + "/" + routine.getTotalSteps() + " Steps";
                tvStatus.setText(statusText);
                
                miniProgress.setMax(routine.getTotalSteps());
                miniProgress.setProgress(routine.getCompletedSteps());
                
                // Mocking "Last Active" for UI demonstration
                tvLast.setText("Updated: Just now");
            } else {
                caretakerLayout.setVisibility(View.GONE);
            }
        }
    }
}
