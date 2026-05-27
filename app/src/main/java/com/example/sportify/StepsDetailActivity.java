package com.example.sportify;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
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
    private View stepsCard, manualEntryCard, cardWalkPlanner;

    private AppDatabase db;
    private DailyRecordDAO recordDao;
    private String todayDate;
    private DailyRecord todayRecord;
    
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
        
        animateEntrance();
        startDecorAnimations();
    }

    private void initUI() {
        tvStepsCount = findViewById(R.id.tvStepsCount);
        tvProgressLabel = findViewById(R.id.tvProgressLabel);
        pbSteps = findViewById(R.id.pbSteps);
        stepsCard = findViewById(R.id.stepsCard);
        manualEntryCard = findViewById(R.id.manualEntryCard);
        cardWalkPlanner = findViewById(R.id.cardWalkPlanner);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddManualSteps).setOnClickListener(v -> addManualSteps());
        
        if (cardWalkPlanner != null) {
            cardWalkPlanner.setOnClickListener(v -> {
                Intent intent = new Intent(StepsDetailActivity.this, WalkPlannerActivity.class);
                startActivity(intent);
            });
        }
    }

    private void initDatabase() {
        db = SportifyApp.getDatabase();
        recordDao = db.dailyRecordDAO();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        loadDataFromDb();
    }

    private void loadDataFromDb() {
        new Thread(() -> {
            todayRecord = recordDao.getByDate(todayDate);
            if (todayRecord == null) {
                todayRecord = new DailyRecord(todayDate);
                todayRecord.setStepGoal(10000); 
                recordDao.insertOrUpdate(todayRecord);
            }
            runOnUiThread(this::updateUI);
        }).start();
    }

    private void updateUI() {
        int steps = todayRecord.getSteps();
        int goal = todayRecord.getStepGoal();

        ValueAnimator textAnim = ValueAnimator.ofInt(lastStepsValue, steps);
        textAnim.setDuration(1000);
        textAnim.addUpdateListener(animation -> 
            tvStepsCount.setText(animation.getAnimatedValue().toString())
        );
        textAnim.start();
        
        lastStepsValue = steps;
        
        tvProgressLabel.setText(String.format(Locale.getDefault(), "%d / %d", steps, goal));
        pbSteps.setMax(goal > 0 ? goal : 10000);
        
        ObjectAnimator anim = ObjectAnimator.ofInt(pbSteps, "progress", pbSteps.getProgress(), steps);
        anim.setDuration(1000);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
        
        pbSteps.animate().scaleY(1.2f).setDuration(200).withEndAction(() -> 
            pbSteps.animate().scaleY(1f).setDuration(200).start()
        ).start();
    }

    private void addManualSteps() {
        String manualStr = ((TextView)findViewById(R.id.etManualSteps)).getText().toString();
        if (manualStr.isEmpty()) return;
        try {
            int newStepsValue = Integer.parseInt(manualStr);
            todayRecord.setSteps(newStepsValue);
            new Thread(() -> recordDao.insertOrUpdate(todayRecord)).start();
            updateUI();
            ((TextView)findViewById(R.id.etManualSteps)).setText("");
            Toast.makeText(this, "Steps updated!", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {}
    }

    private void animateEntrance() {
        View[] cards = {stepsCard, cardWalkPlanner, manualEntryCard};
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
        rotateAnim.setDuration(duration + 800);
        rotateAnim.setRepeatMode(ValueAnimator.REVERSE);
        rotateAnim.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        rotateAnim.setStartDelay(delay);
        rotateAnim.start();
        decorAnimators.add(rotateAnim);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDataFromDb(); 
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ObjectAnimator anim : decorAnimators) anim.cancel();
    }
}
