package com.example.sportify;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;
import com.example.sportify.db.DailyRecordDAO;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepsDetailActivity extends AppCompatActivity implements SensorEventListener {

    private static final int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 100;

    private TextView tvStepsCount, tvProgressLabel;
    private ProgressBar pbSteps;
    private TextInputEditText etStepGoal, etManualSteps;
    private MaterialButton btnSaveGoal, btnAddManualSteps;

    private AppDatabase db;
    private DailyRecordDAO recordDao;
    private String todayDate;
    private DailyRecord todayRecord;

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private boolean isSensorPresent = false;
    private int initialStepCount = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_steps_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI
        ImageButton btnBack = findViewById(R.id.btnBack);
        tvStepsCount = findViewById(R.id.tvStepsCount);
        tvProgressLabel = findViewById(R.id.tvProgressLabel);
        pbSteps = findViewById(R.id.pbSteps);
        etStepGoal = findViewById(R.id.etStepGoal);
        btnSaveGoal = findViewById(R.id.btnSaveGoal);
        etManualSteps = findViewById(R.id.etManualSteps);
        btnAddManualSteps = findViewById(R.id.btnAddManualSteps);

        btnBack.setOnClickListener(v -> finish());

        // Database setup
        db = SportifyApp.getDatabase();
        recordDao = db.dailyRecordDAO();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        loadData();

        btnSaveGoal.setOnClickListener(v -> saveGoal());
        btnAddManualSteps.setOnClickListener(v -> addManualSteps());

        // Sensor setup
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isSensorPresent = true;
        } else {
            Toast.makeText(this, "Step counter sensor not available on this device", Toast.LENGTH_SHORT).show();
        }

        checkPermission();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSION_REQUEST_ACTIVITY_RECOGNITION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                Toast.makeText(this, "Permission denied for step counting", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSensorPresent) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isSensorPresent) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float totalStepsSinceReboot = event.values[0];
            
            if (initialStepCount == -1) {
                initialStepCount = (int) totalStepsSinceReboot;
            } else {
                int sessionSteps = (int) totalStepsSinceReboot - initialStepCount;
                if (sessionSteps > 0) {
                    // Update database
                    todayRecord.setSteps(todayRecord.getSteps() + sessionSteps);
                    recordDao.insertOrUpdate(todayRecord);
                    
                    // Reset session counter
                    initialStepCount = (int) totalStepsSinceReboot;
                    
                    updateUI();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void loadData() {
        todayRecord = recordDao.getByDate(todayDate);
        if (todayRecord == null) {
            todayRecord = new DailyRecord(todayDate);
            todayRecord.setStepGoal(10000); // Default goal
            recordDao.insertOrUpdate(todayRecord);
        }

        updateUI();
    }

    private void updateUI() {
        int steps = todayRecord.getSteps();
        int goal = todayRecord.getStepGoal();

        tvStepsCount.setText(String.valueOf(steps));
        
        // Using string formatting for better localization support
        String progressText = steps + " / " + goal;
        tvProgressLabel.setText(progressText);

        pbSteps.setMax(goal);
        pbSteps.setProgress(steps);

        etStepGoal.setText(String.valueOf(goal));
    }

    private void saveGoal() {
        String goalStr = etStepGoal.getText().toString();
        if (goalStr.isEmpty()) {
            Toast.makeText(this, "Please enter a goal", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int newGoal = Integer.parseInt(goalStr);
            if (newGoal <= 0) {
                Toast.makeText(this, "Goal must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            todayRecord.setStepGoal(newGoal);
            recordDao.insertOrUpdate(todayRecord);

            updateUI();
            Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
        }
    }

    private void addManualSteps() {
        String manualStr = etManualSteps.getText().toString();
        if (manualStr.isEmpty()) {
            Toast.makeText(this, "Please enter steps to add", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int stepsToAdd = Integer.parseInt(manualStr);
            if (stepsToAdd <= 0) {
                Toast.makeText(this, "Steps must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            todayRecord.setSteps(todayRecord.getSteps() + stepsToAdd);
            recordDao.insertOrUpdate(todayRecord);

            updateUI();
            etManualSteps.setText(""); // Clear input
            Toast.makeText(this, "Steps added!", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
        }
    }
}
