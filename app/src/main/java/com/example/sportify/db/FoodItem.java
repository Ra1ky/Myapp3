package com.example.sportify.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "food_items")
public class FoodItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String name;

    private int calories;
    private int protein;
    private int fat;
    private int carbs;
    private int grams;

    @NonNull
    private String mealType; // "Breakfast", "Lunch", "Dinner", "Snack"

    @NonNull
    private String date; // format: "yyyy-MM-dd"

    public FoodItem(@NonNull String name, int calories, int protein, int fat, int carbs, int grams, @NonNull String mealType, @NonNull String date) {
        this.name = name;
        this.calories = calories;
        this.protein = protein;
        this.fat = fat;
        this.carbs = carbs;
        this.grams = grams;
        this.mealType = mealType;
        this.date = date;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public int getProtein() { return protein; }
    public void setProtein(int protein) { this.protein = protein; }

    public int getFat() { return fat; }
    public void setFat(int fat) { this.fat = fat; }

    public int getCarbs() { return carbs; }
    public void setCarbs(int carbs) { this.carbs = carbs; }

    public int getGrams() { return grams; }
    public void setGrams(int grams) { this.grams = grams; }

    @NonNull
    public String getMealType() { return mealType; }
    public void setMealType(@NonNull String mealType) { this.mealType = mealType; }

    @NonNull
    public String getDate() { return date; }
    public void setDate(@NonNull String date) { this.date = date; }
}
