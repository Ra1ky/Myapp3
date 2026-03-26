package com.example.sportify;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ProgressBar;
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
import com.example.sportify.db.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WaterDetailActivity extends AppCompatActivity {

    private TextView tvTotalWaterDisplay, tvWaterProgressLabel;
    private ProgressBar pbWater;
    private TextInputEditText etWaterAmount, etWaterGoal;
    private MaterialButton btnSaveWater, btnSaveWaterGoal;

    private AppDatabase db;
    private DailyRecordDAO recordDao;
    private String todayDate;
    private DailyRecord todayRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_water_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.waterRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI
        ImageButton btnBack = findViewById(R.id.btnBack);
        tvTotalWaterDisplay = findViewById(R.id.tvTotalWaterDisplay);
        tvWaterProgressLabel = findViewById(R.id.tvWaterProgressLabel);
        pbWater = findViewById(R.id.pbWater);
        etWaterAmount = findViewById(R.id.etWaterAmount);
        etWaterGoal = findViewById(R.id.etWaterGoal);
        btnSaveWater = findViewById(R.id.btnSaveWater);
        btnSaveWaterGoal = findViewById(R.id.btnSaveWaterGoal);

        btnBack.setOnClickListener(v -> finish());

        // Database setup
        db = SportifyApp.getDatabase();
        recordDao = db.dailyRecordDAO();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        loadData();

        btnSaveWater.setOnClickListener(v -> addWater());
        btnSaveWaterGoal.setOnClickListener(v -> saveGoal());
    }

    private void loadData() {
        todayRecord = recordDao.getByDate(todayDate);
        if (todayRecord == null) {
            todayRecord = new DailyRecord(todayDate);
            todayRecord.setWaterGoalMl(2500); // Default goal
            recordDao.insertOrUpdate(todayRecord);
        }

        updateUI();
    }

    private void updateUI() {
        int waterMl = todayRecord.getWaterMl();
        int goalMl = todayRecord.getWaterGoalMl();

        double waterLiters = waterMl / 1000.0;
        tvTotalWaterDisplay.setText(String.format(Locale.getDefault(), "%.2f L", waterLiters));
        
        tvWaterProgressLabel.setText(String.format(Locale.getDefault(), "%d / %d ml", waterMl, goalMl));
        
        pbWater.setMax(goalMl > 0 ? goalMl : 2500);
        pbWater.setProgress(waterMl);
        
        etWaterGoal.setText(String.valueOf(goalMl));
    }

    private void addWater() {
        String amountStr = etWaterAmount.getText().toString();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int amount = Integer.parseInt(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            todayRecord.setWaterMl(todayRecord.getWaterMl() + amount);
            recordDao.insertOrUpdate(todayRecord);
            
            updateUI();
            etWaterAmount.setText(""); // Clear input
            Toast.makeText(this, "Water added!", Toast.LENGTH_SHORT).show();
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveGoal() {
        String goalStr = etWaterGoal.getText().toString();
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

            todayRecord.setWaterGoalMl(newGoal);
            recordDao.insertOrUpdate(todayRecord);

            // Sync to profile
            UserProfile profile = db.userProfileDAO().getProfile();
            if (profile != null) {
                profile.setWaterGoalMl(newGoal);
                db.userProfileDAO().insertOrUpdate(profile);
            }
            
            updateUI();
            Toast.makeText(this, "Goal updated!", Toast.LENGTH_SHORT).show();
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
        }
    }
}
