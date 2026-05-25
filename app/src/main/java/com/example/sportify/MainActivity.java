package com.example.sportify;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.sportify.api.OpenFoodFactsService;
import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;
import com.example.sportify.db.FoodItem;
import com.example.sportify.fragments.DashboardFragment;
import com.example.sportify.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_START_ONBOARDING = "start_onboarding";
    private int currentNavId = R.id.nav_dashboard;
    private ActivityResultLauncher<Intent> scannerLauncher;
    private OpenFoodFactsService apiService;
    private AppDatabase db;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                if (Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.ACTIVITY_RECOGNITION, false))) {
                    startStepService();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Prefs.isOnboardingDone(this) &&
                !getIntent().getBooleanExtra(EXTRA_START_ONBOARDING, false)) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        db = SportifyApp.getDatabase();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://world.openfoodfacts.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(OpenFoodFactsService.class);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        scannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String barcode = result.getData().getStringExtra("barcode");
                        fetchAndShowProductDialog(barcode);
                    }
                }
        );

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra(EXTRA_START_ONBOARDING, false)) {
                currentNavId = R.id.nav_profile;
                bottomNav.setVisibility(View.GONE);
                loadFragment(ProfileFragment.newOnboardingInstance());
            } else {
                loadFragment(new DashboardFragment());
            }
        }
        
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_add) {
                Intent intent = new Intent(this, ScannerActivity.class);
                scannerLauncher.launch(intent);
                return false;
            }
            if (id == currentNavId) return true;
            currentNavId = id;
            Fragment fragment = (id == R.id.nav_profile) ? new ProfileFragment() : new DashboardFragment();
            loadFragment(fragment);
            return true;
        });

        checkPermissionsAndStartService();
    }

    private void checkPermissionsAndStartService() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.POST_NOTIFICATIONS};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions = new String[]{Manifest.permission.ACTIVITY_RECOGNITION};
        } else {
            permissions = new String[0];
        }

        if (permissions.length > 0) {
            permissionLauncher.launch(permissions);
        } else {
            startStepService();
        }
    }

    private void startStepService() {
        Intent intent = new Intent(this, StepCounterService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    public void navigateToDashboard() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setVisibility(View.VISIBLE);
        bottomNav.setSelectedItemId(R.id.nav_dashboard);
    }

    private void fetchAndShowProductDialog(String barcode) {
        apiService.getProduct(barcode).enqueue(new Callback<OpenFoodFactsService.ProductResponse>() {
            @Override
            public void onResponse(Call<OpenFoodFactsService.ProductResponse> call, Response<OpenFoodFactsService.ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().status == 1) {
                    showScannedProductDialog(response.body().product);
                } else {
                    Toast.makeText(MainActivity.this, "Product not found", Toast.LENGTH_SHORT).show();
                    showScannedProductDialog(null);
                }
            }
            @Override
            public void onFailure(Call<OpenFoodFactsService.ProductResponse> call, Throwable t) {
                showScannedProductDialog(null);
            }
        });
    }

    private void showScannedProductDialog(OpenFoodFactsService.ProductData productData) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_food, null);
        // ... (UI setup same as before)
        new AlertDialog.Builder(this)
                .setTitle("Add Food")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    // ... (Save logic same as before)
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
