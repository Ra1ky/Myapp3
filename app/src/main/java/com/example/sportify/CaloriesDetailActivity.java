package com.example.sportify;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sportify.api.OpenFoodFactsService;
import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;
import com.example.sportify.db.FoodItem;
import com.example.sportify.db.Product;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CaloriesDetailActivity extends AppCompatActivity {

    private TextView tvTotalCalories, tvCaloriesGoal;
    private TextView tvTotalProtein, tvTotalCarbs, tvTotalFat;
    private TextView tvProteinGoal, tvCarbsGoal, tvFatGoal;
    private ProgressBar progressCalories;
    private RecyclerView rvFoodItems;
    private FoodAdapter adapter;
    private AppDatabase db;
    private String todayDate;
    
    private int goalKcal = 2000;
    private int goalProtein = 150;
    private int goalCarbs = 250;
    private int goalFat = 70;

    private Product selectedProduct = null;
    private OpenFoodFactsService apiService;
    private ActivityResultLauncher<Intent> scannerLauncher;

    // References for the active dialog to update them after scan
    private AutoCompleteTextView activeEtName;
    private EditText activeEtProtein, activeEtCarbs, activeEtFat, activeEtGrams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calories_detail);

        db = SportifyApp.getDatabase();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://world.openfoodfacts.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(OpenFoodFactsService.class);

        scannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String barcode = result.getData().getStringExtra("barcode");
                        fetchProductByBarcode(barcode);
                    }
                }
        );

        initViews();
        setupRecyclerView();
        loadData();
    }

    private void initViews() {
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        tvCaloriesGoal = findViewById(R.id.tvCaloriesGoal);
        
        tvTotalProtein = findViewById(R.id.tvTotalProtein);
        tvTotalCarbs = findViewById(R.id.tvTotalCarbs);
        tvTotalFat = findViewById(R.id.tvTotalFat);

        tvProteinGoal = findViewById(R.id.tvProteinGoal);
        tvCarbsGoal = findViewById(R.id.tvCarbsGoal);
        tvFatGoal = findViewById(R.id.tvFatGoal);

        progressCalories = findViewById(R.id.progressCalories);
        rvFoodItems = findViewById(R.id.rvFoodItems);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        FloatingActionButton fabAdd = findViewById(R.id.fabAddFood);
        fabAdd.setOnClickListener(v -> showAddFoodDialog());
    }

    private void setupRecyclerView() {
        adapter = new FoodAdapter(this::deleteFoodItem);
        rvFoodItems.setLayoutManager(new LinearLayoutManager(this));
        rvFoodItems.setAdapter(adapter);
    }

    private void loadData() {
        DailyRecord record = db.dailyRecordDAO().getByDate(todayDate);
        if (record != null) {
            goalKcal = record.getCaloriesGoal() > 0 ? record.getCaloriesGoal() : 2000;
            goalProtein = record.getProteinGoal() > 0 ? record.getProteinGoal() : (int)((goalKcal * 0.3) / 4);
            goalCarbs = record.getCarbsGoal() > 0 ? record.getCarbsGoal() : (int)((goalKcal * 0.5) / 4);
            goalFat = record.getFatGoal() > 0 ? record.getFatGoal() : (int)((goalKcal * 0.2) / 9);
        }
        
        tvCaloriesGoal.setText("of " + goalKcal + " kcal");
        tvProteinGoal.setText("/ " + goalProtein + "g");
        tvCarbsGoal.setText("/ " + goalCarbs + "g");
        tvFatGoal.setText("/ " + goalFat + "g");
        progressCalories.setMax(goalKcal);

        refreshFoodList();
    }

    private void refreshFoodList() {
        List<FoodItem> items = db.foodItemDAO().getByDate(todayDate);
        adapter.setFoodItems(items);

        int totalKcal = 0;
        int totalProtein = 0;
        int totalCarbs = 0;
        int totalFat = 0;

        for (FoodItem item : items) {
            totalKcal += item.getCalories();
            totalProtein += item.getProtein();
            totalCarbs += item.getCarbs();
            totalFat += item.getFat();
        }

        tvTotalCalories.setText(String.valueOf(totalKcal));
        tvTotalProtein.setText(String.valueOf(totalProtein));
        tvTotalCarbs.setText(String.valueOf(totalCarbs));
        tvTotalFat.setText(String.valueOf(totalFat));
        progressCalories.setProgress(totalKcal);

        DailyRecord record = db.dailyRecordDAO().getByDate(todayDate);
        if (record == null) {
            record = new DailyRecord(todayDate);
            record.setCaloriesGoal(goalKcal);
            record.setProteinGoal(goalProtein);
            record.setCarbsGoal(goalCarbs);
            record.setFatGoal(goalFat);
        }
        record.setCaloriesConsumed(totalKcal);
        db.dailyRecordDAO().insertOrUpdate(record);
    }

    private void showAddFoodDialog() {
        selectedProduct = null;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_food, null);
        Spinner spinnerMeal = view.findViewById(R.id.spinnerMealType);
        activeEtName = view.findViewById(R.id.etFoodName);
        activeEtGrams = view.findViewById(R.id.etFoodGrams);
        EditText etKcal = view.findViewById(R.id.etFoodCalories);
        activeEtProtein = view.findViewById(R.id.etFoodProtein);
        activeEtCarbs = view.findViewById(R.id.etFoodCarbs);
        activeEtFat = view.findViewById(R.id.etFoodFat);

        view.findViewById(R.id.btnScanBarcode).setOnClickListener(v -> {
            Intent intent = new Intent(this, ScannerActivity.class);
            scannerLauncher.launch(intent);
        });

        etKcal.setFocusable(false);
        etKcal.setHint("Calories (auto)");
        activeEtGrams.setText("100");

        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack"};
        ArrayAdapter<String> mealAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mealTypes);
        mealAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMeal.setAdapter(mealAdapter);

        List<Product> allProducts = db.productDAO().searchProducts("%");
        String[] productNames = new String[allProducts.size()];
        for (int i = 0; i < allProducts.size(); i++) productNames[i] = allProducts.get(i).getName();
        
        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, productNames);
        activeEtName.setAdapter(productAdapter);

        activeEtName.setOnItemClickListener((parent, v, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            selectedProduct = db.productDAO().getByName(selected);
            if (selectedProduct != null) {
                updateMacrosByGrams();
            }
        });

        activeEtGrams.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateMacrosByGrams(); }
        });

        TextWatcher macroWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    int p = activeEtProtein.getText().toString().isEmpty() ? 0 : Integer.parseInt(activeEtProtein.getText().toString());
                    int c = activeEtCarbs.getText().toString().isEmpty() ? 0 : Integer.parseInt(activeEtCarbs.getText().toString());
                    int f = activeEtFat.getText().toString().isEmpty() ? 0 : Integer.parseInt(activeEtFat.getText().toString());
                    etKcal.setText(String.valueOf((p * 4) + (c * 4) + (f * 9)));
                } catch (NumberFormatException e) { etKcal.setText("0"); }
            }
        };

        activeEtProtein.addTextChangedListener(macroWatcher);
        activeEtCarbs.addTextChangedListener(macroWatcher);
        activeEtFat.addTextChangedListener(macroWatcher);

        new AlertDialog.Builder(this)
                .setTitle("Add Food")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = activeEtName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        int grams = Integer.parseInt(activeEtGrams.getText().toString().isEmpty() ? "100" : activeEtGrams.getText().toString());
                        int kcal = Integer.parseInt(etKcal.getText().toString());
                        int p = Integer.parseInt(activeEtProtein.getText().toString().isEmpty() ? "0" : activeEtProtein.getText().toString());
                        int c = Integer.parseInt(activeEtCarbs.getText().toString().isEmpty() ? "0" : activeEtCarbs.getText().toString());
                        int f = Integer.parseInt(activeEtFat.getText().toString().isEmpty() ? "0" : activeEtFat.getText().toString());
                        
                        db.foodItemDAO().insert(new FoodItem(name, kcal, p, f, c, grams, spinnerMeal.getSelectedItem().toString(), todayDate));
                        refreshFoodList();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateMacrosByGrams() {
        if (selectedProduct == null) return;
        try {
            int grams = Integer.parseInt(activeEtGrams.getText().toString().isEmpty() ? "0" : activeEtGrams.getText().toString());
            activeEtProtein.setText(String.valueOf((selectedProduct.getProtein() * grams) / 100));
            activeEtCarbs.setText(String.valueOf((selectedProduct.getCarbs() * grams) / 100));
            activeEtFat.setText(String.valueOf((selectedProduct.getFat() * grams) / 100));
        } catch (Exception ignored) {}
    }

    private void fetchProductByBarcode(String barcode) {
        apiService.getProduct(barcode).enqueue(new Callback<OpenFoodFactsService.ProductResponse>() {
            @Override
            public void onResponse(Call<OpenFoodFactsService.ProductResponse> call, Response<OpenFoodFactsService.ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().status == 1) {
                    OpenFoodFactsService.ProductData p = response.body().product;
                    activeEtName.setText(p.productName);
                    activeEtProtein.setText(String.valueOf((int)p.nutriments.proteins100g));
                    activeEtCarbs.setText(String.valueOf((int)p.nutriments.carbohydrates100g));
                    activeEtFat.setText(String.valueOf((int)p.nutriments.fat100g));
                    activeEtGrams.setText("100");
                    selectedProduct = new Product(p.productName, (int)p.nutriments.calories100g, (int)p.nutriments.proteins100g, (int)p.nutriments.fat100g, (int)p.nutriments.carbohydrates100g);
                } else {
                    Toast.makeText(CaloriesDetailActivity.this, "Product not found", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<OpenFoodFactsService.ProductResponse> call, Throwable t) {
                Toast.makeText(CaloriesDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteFoodItem(FoodItem item) {
        db.foodItemDAO().delete(item);
        refreshFoodList();
    }
}
