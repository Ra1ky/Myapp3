package com.example.sportify.fragments;

import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sportify.R;
import com.example.sportify.SportifyApp;
import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;
import com.example.sportify.db.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextInputEditText etAge, etWeight, etHeight;
    private TextInputEditText etStepGoal, etCalorieGoal, etWaterGoal;
    private TextView tvStepsAutoHint, tvCalorieAutoHint, tvWaterAutoHint;
    private AutoCompleteTextView spinnerDietMode;
    private boolean suppressCalorieWatcher = false;
    private LinearLayout layoutMultiplier;
    private com.google.android.material.slider.Slider sliderMultiplier;
    private TextView tvMultiplierValue, tvMultiplierMin, tvMultiplierMax;
    private boolean suppressSliderListener = false;
    private AdapterView.OnItemClickListener dietModeListener;
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
        setupDietSpinner();
        setupMultiplierSlider();
        setupAutoHints();
        setupSaveButton(view);
        loadProfile();
    }

    private void bindViews(View v) {
        etAge = v.findViewById(R.id.etAge);
        etWeight = v.findViewById(R.id.etWeight);
        etHeight = v.findViewById(R.id.etHeight);
        etStepGoal = v.findViewById(R.id.etStepGoal);
        spinnerDietMode = v.findViewById(R.id.spinnerDietMode);
        layoutMultiplier = v.findViewById(R.id.layoutMultiplier);
        sliderMultiplier = v.findViewById(R.id.sliderMultiplier);
        tvMultiplierValue = v.findViewById(R.id.tvMultiplierValue);
        tvMultiplierMin = v.findViewById(R.id.tvMultiplierMin);
        tvMultiplierMax = v.findViewById(R.id.tvMultiplierMax);
        etCalorieGoal = v.findViewById(R.id.etCalorieGoal);
        etWaterGoal = v.findViewById(R.id.etWaterGoal);
        tvStepsAutoHint = v.findViewById(R.id.tvStepsAutoHint);
        tvCalorieAutoHint = v.findViewById(R.id.tvCalorieAutoHint);
        tvWaterAutoHint = v.findViewById(R.id.tvWaterAutoHint);
    }

    private AdapterView.OnItemClickListener createDietModeListener() {
        return (parent, view, position, id) -> {
            configureMultiplierSlider(position);
            updateCalorieGoalFromDiet(position);
        };
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

        etCalorieGoal.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !suppressCalorieWatcher) {
                autoDetectDietMode();
            }
        });
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
            int recommendedCal = temp.getRecommendedCalories(); // always maintenance
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

    private void autoDetectDietMode() {
        float weight = parseFloatSafe(etWeight);
        float height = parseFloatSafe(etHeight);
        int age = parseIntSafe(etAge, 0);
        int enteredCal = parseIntSafe(etCalorieGoal, 0);

        if (weight <= 0 || height <= 0 || enteredCal <= 0) return;

        UserProfile temp = new UserProfile();
        temp.setWeightKg(weight);
        temp.setHeightCm(height);
        temp.setAge(age > 0 ? age : 25);

        spinnerDietMode.setOnItemSelectedListener(null);
        suppressSliderListener = true;
        suppressCalorieWatcher = true;

        syncDietModeToCalories(enteredCal, temp);

        suppressCalorieWatcher = false;
        suppressSliderListener = false;

        spinnerDietMode.post(() ->
                spinnerDietMode.setOnItemClickListener(dietModeListener)
        );
    }

    private void setupDietSpinner() {
        String[] dietModes = {
                getString(R.string.profile_diet_maintain),
                getString(R.string.profile_diet_lose),
                getString(R.string.profile_diet_gain)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, dietModes);
        spinnerDietMode.setAdapter(adapter);
        dietModeListener = createDietModeListener();
    }

    private void configureMultiplierSlider(int dietMode) {
        switch (dietMode) {
            case 1:
                layoutMultiplier.setVisibility(View.VISIBLE);
                sliderMultiplier.setValueFrom(0.75f);
                sliderMultiplier.setValueTo(0.95f);
                sliderMultiplier.setStepSize(0.01f);
                sliderMultiplier.setValue(0.80f);
                tvMultiplierMin.setText(R.string._0_75);
                tvMultiplierMax.setText(R.string._0_95);
                tvMultiplierValue.setText(R.string._0_80);
                break;
            case 2:
                layoutMultiplier.setVisibility(View.VISIBLE);
                sliderMultiplier.setValueFrom(1.05f);
                sliderMultiplier.setValueTo(1.30f);
                sliderMultiplier.setStepSize(0.01f);
                sliderMultiplier.setValue(1.20f);
                tvMultiplierMin.setText(R.string._1_05);
                tvMultiplierMax.setText(R.string._1_30);
                tvMultiplierValue.setText(R.string._1_20);
                break;
            default:
                layoutMultiplier.setVisibility(View.GONE);
                break;
        }
    }

    private void updateCalorieGoalFromDiet(int dietMode) {
        float weight = parseFloatSafe(etWeight);
        float height = parseFloatSafe(etHeight);
        int age = parseIntSafe(etAge, 0);

        if (weight > 0 && height > 0) {
            UserProfile temp = new UserProfile();
            temp.setWeightKg(weight);
            temp.setHeightCm(height);
            temp.setAge(age > 0 ? age : 25);

            float multiplier = (dietMode == 0) ? 1.0f : sliderMultiplier.getValue();
            int recommended = (int)(temp.getRecommendedCalories() * multiplier);

            suppressCalorieWatcher = true;
            etCalorieGoal.setText(String.valueOf(recommended));
            suppressCalorieWatcher = false;
        }
    }

    private void setupMultiplierSlider() {
        sliderMultiplier.addOnChangeListener((slider, value, fromUser) -> {
            if (suppressSliderListener) return;
            tvMultiplierValue.setText(String.format(Locale.US, "×%.2f", value));
            applyMultiplierToCalorieGoal(value);
        });
    }

    private void applyMultiplierToCalorieGoal(float multiplier) {
        float weight = parseFloatSafe(etWeight);
        float height = parseFloatSafe(etHeight);
        int age = parseIntSafe(etAge, 0);

        if (weight > 0 && height > 0) {
            UserProfile temp = new UserProfile();
            temp.setWeightKg(weight);
            temp.setHeightCm(height);
            temp.setAge(age > 0 ? age : 25);
            int base = temp.getRecommendedCalories();
            int adjusted = (int)(base * multiplier);

            suppressCalorieWatcher = true;
            etCalorieGoal.setText(String.valueOf(adjusted));
            suppressCalorieWatcher = false;
        }
    }

    private void setupSaveButton(View v) {
        MaterialButton btnSave = v.findViewById(R.id.btnSave);
        btnSave.setOnClickListener(click -> saveProfile());
    }

    private void loadProfile() {
        UserProfile profile = db.userProfileDAO().getProfile();
        if (profile == null) {
            updateRecommendations();
            return;
        }

        if (profile.getAge() > 0)
            etAge.setText(String.valueOf(profile.getAge()));
        if (profile.getWeightKg() > 0)
            etWeight.setText(formatDecimal(profile.getWeightKg()));
        if (profile.getHeightCm() > 0)
            etHeight.setText(formatDecimal(profile.getHeightCm()));

        etStepGoal.setText(String.valueOf(profile.getStepGoal()));
        etWaterGoal.setText(String.valueOf(profile.getWaterGoalMl()));

        int savedDietMode = profile.getDietMode();
        float savedMultiplier = profile.getDietMultiplier();
        int savedCalories = profile.getCaloriesGoal();

        etCalorieGoal.post(() -> {
            spinnerDietMode.setOnItemSelectedListener(null);
            suppressSliderListener = true;
            suppressCalorieWatcher = true;

            spinnerDietMode.setText(spinnerDietMode.getAdapter().getItem(savedDietMode).toString(), false);
            configureMultiplierSlider(savedDietMode);
            if (savedDietMode != 0 && savedMultiplier > 0) {
                sliderMultiplier.setValue(savedMultiplier);
                tvMultiplierValue.setText(String.format(Locale.US, "×%.2f", savedMultiplier));
            }

            etCalorieGoal.setText(String.valueOf(savedCalories));

            suppressCalorieWatcher = false;
            suppressSliderListener = false;
            spinnerDietMode.setOnItemClickListener(dietModeListener);
        });

        updateRecommendations();
    }

    private void saveProfile() {
        UserProfile profile = new UserProfile();

        profile.setAge(parseIntSafe(etAge, 0));
        profile.setWeightKg(parseFloatSafe(etWeight));
        profile.setHeightCm(parseFloatSafe(etHeight));

        int defaultSteps = profile.getAge() > 0
                ? profile.getRecommendedSteps() : 10000;
        int defaultCalories = (profile.getWeightKg() > 0 && profile.getHeightCm() > 0)
                ? profile.getRecommendedCalories() : 2000;
        int defaultWater = profile.getWeightKg() > 0
                ? profile.getRecommendedWaterMl() : 2500;

        profile.setStepGoal(parseIntSafe(etStepGoal, defaultSteps));

        // Save exactly what the user typed — never recalculate this
        int savedCalories = parseIntSafe(etCalorieGoal, defaultCalories);
        profile.setCaloriesGoal(savedCalories);

        profile.setWaterGoalMl(parseIntSafe(etWaterGoal, defaultWater));

        spinnerDietMode.setOnItemSelectedListener(null);
        suppressSliderListener = true;
        suppressCalorieWatcher = true;

        syncDietModeToCalories(savedCalories, profile);

        int dietMode = getDietModePosition();
        profile.setDietMode(dietMode);
        profile.setDietMultiplier(dietMode == 0 ? 1.0f : sliderMultiplier.getValue());

        suppressCalorieWatcher = false;
        suppressSliderListener = false;

        spinnerDietMode.post(() ->
                spinnerDietMode.setOnItemClickListener(dietModeListener)
        );

        db.userProfileDAO().insertOrUpdate(profile);

        // Sync goals to today's DailyRecord
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DailyRecord existing = db.dailyRecordDAO().getByDate(todayDate);
        DailyRecord record = getDailyRecord(existing, todayDate, profile);
        db.dailyRecordDAO().insertOrUpdate(record);

        etStepGoal.setText(String.valueOf(profile.getStepGoal()));
        etWaterGoal.setText(String.valueOf(profile.getWaterGoalMl()));

        etCalorieGoal.post(() -> {
            suppressCalorieWatcher = true;
            etCalorieGoal.setText(String.valueOf(savedCalories));
            suppressCalorieWatcher = false;
        });

        Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
    }

    private int getDietModePosition() {
        String current = spinnerDietMode.getText().toString();
        for (int i = 0; i < spinnerDietMode.getAdapter().getCount(); i++) {
            if (spinnerDietMode.getAdapter().getItem(i).toString().equals(current)) return i;
        }
        return 0;
    }

    @NonNull
    private static DailyRecord getDailyRecord(DailyRecord existing, String todayDate, UserProfile profile) {
        DailyRecord record = existing != null ? existing : new DailyRecord(todayDate);

        record.setStepGoal(profile.getStepGoal());
        record.setCaloriesGoal(profile.getCaloriesGoal());
        record.setWaterGoalMl(profile.getWaterGoalMl());

        // Also set macro goals based on calorie goal
        int calGoal = profile.getCaloriesGoal();
        record.setProteinGoal((int)((calGoal * 0.3) / 4));
        record.setCarbsGoal((int)((calGoal * 0.5) / 4));
        record.setFatGoal((int)((calGoal * 0.2) / 9));
        return record;
    }

    private void syncDietModeToCalories(int calories, UserProfile profileForCalc) {
        int maintainCal = profileForCalc.getRecommendedCalories();
        if (maintainCal <= 0) return;

        float ratio = (float) calories / maintainCal;

        if (ratio < 0.95f) {
            spinnerDietMode.setText(spinnerDietMode.getAdapter().getItem(1).toString(), false);
            configureMultiplierSlider(1);
            float clamped = Math.max(0.75f, Math.min(0.95f, ratio));
            float rounded = Math.round(clamped / 0.01f) * 0.01f;
            sliderMultiplier.setValue(rounded);
            tvMultiplierValue.setText(String.format(Locale.US, "×%.2f", rounded));
        } else if (ratio > 1.05f) {
            spinnerDietMode.setText(spinnerDietMode.getAdapter().getItem(2).toString(), false);
            configureMultiplierSlider(2);
            float clamped = Math.max(1.05f, Math.min(1.30f, ratio));
            float rounded = Math.round(clamped / 0.01f) * 0.01f;
            sliderMultiplier.setValue(rounded);
            tvMultiplierValue.setText(String.format(Locale.US, "×%.2f", rounded));
        } else {
            spinnerDietMode.setText(spinnerDietMode.getAdapter().getItem(0).toString(), false);
            layoutMultiplier.setVisibility(View.GONE);
        }
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