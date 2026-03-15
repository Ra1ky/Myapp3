package com.example.sportify.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * Sportify Room duomenų bazė
 *
 * Kai pridėsite naują Entity (pvz. FoodProduct), reikia:
 * 1. Pridėti ją į entities = {...} masyvą
 * 2. Pridėti abstract DAO metodą
 */
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

    // TODO: public abstract FoodProductDAO foodProductDAO();
}
