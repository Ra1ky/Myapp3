package com.example.sportify;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText etAge, etWeight, etHeight;
    private TextInputEditText etStepGoal, etCalorieGoal, etWaterGoal;
    private TextView tvWaterAutoHint;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        db = SportifyApp.getDatabase();

        bindViews();
        setupBackButton();
        setupWeightWatcher();
        setupSaveButton();
        loadProfile();
    }

    private void bindViews() {
        etAge = findViewById(R.id.etAge);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        etStepGoal = findViewById(R.id.etStepGoal);
        etCalorieGoal = findViewById(R.id.etCalorieGoal);
        etWaterGoal = findViewById(R.id.etWaterGoal);
        tvWaterAutoHint = findViewById(R.id.tvWaterAutoHint);
    }

    private void setupBackButton() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    // When the user changes their weight – automatically update recommended amount of water
    // (35 ml/kg)
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

    private void setupSaveButton() {
        MaterialButton btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveProfile());
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

        // REPLACE strategy – always id = 1
        db.userProfileDAO().insertOrUpdate(profile);

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
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
