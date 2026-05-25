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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity implements SensorEventListener {

    private LineChart chartWeight, chartCalories;
    private BarChart chartSteps;
    private AppDatabase db;
    private DailyRecordDAO recordDao;
    private String todayDate;

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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.fabAddWeight).setOnClickListener(v -> showWeightDialog());

        // Initialize sensor for shake detection
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        
        loadChartData();
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
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float gForce = (float) Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;

            if (gForce > 2.7f) { // Sensitivity threshold
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastShakeTime > 1000) { // 1 second cooldown
                    lastShakeTime = currentTime;
                    handleShake();
                }
            }
        }
    }

    private void handleShake() {
        // Fun feedback
        Toast.makeText(this, "🔥 KEEP IT UP! YOU ARE DOING GREAT! 🔥", Toast.LENGTH_SHORT).show();
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        
        // Re-animate all charts for a "wow" effect
        chartWeight.animateY(1000);
        chartSteps.animateY(1000);
        chartCalories.animateY(1000);

        // Visual shake of the whole screen
        View root = findViewById(android.R.id.content);
        root.animate()
                .translationXBy(20f)
                .setDuration(50)
                .withEndAction(() -> root.animate()
                        .translationXBy(-40f)
                        .setDuration(50)
                        .withEndAction(() -> root.animate()
                                .translationX(0f)
                                .setDuration(50)
                                .start())
                        .start())
                .start();
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
            if (record == null) {
                record = new DailyRecord(todayDate);
            }
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

    private void loadChartData() {
        new Thread(() -> {
            List<DailyRecord> allRecords = recordDao.getLastDays(30);
            Collections.sort(allRecords, (r1, r2) -> r1.getDate().compareTo(r2.getDate()));

            runOnUiThread(() -> {
                setupWeightChart(allRecords);
                setupStepsChart(allRecords);
                setupCaloriesChart(allRecords);
            });
        }).start();
    }

    private void setupWeightChart(List<DailyRecord> records) {
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
        chartWeight.invalidate();
    }

    private void setupStepsChart(List<DailyRecord> records) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        
        int start = Math.max(0, records.size() - 7);
        int index = 0;
        for (int i = start; i < records.size(); i++) {
            DailyRecord r = records.get(i);
            entries.add(new BarEntry(index, r.getSteps()));
            dates.add(formatDate(r.getDate()));
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Steps");
        dataSet.setColor(ContextCompat.getColor(this, R.color.sportify_progress_calories));
        dataSet.setValueTextColor(Color.GRAY);
        dataSet.setValueTextSize(10f);
        
        chartSteps.setData(new BarData(dataSet));
        setupXAxis(chartSteps.getXAxis(), dates);
        styleChart(chartSteps);
        chartSteps.invalidate();
    }

    private void setupCaloriesChart(List<DailyRecord> records) {
        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        
        int start = Math.max(0, records.size() - 7);
        int index = 0;
        for (int i = start; i < records.size(); i++) {
            DailyRecord r = records.get(i);
            entries.add(new Entry(index, r.getCaloriesConsumed()));
            dates.add(formatDate(r.getDate()));
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Consumed Calories");
        styleLineDataSet(dataSet, ContextCompat.getColor(this, R.color.sportify_progress_sleep));
        
        chartCalories.setData(new LineData(dataSet));
        setupXAxis(chartCalories.getXAxis(), dates);
        styleChart(chartCalories);
        chartCalories.invalidate();
    }

    private void styleLineDataSet(LineDataSet dataSet, int color) {
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setLineWidth(3f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
    }

    private void setupXAxis(XAxis xAxis, List<String> dates) {
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
    }

    private void styleChart(BarLineChartBase<?> chart) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE);
        chart.getAxisRight().setEnabled(false);
        
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setTextColor(Color.GRAY);
        
        chart.animateX(1200);
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
            return outFormat.format(inFormat.parse(dateStr));
        } catch (Exception e) {
            return dateStr;
        }
    }
}
