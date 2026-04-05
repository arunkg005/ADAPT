package com.example.adapt.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapt.R;
import com.example.adapt.data.model.Routine;
import com.example.adapt.ui.task.TaskActivity;
import com.example.adapt.utils.PrefManager;
import com.example.adapt.viewmodel.RoutineViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class RoutineFragment extends Fragment {

    private RoutineViewModel viewModel;
    private RoutineAdapter adapter;
    private PrefManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine_list, container, false);

        prefManager = new PrefManager(requireContext());
        RecyclerView rvRoutines = view.findViewById(R.id.rvRoutines);
        rvRoutines.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Check if user is caretaker to enable modification features
        boolean isCaretaker = prefManager.getRole().equals("caregiver");

        adapter = new RoutineAdapter(new RoutineAdapter.OnRoutineClickListener() {
            @Override
            public void onStartClick(Routine routine) {
                Intent intent = new Intent(getActivity(), TaskActivity.class);
                intent.putExtra("ROUTINE_ID", routine.getId());
                intent.putExtra("ROUTINE_TITLE", routine.getTitle());
                startActivity(intent);
            }
        });

        rvRoutines.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(RoutineViewModel.class);
        viewModel.getAllRoutines().observe(getViewLifecycleOwner(), routines -> {
            if (routines != null && !routines.isEmpty()) {
                adapter.setRoutines(routines);
            } else {
                addInitialData();
            }
        });

        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddRoutine);
        if (isCaretaker) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> {
                // Here we would open an AddRoutineActivity
                Toast.makeText(getContext(), "Planning new routine...", Toast.LENGTH_SHORT).show();
            });
        } else {
            fabAdd.setVisibility(View.GONE);
        }

        return view;
    }

    private void addInitialData() {
        viewModel.insertRoutine(new Routine("Morning Routine", "Sequence: Wake up, Meds, Breakfast", "08:00 AM"));
        viewModel.insertRoutine(new Routine("Health Check", "Wearable sync and vitals log", "11:00 AM"));
        viewModel.insertRoutine(new Routine("Evening Wind-down", "Low sensory activity and light stretches", "09:00 PM"));
    }
}
