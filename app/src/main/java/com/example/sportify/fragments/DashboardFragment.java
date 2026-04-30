package com.example.sportify.fragments;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sportify.CaloriesDetailActivity;
import com.example.sportify.R;
import com.example.sportify.SleepDetailActivity;
import com.example.sportify.SportifyApp;
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
    private TextView tvAssessmentText;
    private ProgressBar progressSteps, progressSleep, progressCalories, progressWater;
    private ImageView imgAssessment;
    private TextView[] moodButtons;
    private View layoutHeader, cardSteps, cardCalories, cardWater, cardSleep, cardMood, cardAssessment;

    // Data
    private AppDatabase db;
    private String todayDate;
    private DailyRecord todayRecord;
    private UserProfile profile;
    
    private final List<ObjectAnimator> decorAnimators = new ArrayList<>();
    private ObjectAnimator moodWobbleAnimator;
    private ObjectAnimator sleepMoodWobbleAnimator;
    private ObjectAnimator assessmentWobbleAnimator;
    private int lastSteps = 0, lastCalories = 0, lastWater = 0, lastSleep = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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
        loadData();
        startDecorAnimations(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
        animateEntrance();
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
        tvAssessmentText = v.findViewById(R.id.tvAssessmentText);
        imgAssessment = v.findViewById(R.id.imgAssessment);
        
        layoutHeader = v.findViewById(R.id.layoutHeader);
        cardSteps = v.findViewById(R.id.cardSteps);
        cardCalories = v.findViewById(R.id.cardCalories);
        cardWater = v.findViewById(R.id.cardWater);
        cardSleep = v.findViewById(R.id.cardSleep);
        cardMood = v.findViewById(R.id.cardMood);
        cardAssessment = v.findViewById(R.id.cardAssessment);

        TextView btnMood1 = v.findViewById(R.id.btnMood1);
        TextView btnMood2 = v.findViewById(R.id.btnMood2);
        TextView btnMood3 = v.findViewById(R.id.btnMood3);
        TextView btnMood4 = v.findViewById(R.id.btnMood4);
        TextView btnMood5 = v.findViewById(R.id.btnMood5);
        moodButtons = new TextView[]{btnMood1, btnMood2, btnMood3, btnMood4, btnMood5};
    }

    private void animateEntrance() {
        View[] elements = {layoutHeader, tvDate, cardSteps, cardCalories, cardWater, cardSleep, cardMood, cardAssessment};
        for (int i = 0; i < elements.length; i++) {
            View v = elements[i];
            if (v != null) {
                v.setAlpha(0f);
                v.setTranslationY(80f);
                v.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(700)
                        .setStartDelay(i * 100)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        }
    }

    private void startDecorAnimations(View v) {
        View d1 = v.findViewById(R.id.decorIcon1);
        View d2 = v.findViewById(R.id.decorIcon2);
        View d3 = v.findViewById(R.id.decorIcon3);

        if (d1 != null) applyFloatingAnimation(d1, 4000, 0, 30f, 20f);
        if (d2 != null) applyFloatingAnimation(d2, 4500, 500, -25f, -15f);
        if (d3 != null) applyFloatingAnimation(d3, 5000, 1000, 20f, 30f);
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

    private void setupCardClickListeners(View v) {
        cardSteps.setOnClickListener(click ->{
            Intent intent = new Intent(requireContext(), StepsDetailActivity.class);
            startActivity(intent);
        });
        
        cardCalories.setOnClickListener(click -> {
            Intent intent = new Intent(requireContext(), CaloriesDetailActivity.class);
            startActivity(intent);
        });

        cardWater.setOnClickListener(click -> {
            Intent intent = new Intent(requireContext(), WaterDetailActivity.class);
            startActivity(intent);
        });

        cardSleep.setOnClickListener(click -> {
            Intent intent = new Intent(requireContext(), SleepDetailActivity.class);
            startActivity(intent);
        });
    }

    private void setupMoodButtons() {
        IntStream.range(0, moodButtons.length)
                .forEach(i -> {
                    final int score = i + 1;
                    moodButtons[i].setOnClickListener(v -> {
                        animateMoodPress(v);
                        selectMood(score);
                    });
                });
    }

    private void animateMoodPress(View v) {
        v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(150).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(new OvershootInterpolator()).start()
        ).start();

        // Damped wobble — rotation oscillates and decays back to 0
        ObjectAnimator wobble = ObjectAnimator.ofFloat(v, "rotation",
                0f, -18f, 18f, -12f, 12f, -6f, 6f, 0f);
        wobble.setDuration(500);
        wobble.start();
    }

    // Shared wobble used by tvSleepMood, the active mood button, and the assessment star.
    // Returns the started animator so each caller can cancel its own when state changes.
    private ObjectAnimator startContinuousWobble(View v, float degrees) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, "rotation", -degrees, degrees);
        anim.setDuration(1200);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
        return anim;
    }

    private void selectMood(int score) {
        IntStream.range(0, moodButtons.length)
                .forEach(i -> moodButtons[i].setBackgroundResource(
                        i + 1 == score
                                ? R.drawable.bg_mood_selected
                                : R.drawable.bg_mood_circle
                ));

        ensureTodayRecord();
        todayRecord.setMoodScore(score);
        db.dailyRecordDAO().insertOrUpdate(todayRecord);

        updateMoodWobble(score);
        updateAssessment();
    }

    // Wobbles whichever of the 5 mood buttons is currently selected. When switching
    // selection, the previously wobbling button's rotation is reset to 0 since
    // cancel() leaves it frozen at whatever angle it had reached.
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

    private void updateSleepMoodWobble(int mood) {
        if (sleepMoodWobbleAnimator != null) {
            sleepMoodWobbleAnimator.cancel();
            sleepMoodWobbleAnimator = null;
        }
        if (mood <= 0) {
            tvSleepMood.setRotation(0f);
            return;
        }
        sleepMoodWobbleAnimator = startContinuousWobble(tvSleepMood, 8f);
    }

    // Wobbles imgAssessment only when it shows a star drawable. Score 0 means
    // "no data" and uses ic_menu_info_details — that should stay still.
    private void updateAssessmentWobble(boolean hasStar) {
        if (!hasStar) {
            if (assessmentWobbleAnimator != null) {
                assessmentWobbleAnimator.cancel();
                assessmentWobbleAnimator = null;
            }
            imgAssessment.setRotation(0f);
            return;
        }
        // Drawable swaps don't affect the View's rotation property, so let the
        // animator keep running across star transitions instead of restarting it.
        if (assessmentWobbleAnimator != null && assessmentWobbleAnimator.isRunning()) {
            return;
        }
        assessmentWobbleAnimator = startContinuousWobble(imgAssessment, 4f);
    }

    private void loadData() {
        profile = db.userProfileDAO().getProfile();
        String displayDate = new SimpleDateFormat("MMMM d", Locale.getDefault()).format(new Date());
        tvDate.setText(displayDate);

        todayRecord = db.dailyRecordDAO().getByDate(todayDate);
        if (todayRecord == null) {
            todayRecord = new DailyRecord(todayDate);
            applyGoalsFromProfile(todayRecord);
            db.dailyRecordDAO().insertOrUpdate(todayRecord);
        }

        updateStepsUI();
        updateSleepUI();
        updateCaloriesUI();
        updateWaterUI();
        updateMoodUI();
        updateAssessment();
    }

    private void applyGoalsFromProfile(DailyRecord record) {
        if (profile != null) {
            if (profile.getWeightKg() > 0 && profile.getHeightCm() > 0) {
                record.setCaloriesGoal(profile.getCaloriesGoal() > 0
                        ? profile.getCaloriesGoal()
                        : profile.getRecommendedCalories());
            } else {
                record.setCaloriesGoal(profile.getCaloriesGoal());
            }

            if (profile.getWeightKg() > 0) {
                record.setWaterGoalMl(profile.getWaterGoalMl() > 0
                        ? profile.getWaterGoalMl()
                        : profile.getRecommendedWaterMl());
            } else {
                record.setWaterGoalMl(profile.getWaterGoalMl());
            }

            record.setStepGoal(profile.getStepGoal());
        } else {
            record.setStepGoal(10000);
            record.setCaloriesGoal(2000);
            record.setWaterGoalMl(2500);
        }
    }

    private void ensureTodayRecord() {
        if (todayRecord == null) {
            todayRecord = new DailyRecord(todayDate);
            applyGoalsFromProfile(todayRecord);
        }
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

    private void updateSleepUI() {
        int minutes = todayRecord.getSleepMinutes();
        int hours = minutes / 60;
        int mins = minutes % 60;
        
        tvSleepHours.setText(minutes > 0 ? String.format(Locale.getDefault(), "%d h %d min", hours, mins) : "-- h");

        int sleepPercent = minutes > 0 ? Math.min(100, (minutes * 100) / 480) : 0;
        animateProgress(progressSleep, sleepPercent);

        int sleepMood = todayRecord.getSleepMood();
        String[] emojis = {"?", "😞", "😕", "😐", "🙂", "😄"};
        tvSleepMood.setText(emojis[Math.max(0, Math.min(sleepMood, 5))]);
        updateSleepMoodWobble(sleepMood);
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

    private void animateCountUp(TextView tv, int start, int end, String suffix) {
        ValueAnimator anim = ValueAnimator.ofInt(start, end);
        anim.setDuration(1000);
        anim.addUpdateListener(animation -> {
            String text = String.format(Locale.getDefault(), "%d%s", (int) animation.getAnimatedValue(), suffix);
            tv.setText(text);
        });
        anim.start();
    }

    private void animateProgress(ProgressBar pb, int value) {
        pb.animate().scaleY(1.2f).setDuration(200).withEndAction(() -> 
            pb.animate().scaleY(1f).setDuration(200).start()
        ).start();

        ObjectAnimator anim = ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), value);
        anim.setDuration(1000);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
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
            updateAssessmentWobble(false);
            return;
        }
        if (score >= 80) {
            tvAssessmentText.setText(R.string.assess_excellent);
            imgAssessment.setImageResource(R.drawable.ic_star_shiny);
        } else if (score >= 60) {
            tvAssessmentText.setText(R.string.assess_good);
            imgAssessment.setImageResource(R.drawable.ic_star_filled);
        } else if (score >= 40) {
            tvAssessmentText.setText(R.string.assess_average);
            imgAssessment.setImageResource(R.drawable.ic_star_half);
        } else {
            tvAssessmentText.setText(R.string.assess_below);
            imgAssessment.setImageResource(R.drawable.ic_star_outline);
        }
        updateAssessmentWobble(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ObjectAnimator anim : decorAnimators) anim.cancel();
        if (sleepMoodWobbleAnimator != null) sleepMoodWobbleAnimator.cancel();
        if (moodWobbleAnimator != null) moodWobbleAnimator.cancel();
        if (assessmentWobbleAnimator != null) assessmentWobbleAnimator.cancel();
    }
}
