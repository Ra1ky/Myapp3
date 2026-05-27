package com.example.sportify.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FoodItemDAO {
    @Insert
    void insert(FoodItem foodItem);

    @Delete
    void delete(FoodItem foodItem);

    @Query("SELECT * FROM food_items WHERE date = :date ORDER BY id DESC")
    List<FoodItem> getByDate(String date);

    @Query("SELECT SUM(calories) FROM food_items WHERE date = :date")
    int getTotalCaloriesForDate(String date);

    /**
     * Gets the most recent unique food items added by the user.
     * Updated to ensure the latest entry for each food name is picked (using MAX(id))
     * and increased the limit to 25 to show more history (covering about a week of typical variety).
     */
    @Query("SELECT * FROM food_items WHERE id IN (SELECT MAX(id) FROM food_items GROUP BY name) ORDER BY id DESC LIMIT 25")
    List<FoodItem> getRecentUniqueItems();
}
