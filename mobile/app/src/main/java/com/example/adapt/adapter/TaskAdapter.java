package com.example.adapt.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapt.R;
import com.example.adapt.databinding.ItemTaskBinding;
import com.example.adapt.model.CareTask;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private final List<CareTask> tasks = new ArrayList<>();

    public void submitList(List<CareTask> nextTasks) {
        tasks.clear();
        if (nextTasks != null) {
            tasks.addAll(nextTasks);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTaskBinding binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(tasks.get(position));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final ItemTaskBinding binding;

        TaskViewHolder(ItemTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CareTask task) {
            binding.taskTitle.setText(task.getTitle());
            binding.taskDescription.setText(task.getDescription());
            binding.taskTime.setText(task.getDueAt() != null ? task.getDueAt() : "No time set");
            binding.taskPriority.setText(task.getPriority());
            binding.taskStatus.setText(task.getStatus());
            
            if ("done".equalsIgnoreCase(task.getStatus())) {
                binding.taskPriority.setBackgroundResource(R.drawable.bg_chip_success);
            } else if ("low".equalsIgnoreCase(task.getPriority())) {
                binding.taskPriority.setBackgroundResource(R.drawable.bg_chip);
            } else {
                binding.taskPriority.setBackgroundResource(R.drawable.bg_chip_accent);
            }
        }
    }
}
