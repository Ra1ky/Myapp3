package com.example.sportify;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StepsDetailActivity extends AppCompatActivity {

    private TextView tvStepsCount, tvProgressLabel;
    private ProgressBar pbSteps;
    private View stepsCard, manualEntryCard;

    private AppDatabase db;
    private DailyRecordDAO recordDao;
    private String todayDate;
    private DailyRecord todayRecord;
    
    private final List<ObjectAnimator> decorAnimators = new ArrayList<>();
    private int lastStepsValue = 0;

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

        // Initialize UI
        ImageButton btnBack = findViewById(R.id.btnBack);
        tvStepsCount = findViewById(R.id.tvStepsCount);
        tvProgressLabel = findViewById(R.id.tvProgressLabel);
        pbSteps = findViewById(R.id.pbSteps);
        
        stepsCard = findViewById(R.id.stepsCard);
        manualEntryCard = findViewById(R.id.manualEntryCard);

        btnBack.setOnClickListener(v -> finish());

        // Database setup
        db = SportifyApp.getDatabase();
        recordDao = db.dailyRecordDAO();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        loadData();

        findViewById(R.id.btnAddManualSteps).setOnClickListener(v -> addManualSteps());
        
        animateEntrance();
        startDecorAnimations();
    }

    private void animateEntrance() {
        if (stepsCard != null) {
            stepsCard.setAlpha(0f);
            stepsCard.setTranslationY(100f);
            stepsCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (manualEntryCard != null) {
            manualEntryCard.setAlpha(0f);
            manualEntryCard.setTranslationY(50f);
            manualEntryCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setStartDelay(300)
                    .start();
        }
    }

    private void startDecorAnimations() {
        View decor1 = findViewById(R.id.decorIcon1);
        View decor2 = findViewById(R.id.decorIcon2);

        if (decor1 != null) {
            applyFloatingAnimation(decor1, 3000, 0, 20f, 15f);
        }
        if (decor2 != null) {
            applyFloatingAnimation(decor2, 3500, 500, -25f, 10f);
        }
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

    private void loadData() {
        todayRecord = recordDao.getByDate(todayDate);
        if (todayRecord == null) {
            todayRecord = new DailyRecord(todayDate);
            todayRecord.setStepGoal(10000); 
            recordDao.insertOrUpdate(todayRecord);
        }

        updateUI();
    }

    private void updateUI() {
        int steps = todayRecord.getSteps();
        int goal = todayRecord.getStepGoal();

        // 1. Count-up animation for the steps number
        ValueAnimator textAnim = ValueAnimator.ofInt(lastStepsValue, steps);
        textAnim.setDuration(1000);
        textAnim.addUpdateListener(animation -> 
            tvStepsCount.setText(animation.getAnimatedValue().toString())
        );
        textAnim.start();
        
        lastStepsValue = steps;
        
        String progressText = steps + " / " + goal;
        tvProgressLabel.setText(progressText);

        pbSteps.setMax(goal);
        
        // 2. Pulse animation for the progress bar
        pbSteps.animate()
                .scaleY(1.4f)
                .setDuration(200)
                .withEndAction(() -> pbSteps.animate().scaleY(1f).setDuration(200).start())
                .start();

        // 3. Smooth progress transition
        ObjectAnimator anim = ObjectAnimator.ofInt(pbSteps, "progress", pbSteps.getProgress(), steps);
        anim.setDuration(1000);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    private void addManualSteps() {
        String manualStr = ((TextView)findViewById(R.id.etManualSteps)).getText().toString();
        if (manualStr.isEmpty()) {
            Toast.makeText(this, "Please enter steps", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int totalSteps = Integer.parseInt(manualStr);
            if (totalSteps < 0) {
                Toast.makeText(this, "Steps cannot be negative", Toast.LENGTH_SHORT).show();
                return;
            }

            todayRecord.setSteps(totalSteps);
            recordDao.insertOrUpdate(todayRecord);

            updateUI();
            ((TextView)findViewById(R.id.etManualSteps)).setText("");
            Toast.makeText(this, "Steps updated!", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ObjectAnimator anim : decorAnimators) {
            anim.cancel();
        }
    }
}
