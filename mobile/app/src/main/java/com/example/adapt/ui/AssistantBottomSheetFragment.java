package com.example.adapt.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adapt.adapter.ChatAdapter;
import com.example.adapt.databinding.BottomSheetAssistantBinding;
import com.example.adapt.viewmodel.AssistantViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AssistantBottomSheetFragment extends BottomSheetDialogFragment {
    private BottomSheetAssistantBinding binding;
    private AssistantViewModel viewModel;
    private ChatAdapter adapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAssistantBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(AssistantViewModel.class);
        
        adapter = new ChatAdapter();
        binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.chatRecyclerView.setAdapter(adapter);

        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.submitList(messages);
            if (!messages.isEmpty()) {
                binding.chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.assistantLoadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        binding.assistantSendButton.setOnClickListener(view -> {
            String text = binding.assistantInputEditText.getText().toString();
            if (!text.isEmpty()) {
                // For now, using a placeholder patientId of 1. 
                // In a real app, this would come from the active patient context.
                viewModel.sendMessage(text, 1);
                binding.assistantInputEditText.setText("");
            }
        });

        binding.closeAssistantButton.setOnClickListener(view -> dismiss());
        
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
