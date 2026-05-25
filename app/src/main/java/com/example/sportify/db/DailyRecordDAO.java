package com.example.sportify.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DailyRecordDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(DailyRecord record);

    @Update
    void update(DailyRecord record);

    @Query("SELECT * FROM daily_record WHERE date = :date")
    DailyRecord getByDate(String date);

    @Query("SELECT * FROM daily_record WHERE date = :date")
    LiveData<DailyRecord> getLiveByDate(String date);

    @Query("SELECT * FROM daily_record ORDER BY date DESC LIMIT :days")
    List<DailyRecord> getLastDays(int days);

    @Query("SELECT * FROM daily_record ORDER BY date DESC")
    List<DailyRecord> getAll();

    @Query("UPDATE daily_record SET weight_kg = :weight WHERE date = :date")
    void updateWeight(String date, float weight);

    @Query("UPDATE daily_record SET steps = :steps WHERE date = :date")
    void updateSteps(String date, int steps);

    @Query("UPDATE daily_record SET sleep_minutes = :minutes, sleep_mood = :mood WHERE date = :date")
    void updateSleep(String date, int minutes, int mood);

    @Query("UPDATE daily_record SET calories_consumed = :kcal WHERE date = :date")
    void updateCalories(String date, int kcal);

    @Query("UPDATE daily_record SET water_ml = :ml WHERE date = :date")
    void updateWater(String date, int ml);

    @Query("UPDATE daily_record SET mood_score = :score WHERE date = :date")
    void updateMood(String date, int score);
}
