package com.example.sportify;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CustomDrawingView extends View {
    private Paint paint;
    private float pulseScale = 1.0f;
    private RectF rectF = new RectF();
    private Matrix gradientMatrix = new Matrix();

    public CustomDrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    // Called by ObjectAnimator to animate the shadow size
    public void setPulseScale(float scale) {
        this.pulseScale = scale;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f + 30; // Offset the shadow downwards

        // Dimensions of the shadow based on the pulseScale
        float rectW = w * 0.55f * pulseScale;
        float rectH = h * 0.15f * pulseScale;

        if (rectW <= 0 || rectH <= 0) return;

        rectF.set(cx - rectW / 2f, cy - rectH / 2f, cx + rectW / 2f, cy + rectH / 2f);
        float cornerRadius = rectH / 2f;

        // Shadow colors (Dark brown/shadow tones with alpha)
        float radius = rectW / 2f;
        RadialGradient shadowGradient = new RadialGradient(
                cx, cy, radius,
                new int[]{0x55331100, 0x11331100, 0x00000000},
                new float[]{0.0f, 0.7f, 1.0f},
                Shader.TileMode.CLAMP
        );

        // Squashes the radial gradient into an oval shape
        gradientMatrix.reset();
        gradientMatrix.setScale(1.0f, rectH / rectW, cx, cy);
        shadowGradient.setLocalMatrix(gradientMatrix);

        paint.setShader(shadowGradient);
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint);
    }
}