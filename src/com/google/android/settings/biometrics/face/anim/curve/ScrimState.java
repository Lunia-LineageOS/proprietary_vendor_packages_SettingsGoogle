package com.google.android.settings.biometrics.face.anim.curve;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;

public class ScrimState {
    private static final int STATE_GONE = 0;
    private static final int STATE_FADING_OUT = 2;

    private final int mGoneColor;
    private final Paint mPaint;
    private ValueAnimator mScrimAnimator;
    private ValueAnimator.AnimatorUpdateListener mScrimAnimatorListener;
    private final int mShowingColor;
    private int mState = STATE_GONE;

    public ScrimState(int goneColor, int showingColor) {
        mGoneColor = goneColor;
        mShowingColor = showingColor;
        mPaint = new Paint();
        mPaint.setColor(goneColor);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mScrimAnimatorListener =
                valueAnimator -> mPaint.setColor((Integer) valueAnimator.getAnimatedValue());
    }

    public boolean isShowing() {
        return mState != STATE_GONE;
    }

    public void fadeOut() {
        if (mState == STATE_GONE || mState == STATE_FADING_OUT) {
            return;
        }
        mState = STATE_FADING_OUT;
        mScrimAnimator = ValueAnimator.ofArgb(mPaint.getColor(), mGoneColor);
        mScrimAnimator.addUpdateListener(mScrimAnimatorListener);
        mScrimAnimator.setDuration(200L);
        mScrimAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        mState = STATE_GONE;
                    }
                });
        mScrimAnimator.start();
    }

    public void draw(Canvas canvas) {
        canvas.drawCircle(0.0f, 0.0f, canvas.getWidth() / 2, mPaint);
    }
}
