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

    public int getCaloriesGoal() { return caloriesGoal; }
    public void setCaloriesGoal(int caloriesGoal) { this.caloriesGoal = caloriesGoal; }

    public int getWaterGoalMl() { return waterGoalMl; }
    public void setWaterGoalMl(int waterGoalMl) { this.waterGoalMl = waterGoalMl; }

    // Calculates the recommended daily water intake based on weight: ~35 ml per 1 kg of weight
    public int getRecommendedWaterMl() {
        return Math.round(weightKg * 35);
    }
}
