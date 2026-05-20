package com.example.adapt.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapt.R;
import com.example.adapt.databinding.ItemScheduleBinding;
import com.example.adapt.model.CareTask;

import java.util.ArrayList;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {
    public interface OnScheduleItemClickListener {
        void onScheduleItemClick(CareTask item, int position);
        void onStartTaskClick(CareTask item, int position);
    }

    private final List<CareTask> items = new ArrayList<>();
    private final OnScheduleItemClickListener listener;

    public ScheduleAdapter(OnScheduleItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CareTask> nextItems) {
        items.clear();
        if (nextItems != null) {
            items.addAll(nextItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemScheduleBinding binding = ItemScheduleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ScheduleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private final ItemScheduleBinding binding;

        ScheduleViewHolder(ItemScheduleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CareTask item, int position) {
            binding.scheduleTime.setText(item.getDueAt() != null ? item.getDueAt() : "Anytime");
            binding.scheduleTitle.setText(item.getTitle());
            binding.schedulePatient.setText(item.getDescription()); // Using description as subtitle
            binding.scheduleTypeIcon.setText("+"); // Default icon

            boolean isHighPriority = "high".equalsIgnoreCase(item.getPriority());
            binding.currentTimeLine.setVisibility(isHighPriority ? View.VISIBLE : View.GONE);
            binding.startTaskButton.setVisibility(isHighPriority ? View.VISIBLE : View.GONE);
            
            binding.scheduleCard.setBackgroundResource(isHighPriority
                    ? R.drawable.bg_schedule_card_active
                    : R.drawable.bg_schedule_card_muted);
                    
            if (isHighPriority) {
                binding.scheduleAccent.setBackgroundResource(R.drawable.bg_vertical_accent);
                binding.scheduleTime.setTextColor(binding.getRoot().getContext().getColor(R.color.adapt_accent));
                binding.scheduleTypeIcon.setTextColor(binding.getRoot().getContext().getColor(R.color.adapt_accent));
            } else {
                binding.scheduleAccent.setBackgroundResource(R.drawable.bg_vertical_primary);
                binding.scheduleTime.setTextColor(binding.getRoot().getContext().getColor(R.color.adapt_on_surface));
                binding.scheduleTypeIcon.setTextColor(binding.getRoot().getContext().getColor(R.color.adapt_primary));
            }
            
            binding.scheduleCard.setOnClickListener(view -> listener.onScheduleItemClick(item, position));
            binding.startTaskButton.setOnClickListener(view -> listener.onStartTaskClick(item, position));
        }
    }
}
