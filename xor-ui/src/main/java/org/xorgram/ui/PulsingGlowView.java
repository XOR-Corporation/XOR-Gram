package org.xorgram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * PulsingGlowView - A view that displays a pulsing glow effect
 * with AMOLED-optimized design (true black background with white accent).
 */
public class PulsingGlowView extends View {

    // AMOLED Colors
    private static final int AMOLED_BLACK = Color.parseColor("#000000");
    private static final int ACCENT_WHITE = Color.parseColor("#FFFFFF");
    private static final int ACCENT_GLOW = Color.parseColor("#33FFFFFF");
    private static final int ACCENT_DIM = Color.parseColor("#1AFFFFFF");

    private Paint glowPaint;
    private Paint centerPaint;
    private Paint ringPaint;
    private Paint textPaint;
    private Paint valuePaint;
    private Paint unitPaint;

    private float pulsePhase = 0f;
    private float glowRadius = 0f;
    private float maxGlowRadius;
    private boolean isPulsing = true;

    private ValueAnimator pulseAnimator;
    private ValueAnimator glowAnimator;

    private String label = "PING";
    private String value = "42";
    private String unit = "ms";
    private float progress = 0.7f; // 0-1 progress for the ring

    private RectF ringRect = new RectF();
    private Path glowPath = new Path();

    public PulsingGlowView(Context context) {
        super(context);
        init();
    }

    public PulsingGlowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PulsingGlowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(AMOLED_BLACK);

        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dpToPx(4));
        ringPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ACCENT_DIM);
        textPaint.setTextSize(spToPx(12));
        textPaint.setTextAlign(Paint.Align.CENTER);

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(ACCENT_WHITE);
        valuePaint.setTextSize(spToPx(32));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);

        unitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        unitPaint.setColor(ACCENT_DIM);
        unitPaint.setTextSize(spToPx(14));
        unitPaint.setTextAlign(Paint.Align.CENTER);

        startPulsing();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        maxGlowRadius = Math.min(w, h) / 2f;
        ringRect.set(
            dpToPx(20), dpToPx(20),
            w - dpToPx(20), h - dpToPx(20)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // Draw pulsing glow
        if (isPulsing && glowRadius > 0) {
            int glowAlpha = (int) (80 * (1 - glowRadius / maxGlowRadius));
            glowPaint.setShader(new RadialGradient(
                centerX, centerY,
                glowRadius,
                Color.argb(glowAlpha, 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            ));
            canvas.drawCircle(centerX, centerY, glowRadius, glowPaint);
        }

        // Draw background circle
        canvas.drawCircle(centerX, centerY, maxGlowRadius - dpToPx(30), centerPaint);

        // Draw progress ring background
        ringPaint.setColor(ACCENT_DIM);
        canvas.drawArc(ringRect, 0, 360, false, ringPaint);

        // Draw progress ring with gradient
        float sweepAngle = 360 * progress;
        ringPaint.setShader(new LinearGradient(
            0, 0, getWidth(), getHeight(),
            ACCENT_WHITE,
            Color.parseColor("#80FFFFFF"),
            Shader.TileMode.CLAMP
        ));
        canvas.drawArc(ringRect, -90, sweepAngle, false, ringPaint);
        ringPaint.setShader(null);

        // Draw label
        canvas.drawText(label, centerX, centerY - dpToPx(20), textPaint);

        // Draw value
        canvas.drawText(value, centerX, centerY + dpToPx(10), valuePaint);

        // Draw unit
        canvas.drawText(unit, centerX, centerY + dpToPx(30), unitPaint);
    }

    private void startPulsing() {
        // Glow expansion animator
        glowAnimator = ValueAnimator.ofFloat(0f, 1f);
        glowAnimator.setDuration(2000);
        glowAnimator.setRepeatCount(ValueAnimator.INFINITE);
        glowAnimator.setInterpolator(new LinearInterpolator());
        glowAnimator.addUpdateListener(animation -> {
            glowRadius = (float) animation.getAnimatedValue() * maxGlowRadius;
            invalidate();
        });

        // Pulse phase animator for subtle breathing effect
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new DecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            pulsePhase = (float) animation.getAnimatedValue();
        });

        glowAnimator.start();
        pulseAnimator.start();
    }

    public void stopPulsing() {
        isPulsing = false;
        if (glowAnimator != null) {
            glowAnimator.cancel();
        }
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
    }

    public void setData(String label, String value, String unit, float progress) {
        this.label = label;
        this.value = value;
        this.unit = unit;
        this.progress = Math.max(0, Math.min(1, progress));
        invalidate();
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(1, progress));
        invalidate();
    }

    public void setValue(String value) {
        this.value = value;
        invalidate();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopPulsing();
    }
}
