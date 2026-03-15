package com.example.sportify;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.sportify.fragments.DashboardFragment;
import com.example.sportify.fragments.PlaceholderFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setting up the initial fragment – Dashboard
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }

        // Bottom Navigation switching
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
            } else if (id == R.id.nav_food) {
                // TODO: change to FoodFragment
                fragment = PlaceholderFragment.newInstance("Food", "This will include food and calorie tracking.");
            } else if (id == R.id.nav_add) {
                // TODO: change to AddFragment
                fragment = PlaceholderFragment.newInstance("Add", "This will include quick data entry.");
            } else if (id == R.id.nav_stats) {
                // TODO: change to StatsFragment
                fragment = PlaceholderFragment.newInstance("Statistics", "This will include diagrams and history.");
            } else if (id == R.id.nav_profile) {
                // TODO: change to ProfileFragment
                fragment = PlaceholderFragment.newInstance("Profile", "This will show the user's profile info.");
            } else {
                fragment = new DashboardFragment();
            }

            loadFragment(fragment);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
