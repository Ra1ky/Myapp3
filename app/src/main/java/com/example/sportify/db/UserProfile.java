package com.example.sportify.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// User profile – age, weight, height and daily goals (only one record is always stored (id = 1))
@Entity(tableName = "user_profile")
public class UserProfile {

    @PrimaryKey
    private int id = 1;

    private int age;

    @ColumnInfo(name = "weight_kg")
    private float weightKg;

    @ColumnInfo(name = "height_cm")
    private float heightCm;

    @ColumnInfo(name = "step_goal")
    private int stepGoal = 10000;

    @ColumnInfo(name = "diet_mode")
    private int dietMode = 0; // 0 = Maintain, 1 = Lose, 2 = Gain

    @ColumnInfo(name = "diet_multiplier")
    private float dietMultiplier = 1.0f;

    @ColumnInfo(name = "calories_goal")
    private int caloriesGoal = 2000;

    @ColumnInfo(name = "water_goal_ml")
    private int waterGoalMl = 2500;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public float getWeightKg() { return weightKg; }
    public void setWeightKg(float weightKg) { this.weightKg = weightKg; }

    public float getHeightCm() { return heightCm; }
    public void setHeightCm(float heightCm) { this.heightCm = heightCm; }

    public int getStepGoal() { return stepGoal; }
    public void setStepGoal(int stepGoal) { this.stepGoal = stepGoal; }

    public int getDietMode() { return dietMode; }
    public void setDietMode(int dietMode) { this.dietMode = dietMode; }

    public float getDietMultiplier() { return dietMultiplier; }
    public void setDietMultiplier(float dietMultiplier) { this.dietMultiplier = dietMultiplier; }

    public int getCaloriesGoal() { return caloriesGoal; }
    public void setCaloriesGoal(int caloriesGoal) { this.caloriesGoal = caloriesGoal; }

    public int getWaterGoalMl() { return waterGoalMl; }
    public void setWaterGoalMl(int waterGoalMl) { this.waterGoalMl = waterGoalMl; }

    // Recommends daily steps based on age (10000 for most adults, adjusted with age)
    public int getRecommendedSteps() {
        if (age <= 0) return 10000;
        if (age < 18) return 12000;
        if (age < 40) return 10000;
        if (age < 60) return 8000;
        return 6000;
    }

    public int getRecommendedCaloriesForDiet() {
        int base = getRecommendedCalories();
        return (int)(base * dietMultiplier);
    }

    // Estimates weight maintenance calories using an averaged Mifflin-St Jeor equation
    // (averages male and female estimates) with a sedentary activity multiplier of 1.2
    public int getRecommendedCalories() {
        if (weightKg <= 0 || heightCm <= 0) return 2000; // default

        //   Male:   10 * weight + 6.25 * height - 5 * age + 5
        //   Female: 10 * weight + 6.25 * height - 5 * age - 161
        // Gender-neutral estimate:
        //   10 * weight + 6.25 * height - 5 * age - 78
        int ageVal = age > 0 ? age : 25; // default assumption
        double bmr = 10 * weightKg + 6.25 * heightCm - 5 * ageVal - 78;
        double tdee = bmr * 1.2; // sedentary multiplier
        return Math.max(1200, (int) Math.round(tdee)); // floor is at 1200 for safety
    }

    // Calculates the recommended daily water intake based on weight: ~35 ml per 1 kg of weight
    public int getRecommendedWaterMl() {
        return Math.round(weightKg * 35);
    }
}
