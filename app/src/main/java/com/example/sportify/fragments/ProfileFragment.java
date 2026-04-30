package com.example.sportify.fragments;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.sportify.MainActivity;
import com.example.sportify.Prefs;
import com.example.sportify.R;
import com.example.sportify.SportifyApp;
import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;
import com.example.sportify.db.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {
    // Onboarding
    private static final String ARG_ONBOARDING = "onboarding_mode";
    private boolean onboardingMode = false;
    private int onboardingStep = 1;

    // Title
    private TextView tvProfileTitle;

    // Personal data inputs
    private TextInputEditText etAge, etWeight, etHeight;
    private ImageView arrowAge, arrowWeight, arrowHeight;
    private View layoutPersonalDataSection;

    // Daily goals inputs
    private TextInputEditText etStepGoal, etCalorieGoal, etWaterGoal;
    private ImageView arrowStepGoal, arrowCalorieGoal, arrowWaterGoal;
    private TextView tvStepsAutoHint, tvCalorieAutoHint, tvWaterAutoHint;
    private TextView tvOnboardingGoalsHint;
    private View dailyGoalsSection;

    // Diet mode + multiplier slider
    private AutoCompleteTextView spinnerDietMode;
    private AdapterView.OnItemClickListener dietModeListener;
    private LinearLayout layoutMultiplier;
    private com.google.android.material.slider.Slider sliderMultiplier;
    private TextView tvMultiplierValue, tvMultiplierMin, tvMultiplierMax;

    // Re-entrancy guards (avoid feedback loops between watchers/listeners)
    private boolean suppressCalorieWatcher = false;
    private boolean suppressSliderListener = false;

    // Save / lock UI
    private MaterialButton btnDone;
    private MaterialButton btnBack;
    private MaterialButton btnSave;
    private View layoutOnboardingNav;
    private TextView tvLockNotice;

    // Animation
    private ObjectAnimator arrowAnimator;

    // Data
    private AppDatabase db;
    private UserProfile loadedProfile;

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

        if (getArguments() != null) {
            onboardingMode = getArguments().getBoolean(ARG_ONBOARDING, false);
        }

        bindViews(view);

        if (onboardingMode) {
            enterOnboardingMode();
        } else {
            setupDietSpinner();
            setupMultiplierSlider();
            setupAutoHints();
            setupSaveButton();
            loadProfile();
            refreshSaveButtonState();
        }
    }

    private void bindViews(View v) {
        tvProfileTitle = v.findViewById(R.id.tvProfileTitle);

        // Personal data inputs
        etAge = v.findViewById(R.id.etAge);
        etWeight = v.findViewById(R.id.etWeight);
        etHeight = v.findViewById(R.id.etHeight);
        arrowAge = v.findViewById(R.id.arrowAge);
        arrowWeight = v.findViewById(R.id.arrowWeight);
        arrowHeight = v.findViewById(R.id.arrowHeight);
        layoutPersonalDataSection = v.findViewById(R.id.layoutPersonalDataSection);

        // Daily goals inputs
        etStepGoal = v.findViewById(R.id.etStepGoal);
        etCalorieGoal = v.findViewById(R.id.etCalorieGoal);
        etWaterGoal = v.findViewById(R.id.etWaterGoal);
        arrowStepGoal = v.findViewById(R.id.arrowStepGoal);
        arrowCalorieGoal = v.findViewById(R.id.arrowCalorieGoal);
        arrowWaterGoal = v.findViewById(R.id.arrowWaterGoal);
        tvStepsAutoHint = v.findViewById(R.id.tvStepsAutoHint);
        tvCalorieAutoHint = v.findViewById(R.id.tvCalorieAutoHint);
        tvWaterAutoHint = v.findViewById(R.id.tvWaterAutoHint);
        tvOnboardingGoalsHint = v.findViewById(R.id.tvOnboardingGoalsHint);
        dailyGoalsSection = v.findViewById(R.id.layoutDailyGoalsSection);

        // Diet mode + multiplier slider
        spinnerDietMode = v.findViewById(R.id.spinnerDietMode);
        layoutMultiplier = v.findViewById(R.id.layoutMultiplier);
        sliderMultiplier = v.findViewById(R.id.sliderMultiplier);
        tvMultiplierValue = v.findViewById(R.id.tvMultiplierValue);
        tvMultiplierMin = v.findViewById(R.id.tvMultiplierMin);
        tvMultiplierMax = v.findViewById(R.id.tvMultiplierMax);

        // Save / lock UI
        btnDone = v.findViewById(R.id.btnDone);
        btnBack = v.findViewById(R.id.btnBack);
        btnSave = v.findViewById(R.id.btnSave);
        layoutOnboardingNav = v.findViewById(R.id.layoutOnboardingNav);
        tvLockNotice = v.findViewById(R.id.tvLockNotice);
    }

    public static ProfileFragment newOnboardingInstance() {
        ProfileFragment f = new ProfileFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_ONBOARDING, true);
        f.setArguments(args);
        return f;
    }

    private AdapterView.OnItemClickListener createDietModeListener() {
        return (parent, view, position, id) -> {
            configureMultiplierSlider(position);
            updateCalorieGoalFromDiet(position);
        };
    }

    // When the user changes their data, automatically update the recommended steps, calorie, and water hints
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

        // Real-time: re-detect diet mode + reposition slider as the user types calories.
        etCalorieGoal.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!suppressCalorieWatcher) autoDetectDietMode();
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
        spinnerDietMode.setOnItemClickListener(dietModeListener);
    }

    private void configureMultiplierSlider(int dietMode) {
        boolean wasHidden = layoutMultiplier.getVisibility() != View.VISIBLE;
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
                return;
        }

        // Only fade in on the GONE to VISIBLE transition. Switching between Lose
        // and Gain leaves visibility unchanged and shouldn't re-animate.
        if (wasHidden) {
            layoutMultiplier.setAlpha(0f);
            layoutMultiplier.setTranslationY(60f);
            layoutMultiplier.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void updateCalorieGoalFromDiet(int dietMode) {
        float weight = parseFloatSafe(etWeight);
        float height = parseFloatSafe(etHeight);
        int age = parseIntSafe(etAge, 0);

        UserProfile temp = new UserProfile();
        temp.setWeightKg(weight > 0 ? weight : 70f);
        temp.setHeightCm(height > 0 ? height : 170f);
        temp.setAge(age > 0 ? age : 25);

        float multiplier = (dietMode == 0) ? 1.0f : sliderMultiplier.getValue();
        int recommended = (int)(temp.getRecommendedCalories() * multiplier);

        suppressCalorieWatcher = true;
        etCalorieGoal.setText(String.valueOf(recommended));
        suppressCalorieWatcher = false;
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

        UserProfile temp = new UserProfile();
        temp.setWeightKg(weight > 0 ? weight : 70f);
        temp.setHeightCm(height > 0 ? height : 170f);
        temp.setAge(age > 0 ? age : 25);

        suppressCalorieWatcher = true;
        etCalorieGoal.setText(String.valueOf((int)(temp.getRecommendedCalories() * multiplier)));
        suppressCalorieWatcher = false;
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(click -> saveProfile());
    }

    private void loadProfile() {
        UserProfile profile = db.userProfileDAO().getProfile();
        loadedProfile = profile; // snapshot for change detection on save
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

        if (savedDietMode != 0) {
            layoutMultiplier.setVisibility(View.VISIBLE);
        }

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
        UserProfile candidate = buildProfileFromInputs();

        if (!hasChanges(candidate)) {
            Toast.makeText(requireContext(), R.string.profile_no_changes, Toast.LENGTH_SHORT).show();
            return;
        }

        // Real changes — confirm before persisting and locking for 7 days.
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_save_warning_title)
                .setMessage(R.string.profile_save_warning_message)
                .setPositiveButton(R.string.profile_save_warning_yes, (d, w) -> persistProfile(candidate))
                .setNegativeButton(R.string.profile_save_warning_no, null)
                .show();
    }

    // Builds a UserProfile reflecting whatever is currently in the inputs, falling
    // back to recommended/default values when fields are blank.
    private UserProfile buildProfileFromInputs() {
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
        profile.setCaloriesGoal(parseIntSafe(etCalorieGoal, defaultCalories));
        profile.setWaterGoalMl(parseIntSafe(etWaterGoal, defaultWater));

        int dietMode = getDietModePosition();
        profile.setDietMode(dietMode);
        profile.setDietMultiplier(dietMode == 0 ? 1.0f : sliderMultiplier.getValue());
        return profile;
    }

    // Compares a candidate profile to the snapshot loaded from the DB.
    private boolean hasChanges(UserProfile newProfile) {
        if (loadedProfile == null) return true; // never saved before — counts as a change
        return loadedProfile.getAge()           != newProfile.getAge()
                || loadedProfile.getWeightKg()      != newProfile.getWeightKg()
                || loadedProfile.getHeightCm()      != newProfile.getHeightCm()
                || loadedProfile.getStepGoal()      != newProfile.getStepGoal()
                || loadedProfile.getCaloriesGoal()  != newProfile.getCaloriesGoal()
                || loadedProfile.getWaterGoalMl()   != newProfile.getWaterGoalMl()
                || loadedProfile.getDietMode()      != newProfile.getDietMode()
                || Math.abs(loadedProfile.getDietMultiplier() - newProfile.getDietMultiplier()) > 0.001f;
    }

    // Confirmed save: do the DB writes, mirror to today's DailyRecord, start the 7-day lock, and refresh the UI.
    private void persistProfile(UserProfile profile) {
        int savedCalories = profile.getCaloriesGoal();

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

        // Start the 7-day lock and refresh the snapshot so subsequent identical saves don't re-trigger the warning popup.
        Prefs.markSavedNow(requireContext());
        loadedProfile = profile;
        refreshSaveButtonState();

        Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
    }

    // Disables the Save button while the lock is active and surfaces the unlock date.
    private void refreshSaveButtonState() {
        if (Prefs.isProfileLocked(requireContext())) {
            btnSave.setEnabled(false);
            long unlockMs = Prefs.getUnlockMillis(requireContext());
            String dateStr = new SimpleDateFormat("MMMM d", Locale.getDefault()).format(new Date(unlockMs));
            tvLockNotice.setText(getString(R.string.profile_locked_until, dateStr));
            tvLockNotice.setVisibility(View.VISIBLE);
        } else {
            btnSave.setEnabled(true);
            tvLockNotice.setVisibility(View.GONE);
        }
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

    // Step 1: personal data only. Daily-goals section stays hidden until "Next".
    private void enterOnboardingMode() {
        onboardingStep = 1;
        dailyGoalsSection.setVisibility(View.GONE);
        btnSave.setVisibility(View.GONE);
        tvLockNotice.setVisibility(View.GONE);

        layoutOnboardingNav.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.GONE);
        btnDone.setText(R.string.onboarding_next);
        btnDone.setOnClickListener(v -> enterOnboardingStep2());

        setupOnboardingFocusListeners();

        // Default to the age field. requestFocus() will fire the focus listener
        // and showArrowFor will be triggered through that path.
        etAge.requestFocus();
        showArrowFor(arrowAge);
    }

    // Step 2: hide personal data, prefill recommended goals, attach diet/slider/arrows,
    // and rebind the Done button to actually finalize onboarding.
    private void enterOnboardingStep2() {
        onboardingStep = 2;
        stopArrowAnimation();

        layoutPersonalDataSection.setVisibility(View.GONE);
        dailyGoalsSection.setVisibility(View.VISIBLE);
        tvOnboardingGoalsHint.setVisibility(View.VISIBLE);

        btnBack.setVisibility(View.VISIBLE);
        btnBack.setOnClickListener(v -> returnToOnboardingStep1());

        // Wire up the goals-section interactions (diet dropdown, slider, auto hints).
        setupDietSpinner();
        setupMultiplierSlider();
        setupAutoHints();
        updateRecommendations();

        // Prefill goal fields with recommendations derived from step 1's inputs.
        UserProfile temp = new UserProfile();
        temp.setAge(parseIntSafe(etAge, 0));
        temp.setWeightKg(parseFloatSafe(etWeight));
        temp.setHeightCm(parseFloatSafe(etHeight));
        etStepGoal.setText(String.valueOf(temp.getRecommendedSteps()));
        etCalorieGoal.setText(String.valueOf(temp.getRecommendedCalories()));
        int water = temp.getRecommendedWaterMl();
        etWaterGoal.setText(String.valueOf(water > 0 ? water : 2500));

        // Default diet mode to "Maintain" so the slider stays hidden initially.
        spinnerDietMode.setText(spinnerDietMode.getAdapter().getItem(0).toString(), false);

        // Hook focus listeners for the goal-field arrows. The calorie listener also
        // preserves the auto-detect-diet behavior installed by setupAutoHints().
        etStepGoal.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showArrowFor(arrowStepGoal);
        });
        etCalorieGoal.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showArrowFor(arrowCalorieGoal);
        });
        etWaterGoal.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showArrowFor(arrowWaterGoal);
        });

        // Rebind Done to actually finish onboarding now.
        btnDone.setText(R.string.onboarding_done);
        btnDone.setOnClickListener(v -> completeOnboarding());

        // Default focus on the first goal field.
        etStepGoal.requestFocus();
        showArrowFor(arrowStepGoal);

        animateEntrance();
    }

    // Step 2 → step 1: re-show personal data, hide goals + Back, restore Next.
    private void returnToOnboardingStep1() {
        onboardingStep = 1;
        stopArrowAnimation();

        layoutPersonalDataSection.setVisibility(View.VISIBLE);
        dailyGoalsSection.setVisibility(View.GONE);
        tvOnboardingGoalsHint.setVisibility(View.GONE);

        btnBack.setVisibility(View.GONE);
        btnDone.setText(R.string.onboarding_next);
        btnDone.setOnClickListener(v -> enterOnboardingStep2());

        etAge.requestFocus();
        showArrowFor(arrowAge);

        animateEntrance();
    }

    private void setupOnboardingFocusListeners() {
        etAge.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showArrowFor(arrowAge);
        });
        etWeight.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showArrowFor(arrowWeight);
        });
        etHeight.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showArrowFor(arrowHeight);
        });
    }

    // Hides every arrow, then shows + animates the active one.
    private void showArrowFor(ImageView activeArrow) {
        arrowAge.setVisibility(View.INVISIBLE);
        arrowWeight.setVisibility(View.INVISIBLE);
        arrowHeight.setVisibility(View.INVISIBLE);
        arrowStepGoal.setVisibility(View.INVISIBLE);
        arrowCalorieGoal.setVisibility(View.INVISIBLE);
        arrowWaterGoal.setVisibility(View.INVISIBLE);
        stopArrowAnimation();

        activeArrow.setVisibility(View.VISIBLE);
        startArrowAnimation(activeArrow);
    }

    // Builds the final profile from whatever's in the inputs, persists, and shows the wrap-up popup.
    private void completeOnboarding() {
        stopArrowAnimation();

        UserProfile profile = buildProfileFromInputs();
        db.userProfileDAO().insertOrUpdate(profile);

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DailyRecord existing = db.dailyRecordDAO().getByDate(todayDate);
        DailyRecord record = getDailyRecord(existing, todayDate, profile);
        db.dailyRecordDAO().insertOrUpdate(record);

        Prefs.setOnboardingDone(requireContext(), true);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.onboarding_done_title)
                .setMessage(R.string.onboarding_done_message)
                .setCancelable(false)
                .setPositiveButton(R.string.onboarding_done_button, (d, w) -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToDashboard();
                    }
                })
                .show();
    }

    // Subtle lunge toward the active input — repeats forever until canceled.
    private void startArrowAnimation(View arrow) {
        stopArrowAnimation();
        arrowAnimator = ObjectAnimator.ofFloat(arrow, "translationX", 0f, -16f);
        arrowAnimator.setDuration(500);
        arrowAnimator.setRepeatMode(ValueAnimator.REVERSE);
        arrowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        arrowAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        arrowAnimator.start();
    }

    private void stopArrowAnimation() {
        if (arrowAnimator != null) {
            arrowAnimator.cancel();
            arrowAnimator = null;
        }
    }

    private void animateEntrance() {
        List<View> visible = new ArrayList<>();
        addIfVisible(visible, tvProfileTitle);
        addVisibleChildren(visible, layoutPersonalDataSection);
        addVisibleChildren(visible, dailyGoalsSection);
        addIfVisible(visible, layoutOnboardingNav);
        addIfVisible(visible, btnSave);
        addIfVisible(visible, tvLockNotice);

        for (int i = 0; i < visible.size(); i++) {
            View v = visible.get(i);
            v.setAlpha(0f);
            v.setTranslationY(60f);
            v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setStartDelay(i * 70L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void addIfVisible(List<View> out, View v) {
        if (v != null && v.getVisibility() == View.VISIBLE) out.add(v);
    }

    // Adds each visible direct child of `section` to `out`, or nothing if the
    // section itself is hidden. Used so each row inside a section animates
    // individually instead of the whole section sliding in as one block.
    private void addVisibleChildren(List<View> out, View section) {
        if (section == null || section.getVisibility() != View.VISIBLE) return;
        if (!(section instanceof ViewGroup)) {
            out.add(section);
            return;
        }
        ViewGroup group = (ViewGroup) section;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) out.add(child);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        animateEntrance();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopArrowAnimation();
    }
}