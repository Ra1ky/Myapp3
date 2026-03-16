package com.example.sportify.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.sportify.R;
import com.example.sportify.CardDetailActivity;
import com.example.sportify.SportifyApp;
import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;
import com.example.sportify.db.UserProfile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    // Data
    private AppDatabase db;
    private String todayDate;
    private DailyRecord todayRecord;
    private UserProfile profile;

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
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
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
        TextView btnMood1 = v.findViewById(R.id.btnMood1);
        TextView btnMood2 = v.findViewById(R.id.btnMood2);
        TextView btnMood3 = v.findViewById(R.id.btnMood3);
        TextView btnMood4 = v.findViewById(R.id.btnMood4);
        TextView btnMood5 = v.findViewById(R.id.btnMood5);
        moodButtons = new TextView[]{btnMood1, btnMood2, btnMood3, btnMood4, btnMood5};
    }

    private void setupCardClickListeners(View v) {
        CardView cardSteps = v.findViewById(R.id.cardSteps);
        CardView cardCalories = v.findViewById(R.id.cardCalories);
        CardView cardWater = v.findViewById(R.id.cardWater);
        CardView cardSleep = v.findViewById(R.id.cardSleep);

        cardSteps.setOnClickListener(click ->
                openDetail("Steps", "Step tracking detail screen – coming soon."));
        cardCalories.setOnClickListener(click ->
                openDetail("Food", "Calorie and food tracking detail screen – coming soon."));
        cardWater.setOnClickListener(click ->
                openDetail("Water", "Water intake detail screen – coming soon."));
        cardSleep.setOnClickListener(click ->
                openDetail("Sleep", "Sleep tracking detail screen – coming soon."));
    }

    private void openDetail(String title, String description) {
        Intent intent = new Intent(requireContext(), CardDetailActivity.class);
        intent.putExtra(CardDetailActivity.EXTRA_TITLE, title);
        intent.putExtra(CardDetailActivity.EXTRA_DESC, description);
        startActivity(intent);
    }

    private void setupMoodButtons() {
        for (int i = 0; i < moodButtons.length; i++) {
            final int score = i + 1;  // 1–5
            moodButtons[i].setOnClickListener(v -> selectMood(score));
        }
    }

    private void selectMood(int score) {
        for (int i = 0; i < moodButtons.length; i++) {
            if (i + 1 == score) {
                moodButtons[i].setBackgroundResource(R.drawable.bg_mood_selected);
            } else {
                moodButtons[i].setBackgroundResource(R.drawable.bg_mood_circle);
            }
        }

        ensureTodayRecord();
        todayRecord.setMoodScore(score);
        db.dailyRecordDAO().insertOrUpdate(todayRecord);

        updateAssessment();
    }

    private void loadData() {
        // Goals
        profile = db.userProfileDAO().getProfile();

        // Today's date
        String displayDate = new SimpleDateFormat("MMMM d", Locale.getDefault()).format(new Date());
        tvDate.setText(displayDate);

        // Today's record
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
            record.setStepGoal(profile.getStepGoal());
            record.setCaloriesGoal(profile.getCaloriesGoal());
            record.setWaterGoalMl(profile.getWaterGoalMl());
        } else {
            // Defaults
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

    // Steps
    private void updateStepsUI() {
        int steps = todayRecord.getSteps();
        int goal = todayRecord.getStepGoal();

        tvStepsCount.setText(String.valueOf(steps));
        tvStepsGoal.setText(String.format(Locale.getDefault(), "%d / %d", steps, goal));
        progressSteps.setMax(goal > 0 ? goal : 10000);
        progressSteps.setProgress(steps);
    }

    // Sleep
    private void updateSleepUI() {
        int minutes = todayRecord.getSleepMinutes();
        int hours = minutes / 60;
        int mins = minutes % 60;

        if (minutes > 0) {
            tvSleepHours.setText(String.format(Locale.getDefault(), "%d h %d min", hours, mins));
        } else {
            tvSleepHours.setText("-- h");
        }

        // Sleep progress bar: 8 h (480 min) — 100%
        int sleepPercent = minutes > 0 ? Math.min(100, (minutes * 100) / 480) : 0;
        progressSleep.setProgress(sleepPercent);

        int sleepMood = todayRecord.getSleepMood();
        String[] emojis = {"?", "😞", "😕", "😐", "🙂", "😄"};
        tvSleepMood.setText(emojis[Math.max(0, Math.min(sleepMood, 5))]);
    }

    // Calories
    private void updateCaloriesUI() {
        int consumed = todayRecord.getCaloriesConsumed();
        int goal = todayRecord.getCaloriesGoal();

        tvCaloriesCount.setText(String.valueOf(consumed));
        tvCaloriesGoal.setText(String.format(Locale.getDefault(), "/ %d kcal", goal));
        progressCalories.setMax(goal > 0 ? goal : 2000);
        progressCalories.setProgress(consumed);
    }

    // Water
    private void updateWaterUI() {
        int ml = todayRecord.getWaterMl();
        int goal = todayRecord.getWaterGoalMl();

        tvWaterCount.setText(String.format(Locale.getDefault(), "%d ml", ml));
        tvWaterGoal.setText(String.format(Locale.getDefault(), "/ %d ml", goal));
        progressWater.setMax(goal > 0 ? goal : 2500);
        progressWater.setProgress(ml);
    }

    // Mood
    private void updateMoodUI() {
        int mood = todayRecord.getMoodScore();
        for (int i = 0; i < moodButtons.length; i++) {
            if (i + 1 == mood) {
                moodButtons[i].setBackgroundResource(R.drawable.bg_mood_selected);
            } else {
                moodButtons[i].setBackgroundResource(R.drawable.bg_mood_circle);
            }
        }
    }

    private void updateAssessment() {
        int score = todayRecord.calculateHealthScore();

        if (score == 0) {
            tvAssessmentText.setText(R.string.assess_no_data);
            imgAssessment.setImageResource(android.R.drawable.ic_menu_info_details);
            return;
        }

        if (score >= 80) {
            tvAssessmentText.setText(R.string.assess_excellent);
            imgAssessment.setImageResource(android.R.drawable.btn_star_big_on);
        } else if (score >= 60) {
            tvAssessmentText.setText(R.string.assess_good);
            imgAssessment.setImageResource(android.R.drawable.btn_star_big_on);
        } else if (score >= 40) {
            tvAssessmentText.setText(R.string.assess_average);
            imgAssessment.setImageResource(android.R.drawable.ic_menu_info_details);
        } else {
            tvAssessmentText.setText(R.string.assess_below);
            imgAssessment.setImageResource(android.R.drawable.ic_menu_info_details);
        }
    }
}