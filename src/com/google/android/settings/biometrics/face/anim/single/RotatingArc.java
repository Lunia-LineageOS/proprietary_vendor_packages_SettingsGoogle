package com.google.android.settings.biometrics.face.anim.single;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class RotatingArc {
    private float mAngle;
    private ValueAnimator mColorAnimator;
    private final int[] mColors;
    private final int mIndex;
    private final Paint mPaint;
    private float mRotateSpeed;
    private float mSweepAngle;

    public RotatingArc(int index, int count, int[] colors) {
        mIndex = index;
        mColors = colors;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(20.0f);
        mPaint.setColor(getColorForIndex(index));
        mAngle = (360 / count) * index;
    }

    public void setSweepAngle(float sweepAngle) {
        mSweepAngle = sweepAngle;
    }

    public void setRotateSpeed(float rotateSpeed) {
        mRotateSpeed = rotateSpeed;
    }

    public int getColorForIndex(int index) {
        return mColors[index % mColors.length];
    }

    public void update(long currentTimeMs, long deltaTimeMs) {
        mAngle += (mRotateSpeed * deltaTimeMs) / 1000.0f;
    }

    public void draw(Canvas canvas) {
        float width = (canvas.getWidth() / 2) - (mPaint.getStrokeWidth() / 2.0f);
        canvas.drawArc(
                (canvas.getWidth() / 2) - width,
                (canvas.getHeight() / 2) - width,
                (canvas.getWidth() / 2) + width,
                (canvas.getWidth() / 2) + width,
                mAngle,
                mSweepAngle,
                false,
                mPaint);
    }

    public void stopCurrentAnimation() {
        if (mColorAnimator != null) {
            mColorAnimator.cancel();
        }
    }

    public void stopRotating(long durationMs) {
        mColorAnimator = ValueAnimator.ofArgb(mPaint.getColor(), Color.TRANSPARENT);
        mColorAnimator.setDuration(durationMs);
        mColorAnimator.addUpdateListener(
                valueAnimator -> mPaint.setColor((Integer) valueAnimator.getAnimatedValue()));
        mColorAnimator.start();
    }

    public void startRotating(long durationMs) {
        mColorAnimator = ValueAnimator.ofArgb(mPaint.getColor(), getColorForIndex(mIndex));
        mColorAnimator.setDuration(durationMs);
        mColorAnimator.addUpdateListener(
                valueAnimator -> mPaint.setColor((Integer) valueAnimator.getAnimatedValue()));
        mColorAnimator.start();
    }

    public void startFinishing(long durationMs) {
        startRotating(durationMs);
    }
}
