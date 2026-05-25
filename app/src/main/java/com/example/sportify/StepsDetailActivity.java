package com.example.sportify;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StepsDetailActivity extends AppCompatActivity implements SensorEventListener {

    private TextView tvStepsCount, tvProgressLabel;
    private ProgressBar pbSteps;
    private View stepsCard, manualEntryCard;

    private AppDatabase db;
    private DailyRecordDAO recordDao;
    private String todayDate;
    private DailyRecord todayRecord;
    
    private SensorManager sensorManager;
    private Sensor stepDetectorSensor;
    private Sensor stepCounterSensor;
    
    private int lastStepsValue = 0;
    private final List<ObjectAnimator> decorAnimators = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_steps_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initUI();
        initDatabase();
        initSensors();
        
        animateEntrance();
        startDecorAnimations();
    }

    private void initUI() {
        tvStepsCount = findViewById(R.id.tvStepsCount);
        tvProgressLabel = findViewById(R.id.tvProgressLabel);
        pbSteps = findViewById(R.id.pbSteps);
        stepsCard = findViewById(R.id.stepsCard);
        manualEntryCard = findViewById(R.id.manualEntryCard);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddManualSteps).setOnClickListener(v -> addManualSteps());
    }

    private void initDatabase() {
        db = SportifyApp.getDatabase();
        recordDao = db.dailyRecordDAO();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        loadDataFromDb();
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        
        // STEP_DETECTOR — срабатывает мгновенно на каждый шаг
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        // STEP_COUNTER — для накопленного результата (более точный, но обновляется реже)
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepDetectorSensor == null && stepCounterSensor == null) {
            Toast.makeText(this, "No step sensors found on this device", Toast.LENGTH_LONG).show();
        } else {
            checkPermissionAndRegister();
        }
    }

    private void checkPermissionAndRegister() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 100);
            } else {
                registerSensors();
            }
        } else {
            registerSensors();
        }
    }

    private void registerSensors() {
        if (stepDetectorSensor != null) {
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Мы реагируем на Step Detector для мгновенного обновления UI
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            onStepDetected();
        }
    }

    private void onStepDetected() {
        // 1. Увеличиваем шаги
        int currentSteps = todayRecord.getSteps() + 1;
        todayRecord.setSteps(currentSteps);
        
        // 2. Сохраняем в базу данных в отдельном потоке
        new Thread(() -> recordDao.insertOrUpdate(todayRecord)).start();

        // 3. Обновляем UI
        runOnUiThread(() -> {
            tvStepsCount.setText(String.valueOf(currentSteps));
            animateStepPop();
            updateProgressVisuals();
        });
    }

    private void animateStepPop() {
        // Тактильный отклик (вибрация) при шаге
        tvStepsCount.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        
        // Анимация "прыжка" текста
        tvStepsCount.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(100)
                .withEndAction(() -> tvStepsCount.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                .start();
    }

    private void updateProgressVisuals() {
        int steps = todayRecord.getSteps();
        int goal = todayRecord.getStepGoal();
        tvProgressLabel.setText(steps + " / " + goal);
        
        // Плавное заполнение прогресс-бара
        ObjectAnimator.ofInt(pbSteps, "progress", pbSteps.getProgress(), steps)
                .setDuration(300)
                .start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            registerSensors();
        }
    }

    private void loadDataFromDb() {
        todayRecord = recordDao.getByDate(todayDate);
        if (todayRecord == null) {
            todayRecord = new DailyRecord(todayDate);
            todayRecord.setStepGoal(10000); 
            recordDao.insertOrUpdate(todayRecord);
        }
        updateUIInitial();
    }

    private void updateUIInitial() {
        tvStepsCount.setText(String.valueOf(todayRecord.getSteps()));
        tvProgressLabel.setText(todayRecord.getSteps() + " / " + todayRecord.getStepGoal());
        pbSteps.setMax(todayRecord.getStepGoal());
        pbSteps.setProgress(todayRecord.getSteps());
        lastStepsValue = todayRecord.getSteps();
    }

    private void addManualSteps() {
        String manualStr = ((TextView)findViewById(R.id.etManualSteps)).getText().toString();
        if (manualStr.isEmpty()) return;
        try {
            int addedSteps = Integer.parseInt(manualStr);
            todayRecord.setSteps(todayRecord.getSteps() + addedSteps);
            new Thread(() -> recordDao.insertOrUpdate(todayRecord)).start();
            updateUIInitial();
            ((TextView)findViewById(R.id.etManualSteps)).setText("");
            Toast.makeText(this, "Steps added!", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {}
    }

    private void animateEntrance() {
        if (stepsCard != null) {
            stepsCard.setAlpha(0f);
            stepsCard.setTranslationY(100f);
            stepsCard.animate().alpha(1f).translationY(0f).setDuration(800).setInterpolator(new DecelerateInterpolator()).start();
        }
        if (manualEntryCard != null) {
            manualEntryCard.setAlpha(0f);
            manualEntryCard.setTranslationY(50f);
            manualEntryCard.animate().alpha(1f).translationY(0f).setDuration(800).setStartDelay(300).start();
        }
    }

    private void startDecorAnimations() {
        View decor1 = findViewById(R.id.decorIcon1);
        View decor2 = findViewById(R.id.decorIcon2);
        if (decor1 != null) applyFloatingAnimation(decor1, 3000, 0, 20f, 15f);
        if (decor2 != null) applyFloatingAnimation(decor2, 3500, 500, -25f, 10f);
    }

    private void applyFloatingAnimation(View v, long duration, long delay, float translationY, float rotation) {
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(v, "translationY", -translationY, translationY);
        floatAnim.setDuration(duration);
        floatAnim.setRepeatMode(ValueAnimator.REVERSE);
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        floatAnim.setStartDelay(delay);
        floatAnim.start();
        decorAnimators.add(floatAnim);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensors();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Останавливаем слушатель, когда активность не видна, чтобы не тратить батарею
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ObjectAnimator anim : decorAnimators) anim.cancel();
    }
}
