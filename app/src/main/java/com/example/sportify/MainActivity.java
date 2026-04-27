package com.example.sportify;

import android.app.AlertDialog;
import android.content.Intent;
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
import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Send the user to the welcome screen if they haven't finished onboarding.
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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Don't pad bottom to allow nav bar to be at edge
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
                // Coming from WelcomeActivity — open profile in onboarding mode and
                // hide the bottom nav so the user can't escape mid-flow.
                currentNavId = R.id.nav_profile;
                bottomNav.setVisibility(View.GONE);
                loadFragment(ProfileFragment.newOnboardingInstance());
            } else {
                loadFragment(new DashboardFragment());
            }
        }
        
        // Color state list for navigation
        int[][] states = {{android.R.attr.state_checked}, {-android.R.attr.state_checked}};
        int[] colors = {getColor(R.color.sportify_nav_icon_active), getColor(R.color.sportify_nav_icon_inactive)};
        android.content.res.ColorStateList navColors = new android.content.res.ColorStateList(states, colors);
        bottomNav.setItemIconTintList(navColors);
        bottomNav.setItemTextColor(navColors);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_add) {
                // Launch scanner immediately when center button is pressed
                Intent intent = new Intent(this, ScannerActivity.class);
                scannerLauncher.launch(intent);
                return false; // Don't select this item in the nav bar
            }

            if (id == currentNavId) return true;
            currentNavId = id;

            Fragment fragment;
            if (id == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
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

    // Restores the bottom nav (hidden during onboarding) and switches to the dashboard.
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
                    Toast.makeText(MainActivity.this, "Product not found. Opening manual add...", Toast.LENGTH_SHORT).show();
                    showScannedProductDialog(null); // Open empty dialog
                }
            }
            @Override
            public void onFailure(Call<OpenFoodFactsService.ProductResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Network error. Opening manual add...", Toast.LENGTH_SHORT).show();
                showScannedProductDialog(null);
            }
        });
    }

    private void showScannedProductDialog(OpenFoodFactsService.ProductData productData) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_food, null);
        Spinner spinnerMeal = view.findViewById(R.id.spinnerMealType);
        AutoCompleteTextView etName = view.findViewById(R.id.etFoodName);
        EditText etGrams = view.findViewById(R.id.etFoodGrams);
        EditText etKcal = view.findViewById(R.id.etFoodCalories);
        EditText etProtein = view.findViewById(R.id.etFoodProtein);
        EditText etCarbs = view.findViewById(R.id.etFoodCarbs);
        EditText etFat = view.findViewById(R.id.etFoodFat);
        
        // Hide the scan button inside the dialog since we just came from scanning
        view.findViewById(R.id.btnScanBarcode).setVisibility(View.GONE);

        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack"};
        spinnerMeal.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mealTypes));

        if (productData != null) {
            etName.setText(productData.productName);
            etProtein.setText(String.valueOf((int)productData.nutriments.proteins100g));
            etCarbs.setText(String.valueOf((int)productData.nutriments.carbohydrates100g));
            etFat.setText(String.valueOf((int)productData.nutriments.fat100g));
            etKcal.setText(String.valueOf((int)productData.nutriments.calories100g));
        }
        etGrams.setText("100");

        new AlertDialog.Builder(this)
                .setTitle("Add Scanned Food")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    String name = etName.getText().toString();
                    int grams = Integer.parseInt(etGrams.getText().toString().isEmpty() ? "100" : etGrams.getText().toString());
                    int p = Integer.parseInt(etProtein.getText().toString().isEmpty() ? "0" : etProtein.getText().toString());
                    int c = Integer.parseInt(etCarbs.getText().toString().isEmpty() ? "0" : etCarbs.getText().toString());
                    int f = Integer.parseInt(etFat.getText().toString().isEmpty() ? "0" : etFat.getText().toString());
                    int kcal = (p * 4) + (c * 4) + (f * 9);

                    db.foodItemDAO().insert(new FoodItem(name, kcal, p, f, c, grams, spinnerMeal.getSelectedItem().toString(), todayDate));
                    
                    // Update daily record consumed calories
                    DailyRecord record = db.dailyRecordDAO().getByDate(todayDate);
                    if (record == null) record = new DailyRecord(todayDate);
                    record.setCaloriesConsumed(record.getCaloriesConsumed() + kcal);
                    db.dailyRecordDAO().insertOrUpdate(record);
                    
                    Toast.makeText(this, "Food added!", Toast.LENGTH_SHORT).show();
                    
                    // Refresh dashboard if active
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof DashboardFragment) {
                        ((DashboardFragment) currentFragment).onResume(); // Refresh dashboard
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
