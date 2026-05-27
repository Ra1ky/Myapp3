package com.example.sportify;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;
import com.example.sportify.db.DailyRecordDAO;
import com.example.sportify.db.UserProfile;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class StatisticsActivity extends AppCompatActivity implements SensorEventListener {

    private LineChart chartWeight, chartCalories;
    private BarChart chartSteps, chartSleepQuality;
    private MaterialButtonToggleGroup toggleWeight, toggleSteps, toggleCalories, toggleSleepQuality;
    private AppDatabase db;
    private DailyRecordDAO recordDao;
    private String todayDate;

    private List<DailyRecord> cachedRecords = new ArrayList<>();
    private int weightDays = 7;
    private int stepsDays = 7;
    private int caloriesDays = 7;
    private int sleepQualityDays = 7;
    private int debugPattern = 0;

    // Shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        db = SportifyApp.getDatabase();
        recordDao = db.dailyRecordDAO();
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        chartWeight = findViewById(R.id.chartWeight);
        chartSteps = findViewById(R.id.chartSteps);
        chartCalories = findViewById(R.id.chartCalories);
        chartSleepQuality = findViewById(R.id.chartSleepQuality);
        toggleWeight = findViewById(R.id.toggleWeight);
        toggleSteps = findViewById(R.id.toggleSteps);
        toggleCalories = findViewById(R.id.toggleCalories);
        toggleSleepQuality = findViewById(R.id.toggleSleepQuality);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAddWeight).setOnClickListener(v -> showWeightDialog());
        findViewById(R.id.btnDebugRandomData).setOnClickListener(v -> fillRandomData());

        setupToggleListeners();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        loadChartData();
    }

    private void setupToggleListeners() {
        toggleWeight.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnWeight7d) weightDays = 7;
            else if (checkedId == R.id.btnWeight30d) weightDays = 30;
            else weightDays = 90;
            if (!cachedRecords.isEmpty()) setupWeightChart(cachedRecords);
        });

        toggleSteps.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnSteps7d) stepsDays = 7;
            else if (checkedId == R.id.btnSteps14d) stepsDays = 14;
            else stepsDays = 30;
            if (!cachedRecords.isEmpty()) setupStepsChart(cachedRecords);
        });

        toggleCalories.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnCal7d) caloriesDays = 7;
            else if (checkedId == R.id.btnCal14d) caloriesDays = 14;
            else caloriesDays = 30;
            if (!cachedRecords.isEmpty()) setupCaloriesChart(cachedRecords);
        });

        toggleSleepQuality.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnSleep7d) sleepQualityDays = 7;
            else if (checkedId == R.id.btnSleep14d) sleepQualityDays = 14;
            else sleepQualityDays = 30;
            if (!cachedRecords.isEmpty()) setupSleepQualityChart(cachedRecords);
        });

        toggleWeight.check(R.id.btnWeight7d);
        toggleSteps.check(R.id.btnSteps7d);
        toggleCalories.check(R.id.btnCal7d);
        toggleSleepQuality.check(R.id.btnSleep7d);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0], y = event.values[1], z = event.values[2];
            float gForce = (float) Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
            if (gForce > 2.7f) {
                long now = System.currentTimeMillis();
                if (now - lastShakeTime > 1000) {
                    lastShakeTime = now;
                    handleShake();
                }
            }
        }
    }

    private void handleShake() {
        Toast.makeText(this, "🔥 KEEP IT UP! YOU ARE DOING GREAT! 🔥", Toast.LENGTH_SHORT).show();
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        chartWeight.animateY(1000);
        chartSteps.animateY(1000);
        chartCalories.animateY(1000);
        chartSleepQuality.animateY(1000);
        View root = findViewById(android.R.id.content);
        root.animate().translationXBy(20f).setDuration(50)
                .withEndAction(() -> root.animate().translationXBy(-40f).setDuration(50)
                        .withEndAction(() -> root.animate().translationX(0f).setDuration(50).start()).start()).start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void showWeightDialog() {
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setHint("e.g. 70.5");

        new Thread(() -> {
            DailyRecord record = recordDao.getByDate(todayDate);
            if (record != null && record.getWeightKg() > 0) {
                runOnUiThread(() -> et.setText(String.valueOf(record.getWeightKg())));
            }
        }).start();

        new AlertDialog.Builder(this)
                .setTitle("Log Today's Weight")
                .setMessage("Enter your weight in kg:")
                .setView(et)
                .setPositiveButton("Save", (dialog, which) -> {
                    String value = et.getText().toString();
                    if (!value.isEmpty()) {
                        try {
                            saveWeight(Float.parseFloat(value));
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Invalid weight format", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveWeight(float weight) {
        new Thread(() -> {
            DailyRecord record = recordDao.getByDate(todayDate);
            if (record == null) record = new DailyRecord(todayDate);
            record.setWeightKg(weight);
            recordDao.insertOrUpdate(record);

            UserProfile profile = db.userProfileDAO().getProfile();
            if (profile != null) {
                profile.setWeightKg(weight);
                db.userProfileDAO().insertOrUpdate(profile);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Weight saved!", Toast.LENGTH_SHORT).show();
                loadChartData();
            });
        }).start();
    }

    private void fillRandomData() {
        Random rng = new Random();
        int points = 14;
        // Cycle through 4 patterns: random, upward trend, downward trend, spiky
        int pattern = debugPattern % 4;
        debugPattern++;

        // Weight chart
        List<Entry> weightEntries = new ArrayList<>();
        List<String> weightDates = new ArrayList<>();
        float weightBase = 65f + rng.nextFloat() * 20f;
        for (int i = 0; i < points; i++) {
            float delta;
            if (pattern == 1)      delta = i * 0.3f + rng.nextFloat() * 1.5f;
            else if (pattern == 2) delta = -i * 0.3f + rng.nextFloat() * 1.5f;
            else if (pattern == 3) delta = (i % 3 == 0 ? 4f : -1f) * rng.nextFloat();
            else                   delta = (rng.nextFloat() - 0.5f) * 5f;
            weightEntries.add(new Entry(i, Math.max(40f, weightBase + delta)));
            weightDates.add("D" + (i + 1));
        }
        LineDataSet weightSet = new LineDataSet(weightEntries, "Weight (kg)");
        styleLineDataSet(weightSet, ContextCompat.getColor(this, R.color.sportify_green));
        chartWeight.setData(new LineData(weightSet));
        setupXAxis(chartWeight.getXAxis(), weightDates);
        styleChart(chartWeight);
        chartWeight.invalidate();

        // Steps chart
        List<BarEntry> stepsEntries = new ArrayList<>();
        List<String> stepsDates = new ArrayList<>();
        for (int i = 0; i < points; i++) {
            float base;
            if (pattern == 1)      base = 3000f + i * 500f + rng.nextFloat() * 1000f;
            else if (pattern == 2) base = 12000f - i * 500f + rng.nextFloat() * 1000f;
            else if (pattern == 3) base = i % 2 == 0 ? 1000f + rng.nextFloat() * 2000f : 8000f + rng.nextFloat() * 5000f;
            else                   base = 2000f + rng.nextFloat() * 11000f;
            stepsEntries.add(new BarEntry(i, Math.max(0f, base)));
            stepsDates.add("D" + (i + 1));
        }
        BarDataSet stepsSet = new BarDataSet(stepsEntries, "Daily Steps");
        stepsSet.setColor(ContextCompat.getColor(this, R.color.sportify_progress_calories));
        stepsSet.setValueTextColor(Color.GRAY);
        stepsSet.setValueTextSize(10f);
        stepsSet.setHighlightEnabled(false);
        chartSteps.setData(new BarData(stepsSet));
        setupXAxis(chartSteps.getXAxis(), stepsDates);
        styleChart(chartSteps);
        chartSteps.invalidate();

        // Calories chart
        List<Entry> calEntries = new ArrayList<>();
        List<String> calDates = new ArrayList<>();
        float calBase = 1500f + rng.nextFloat() * 500f;
        for (int i = 0; i < points; i++) {
            float delta;
            if (pattern == 1)      delta = i * 40f + rng.nextFloat() * 200f;
            else if (pattern == 2) delta = -i * 40f + rng.nextFloat() * 200f;
            else if (pattern == 3) delta = (i % 3 == 0 ? 800f : -200f) * rng.nextFloat();
            else                   delta = (rng.nextFloat() - 0.5f) * 800f;
            calEntries.add(new Entry(i, Math.max(0f, calBase + delta)));
            calDates.add("D" + (i + 1));
        }
        LineDataSet calSet = new LineDataSet(calEntries, "Consumed Calories");
        styleLineDataSet(calSet, ContextCompat.getColor(this, R.color.sportify_progress_sleep));
        chartCalories.setData(new LineData(calSet));
        setupXAxis(chartCalories.getXAxis(), calDates);
        styleChart(chartCalories);
        chartCalories.invalidate();

        // Sleep quality chart
        List<BarEntry> sleepEntries = new ArrayList<>();
        List<String> sleepDates = new ArrayList<>();
        for (int i = 0; i < points; i++) {
            float mood;
            if (pattern == 1)      mood = Math.min(5f, 1f + i * (4f / points) + rng.nextFloat());
            else if (pattern == 2) mood = Math.max(1f, 5f - i * (4f / points) + rng.nextFloat() - 0.5f);
            else if (pattern == 3) mood = 1f + (i % 5);
            else                   mood = 1f + rng.nextInt(5);
            sleepEntries.add(new BarEntry(i, mood));
            sleepDates.add("D" + (i + 1));
        }
        BarDataSet sleepSet = new BarDataSet(sleepEntries, "Sleep Quality");
        sleepSet.setColor(ContextCompat.getColor(this, R.color.sportify_progress_sleep));
        sleepSet.setValueTextColor(Color.GRAY);
        sleepSet.setValueTextSize(10f);
        sleepSet.setHighlightEnabled(false);
        sleepSet.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) { return String.valueOf((int) value); }
        });
        chartSleepQuality.setData(new BarData(sleepSet));
        setupXAxis(chartSleepQuality.getXAxis(), sleepDates);
        styleChart(chartSleepQuality);
        chartSleepQuality.getAxisLeft().setAxisMaximum(5f);
        chartSleepQuality.getAxisLeft().setGranularity(1f);
        chartSleepQuality.invalidate();
    }

    private void loadChartData() {
        new Thread(() -> {
            List<DailyRecord> allRecords = recordDao.getLastDays(90);
            Collections.sort(allRecords, (r1, r2) -> r1.getDate().compareTo(r2.getDate()));
            cachedRecords = allRecords;

            // Ensure today's record has the profile weight so the chart always has at least one point
            boolean hasWeightData = allRecords.stream().anyMatch(r -> r.getWeightKg() > 0);
            if (!hasWeightData) {
                UserProfile profile = db.userProfileDAO().getProfile();
                if (profile != null && profile.getWeightKg() > 0) {
                    DailyRecord today = recordDao.getByDate(todayDate);
                    if (today == null) today = new DailyRecord(todayDate);
                    today.setWeightKg(profile.getWeightKg());
                    recordDao.insertOrUpdate(today);
                    // Reload so the chart reflects the seeded value
                    List<DailyRecord> refreshed = recordDao.getLastDays(90);
                    Collections.sort(refreshed, (r1, r2) -> r1.getDate().compareTo(r2.getDate()));
                    cachedRecords = refreshed;
                }
            }

            runOnUiThread(() -> {
                setupWeightChart(cachedRecords);
                setupStepsChart(cachedRecords);
                setupCaloriesChart(cachedRecords);
                setupSleepQualityChart(cachedRecords);
            });
        }).start();
    }

    private void setupWeightChart(List<DailyRecord> allRecords) {
        List<DailyRecord> records = allRecords.subList(
                Math.max(0, allRecords.size() - weightDays), allRecords.size());

        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        int index = 0;
        for (DailyRecord r : records) {
            if (r.getWeightKg() > 0) {
                entries.add(new Entry(index, r.getWeightKg()));
                dates.add(formatDate(r.getDate()));
                index++;
            }
        }

        if (entries.isEmpty()) {
            chartWeight.setNoDataText("No weight data recorded yet.");
            chartWeight.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Weight (kg)");
        styleLineDataSet(dataSet, ContextCompat.getColor(this, R.color.sportify_green));
        chartWeight.setData(new LineData(dataSet));
        setupXAxis(chartWeight.getXAxis(), dates);
        styleChart(chartWeight);
        chartWeight.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) { return (int) value + " kg"; }
        });
        chartWeight.invalidate();
    }

    private void setupStepsChart(List<DailyRecord> allRecords) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        int start = Math.max(0, allRecords.size() - stepsDays);
        int index = 0;
        for (int i = start; i < allRecords.size(); i++) {
            DailyRecord r = allRecords.get(i);
            entries.add(new BarEntry(index, r.getSteps()));
            dates.add(formatDate(r.getDate()));
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Steps");
        dataSet.setColor(ContextCompat.getColor(this, R.color.sportify_progress_calories));
        dataSet.setValueTextColor(Color.GRAY);
        dataSet.setValueTextSize(13f);
        dataSet.setHighlightEnabled(false);

        chartSteps.setData(new BarData(dataSet));
        setupXAxis(chartSteps.getXAxis(), dates);
        styleChart(chartSteps);
        chartSteps.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return value >= 1000 ? (int)(value / 1000) + "k" : (int) value + "";
            }
        });
        chartSteps.invalidate();
    }

    private void setupCaloriesChart(List<DailyRecord> allRecords) {
        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        int start = Math.max(0, allRecords.size() - caloriesDays);
        int index = 0;
        for (int i = start; i < allRecords.size(); i++) {
            DailyRecord r = allRecords.get(i);
            entries.add(new Entry(index, r.getCaloriesConsumed()));
            dates.add(formatDate(r.getDate()));
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Consumed Calories");
        styleLineDataSet(dataSet, ContextCompat.getColor(this, R.color.sportify_progress_sleep));
        chartCalories.setData(new LineData(dataSet));
        setupXAxis(chartCalories.getXAxis(), dates);
        styleChart(chartCalories);
        chartCalories.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) { return (int) value + " kcal"; }
        });
        chartCalories.invalidate();
    }

    private void setupSleepQualityChart(List<DailyRecord> allRecords) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        int start = Math.max(0, allRecords.size() - sleepQualityDays);
        int index = 0;
        for (int i = start; i < allRecords.size(); i++) {
            DailyRecord r = allRecords.get(i);
            entries.add(new BarEntry(index, r.getSleepMood()));
            dates.add(formatDate(r.getDate()));
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Sleep Quality");
        dataSet.setColor(ContextCompat.getColor(this, R.color.sportify_progress_sleep));
        dataSet.setValueTextColor(Color.GRAY);
        dataSet.setValueTextSize(13f);
        dataSet.setHighlightEnabled(false);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return value == 0 ? "" : String.valueOf((int) value);
            }
        });

        chartSleepQuality.setData(new BarData(dataSet));
        setupXAxis(chartSleepQuality.getXAxis(), dates);
        styleChart(chartSleepQuality);
        chartSleepQuality.getAxisLeft().setAxisMaximum(5f);
        chartSleepQuality.getAxisLeft().setGranularity(1f);
        chartSleepQuality.getAxisLeft().setValueFormatter(new ValueFormatter() {
            private final String[] labels = {"", "😞", "😕", "😐", "😊", "😄"};
            @Override public String getFormattedValue(float value) {
                int i = (int) value;
                return (i >= 0 && i < labels.length) ? labels[i] : "";
            }
        });
        chartSleepQuality.setNoDataText("No sleep mood recorded yet.");
        chartSleepQuality.invalidate();
    }

    private void styleLineDataSet(LineDataSet dataSet, int color) {
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(3f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setValueTextSize(13f);
        dataSet.setHighlightEnabled(false);
    }

    private void setupXAxis(XAxis xAxis, List<String> dates) {
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(12f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
    }

    private void styleChart(BarLineChartBase<?> chart) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE);
        chart.getAxisRight().setEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setScaleEnabled(false);
        chart.setHighlightPerTapEnabled(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setTextSize(12f);

        chart.animateX(1200);
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outFormat = new SimpleDateFormat("d MMM", new Locale("lt", "LT"));
            return outFormat.format(inFormat.parse(dateStr));
        } catch (Exception e) {
            return dateStr;
        }
    }
}
