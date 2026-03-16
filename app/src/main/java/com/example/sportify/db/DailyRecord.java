package com.example.sportify.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

// Record of a day's health
@Entity(tableName = "daily_record")
public class DailyRecord {

    @PrimaryKey
    @NonNull
    private String date;  // format: "yyyy-MM-dd"

    private int steps;

    @ColumnInfo(name = "step_goal")
    private int stepGoal;

    @ColumnInfo(name = "sleep_minutes")
    private int sleepMinutes;

    @ColumnInfo(name = "sleep_mood")
    private int sleepMood;  // 1-5

    @ColumnInfo(name = "calories_consumed")
    private int caloriesConsumed;

    @ColumnInfo(name = "calories_goal")
    private int caloriesGoal;

    @ColumnInfo(name = "water_ml")
    private int waterMl;

    @ColumnInfo(name = "water_goal_ml")
    private int waterGoalMl;

    @ColumnInfo(name = "mood_score")
    private int moodScore;  // 1-5, 0 — not specified

    public DailyRecord(@NonNull String date) {
        this.date = date;
    }

    @NonNull
    public String getDate() { return date; }
    public void setDate(@NonNull String date) { this.date = date; }

    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; }

    public int getStepGoal() { return stepGoal; }
    public void setStepGoal(int stepGoal) { this.stepGoal = stepGoal; }

    public int getSleepMinutes() { return sleepMinutes; }
    public void setSleepMinutes(int sleepMinutes) { this.sleepMinutes = sleepMinutes; }

    public int getSleepMood() { return sleepMood; }
    public void setSleepMood(int sleepMood) { this.sleepMood = sleepMood; }

    public int getCaloriesConsumed() { return caloriesConsumed; }
    public void setCaloriesConsumed(int caloriesConsumed) { this.caloriesConsumed = caloriesConsumed; }

    public int getCaloriesGoal() { return caloriesGoal; }
    public void setCaloriesGoal(int caloriesGoal) { this.caloriesGoal = caloriesGoal; }

    public int getWaterMl() { return waterMl; }
    public void setWaterMl(int waterMl) { this.waterMl = waterMl; }

    public int getWaterGoalMl() { return waterGoalMl; }
    public void setWaterGoalMl(int waterGoalMl) { this.waterGoalMl = waterGoalMl; }

    public int getMoodScore() { return moodScore; }
    public void setMoodScore(int moodScore) { this.moodScore = moodScore; }

    // Calculates a daily health score (0-100)
    public int calculateHealthScore() {
        final List<Integer> scores = new ArrayList<>();

        // Steps: % of goal
        if (stepGoal > 0) {
            scores.add(Math.min(100, (steps * 100) / stepGoal));
        }

        // Sleep: 7–9h = 100, proportional otherwise
        if (sleepMinutes > 0) {
            int optimal = 480; // 8h
            int diff = Math.abs(sleepMinutes - optimal);
            scores.add(Math.max(0, 100 - diff));
        }

        // Calories: proximity to goal
        if (caloriesGoal > 0 && caloriesConsumed > 0) {
            int diff = Math.abs(caloriesConsumed - caloriesGoal);
            scores.add(Math.max(0, 100 - (diff * 100 / caloriesGoal)));
        }

        // Water: % of goal
        if (waterGoalMl > 0) {
            scores.add(Math.min(100, (waterMl * 100) / waterGoalMl));
        }

        // Mood: 1 star = 20, 5 stars = 100
        if (moodScore > 0) {
            scores.add(moodScore * 20);
        }

        if (scores.isEmpty()) {
            return 0;
        }

        final int sum = scores.stream().mapToInt(Integer::intValue).sum();
        return sum / scores.size();
    }
}