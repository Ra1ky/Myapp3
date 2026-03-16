package com.example.sportify.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sportify.R;
import com.example.sportify.SportifyApp;
import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileFragment extends Fragment {

    private TextInputEditText etAge, etWeight, etHeight;
    private TextInputEditText etStepGoal, etCalorieGoal, etWaterGoal;
    private TextView tvWaterAutoHint;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = SportifyApp.getDatabase();

        bindViews(view);
        setupWeightWatcher();
        setupSaveButton(view);
        loadProfile();
    }

    private void bindViews(View v) {
        etAge = v.findViewById(R.id.etAge);
        etWeight = v.findViewById(R.id.etWeight);
        etHeight = v.findViewById(R.id.etHeight);
        etStepGoal = v.findViewById(R.id.etStepGoal);
        etCalorieGoal = v.findViewById(R.id.etCalorieGoal);
        etWaterGoal = v.findViewById(R.id.etWaterGoal);
        tvWaterAutoHint = v.findViewById(R.id.tvWaterAutoHint);
    }

    // When the user changes their weight – automatically update recommended amount of water (35 ml/kg)
    private void setupWeightWatcher() {
        etWeight.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    float weight = Float.parseFloat(s.toString());
                    int recommended = Math.round(weight * 35);
                    tvWaterAutoHint.setText(
                            getString(R.string.settings_water_auto, recommended)
                    );
                } catch (NumberFormatException e) {
                    tvWaterAutoHint.setText("");
                }
            }
        });
    }

    private void setupSaveButton(View v) {
        MaterialButton btnSave = v.findViewById(R.id.btnSave);
        btnSave.setOnClickListener(click -> saveProfile());
    }

    private void loadProfile() {
        UserProfile profile = db.userProfileDAO().getProfile();
        if (profile == null) return;

        if (profile.getAge() > 0)
            etAge.setText(String.valueOf(profile.getAge()));

        if (profile.getWeightKg() > 0)
            etWeight.setText(String.valueOf(profile.getWeightKg()));

        if (profile.getHeightCm() > 0)
            etHeight.setText(String.valueOf(profile.getHeightCm()));

        etStepGoal.setText(String.valueOf(profile.getStepGoal()));
        etCalorieGoal.setText(String.valueOf(profile.getCaloriesGoal()));
        etWaterGoal.setText(String.valueOf(profile.getWaterGoalMl()));
    }

    private void saveProfile() {
        UserProfile profile = new UserProfile();

        profile.setAge(parseIntSafe(etAge, 0));
        profile.setWeightKg(parseFloatSafe(etWeight));
        profile.setHeightCm(parseFloatSafe(etHeight));
        profile.setStepGoal(parseIntSafe(etStepGoal, 10000));
        profile.setCaloriesGoal(parseIntSafe(etCalorieGoal, 2000));
        profile.setWaterGoalMl(parseIntSafe(etWaterGoal, 2500));

        // "REPLACE" strategy – always id = 1
        db.userProfileDAO().insertOrUpdate(profile);

        Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }

    private int parseIntSafe(TextInputEditText field, int defaultVal) {
        try {
            String text = field.getText() != null ? field.getText().toString().trim() : "";
            return text.isEmpty() ? defaultVal : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private float parseFloatSafe(TextInputEditText field) {
        try {
            String text = field.getText() != null ? field.getText().toString().trim() : "";
            return text.isEmpty() ? 0f : Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}