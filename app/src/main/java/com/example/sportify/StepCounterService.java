package com.example.sportify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepCounterService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private AppDatabase db;
    private String todayDate;

    @Override
    public void onCreate() {
        super.onCreate();
        db = SportifyApp.getDatabase();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }

        createNotificationChannel();
        startForeground(1, getNotification("Tracking steps..."));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "step_channel",
                    "Step Tracker Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification getNotification(String content) {
        return new NotificationCompat.Builder(this, "step_channel")
                .setContentTitle("Sportify Active")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            updateStepsInDb();
        }
    }

    private void updateStepsInDb() {
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        new Thread(() -> {
            DailyRecord record = db.dailyRecordDAO().getByDate(todayDate);
            if (record == null) {
                record = new DailyRecord(todayDate);
                record.setStepGoal(10000);
            }
            record.setSteps(record.getSteps() + 1);
            db.dailyRecordDAO().insertOrUpdate(record);
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
