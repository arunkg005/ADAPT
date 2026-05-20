package com.example.adapt.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapt.R;
import com.example.adapt.databinding.ItemRoutineBinding;
import com.example.adapt.model.RoutineGroup;
import com.example.adapt.model.RoutineTask;

import java.util.ArrayList;
import java.util.List;

public class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder> {
    public interface OnRoutineHeaderClickListener {
        void onRoutineHeaderClick(RoutineGroup group);
    }

    private final List<RoutineGroup> routines = new ArrayList<>();
    private final OnRoutineHeaderClickListener listener;

    public RoutineAdapter(OnRoutineHeaderClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<RoutineGroup> nextRoutines) {
        routines.clear();
        routines.addAll(nextRoutines);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoutineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRoutineBinding binding = ItemRoutineBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new RoutineViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RoutineViewHolder holder, int position) {
        holder.bind(routines.get(position));
    }

    @Override
    public int getItemCount() {
        return routines.size();
    }

    class RoutineViewHolder extends RecyclerView.ViewHolder {
        private final ItemRoutineBinding binding;

        RoutineViewHolder(ItemRoutineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(RoutineGroup routine) {
            binding.routineTitle.setText(routine.getTitle());
            binding.routineSubtitle.setText(routine.getSubtitle());
            binding.routineExpandIndicator.setText(routine.isExpanded() ? "⌃" : "⌄");
            binding.routineTaskContainer.removeAllViews();
            binding.routineHeader.setOnClickListener(view -> listener.onRoutineHeaderClick(routine));

            if (!routine.isExpanded()) {
                binding.routineTaskContainer.setVisibility(android.view.View.GONE);
                return;
            }
            binding.routineTaskContainer.setVisibility(android.view.View.VISIBLE);
            for (RoutineTask task : routine.getTasks()) {
                CompoundButton row = new Switch(binding.getRoot().getContext());
                row.setText(task.getTitle() + "\n" + task.getDetail());
                row.setTextColor(binding.getRoot().getContext().getColor(R.color.adapt_on_surface));
                row.setTextSize(15);
                row.setChecked(task.isEnabled());
                row.setPadding(8, 12, 8, 12);
                row.setOnCheckedChangeListener((buttonView, isChecked) -> task.setEnabled(isChecked));
                binding.routineTaskContainer.addView(row, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
            }
        }
    }
}
