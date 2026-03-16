package com.example.sportify;

import android.app.Application;
import androidx.room.Room;
import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.Product;
import com.example.sportify.db.ProductDAO;

import java.util.concurrent.Executors;

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

        // Populate database with some products if empty
        Executors.newSingleThreadExecutor().execute(() -> {
            ProductDAO dao = db.productDAO();
            if (dao.getCount() == 0) {
                dao.insert(new Product("Apple", 52, 0, 0, 14));
                dao.insert(new Product("Banana", 89, 1, 0, 23));
                dao.insert(new Product("Chicken Breast", 165, 31, 4, 0));
                dao.insert(new Product("Rice", 130, 3, 0, 28));
                dao.insert(new Product("Egg", 155, 13, 11, 1));
                dao.insert(new Product("Milk", 42, 3, 1, 5));
                dao.insert(new Product("Oatmeal", 389, 17, 7, 66));
            }
        });
    }

   // Global access point to the DB
    public static AppDatabase getDatabase() {
        return db;
    }
}
