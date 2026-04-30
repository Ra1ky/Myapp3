package com.example.sportify;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
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

public class WaterDetailActivity extends AppCompatActivity {

    private TextView tvTotalWaterDisplay, tvWaterProgressLabel;
    private ProgressBar pbWater;
    private View vWaterWave;
    private View summaryCard, glQuickLog, tvQuickLogTitle;

    private AppDatabase db;
    private DailyRecordDAO recordDao;
    private String todayDate;
    private DailyRecord todayRecord;
    
    private final List<ObjectAnimator> decorAnimators = new ArrayList<>();
    private int lastWaterValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_water_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.waterRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Initialize UI
        ImageButton btnBack = findViewById(R.id.btnBack);
        tvTotalWaterDisplay = findViewById(R.id.tvTotalWaterDisplay);
        tvWaterProgressLabel = findViewById(R.id.tvWaterProgressLabel);
        pbWater = findViewById(R.id.pbWater);
        vWaterWave = findViewById(R.id.vWaterWave);
        
        summaryCard = findViewById(R.id.summaryCard);
        glQuickLog = findViewById(R.id.glQuickLog);
        tvQuickLogTitle = findViewById(R.id.tvQuickLogTitle);

        btnBack.setOnClickListener(v -> finish());

        // Database setup
        db = SportifyApp.getDatabase();
        recordDao = db.dailyRecordDAO();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        loadData();

        setupWaterContainer(R.id.container250, R.id.btnPlus250, R.id.btnMinus250, 250);
        setupWaterContainer(R.id.container500, R.id.btnPlus500, R.id.btnMinus500, 500);
        setupWaterContainer(R.id.container750, R.id.btnPlus750, R.id.btnMinus750, 750);
        setupWaterContainer(R.id.container1000, R.id.btnPlus1000, R.id.btnMinus1000, 1000);
        
        animateEntrance();
        startDecorAnimations();
    }

    private void animateEntrance() {
        if (summaryCard != null) {
            summaryCard.setAlpha(0f);
            summaryCard.setTranslationY(100f);
            summaryCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        View[] views = {tvQuickLogTitle, glQuickLog};
        for (int i = 0; i < views.length; i++) {
            View v = views[i];
            if (v != null) {
                v.setAlpha(0f);
                v.setTranslationY(50f);
                v.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(800)
                        .setStartDelay(300 + (i * 100))
                        .start();
            }
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

    private void setupWaterContainer(int containerId, int plusId, int minusId, int amount) {
        View container = findViewById(containerId);
        View plus = findViewById(plusId);
        View minus = findViewById(minusId);

        plus.setScaleX(0); plus.setScaleY(0);
        minus.setScaleX(0); minus.setScaleY(0);

        container.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> 
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            ).start();

            if (plus.getVisibility() == View.INVISIBLE) {
                plus.setVisibility(View.VISIBLE);
                minus.setVisibility(View.VISIBLE);
                plus.animate().scaleX(1).scaleY(1).setDuration(300).setInterpolator(new OvershootInterpolator()).start();
                minus.animate().scaleX(1).scaleY(1).setDuration(300).setInterpolator(new OvershootInterpolator()).start();
            } else {
                plus.animate().scaleX(0).scaleY(0).setDuration(200).withEndAction(() -> plus.setVisibility(View.INVISIBLE)).start();
                minus.animate().scaleX(0).scaleY(0).setDuration(200).withEndAction(() -> minus.setVisibility(View.INVISIBLE)).start();
            }
        });

        plus.setOnClickListener(v -> {
            updateWaterAmount(amount);
            container.performClick();
        });

        minus.setOnClickListener(v -> {
            updateWaterAmount(-amount);
            container.performClick();
        });
    }

    private void loadData() {
        todayRecord = recordDao.getByDate(todayDate);
        if (todayRecord == null) {
            todayRecord = new DailyRecord(todayDate);
            todayRecord.setWaterGoalMl(2500); 
            recordDao.insertOrUpdate(todayRecord);
        }
        updateUI(false);
    }

    private void updateUI(boolean animateWave) {
        int waterMl = todayRecord.getWaterMl();
        int goalMl = todayRecord.getWaterGoalMl();

        // 1. Count-up animation for the Litres text
        ValueAnimator textAnim = ValueAnimator.ofInt(lastWaterValue, waterMl);
        textAnim.setDuration(1000);
        textAnim.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            tvTotalWaterDisplay.setText(String.format(Locale.getDefault(), "%.2f L", val / 1000.0));
        });
        textAnim.start();
        
        lastWaterValue = waterMl;
        tvWaterProgressLabel.setText(String.format(Locale.getDefault(), "%d / %d ml", waterMl, goalMl));
        
        pbWater.setMax(goalMl > 0 ? goalMl : 2500);
        
        // 2. Pulse animation for the progress bar
        pbWater.animate()
                .scaleY(1.4f)
                .setDuration(200)
                .withEndAction(() -> pbWater.animate().scaleY(1f).setDuration(200).start())
                .start();

        // 3. Smooth progress transition
        ObjectAnimator anim = ObjectAnimator.ofInt(pbWater, "progress", pbWater.getProgress(), waterMl);
        anim.setDuration(1000);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();

        if (animateWave) {
            playLiteralWaveAnimation();
        }
    }

    private void playLiteralWaveAnimation() {
        vWaterWave.setVisibility(View.VISIBLE);
        vWaterWave.setTranslationX(-vWaterWave.getWidth());
        
        vWaterWave.animate()
                .translationX(pbWater.getWidth())
                .setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        vWaterWave.setVisibility(View.INVISIBLE);
                    }
                })
                .start();
    }

    private void updateWaterAmount(int delta) {
        int current = todayRecord.getWaterMl();
        int newValue = Math.max(0, current + delta);

        todayRecord.setWaterMl(newValue);
        recordDao.insertOrUpdate(todayRecord);
        
        updateUI(true);
        
        String message = delta > 0 ? "+" + delta + " ml added" : delta + " ml removed";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ObjectAnimator anim : decorAnimators) {
            anim.cancel();
        }
    }
}
