package com.example.sportify.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

// ROOM DATABASE

// When you add a new Entity (e.g. FoodProduct) you need:
// 1. Add it to entities = {...} array
// 2. Add an abstract DAO method
@Database(
    entities = {
        UserProfile.class,
        DailyRecord.class
        // TODO: FoodProduct.class, FoodEntry.class...
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserProfileDAO userProfileDAO();

    public abstract DailyRecordDAO dailyRecordDAO();

    // TODO: public abstract FoodProductDAO foodProductDAO();...
}
