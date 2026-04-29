package com.example.sportify;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sportify.api.OpenFoodFactsService;
import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;
import com.example.sportify.db.FoodItem;
import com.example.sportify.db.Product;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CaloriesDetailActivity extends AppCompatActivity {

    private TextView tvTotalCalories, tvCaloriesGoal, tvStatusMessage;
    private TextView tvTotalProtein, tvTotalCarbs, tvTotalFat;
    private TextView tvProteinGoal, tvCarbsGoal, tvFatGoal;
    private ProgressBar progressCalories, progressProtein, progressCarbs, progressFat;
    private View blockProtein, blockCarbs, blockFat;
    private RecyclerView rvFoodItems;
    private FoodAdapter adapter;
    private AppDatabase db;
    private String todayDate;
    
    private int goalKcal = 2000;
    private int goalProtein = 150;
    private int goalCarbs = 250;
    private int goalFat = 70;

    private int lastCaloriesValue = 0;
    private int lastProteinValue = 0;
    private int lastCarbsValue = 0;
    private int lastFatValue = 0;

    private Product selectedProduct = null;
    private OpenFoodFactsService apiService;
    private ActivityResultLauncher<Intent> scannerLauncher;

    private AutoCompleteTextView activeEtName;
    private EditText activeEtProtein, activeEtCarbs, activeEtFat, activeEtGrams;

    private final List<ObjectAnimator> decorAnimators = new ArrayList<>();

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
        animateEntrance();
        startDecorAnimations();
    }

    private void initViews() {
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        tvCaloriesGoal = findViewById(R.id.tvCaloriesGoal);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        tvTotalProtein = findViewById(R.id.tvTotalProtein);
        tvTotalCarbs = findViewById(R.id.tvTotalCarbs);
        tvTotalFat = findViewById(R.id.tvTotalFat);
        tvProteinGoal = findViewById(R.id.tvProteinGoal);
        tvCarbsGoal = findViewById(R.id.tvCarbsGoal);
        tvFatGoal = findViewById(R.id.tvFatGoal);
        progressCalories = findViewById(R.id.progressCalories);
        progressProtein = findViewById(R.id.progressProtein);
        progressCarbs = findViewById(R.id.progressCarbs);
        progressFat = findViewById(R.id.progressFat);
        
        blockProtein = findViewById(R.id.blockProtein);
        blockCarbs = findViewById(R.id.blockCarbs);
        blockFat = findViewById(R.id.blockFat);
        
        rvFoodItems = findViewById(R.id.rvFoodItems);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAddFood).setOnClickListener(v -> showAddFoodDialog());
    }

    private void animateEntrance() {
        View summaryCard = findViewById(R.id.summaryCard);
        if (summaryCard != null) {
            summaryCard.setAlpha(0f);
            summaryCard.setTranslationY(100f);
            summaryCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        View[] blocks = {blockProtein, blockCarbs, blockFat};
        for (int i = 0; i < blocks.length; i++) {
            View b = blocks[i];
            if (b != null) {
                b.setAlpha(0f);
                b.setTranslationY(30f);
                b.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(600)
                        .setStartDelay(500 + (i * 150))
                        .setInterpolator(new OvershootInterpolator())
                        .start();
            }
        }
        
        rvFoodItems.setAlpha(0f);
        rvFoodItems.setTranslationY(50f);
        rvFoodItems.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(300)
                .start();

        FloatingActionButton fab = findViewById(R.id.fabAddFood);
        fab.setScaleX(0f);
        fab.setScaleY(0f);
        fab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setStartDelay(800)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    private void startDecorAnimations() {
        View decor1 = findViewById(R.id.decorIcon1);
        View decor2 = findViewById(R.id.decorIcon2);

        if (decor1 != null) {
            applyFloatingAnimation(decor1, 3000, 0, 20f, 10f);
        }
        if (decor2 != null) {
            applyFloatingAnimation(decor2, 3500, 500, -25f, 15f);
        }
    }

    private void applyFloatingAnimation(View v, long duration, long delay, float translationY, float rotation) {
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(v, "translationY", -translationY, translationY);
        floatAnim.setDuration(duration);
        floatAnim.setRepeatMode(ValueAnimator.REVERSE);
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        floatAnim.setStartDelay(delay);
        floatAnim.start();
        decorAnimators.add(floatAnim);

        ObjectAnimator rotateAnim = ObjectAnimator.ofFloat(v, "rotation", -rotation, rotation);
        rotateAnim.setDuration(duration + 500);
        rotateAnim.setRepeatMode(ValueAnimator.REVERSE);
        rotateAnim.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        rotateAnim.setStartDelay(delay);
        rotateAnim.start();
        decorAnimators.add(rotateAnim);
    }

    private void setupRecyclerView() {
        adapter = new FoodAdapter(this::deleteFoodItem);
        rvFoodItems.setLayoutManager(new LinearLayoutManager(this));
        rvFoodItems.setAdapter(adapter);
        rvFoodItems.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
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
        tvProteinGoal.setText("/" + goalProtein + "g");
        tvCarbsGoal.setText("/" + goalCarbs + "g");
        tvFatGoal.setText("/" + goalFat + "g");
        
        progressCalories.setMax(goalKcal);
        progressProtein.setMax(goalProtein);
        progressCarbs.setMax(goalCarbs);
        progressFat.setMax(goalFat);

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

        animateSummaryUpdate(totalKcal, totalProtein, totalCarbs, totalFat);

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

    private void animateSummaryUpdate(int kcal, int p, int c, int f) {
        updateStatusMessage(kcal);

        progressCalories.animate()
                .scaleY(1.3f)
                .setDuration(200)
                .withEndAction(() -> progressCalories.animate().scaleY(1f).setDuration(200).start())
                .start();

        ObjectAnimator animCalories = ObjectAnimator.ofInt(progressCalories, "progress", progressCalories.getProgress(), kcal);
        animCalories.setDuration(1000);
        animCalories.setInterpolator(new DecelerateInterpolator());
        animCalories.start();
        
        ObjectAnimator animProtein = ObjectAnimator.ofInt(progressProtein, "progress", progressProtein.getProgress(), p);
        animProtein.setDuration(1000);
        animProtein.setInterpolator(new DecelerateInterpolator());
        animProtein.start();
        
        ObjectAnimator animCarbs = ObjectAnimator.ofInt(progressCarbs, "progress", progressCarbs.getProgress(), c);
        animCarbs.setDuration(1000);
        animCarbs.setInterpolator(new DecelerateInterpolator());
        animCarbs.start();
        
        ObjectAnimator animFat = ObjectAnimator.ofInt(progressFat, "progress", progressFat.getProgress(), f);
        animFat.setDuration(1000);
        animFat.setInterpolator(new DecelerateInterpolator());
        animFat.start();

        animateTextView(tvTotalCalories, lastCaloriesValue, kcal);
        animateTextView(tvTotalProtein, lastProteinValue, p);
        animateTextView(tvTotalCarbs, lastCarbsValue, c);
        animateTextView(tvTotalFat, lastFatValue, f);

        if (kcal > goalKcal) {
            tvTotalCalories.setTextColor(ContextCompat.getColor(this, R.color.sportify_error));
            shakeView(tvTotalCalories);
        } else {
            tvTotalCalories.setTextColor(ContextCompat.getColor(this, R.color.sportify_progress_calories));
        }
        
        lastCaloriesValue = kcal;
        lastProteinValue = p;
        lastCarbsValue = c;
        lastFatValue = f;
    }

    private void updateStatusMessage(int kcal) {
        if (kcal == 0) {
            tvStatusMessage.setText("Let's start tracking!");
            tvStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.sportify_text_secondary));
        } else if (kcal < goalKcal * 0.5) {
            tvStatusMessage.setText("Good start! Keep it up.");
            tvStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.sportify_green));
        } else if (kcal < goalKcal * 0.9) {
            tvStatusMessage.setText("You're doing great! Almost there.");
            tvStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.sportify_green));
        } else if (kcal <= goalKcal) {
            tvStatusMessage.setText("Perfect! Goal reached.");
            tvStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.sportify_green));
        } else {
            tvStatusMessage.setText("Over the limit. Watch out!");
            tvStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.sportify_error));
        }
        
        tvStatusMessage.setAlpha(0f);
        tvStatusMessage.animate().alpha(1f).setDuration(500).start();
    }

    private void animateTextView(TextView tv, int start, int end) {
        ValueAnimator anim = ValueAnimator.ofInt(start, end);
        anim.setDuration(1000);
        anim.addUpdateListener(animation -> tv.setText(animation.getAnimatedValue().toString()));
        anim.start();
    }

    private void shakeView(View view) {
        ObjectAnimator.ofFloat(view, "translationX", 0, 20, -20, 20, -20, 10, -10, 5, -5, 0)
                .setDuration(500)
                .start();
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

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Food")
                .setView(view)
                .setPositiveButton("Add", (d, which) -> {
                    String name = activeEtName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        int grams = Integer.parseInt(activeEtGrams.getText().toString().isEmpty() ? "100" : activeEtGrams.getText().toString());
                        int kcal = Integer.parseInt(etKcal.getText().toString().isEmpty() ? "0" : etKcal.getText().toString());
                        int p = Integer.parseInt(activeEtProtein.getText().toString().isEmpty() ? "0" : activeEtProtein.getText().toString());
                        int c = Integer.parseInt(activeEtCarbs.getText().toString().isEmpty() ? "0" : activeEtCarbs.getText().toString());
                        int f = Integer.parseInt(activeEtFat.getText().toString().isEmpty() ? "0" : activeEtFat.getText().toString());
                        
                        db.foodItemDAO().insert(new FoodItem(name, kcal, p, f, c, grams, spinnerMeal.getSelectedItem().toString(), todayDate));
                        refreshFoodList();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        view.post(() -> animateDialogContent((ViewGroup) view));
    }

    private void animateDialogContent(ViewGroup root) {
        int delay = 0;
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);

            if (child instanceof ViewGroup && !(child instanceof com.google.android.material.textfield.TextInputLayout)) {
                ViewGroup group = (ViewGroup) child;
                for (int j = 0; j < group.getChildCount(); j++) {
                    View subChild = group.getChildAt(j);
                    applyStrongPopAnimation(subChild, delay);
                    delay += 70;
                }
            } else {
                applyStrongPopAnimation(child, delay);
                delay += 100;
            }
        }
    }

    private void applyStrongPopAnimation(View v, int delay) {
        v.setAlpha(0f);
        v.setTranslationY(150f); 
        v.setScaleX(0.4f);
        v.setScaleY(0.4f);
        
        v.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setStartDelay(delay)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ObjectAnimator anim : decorAnimators) {
            anim.cancel();
        }
        decorAnimators.clear();
    }
}
