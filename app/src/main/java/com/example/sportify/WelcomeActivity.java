package com.example.sportify;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

// Shown the very first time the app starts
public class WelcomeActivity extends AppCompatActivity {

    private AnimatorSet ring1Set, ring2Set;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.welcomeRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View pulseRing1 = findViewById(R.id.pulseRing1);
        View pulseRing2 = findViewById(R.id.pulseRing2);
        TextView title = findViewById(R.id.tvWelcomeTitle);
        TextView subtitle = findViewById(R.id.tvWelcomeSubtitle);
        MaterialButton btn = findViewById(R.id.btnGetStarted);

        pulseRing1.setAlpha(0f);
        pulseRing2.setAlpha(0f);
        playEntrance(title, subtitle, btn);
        ring1Set = startRipple(pulseRing1, 1550L);
        ring2Set = startRipple(pulseRing2, 1550L + 1100L);

        btn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_START_ONBOARDING, true);
            startActivity(intent);
            finish();
        });
    }

    // One ripple = scale outward + fade out, looped forever. The two rings share the same animation but start at different times.
    private AnimatorSet startRipple(View ring, long startDelay) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ring, "scaleX", 1f, 1.25f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ring, "scaleY", 1f, 1.25f);
        ObjectAnimator alpha  = ObjectAnimator.ofFloat(ring, "alpha",  0.7f, 0f);

        // Looping is set on each child so the whole AnimatorSet keeps cycling.
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha .setRepeatCount(ValueAnimator.INFINITE);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setDuration(2200);
        set.setStartDelay(startDelay);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
        return set;
    }

    // Staggered entrance: title drops in, subtitle fades, button rises.
    private void playEntrance(View title, View subtitle, View btn) {
        title.setAlpha(0f);
        title.setTranslationY(-40f);
        subtitle.setAlpha(0f);
        btn.setAlpha(0f);
        btn.setTranslationY(40f);

        title.animate()
                .alpha(1f).translationY(0f)
                .setDuration(700).setStartDelay(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        subtitle.animate()
                .alpha(1f)
                .setDuration(600).setStartDelay(550)
                .start();

        btn.animate()
                .alpha(1f).translationY(0f)
                .setDuration(700).setStartDelay(850)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ring1Set != null) ring1Set.cancel();
        if (ring2Set != null) ring2Set.cancel();
    }
}