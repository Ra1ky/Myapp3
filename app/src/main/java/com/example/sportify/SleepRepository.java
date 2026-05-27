package com.example.sportify;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.records.SleepSessionRecord;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.response.ReadRecordsResponse;
import androidx.health.connect.client.time.TimeRangeFilter;

import com.example.sportify.db.AppDatabase;
import com.example.sportify.db.DailyRecord;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.JvmClassMappingKt;
import kotlinx.coroutines.BuildersKt;

public class SleepRepository {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SyncCallback {
        void onResult(int sleepMinutes);
        void onError(String message);
    }

    public static boolean isAvailable(Context context) {
        return HealthConnectClient.getSdkStatus(context.getApplicationContext())
                == HealthConnectClient.SDK_AVAILABLE;
    }

    @SuppressWarnings("unchecked")
    public static void syncLastNight(Context context, AppDatabase db, String todayDate, SyncCallback callback) {
        executor.execute(() -> {
            try {
                HealthConnectClient client = HealthConnectClient.getOrCreate(context.getApplicationContext());

                LocalDate today = LocalDate.parse(todayDate);
                // Yesterday 3 pm → today noon: captures overnight sleep
                Instant windowStart = today.minusDays(1)
                        .atTime(LocalTime.of(15, 0))
                        .atZone(ZoneId.systemDefault()).toInstant();
                Instant windowEnd = today
                        .atTime(LocalTime.of(12, 0))
                        .atZone(ZoneId.systemDefault()).toInstant();

                // JvmClassMappingKt.getKotlinClass bridges Java Class → Kotlin KClass (stdlib, no kotlin-reflect needed)
                ReadRecordsRequest<SleepSessionRecord> request = new ReadRecordsRequest<>(
                        JvmClassMappingKt.getKotlinClass(SleepSessionRecord.class),
                        TimeRangeFilter.between(windowStart, windowEnd),
                        Collections.emptySet(),
                        true,
                        1000,
                        null
                );

                // BuildersKt.runBlocking is safe on a background thread; it bridges the suspend function to blocking Java
                ReadRecordsResponse<SleepSessionRecord> response =
                        BuildersKt.<ReadRecordsResponse<SleepSessionRecord>>runBlocking(
                                EmptyCoroutineContext.INSTANCE,
                                (scope, cont) -> client.readRecords(request, cont)
                        );

                long totalMillis = 0;
                for (SleepSessionRecord s : response.getRecords()) {
                    totalMillis += s.getEndTime().toEpochMilli() - s.getStartTime().toEpochMilli();
                }
                int totalMinutes = (int) (totalMillis / 60_000L);

                DailyRecord record = db.dailyRecordDAO().getByDate(todayDate);
                if (record == null) record = new DailyRecord(todayDate);
                record.setSleepMinutes(totalMinutes);
                db.dailyRecordDAO().insertOrUpdate(record);

                final int result = totalMinutes;
                mainHandler.post(() -> callback.onResult(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Health Connect error"
                ));
            }
        });
    }
}
