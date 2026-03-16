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
    private TextView tvStepsAutoHint, tvCalorieAutoHint, tvWaterAutoHint;
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
        setupAutoHints();
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
        tvStepsAutoHint = v.findViewById(R.id.tvStepsAutoHint);
        tvCalorieAutoHint = v.findViewById(R.id.tvCalorieAutoHint);
        tvWaterAutoHint = v.findViewById(R.id.tvWaterAutoHint);
    }

    // When the user changes their weight or weight – automatically update recommended calorie/water daily intake
    private void setupAutoHints() {
        TextWatcher recalcWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateRecommendations();
            }
        };

        etWeight.addTextChangedListener(recalcWatcher);
        etHeight.addTextChangedListener(recalcWatcher);
        etAge.addTextChangedListener(recalcWatcher);
    }

    private void updateRecommendations() {
        float weight = parseFloatSafe(etWeight);
        float height = parseFloatSafe(etHeight);
        int age = parseIntSafe(etAge, 0);

        String fallback = getString(R.string.profile_no_recommendation);

        // Steps recommendation (age-based)
        int ageVal = parseIntSafe(etAge, 0);
        if (ageVal > 0) {
            // Build a temporary profile to reuse the calculation
            UserProfile temp = new UserProfile();
            temp.setAge(ageVal);
            tvStepsAutoHint.setText(getString(R.string.profile_steps_auto, temp.getRecommendedSteps()));
        } else {
            tvStepsAutoHint.setText(fallback);
        }

        // Calorie recommendation (weight-based + height-based)
        if (weight > 0 && height > 0) {
            UserProfile temp = new UserProfile();
            temp.setWeightKg(weight);
            temp.setHeightCm(height);
            temp.setAge(age > 0 ? age : 25);
            int recommendedCal = temp.getRecommendedCalories();
            tvCalorieAutoHint.setText(getString(R.string.profile_calorie_auto, recommendedCal));
        } else {
            tvCalorieAutoHint.setText(fallback);
        }

        // Water recommendation (weight-based)
        if (weight > 0) {
            int recommendedWater = Math.round(weight * 35);
            tvWaterAutoHint.setText(getString(R.string.profile_water_auto, recommendedWater));
        } else {
            tvWaterAutoHint.setText(fallback);
        }
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
            etWeight.setText(formatDecimal(profile.getWeightKg()));

        if (profile.getHeightCm() > 0)
            etHeight.setText(formatDecimal(profile.getHeightCm()));

        etStepGoal.setText(String.valueOf(profile.getStepGoal()));
        etCalorieGoal.setText(String.valueOf(profile.getCaloriesGoal()));
        etWaterGoal.setText(String.valueOf(profile.getWaterGoalMl()));

        updateRecommendations();
    }

    private void saveProfile() {
        UserProfile profile = new UserProfile();

        profile.setAge(parseIntSafe(etAge, 0));
        profile.setWeightKg(parseFloatSafe(etWeight));
        profile.setHeightCm(parseFloatSafe(etHeight));

        // Use recommended values as defaults when goal fields are left empty
        int defaultSteps = profile.getAge() > 0
                ? profile.getRecommendedSteps() : 10000;
        int defaultCalories = (profile.getWeightKg() > 0 && profile.getHeightCm() > 0)
                ? profile.getRecommendedCalories() : 2000;
        int defaultWater = profile.getWeightKg() > 0
                ? profile.getRecommendedWaterMl() : 2500;

        profile.setStepGoal(parseIntSafe(etStepGoal, defaultSteps));
        profile.setCaloriesGoal(parseIntSafe(etCalorieGoal, defaultCalories));
        profile.setWaterGoalMl(parseIntSafe(etWaterGoal, defaultWater));

        // "REPLACE" strategy – always id = 1
        db.userProfileDAO().insertOrUpdate(profile);

        Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
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

    private String formatDecimal(float value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }
}