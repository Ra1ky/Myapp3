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

import android.text.Editable;
import android.text.TextWatcher;

import com.example.sportify.api.OpenFoodFactsService;
import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;
import com.example.sportify.db.FoodItem;
import com.example.sportify.db.Product;
import com.example.sportify.fragments.DashboardFragment;
import com.example.sportify.fragments.ProfileFragment;

import java.util.List;
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
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_food, null);
        Spinner spinnerMeal = view.findViewById(R.id.spinnerMealType);
        AutoCompleteTextView etName = view.findViewById(R.id.etFoodName);
        EditText etGrams = view.findViewById(R.id.etFoodGrams);
        EditText etKcal = view.findViewById(R.id.etFoodCalories);
        EditText etProtein = view.findViewById(R.id.etFoodProtein);
        EditText etCarbs = view.findViewById(R.id.etFoodCarbs);
        EditText etFat = view.findViewById(R.id.etFoodFat);

        // Hide scan button — scan already happened
        view.findViewById(R.id.btnScanBarcode).setVisibility(View.GONE);

        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack"};
        ArrayAdapter<String> mealAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mealTypes);
        mealAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMeal.setAdapter(mealAdapter);

        List<Product> allProducts = db.productDAO().getAllProducts();
        String[] productNames = new String[allProducts.size()];
        for (int i = 0; i < allProducts.size(); i++) productNames[i] = allProducts.get(i).getName();
        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, productNames);
        etName.setAdapter(productAdapter);

        Product[] selected = {null};

        Runnable updateMacros = () -> {
            if (selected[0] == null) return;
            try {
                int g = etGrams.getText().toString().isEmpty() ? 0 : Integer.parseInt(etGrams.getText().toString());
                etProtein.setText(String.valueOf((selected[0].getProtein() * g) / 100));
                etCarbs.setText(String.valueOf((selected[0].getCarbs() * g) / 100));
                etFat.setText(String.valueOf((selected[0].getFat() * g) / 100));
            } catch (Exception ignored) {}
        };

        etName.setOnItemClickListener((parent, v, position, id) -> {
            String s = (String) parent.getItemAtPosition(position);
            selected[0] = db.productDAO().getByName(s);
            updateMacros.run();
        });

        etGrams.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateMacros.run(); }
        });

        TextWatcher macroWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    int p = etProtein.getText().toString().isEmpty() ? 0 : Integer.parseInt(etProtein.getText().toString());
                    int c = etCarbs.getText().toString().isEmpty() ? 0 : Integer.parseInt(etCarbs.getText().toString());
                    int f = etFat.getText().toString().isEmpty() ? 0 : Integer.parseInt(etFat.getText().toString());
                    etKcal.setText(String.valueOf((p * 4) + (c * 4) + (f * 9)));
                } catch (NumberFormatException e) { etKcal.setText("0"); }
            }
        };

        etProtein.addTextChangedListener(macroWatcher);
        etCarbs.addTextChangedListener(macroWatcher);
        etFat.addTextChangedListener(macroWatcher);

        etGrams.setText("100");

        if (productData != null) {
            String pName = productData.productName != null ? productData.productName.trim() : "";
            selected[0] = new Product(
                    pName,
                    (int) productData.nutriments.calories100g,
                    (int) productData.nutriments.proteins100g,
                    (int) productData.nutriments.fat100g,
                    (int) productData.nutriments.carbohydrates100g
            );
            etName.setText(pName);
            updateMacros.run();
            new Thread(() -> db.productDAO().insert(selected[0])).start();
        }

        new AlertDialog.Builder(this)
                .setTitle("Add Food")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) return;
                    try {
                        int grams = Integer.parseInt(etGrams.getText().toString().isEmpty() ? "100" : etGrams.getText().toString());
                        int kcal = Integer.parseInt(etKcal.getText().toString().isEmpty() ? "0" : etKcal.getText().toString());
                        int p = Integer.parseInt(etProtein.getText().toString().isEmpty() ? "0" : etProtein.getText().toString());
                        int c = Integer.parseInt(etCarbs.getText().toString().isEmpty() ? "0" : etCarbs.getText().toString());
                        int f = Integer.parseInt(etFat.getText().toString().isEmpty() ? "0" : etFat.getText().toString());
                        String mealType = spinnerMeal.getSelectedItem() != null ? spinnerMeal.getSelectedItem().toString() : "Breakfast";
                        db.foodItemDAO().insert(new FoodItem(name, kcal, p, f, c, grams, mealType, todayDate));
                        int per100kcal, per100p, per100c, per100f;
                        if (selected[0] != null) {
                            per100kcal = selected[0].getCalories();
                            per100p = selected[0].getProtein();
                            per100c = selected[0].getCarbs();
                            per100f = selected[0].getFat();
                        } else {
                            int g = grams > 0 ? grams : 100;
                            per100kcal = kcal * 100 / g;
                            per100p = p * 100 / g;
                            per100c = c * 100 / g;
                            per100f = f * 100 / g;
                        }
                        db.productDAO().insert(new Product(name, per100kcal, per100p, per100f, per100c));
                        DailyRecord record = db.dailyRecordDAO().getByDate(todayDate);
                        if (record == null) record = new DailyRecord(todayDate);
                        record.setCaloriesConsumed(record.getCaloriesConsumed() + kcal);
                        db.dailyRecordDAO().insertOrUpdate(record);
                        Toast.makeText(this, name + " added", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Please check the entered values", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
