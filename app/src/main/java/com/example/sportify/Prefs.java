package com.example.sportify;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

// Lightweight wrapper for app-wide flags that don't belong in the Room DB:
//  – has the user finished the onboarding sequence?
//  – when did they last save profile changes?
public class Prefs {

    private static final String FILE                = "sportify_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_LAST_SAVE_MS    = "last_save_millis";

    private static final long ONE_WEEK_MS = 7L * 24 * 60 * 60 * 1000;

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean isOnboardingDone(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public static void setOnboardingDone(Context ctx, boolean done) {
        prefs(ctx).edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    public static void markSavedNow(Context ctx) {
        prefs(ctx).edit().putLong(KEY_LAST_SAVE_MS, System.currentTimeMillis()).apply();
    }

    // True if the lock from the last save is still in effect.
    public static boolean isProfileLocked(Context ctx) {
        long unlock = getUnlockMillis(ctx);
        if (unlock == 0L) return false;
        return System.currentTimeMillis() < unlock;
    }

    // The exact moment (ms) the profile becomes editable again.
    // Computed as: save time + 7 days, then rounded up to the next 00:00.
    // So a save Monday at 3pm unlocks Tuesday next week at midnight, not at 3pm.
    public static long getUnlockMillis(Context ctx) {
        long last = prefs(ctx).getLong(KEY_LAST_SAVE_MS, 0L);
        if (last == 0L) return 0L;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(last + ONE_WEEK_MS);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, 1); // ceiling to next midnight
        return cal.getTimeInMillis();
    }
}