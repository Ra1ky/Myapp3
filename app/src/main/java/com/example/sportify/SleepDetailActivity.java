package com.example.sportify;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.PermissionController;

import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;
import com.example.sportify.db.DailyRecordDAO;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SleepDetailActivity extends AppCompatActivity {

    private static final Set<String> HC_PERMISSIONS =
            Collections.singleton("android.permission.health.READ_SLEEP");

    private TextView tvSleepDurationDisplay, tvTotalSleepDisplay;
    private MaterialButton btnToggleSleep;
    private TextInputEditText etSleepHours, etSleepMinutes;
    private MaterialButton btnSaveManualSleep;
    private TextView[] moodButtons;
    private View cardTotalSleep, cardTracking, cardManual, cardMood, cardHealthConnect;
    private TextView tvHcStatus;
    private MaterialButton btnSyncHc;

    private ActivityResultLauncher<Set<String>> healthPermLauncher;
    private ActivityResultLauncher<Intent> hcSettingsLauncher;

    private AppDatabase db;
    private DailyRecordDAO recordDao;
    private String todayDate;
    private DailyRecord todayRecord;

    private final List<ObjectAnimator> decorAnimators = new ArrayList<>();
    private ObjectAnimator moodWobbleAnimator;
    private int lastSleepMinutes = 0;

    private boolean isTracking = false;
    private long startTime = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds = seconds % 60;
            minutes = minutes % 60;
            tvSleepDurationDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sleep_detail);

        healthPermLauncher = registerForActivityResult(
                PermissionController.createRequestPermissionResultContract(),
                grantedPerms -> {
                    if (grantedPerms.containsAll(HC_PERMISSIONS)) {
                        syncFromHealthConnect();
                    } else {
                        refreshHcStatus();
                    }
                }
        );

        hcSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (checkSelfPermission("android.permission.health.READ_SLEEP") == PackageManager.PERMISSION_GRANTED) {
                        syncFromHealthConnect();
                    } else {
                        refreshHcStatus();
                    }
                }
        );

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sleepRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        tvSleepDurationDisplay = findViewById(R.id.tvSleepDurationDisplay);
        tvTotalSleepDisplay = findViewById(R.id.tvTotalSleepDisplay);
        btnToggleSleep = findViewById(R.id.btnToggleSleep);
        etSleepHours = findViewById(R.id.etSleepHours);
        etSleepMinutes = findViewById(R.id.etSleepMinutes);
        btnSaveManualSleep = findViewById(R.id.btnSaveManualSleep);
        cardTotalSleep = findViewById(R.id.cardTotalSleep);
        cardTracking = findViewById(R.id.cardTracking);
        cardManual = findViewById(R.id.cardManual);
        cardMood = findViewById(R.id.cardMood);
        cardHealthConnect = findViewById(R.id.cardHealthConnect);
        tvHcStatus = findViewById(R.id.tvHcStatus);
        btnSyncHc = findViewById(R.id.btnSyncHc);

        moodButtons = new TextView[]{
                findViewById(R.id.sleepMood1),
                findViewById(R.id.sleepMood2),
                findViewById(R.id.sleepMood3),
                findViewById(R.id.sleepMood4),
                findViewById(R.id.sleepMood5)
        };

        btnBack.setOnClickListener(v -> finish());

        db = SportifyApp.getDatabase();
        recordDao = db.dailyRecordDAO();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        loadData();
        setupHealthConnectCard();

        btnToggleSleep.setOnClickListener(v -> toggleSleepTracking());
        btnSaveManualSleep.setOnClickListener(v -> saveManualSleep());
        btnSyncHc.setOnClickListener(v -> checkAndSync());

        for (int i = 0; i < moodButtons.length; i++) {
            final int score = i + 1;
            moodButtons[i].setOnClickListener(v -> {
                animateMoodPress(v);
                selectSleepMood(score);
            });
        }

        animateEntrance();
        startDecorAnimations();
    }

    private void animateEntrance() {
        View[] cards = {cardTotalSleep, cardTracking, cardManual, cardMood};
        for (int i = 0; i < cards.length; i++) {
            View v = cards[i];
            if (v != null) {
                v.setAlpha(0f);
                v.setTranslationY(100f);
                v.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(800)
                        .setStartDelay(i * 150)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
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

        ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(v, "rotation", -rotation, rotation);
        rotateAnim.setDuration(duration + 500);
        rotateAnim.setRepeatMode(ValueAnimator.REVERSE);
        rotateAnim.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        rotateAnim.setStartDelay(delay);
        rotateAnim.start();
        decorAnimators.add(rotateAnim);
    }

    private void animateMoodPress(View v) {
        v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(150).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(new OvershootInterpolator()).start()
        ).start();

        ObjectAnimator wobble = ObjectAnimator.ofFloat(v, "rotation",
                0f, -18f, 18f, -12f, 12f, -6f, 6f, 0f);
        wobble.setDuration(500);
        wobble.start();
    }

    private ObjectAnimator startContinuousWobble(View v, float degrees) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, "rotation", -degrees, degrees);
        anim.setDuration(1200);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
        return anim;
    }

    private void updateMoodWobble(int score) {
        if (moodWobbleAnimator != null) {
            Object target = moodWobbleAnimator.getTarget();
            moodWobbleAnimator.cancel();
            if (target instanceof View) ((View) target).setRotation(0f);
            moodWobbleAnimator = null;
        }
        if (score < 1 || score > 5) return;
        moodWobbleAnimator = startContinuousWobble(moodButtons[score - 1], 8f);
    }

    private void loadData() {
        todayRecord = recordDao.getByDate(todayDate);
        if (todayRecord == null) {
            todayRecord = new DailyRecord(todayDate);
            recordDao.insertOrUpdate(todayRecord);
        }
        updateTotalSleepUI();
        updateMoodUI();
    }

    private void updateTotalSleepUI() {
        int totalMinutes = todayRecord.getSleepMinutes();
        ValueAnimator textAnim = ValueAnimator.ofInt(lastSleepMinutes, totalMinutes);
        textAnim.setDuration(1000);
        textAnim.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            tvTotalSleepDisplay.setText(String.format(Locale.getDefault(), "%d h %d min", val / 60, val % 60));
        });
        textAnim.start();
        lastSleepMinutes = totalMinutes;
    }

    private void toggleSleepTracking() {
        if (!isTracking) {
            isTracking = true;
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
            btnToggleSleep.setText("Stop Sleep");
            btnToggleSleep.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.sportify_progress_calories)));
        } else {
            isTracking = false;
            timerHandler.removeCallbacks(timerRunnable);
            int sessionMinutes = (int) ((System.currentTimeMillis() - startTime) / (1000 * 60));
            todayRecord.setSleepMinutes(todayRecord.getSleepMinutes() + sessionMinutes);
            recordDao.insertOrUpdate(todayRecord);
            btnToggleSleep.setText("Start Sleep");
            btnToggleSleep.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.sportify_green)));
            tvSleepDurationDisplay.setText("00:00:00");
            updateTotalSleepUI();
            Toast.makeText(this, "Sleep record updated!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveManualSleep() {
        String hStr = etSleepHours.getText().toString();
        String mStr = etSleepMinutes.getText().toString();
        if (hStr.isEmpty() && mStr.isEmpty()) {
            Toast.makeText(this, "Please enter sleep duration", Toast.LENGTH_SHORT).show();
            return;
        }
        int totalMinutes = (hStr.isEmpty() ? 0 : Integer.parseInt(hStr)) * 60
                + (mStr.isEmpty() ? 0 : Integer.parseInt(mStr));
        todayRecord.setSleepMinutes(totalMinutes);
        recordDao.insertOrUpdate(todayRecord);
        etSleepHours.setText("");
        etSleepMinutes.setText("");
        updateTotalSleepUI();
        Toast.makeText(this, "Manual entry saved!", Toast.LENGTH_SHORT).show();
    }

    private void selectSleepMood(int score) {
        todayRecord.setSleepMood(score);
        recordDao.insertOrUpdate(todayRecord);
        updateMoodUI();
    }

    private void updateMoodUI() {
        int selectedMood = todayRecord.getSleepMood();
        for (int i = 0; i < moodButtons.length; i++) {
            moodButtons[i].setBackgroundResource(i + 1 == selectedMood
                    ? R.drawable.bg_mood_selected
                    : R.drawable.bg_mood_circle);
        }
        updateMoodWobble(selectedMood);
    }

    private void setupHealthConnectCard() {
        if (HealthConnectClient.getSdkStatus(this) != HealthConnectClient.SDK_AVAILABLE) {
            tvHcStatus.setText("Health Connect is not installed.");
            btnSyncHc.setText("Install Health Connect");
            btnSyncHc.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=com.google.android.apps.healthdata")));
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")));
                }
            });
        } else {
            refreshHcStatus();
        }
    }

    private void refreshHcStatus() {
        boolean granted = checkSelfPermission("android.permission.health.READ_SLEEP") == PackageManager.PERMISSION_GRANTED;
        btnSyncHc.setText(granted ? "Sync Sleep" : "Open Health Connect");
        btnSyncHc.setOnClickListener(v -> checkAndSync());
        btnSyncHc.setEnabled(true);
        tvHcStatus.setText(granted ? "Ready to sync last night's sleep." : "Grant sleep access in Health Connect.");
    }

    private void checkAndSync() {
        if (HealthConnectClient.getSdkStatus(this) != HealthConnectClient.SDK_AVAILABLE) return;
        if (checkSelfPermission("android.permission.health.READ_SLEEP") == PackageManager.PERMISSION_GRANTED) {
            syncFromHealthConnect();
            return;
        }
        btnSyncHc.setEnabled(false);
        launchHcPermissionScreen();
    }

    private void launchHcPermissionScreen() {
        Intent[] candidates = {
                new Intent("android.health.connect.action.HEALTH_HOME_SETTINGS"),
                new Intent("androidx.health.ACTION_HEALTH_HOME_SETTINGS"),
        };
        for (Intent intent : candidates) {
            if (getPackageManager().resolveActivity(intent, 0) != null) {
                hcSettingsLauncher.launch(intent);
                return;
            }
        }
        String[] hcPackages = {
                "com.google.android.healthconnect.controller",
                "com.android.healthconnect.controller",
                "com.google.android.apps.healthdata"
        };
        for (String pkg : hcPackages) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch != null) {
                hcSettingsLauncher.launch(launch);
                return;
            }
        }
        hcSettingsLauncher.launch(new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName())));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (HealthConnectClient.getSdkStatus(this) == HealthConnectClient.SDK_AVAILABLE) {
            refreshHcStatus();
        }
    }

    private void syncFromHealthConnect() {
        tvHcStatus.setText("Syncing…");
        SleepRepository.syncLastNight(this, db, todayDate, new SleepRepository.SyncCallback() {
            @Override
            public void onResult(int sleepMinutes) {
                btnSyncHc.setEnabled(true);
                if (sleepMinutes == 0) {
                    tvHcStatus.setText("No sleep session found for last night.");
                } else {
                    int h = sleepMinutes / 60;
                    int m = sleepMinutes % 60;
                    tvHcStatus.setText(String.format(Locale.getDefault(), "Synced: %d h %d min.", h, m));
                }
                loadData();
            }

            @Override
            public void onError(String message) {
                btnSyncHc.setEnabled(true);
                tvHcStatus.setText("Sync failed.");
                Toast.makeText(SleepDetailActivity.this, "Health Connect error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ObjectAnimator anim : decorAnimators) anim.cancel();
        if (moodWobbleAnimator != null) moodWobbleAnimator.cancel();
    }
}
