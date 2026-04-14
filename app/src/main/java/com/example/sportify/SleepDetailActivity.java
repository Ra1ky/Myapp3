package com.example.sportify;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

public class SleepDetailActivity extends AppCompatActivity {

    private TextView tvSleepDurationDisplay, tvTotalSleepDisplay;
    private MaterialButton btnToggleSleep;
    private TextInputEditText etSleepHours, etSleepMinutes;
    private MaterialButton btnSaveManualSleep;
    private TextView[] moodButtons;

    private AppDatabase db;
    private DailyRecordDAO recordDao;
    private String todayDate;
    private DailyRecord todayRecord;

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sleepRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // UI Initialization
        ImageButton btnBack = findViewById(R.id.btnBack);
        tvSleepDurationDisplay = findViewById(R.id.tvSleepDurationDisplay);
        tvTotalSleepDisplay = findViewById(R.id.tvTotalSleepDisplay);
        btnToggleSleep = findViewById(R.id.btnToggleSleep);
        etSleepHours = findViewById(R.id.etSleepHours);
        etSleepMinutes = findViewById(R.id.etSleepMinutes);
        btnSaveManualSleep = findViewById(R.id.btnSaveManualSleep);

        moodButtons = new TextView[]{
                findViewById(R.id.sleepMood1),
                findViewById(R.id.sleepMood2),
                findViewById(R.id.sleepMood3),
                findViewById(R.id.sleepMood4),
                findViewById(R.id.sleepMood5)
        };

        btnBack.setOnClickListener(v -> finish());

        // Database
        db = SportifyApp.getDatabase();
        recordDao = db.dailyRecordDAO();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        loadData();

        btnToggleSleep.setOnClickListener(v -> toggleSleepTracking());
        btnSaveManualSleep.setOnClickListener(v -> saveManualSleep());

        for (int i = 0; i < moodButtons.length; i++) {
            final int score = i + 1;
            moodButtons[i].setOnClickListener(v -> selectSleepMood(score));
        }
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
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        if (tvTotalSleepDisplay != null) {
            tvTotalSleepDisplay.setText(String.format(Locale.getDefault(), "%d h %d min", h, m));
        }
    }

    private void toggleSleepTracking() {
        if (!isTracking) {
            // Start
            isTracking = true;
            startTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
            btnToggleSleep.setText("Stop Sleep");
            btnToggleSleep.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.sportify_progress_calories)));
        } else {
            // Stop
            isTracking = false;
            timerHandler.removeCallbacks(timerRunnable);
            long millis = System.currentTimeMillis() - startTime;
            int sessionMinutes = (int) (millis / (1000 * 60));

            // ADD session duration to total
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

        int h = hStr.isEmpty() ? 0 : Integer.parseInt(hStr);
        int m = mStr.isEmpty() ? 0 : Integer.parseInt(mStr);
        int totalMinutes = (h * 60) + m;

        // SET total sleep from manual entry
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
            if (i + 1 == selectedMood) {
                moodButtons[i].setBackgroundResource(R.drawable.bg_mood_selected);
            } else {
                moodButtons[i].setBackgroundResource(R.drawable.bg_mood_circle);
            }
        }
    }
}
