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
}
