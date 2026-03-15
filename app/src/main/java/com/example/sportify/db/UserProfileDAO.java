package com.example.sportify.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserProfileDAO {
    // Save the user profile as a new entry or overwrite the old record if the user exists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(UserProfile profile);

    @Query("SELECT * FROM user_profile WHERE id = 1")
    UserProfile getProfile();
}
