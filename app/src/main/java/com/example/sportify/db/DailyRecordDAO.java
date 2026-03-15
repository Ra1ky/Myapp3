package com.example.sportify.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DailyRecordDAO {
    // Save the daily record as a new entry or overwrite the old record if the daily record exists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(DailyRecord record);

    @Update
    void update(DailyRecord record);

    // Record by date
    @Query("SELECT * FROM daily_record WHERE date = :date")
    DailyRecord getByDate(String date);

    // Specific number of recent records
    @Query("SELECT * FROM daily_record ORDER BY date DESC LIMIT :days")
    List<DailyRecord> getLastDays(int days);

    // All stored records
    @Query("SELECT * FROM daily_record ORDER BY date DESC")
    List<DailyRecord> getAll();

    // Update step count
    @Query("UPDATE daily_record SET steps = :steps WHERE date = :date")
    void updateSteps(String date, int steps);

    // Update sleep duration and sleep mood
    @Query("UPDATE daily_record SET sleep_minutes = :minutes, sleep_mood = :mood WHERE date = :date")
    void updateSleep(String date, int minutes, int mood);

    // Update calorie intake
    @Query("UPDATE daily_record SET calories_consumed = :kcal WHERE date = :date")
    void updateCalories(String date, int kcal);

    // Update water consumption
    @Query("UPDATE daily_record SET water_ml = :ml WHERE date = :date")
    void updateWater(String date, int ml);

    // Update mood score
    @Query("UPDATE daily_record SET mood_score = :score WHERE date = :date")
    void updateMood(String date, int score);
}
