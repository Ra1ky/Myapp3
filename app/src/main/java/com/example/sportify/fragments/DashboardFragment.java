package com.example.sportify.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.sportify.CaloriesDetailActivity;
import com.example.sportify.R;
import com.example.sportify.SleepDetailActivity;
import com.example.sportify.SleepRepository;
import com.example.sportify.SportifyApp;
import com.example.sportify.StatisticsActivity;
import com.example.sportify.StepsDetailActivity;
import com.example.sportify.WaterDetailActivity;
import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;
import com.example.sportify.db.UserProfile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public class DashboardFragment extends Fragment {

    // UI
    private TextView tvDate, tvStepsCount, tvStepsGoal;
    private TextView tvSleepHours, tvSleepMood;
    private TextView tvCaloriesCount, tvCaloriesGoal;
    private TextView tvWaterCount, tvWaterGoal;
    private TextView tvWeightValue;
    private TextView tvAssessmentText;
    private ProgressBar progressSteps, progressSleep, progressCalories, progressWater;
    private ImageView imgAssessment;
    private TextView[] moodButtons;
    private View btnStatistics;
    private View layoutHeader, cardSteps, cardCalories, cardWater, cardSleep, cardMood, cardAssessment, cardWeight;

    // Data
    private AppDatabase db;
    private String todayDate;
    private DailyRecord todayRecord;
    private UserProfile profile;
    
    private final List<ObjectAnimator> decorAnimators = new ArrayList<>();
    private ObjectAnimator moodWobbleAnimator;
    private ObjectAnimator sleepMoodWobbleAnimator;
    private ObjectAnimator assessmentWobbleAnimator;
    private Runnable assessmentSpinRunnable;
    private ImageView imgSparkle1, imgSparkle2;
    private boolean assessmentSpinning = false;
    private boolean assessmentSparkling = false;
    
    // Values for count-up animations
    private int lastSteps = 0, lastCalories = 0, lastWater = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = SportifyApp.getDatabase();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        bindViews(view);
        setupCardClickListeners(view);
        setupMoodButtons();
        startDecorAnimations(view);

        // SETUP REAL-TIME OBSERVING
        setupObservers();
    }

    private void setupObservers() {
        // Observe Today's Record
        db.dailyRecordDAO().getLiveByDate(todayDate).observe(getViewLifecycleOwner(), record -> {
            if (record != null) {
                todayRecord = record;
                updateAllUI();
            } else {
                // Create if not exists
                createNewDailyRecord();
            }
        });

        // Observe Profile (for goals and weight)
        db.userProfileDAO().getLiveProfile().observe(getViewLifecycleOwner(), p -> {
            profile = p;
            if (todayRecord != null) updateAllUI();
        });
    }

    private void createNewDailyRecord() {
        new Thread(() -> {
            DailyRecord newRecord = new DailyRecord(todayDate);
            // Default goals if profile is null
            newRecord.setStepGoal(10000);
            newRecord.setCaloriesGoal(2000);
            newRecord.setWaterGoalMl(2500);
            
            // If profile exists, use its goals
            UserProfile p = db.userProfileDAO().getProfile();
            if (p != null) {
                newRecord.setStepGoal(p.getStepGoal());
                newRecord.setCaloriesGoal(p.getCaloriesGoal());
                newRecord.setWaterGoalMl(p.getWaterGoalMl());
                newRecord.setWeightKg(p.getWeightKg());
            }
            db.dailyRecordDAO().insertOrUpdate(newRecord);
        }).start();
    }

    private void updateAllUI() {
        if (todayRecord == null) return;
        
        String displayDate = new SimpleDateFormat("MMMM d", Locale.getDefault()).format(new Date());
        tvDate.setText(displayDate);

        updateStepsUI();
        updateSleepUI();
        updateCaloriesUI();
        updateWaterUI();
        updateMoodUI();
        updateWeightUI();
        updateAssessment();
    }

    private void bindViews(View v) {
        tvDate = v.findViewById(R.id.tvDate);
        tvStepsCount = v.findViewById(R.id.tvStepsCount);
        tvStepsGoal = v.findViewById(R.id.tvStepsGoal);
        progressSteps = v.findViewById(R.id.progressSteps);
        tvSleepHours = v.findViewById(R.id.tvSleepHours);
        tvSleepMood = v.findViewById(R.id.tvSleepMood);
        progressSleep = v.findViewById(R.id.progressSleep);
        tvCaloriesCount = v.findViewById(R.id.tvCaloriesCount);
        tvCaloriesGoal = v.findViewById(R.id.tvCaloriesGoal);
        progressCalories = v.findViewById(R.id.progressCalories);
        tvWaterCount = v.findViewById(R.id.tvWaterCount);
        tvWaterGoal = v.findViewById(R.id.tvWaterGoal);
        progressWater = v.findViewById(R.id.progressWater);
        tvWeightValue = v.findViewById(R.id.tvWeightValue);
        tvAssessmentText = v.findViewById(R.id.tvAssessmentText);
        imgAssessment = v.findViewById(R.id.imgAssessment);
        imgSparkle1 = v.findViewById(R.id.imgSparkle1);
        imgSparkle2 = v.findViewById(R.id.imgSparkle2);
        
        btnStatistics = v.findViewById(R.id.btnStatistics);
        layoutHeader = v.findViewById(R.id.layoutHeader);
        cardWeight = v.findViewById(R.id.cardWeight);
        cardSteps = v.findViewById(R.id.cardSteps);
        cardCalories = v.findViewById(R.id.cardCalories);
        cardWater = v.findViewById(R.id.cardWater);
        cardSleep = v.findViewById(R.id.cardSleep);
        cardMood = v.findViewById(R.id.cardMood);
        cardAssessment = v.findViewById(R.id.cardAssessment);

        moodButtons = new TextView[]{
                v.findViewById(R.id.btnMood1), v.findViewById(R.id.btnMood2),
                v.findViewById(R.id.btnMood3), v.findViewById(R.id.btnMood4), v.findViewById(R.id.btnMood5)
        };
    }

    private void updateStepsUI() {
        int steps = todayRecord.getSteps();
        int goal = todayRecord.getStepGoal();
        animateCountUp(tvStepsCount, lastSteps, steps, "");
        lastSteps = steps;
        tvStepsGoal.setText(String.format(Locale.getDefault(), "%d / %d", steps, goal));
        progressSteps.setMax(goal > 0 ? goal : 10000);
        animateProgress(progressSteps, steps);
    }

    private void updateCaloriesUI() {
        int consumed = todayRecord.getCaloriesConsumed();
        int goal = todayRecord.getCaloriesGoal();
        animateCountUp(tvCaloriesCount, lastCalories, consumed, " kcal");
        lastCalories = consumed;
        tvCaloriesGoal.setText(String.format(Locale.getDefault(), "/ %d kcal", goal));
        progressCalories.setMax(goal > 0 ? goal : 2000);
        animateProgress(progressCalories, consumed);
    }

    private void updateWaterUI() {
        int ml = todayRecord.getWaterMl();
        int goal = todayRecord.getWaterGoalMl();
        animateCountUp(tvWaterCount, lastWater, ml, " ml");
        lastWater = ml;
        tvWaterGoal.setText(String.format(Locale.getDefault(), "/ %d ml", goal));
        progressWater.setMax(goal > 0 ? goal : 2500);
        animateProgress(progressWater, ml);
    }

    private void updateWeightUI() {
        if (todayRecord.getWeightKg() > 0) {
            tvWeightValue.setText(String.format(Locale.getDefault(), "%.1f kg", todayRecord.getWeightKg()));
        } else if (profile != null && profile.getWeightKg() > 0) {
            tvWeightValue.setText(String.format(Locale.getDefault(), "%.1f kg (profile)", profile.getWeightKg()));
        } else {
            tvWeightValue.setText("-- kg");
        }
    }

    private void updateSleepUI() {
        int minutes = todayRecord.getSleepMinutes();
        tvSleepHours.setText(minutes > 0 ? String.format(Locale.getDefault(), "%d h %d min", minutes / 60, minutes % 60) : "-- h");
        animateProgress(progressSleep, minutes > 0 ? Math.min(100, (minutes * 100) / 480) : 0);
        
        int sleepMood = todayRecord.getSleepMood();
        String[] emojis = {"?", "😞", "😕", "😐", "🙂", "😄"};
        tvSleepMood.setText(emojis[Math.max(0, Math.min(sleepMood, 5))]);
        updateSleepMoodWobble(sleepMood);
    }

    private void updateMoodUI() {
        int mood = todayRecord.getMoodScore();
        for (int i = 0; i < moodButtons.length; i++) {
            moodButtons[i].setBackgroundResource(i + 1 == mood ? R.drawable.bg_mood_selected : R.drawable.bg_mood_circle);
        }
        updateMoodWobble(mood);
    }

    private void updateAssessment() {
        int score = todayRecord.calculateHealthScore();
        if (score == 0) {
            tvAssessmentText.setText(R.string.assess_no_data);
            imgAssessment.setImageResource(android.R.drawable.ic_menu_info_details);
            stopAssessmentAnimation();
        } else if (score >= 80) {
            tvAssessmentText.setText(R.string.assess_excellent);
            imgAssessment.setImageResource(R.drawable.ic_star_shiny);
            startAssessmentSpin(true);
        } else {
            tvAssessmentText.setText(score >= 60 ? R.string.assess_good : score >= 40 ? R.string.assess_average : R.string.assess_below);
            imgAssessment.setImageResource(score >= 60 ? R.drawable.ic_star_gold : score >= 40 ? R.drawable.ic_star_silver : R.drawable.ic_star_bronze);
            startAssessmentSpin(false);
        }
    }

    private void showWeightDialog() {
        EditText et = new EditText(requireContext());
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (todayRecord != null && todayRecord.getWeightKg() > 0) et.setText(String.valueOf(todayRecord.getWeightKg()));

        new AlertDialog.Builder(requireContext())
                .setTitle("Log Weight")
                .setMessage("Enter your weight for today (kg):")
                .setView(et)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        float weight = Float.parseFloat(et.getText().toString());
                        new Thread(() -> {
                            todayRecord.setWeightKg(weight);
                            db.dailyRecordDAO().insertOrUpdate(todayRecord);
                            if (profile != null) {
                                profile.setWeightKg(weight);
                                db.userProfileDAO().insertOrUpdate(profile);
                            }
                        }).start();
                    } catch (Exception e) { Toast.makeText(requireContext(), "Invalid weight", Toast.LENGTH_SHORT).show(); }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void animateCountUp(TextView tv, int start, int end, String suffix) {
        if (start == end) return;
        ValueAnimator anim = ValueAnimator.ofInt(start, end);
        anim.setDuration(800);
        anim.addUpdateListener(animation -> tv.setText(String.format(Locale.getDefault(), "%d%s", (int) animation.getAnimatedValue(), suffix)));
        anim.start();
    }

    private void animateProgress(ProgressBar pb, int value) {
        ObjectAnimator anim = ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), value);
        anim.setDuration(1000);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    private void animateEntrance() {
        View[] elements = {layoutHeader, tvDate, cardWeight, cardSteps, cardCalories, cardWater, cardSleep, cardMood, cardAssessment};
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == null) continue;
            elements[i].setAlpha(0f);
            elements[i].setTranslationY(80f);
            elements[i].animate().alpha(1f).translationY(0f).setDuration(700).setStartDelay(i * 100L).setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private void setupCardClickListeners(View v) {
        btnStatistics.setOnClickListener(c -> startActivity(new Intent(requireContext(), StatisticsActivity.class)));
        cardWeight.setOnClickListener(c -> showWeightDialog());
        v.findViewById(R.id.btnLogWeight).setOnClickListener(c -> showWeightDialog());
        cardSteps.setOnClickListener(c -> startActivity(new Intent(requireContext(), StepsDetailActivity.class)));
        cardCalories.setOnClickListener(c -> startActivity(new Intent(requireContext(), CaloriesDetailActivity.class)));
        cardWater.setOnClickListener(c -> startActivity(new Intent(requireContext(), WaterDetailActivity.class)));
        cardSleep.setOnClickListener(c -> startActivity(new Intent(requireContext(), SleepDetailActivity.class)));
    }

    private void setupMoodButtons() {
        IntStream.range(0, moodButtons.length).forEach(i -> {
            int score = i + 1;
            moodButtons[i].setOnClickListener(v -> {
                v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(150).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(new OvershootInterpolator()).start()).start();
                new Thread(() -> {
                    todayRecord.setMoodScore(score);
                    db.dailyRecordDAO().insertOrUpdate(todayRecord);
                }).start();
            });
        });
    }

    private void startDecorAnimations(View v) {
        View d1 = v.findViewById(R.id.decorIcon1), d2 = v.findViewById(R.id.decorIcon2), d3 = v.findViewById(R.id.decorIcon3);
        if (d1 != null) applyFloatingAnimation(d1, 4000, 0, 30f, 20f);
        if (d2 != null) applyFloatingAnimation(d2, 4500, 500, -25f, -15f);
        if (d3 != null) applyFloatingAnimation(d3, 5000, 1000, 20f, 30f);
    }

    private void applyFloatingAnimation(View v, long duration, long delay, float translationY, float rotation) {
        ObjectAnimator f = ObjectAnimator.ofFloat(v, "translationY", -translationY, translationY);
        f.setDuration(duration); f.setRepeatMode(ValueAnimator.REVERSE); f.setRepeatCount(ValueAnimator.INFINITE);
        f.setInterpolator(new AccelerateDecelerateInterpolator()); f.setStartDelay(delay); f.start(); decorAnimators.add(f);
        ObjectAnimator r = ObjectAnimator.ofFloat(v, "rotation", -rotation, rotation);
        r.setDuration(duration + 800); r.setRepeatMode(ValueAnimator.REVERSE); r.setRepeatCount(ValueAnimator.INFINITE);
        r.setInterpolator(new AccelerateDecelerateInterpolator()); r.setStartDelay(delay); r.start(); decorAnimators.add(r);
    }

    private void updateMoodWobble(int score) {
        if (moodWobbleAnimator != null) {
            ((View) moodWobbleAnimator.getTarget()).setRotation(0f);
            moodWobbleAnimator.cancel(); moodWobbleAnimator = null;
        }
        if (score >= 1 && score <= 5) moodWobbleAnimator = startContinuousWobble(moodButtons[score - 1], 8f);
    }

    private void updateSleepMoodWobble(int mood) {
        if (sleepMoodWobbleAnimator != null) { sleepMoodWobbleAnimator.cancel(); sleepMoodWobbleAnimator = null; }
        if (mood > 0) sleepMoodWobbleAnimator = startContinuousWobble(tvSleepMood, 8f); else tvSleepMood.setRotation(0f);
    }

    private void startAssessmentSpin(boolean withSparkles) {
        stopAssessmentAnimation();
        assessmentSpinning = true;
        doAssessmentSpin();
        if (withSparkles) {
            assessmentSparkling = true;
            imgSparkle1.setVisibility(View.VISIBLE);
            imgSparkle2.setVisibility(View.VISIBLE);
            doSparkle(imgSparkle1, 0);
            doSparkle(imgSparkle2, 500);
        }
    }

    private void stopAssessmentAnimation() {
        assessmentSpinning = false;
        assessmentSparkling = false;
        if (assessmentSpinRunnable != null) { imgAssessment.removeCallbacks(assessmentSpinRunnable); assessmentSpinRunnable = null; }
        if (assessmentWobbleAnimator != null) { assessmentWobbleAnimator.cancel(); assessmentWobbleAnimator = null; }
        imgAssessment.setRotationY(0f);
        if (imgSparkle1 != null) { imgSparkle1.setVisibility(View.GONE); imgSparkle1.setScaleX(1f); imgSparkle1.setScaleY(1f); imgSparkle1.setAlpha(1f); }
        if (imgSparkle2 != null) { imgSparkle2.setVisibility(View.GONE); imgSparkle2.setScaleX(1f); imgSparkle2.setScaleY(1f); imgSparkle2.setAlpha(1f); }
    }

    private void doAssessmentSpin() {
        if (!assessmentSpinning || !isAdded()) return;
        assessmentWobbleAnimator = ObjectAnimator.ofFloat(imgAssessment, "rotationY", 0f, 360f);
        assessmentWobbleAnimator.setDuration(1000);
        assessmentWobbleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        assessmentWobbleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!assessmentSpinning || !isAdded()) return;
                assessmentSpinRunnable = () -> doAssessmentSpin();
                imgAssessment.postDelayed(assessmentSpinRunnable, 2000);
            }
        });
        assessmentWobbleAnimator.start();
    }

    private void doSparkle(ImageView v, long delay) {
        if (!assessmentSparkling || !isAdded()) return;
        v.postDelayed(() -> {
            if (!assessmentSparkling || !isAdded() || v.getWindowToken() == null) return;
            ObjectAnimator sx = ObjectAnimator.ofFloat(v, "scaleX", 1f, 2f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(v, "scaleY", 1f, 2f);
            ObjectAnimator al = ObjectAnimator.ofFloat(v, "alpha", 1f, 0f);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(sx, sy, al);
            set.setDuration(600);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    v.setScaleX(1f); v.setScaleY(1f); v.setAlpha(1f);
                    doSparkle(v, 1000);
                }
            });
            set.start();
        }, delay);
    }

    private ObjectAnimator startContinuousWobble(View v, float degrees) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "rotation", -degrees, degrees);
        a.setDuration(1200); a.setRepeatMode(ValueAnimator.REVERSE); a.setRepeatCount(ValueAnimator.INFINITE);
        a.setInterpolator(new AccelerateDecelerateInterpolator()); a.start(); return a;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (SleepRepository.isAvailable(requireContext())) {
            SleepRepository.syncLastNight(requireContext(), db, todayDate, new SleepRepository.SyncCallback() {
                @Override public void onResult(int sleepMinutes) { /* LiveData picks up DB change automatically */ }
                @Override public void onError(String message) { /* silent — don't bother user on dashboard */ }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ObjectAnimator a : decorAnimators) a.cancel();
        if (sleepMoodWobbleAnimator != null) sleepMoodWobbleAnimator.cancel();
        if (moodWobbleAnimator != null) moodWobbleAnimator.cancel();
        assessmentSpinning = false;
        assessmentSparkling = false;
        if (assessmentWobbleAnimator != null) assessmentWobbleAnimator.cancel();
    }
}
