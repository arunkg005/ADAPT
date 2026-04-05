package com.example.adapt.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.adapt.R;

public class StatusFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container, false);

        Button btnViewDetails = view.findViewById(R.id.btnViewDetails);
        btnViewDetails.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DetailsActivity.class);
            startActivity(intent);
        });

        return view;
    }
}
