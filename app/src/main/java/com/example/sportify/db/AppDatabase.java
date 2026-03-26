package com.example.sportify.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
    entities = {
        UserProfile.class,
        DailyRecord.class,
        FoodItem.class,
        Product.class
    },
    version = 8,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserProfileDAO userProfileDAO();

    public abstract DailyRecordDAO dailyRecordDAO();

    public abstract FoodItemDAO foodItemDAO();

    public abstract ProductDAO productDAO();
}
