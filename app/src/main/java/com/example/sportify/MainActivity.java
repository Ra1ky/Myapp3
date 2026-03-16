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
import com.example.sportify.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private int currentNavId = R.id.nav_dashboard;

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

        // Setting up the initial fragment — Dashboard
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }

        // Bottom Navigation switching
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        int[][] states = {
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] colors = {
                getColor(R.color.sportify_nav_icon_active),
                getColor(R.color.sportify_nav_icon_inactive)
        };
        android.content.res.ColorStateList navColors =
                new android.content.res.ColorStateList(states, colors);

        bottomNav.setItemIconTintList(navColors);
        bottomNav.setItemTextColor(navColors);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == currentNavId) return true; // already on this tab
            currentNavId = id;

            Fragment fragment;
            if (id == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
            } else if (id == R.id.nav_add) {
                // TODO: change to CameraFragment
                fragment = PlaceholderFragment.newInstance("Camera", "This will include quick food data entry via the camera.");
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
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