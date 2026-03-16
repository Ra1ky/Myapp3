package com.example.sportify;

import android.app.Application;
import androidx.room.Room;
import com.example.sportify.db.AppDatabase;

// Application class that runs once when the app starts (android:name=".SportifyApp")
public class SportifyApp extends Application {

    private static AppDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();
        db = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "sportify_db"
        )
        .allowMainThreadQueries() // should use AsyncTask/Executor later?..
        .fallbackToDestructiveMigration(true) // if schema changes, delete and recreate
        .build();
    }

   // Global access point to the DB
    public static AppDatabase getDatabase() {
        return db;
    }
}
