package com.example.sportify.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ProductDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Product product);

    @Query("SELECT * FROM products WHERE name LIKE :query LIMIT 5")
    List<Product> searchProducts(String query);

    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    Product getByName(String name);

    @Query("SELECT COUNT(*) FROM products")
    int getCount();
}
