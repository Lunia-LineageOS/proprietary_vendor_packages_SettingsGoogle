package com.google.android.settings.biometrics.face.anim.curve;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;

import com.google.android.settings.R;

public class GridState {
    private static final int STATE_HIDDEN = 0;
    private static final int STATE_SHOWN = 1;
    private static final int STATE_ANIMATING = 2;

    private ValueAnimator mAnimator;
    private ValueAnimator.AnimatorUpdateListener mAnimatorUpdateListener;
    private final Paint mEdgePaint;
    private final Handler mHandler;
    private int mState;

    public GridState(Context context, Handler handler) {
        mHandler = handler;
        mEdgePaint = new Paint();
        mEdgePaint.setColor(context.getColor(R.color.face_enroll_grid));
        mEdgePaint.setAntiAlias(true);
        mEdgePaint.setStyle(Paint.Style.STROKE);
        mEdgePaint.setStrokeWidth(3.0f);
        mEdgePaint.setAlpha(0);
        mState = STATE_HIDDEN;
        mAnimatorUpdateListener =
                valueAnimator -> mEdgePaint.setAlpha((Integer) valueAnimator.getAnimatedValue());
    }

    public void fadeIn() {
        if (mState == STATE_SHOWN) {
            return;
        }
        mState = STATE_ANIMATING;
        mAnimator = ValueAnimator.ofInt(mEdgePaint.getAlpha(), 64);
        mAnimator.removeAllUpdateListeners();
        mAnimator.addUpdateListener(mAnimatorUpdateListener);
        mAnimator.removeAllListeners();
        mAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        mState = STATE_SHOWN;
                    }
                });
        mAnimator.start();
    }

    public void fadeOut(final Runnable runnable) {
        if (mState == STATE_HIDDEN) {
            mHandler.post(runnable);
            return;
        }
        mState = STATE_ANIMATING;
        mAnimator = ValueAnimator.ofInt(mEdgePaint.getAlpha(), 0);
        mAnimator.addUpdateListener(mAnimatorUpdateListener);
        mAnimator.removeAllListeners();
        mAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        mState = STATE_HIDDEN;
                        mHandler.post(runnable);
                    }
                });
        mAnimator.start();
    }

    public void draw(Canvas canvas) {
        float halfWidth = canvas.getWidth() / 2;
        float halfHeight = canvas.getHeight() / 2;
        canvas.drawCircle(0.0f, 0.0f, halfWidth - (mEdgePaint.getStrokeWidth() / 2.0f), mEdgePaint);
        float halfInner = (canvas.getWidth() * 0.32f) / 2.0f;
        float halfOuter = (canvas.getWidth() * 0.78f) / 2.0f;
        canvas.drawArc(
                new RectF(-halfInner, -halfHeight, halfInner, halfHeight),
                0.0f,
                360.0f,
                false,
                mEdgePaint);
        canvas.drawArc(
                new RectF(-halfWidth, -halfInner, halfWidth, halfInner),
                0.0f,
                360.0f,
                false,
                mEdgePaint);
        canvas.drawArc(
                new RectF(-halfOuter, -halfHeight, halfOuter, halfHeight),
                0.0f,
                360.0f,
                false,
                mEdgePaint);
        canvas.drawArc(
                new RectF(-halfWidth, -halfOuter, halfWidth, halfOuter),
                0.0f,
                360.0f,
                false,
                mEdgePaint);
    }
}
