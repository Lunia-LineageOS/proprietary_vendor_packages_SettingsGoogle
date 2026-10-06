package com.google.android.settings.biometrics.face.anim.curve;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.google.android.settings.R;
import com.google.android.settings.biometrics.face.anim.FaceEnrollAnimationMultiAngleDrawable;

public class CellState {
    private static final String TAG = "FaceEnroll/CellState";
    private static final String SETTING_ALTERNATE_CURSOR =
            "com.google.android.settings.future.biometrics.face.anim.curve.alternate_cursor";
    private static final String SETTING_DISABLE_CURSOR =
            "com.google.android.settings.future.biometrics.face.anim.curve.disable_cursor";

    static final int SCRIM_FADE_REASON_DONE = 1;
    static final int SCRIM_FADE_REASON_USER_LEFT = 2;

    private static final int SCRIM_ANIMATION_NONE = 0;
    private static final int SCRIM_ANIMATION_FADE_OUT = 1;
    private static final int SCRIM_ANIMATION_FADE_IN = 2;
    private static final int SCRIM_ANIMATION_NOT_ENROLLED_COLOR = 3;

    private static final int CURSOR_STATE_GONE = 0;
    private static final int CURSOR_STATE_ACQUIRING = 1;
    private static final int CURSOR_STATE_FADING = 2;
    private static final int CURSOR_STATE_ACQUIRED = 3;

    private static final int MSG_FADE_CURSOR = 1;

    private final boolean mAlternateCursor;
    private final FaceEnrollAnimationMultiAngleDrawable.BucketListener mBucketListener;
    private CellConfig mCellConfig;
    private ValueAnimator mCursorAnimator;
    private ValueAnimator.AnimatorUpdateListener mCursorAnimatorListener;
    private final int mCursorColorAcquired;
    private final int mCursorColorGone;
    private Paint mCursorEdgePaint;
    private int mCursorState;
    private final boolean mDisableCursor;
    private boolean mDone;
    private final Handler mHandler =
            new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    if (message.what != MSG_FADE_CURSOR) {
                        return;
                    }
                    handleFadeCursor();
                }
            };
    private final int mIndex;
    private ValueAnimator mNoActivityAnimator;
    private ValueAnimator.AnimatorUpdateListener mNoActivityAnimatorListener;
    private Paint mNoActivityPaint;
    private boolean mNoActivityPulseShouldRepeat;
    private int mScrimAnimationState;
    private ValueAnimator mScrimAnimator;
    private ValueAnimator.AnimatorUpdateListener mScrimAnimatorListener;
    private final int mScrimColorEnrolled;
    private final int mScrimColorNoActivityEnd;
    private final int mScrimColorNoActivityStart;
    private int mScrimColorNotEnrolled;
    private Paint mScrimPaint;

    public CellState(
            Context context,
            int index,
            FaceEnrollAnimationMultiAngleDrawable.BucketListener bucketListener,
            int scrimColorNotEnrolled) {
        mIndex = index;
        mBucketListener = bucketListener;
        mScrimColorNotEnrolled = scrimColorNotEnrolled;
        mScrimColorEnrolled = context.getColor(R.color.face_enroll_cell_enrolled);
        mCursorColorAcquired = context.getColor(R.color.face_enroll_cursor_acquired);
        mCursorColorGone = context.getColor(R.color.face_enroll_cursor_gone);
        mScrimColorNoActivityStart = context.getColor(R.color.face_enroll_cell_no_activity_start);
        mScrimColorNoActivityEnd = context.getColor(R.color.face_enroll_cell_no_activity_end);
        mScrimAnimationState = SCRIM_ANIMATION_NONE;
        mCursorState = CURSOR_STATE_GONE;
        mScrimPaint = new Paint();
        mScrimPaint.setAntiAlias(true);
        mScrimPaint.setAlpha(0);
        mAlternateCursor =
                Settings.Secure.getInt(context.getContentResolver(), SETTING_ALTERNATE_CURSOR, 0)
                        != 0;
        mDisableCursor =
                Settings.Secure.getInt(context.getContentResolver(), SETTING_DISABLE_CURSOR, 0)
                        != 0;
        int shadowColor =
                mAlternateCursor ? Color.RED : context.getColor(R.color.face_enroll_cursor_shadow);
        mCursorEdgePaint = new Paint();
        mCursorEdgePaint.setColor(mCursorColorGone);
        mCursorEdgePaint.setAntiAlias(true);
        mCursorEdgePaint.setShadowLayer(6.0f, 0.0f, 0.0f, shadowColor);
        mCursorEdgePaint.setStrokeCap(Paint.Cap.ROUND);
        mCursorEdgePaint.setStyle(Paint.Style.STROKE);
        mCursorEdgePaint.setStrokeWidth(12.0f);
        mNoActivityPaint = new Paint();
        mNoActivityPaint.setAntiAlias(true);
        mNoActivityPaint.setColor(mScrimColorNoActivityStart);
        mScrimAnimatorListener =
                valueAnimator -> mScrimPaint.setColor((Integer) valueAnimator.getAnimatedValue());
        mCursorAnimatorListener =
                valueAnimator ->
                        mCursorEdgePaint.setColor((Integer) valueAnimator.getAnimatedValue());
        mNoActivityAnimatorListener =
                valueAnimator ->
                        mNoActivityPaint.setColor((Integer) valueAnimator.getAnimatedValue());
    }

    public void updateConfig(CellConfig cellConfig) {
        mCellConfig = cellConfig;
    }

    public void draw(Canvas canvas) {
        canvas.save();
        CellConfig cellConfig = mCellConfig;
        if (cellConfig == null) {
            return;
        }
        if (cellConfig.mFlipVertical) {
            canvas.scale(1.0f, -1.0f, 0.0f, 0.0f);
        }
        canvas.rotate(mCellConfig.mRotation);
        canvas.drawPath(mCellConfig.mPath, mScrimPaint);
        canvas.drawPath(mCellConfig.mPath, mNoActivityPaint);
        canvas.restore();
    }

    public void drawCursor(Canvas canvas) {
        canvas.save();
        CellConfig cellConfig = mCellConfig;
        if (cellConfig == null) {
            return;
        }
        if (cellConfig.mFlipVertical) {
            canvas.scale(1.0f, -1.0f, 0.0f, 0.0f);
        }
        canvas.rotate(mCellConfig.mRotation);
        if (!mDisableCursor) {
            canvas.drawPath(mCellConfig.mPath, mCursorEdgePaint);
        }
        canvas.restore();
    }

    public boolean isDone() {
        return mDone;
    }

    public void setEarlyDone() {
        mDone = true;
    }

    public void stopPulseForNoActivity() {
        mNoActivityPulseShouldRepeat = false;
    }

    public void pulseForNoActivity(int times) {
        mNoActivityPulseShouldRepeat = true;
        if (isAnimating(mNoActivityAnimator)) {
            return;
        }
        ValueAnimator valueAnimator =
                ValueAnimator.ofArgb(
                        mScrimColorNoActivityStart,
                        mScrimColorNoActivityEnd,
                        mScrimColorNoActivityEnd,
                        mScrimColorNoActivityStart);
        mNoActivityAnimator = valueAnimator;
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mNoActivityAnimator.addUpdateListener(mNoActivityAnimatorListener);
        mNoActivityAnimator.setDuration(1233L);
        mNoActivityAnimator.addListener(
                new AnimatorListenerAdapter() {
                    private int curPulses = 1;
                    private final int numPulses;

                    {
                        numPulses = times;
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        if (mNoActivityPulseShouldRepeat && curPulses < numPulses) {
                            mNoActivityAnimator.start();
                            curPulses++;
                        } else {
                            mBucketListener.onNoActivityAnimationFinished();
                        }
                    }
                });
        mNoActivityAnimator.start();
    }

    public void fadeScrimOut(int reason) {
        animateScrimColor(
                reason == SCRIM_FADE_REASON_USER_LEFT
                        ? Color.TRANSPARENT
                        : (mDone ? mScrimColorEnrolled : mScrimColorNotEnrolled),
                200L,
                SCRIM_ANIMATION_FADE_OUT);
    }

    public void fadeScrimIn() {
        fadeScrimIn(200L);
    }

    private void fadeScrimIn(long duration) {
        animateScrimColor(
                mDone ? mScrimColorEnrolled : mScrimColorNotEnrolled,
                duration,
                SCRIM_ANIMATION_FADE_IN);
    }

    public void onAcquired() {
        if (mHandler.hasMessages(MSG_FADE_CURSOR)) {
            mHandler.removeMessages(MSG_FADE_CURSOR);
            mHandler.sendEmptyMessageDelayed(MSG_FADE_CURSOR, 300L);
        }
        if (mCursorState == CURSOR_STATE_GONE || !mDone) {
            mCursorState = CURSOR_STATE_ACQUIRING;
            if (!mDone) {
                mBucketListener.onStartFinishing();
            }
            mDone = true;
            ValueAnimator valueAnimator =
                    ValueAnimator.ofArgb(mCursorEdgePaint.getColor(), mCursorColorAcquired);
            mCursorAnimator = valueAnimator;
            valueAnimator.setDuration(300L);
            mCursorAnimator.addUpdateListener(mCursorAnimatorListener);
            mCursorAnimator.addListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            mCursorState = CURSOR_STATE_ACQUIRED;
                            if (mScrimAnimationState != SCRIM_ANIMATION_FADE_OUT) {
                                fadeScrimOut(SCRIM_FADE_REASON_DONE);
                            } else {
                                Log.w(
                                        TAG,
                                        "Index "
                                                + mIndex
                                                + " intentionally not going to"
                                                + " SCRIM_FADE_REASON_DONE");
                            }
                            mHandler.sendEmptyMessageDelayed(MSG_FADE_CURSOR, 300L);
                        }
                    });
            mCursorAnimator.start();
        }
    }

    public void updateScrimNotEnrolledColor(int color, boolean animate) {
        mScrimColorNotEnrolled = color;
        if (animate) {
            if (mScrimAnimationState == SCRIM_ANIMATION_NONE) {
                animateScrimNotEnrolledColor(200L);
            } else if (mScrimAnimationState == SCRIM_ANIMATION_FADE_IN) {
                fadeScrimIn(getRemainingAnimationTime(mScrimAnimator));
            } else if (mScrimAnimationState == SCRIM_ANIMATION_NOT_ENROLLED_COLOR) {
                animateScrimNotEnrolledColor(getRemainingAnimationTime(mScrimAnimator));
            }
        }
    }

    private void animateScrimNotEnrolledColor(long duration) {
        if (mDone) {
            return;
        }
        if (mScrimPaint.getColor() == mScrimColorNotEnrolled) {
            return;
        }
        animateScrimColor(mScrimColorNotEnrolled, duration, SCRIM_ANIMATION_NOT_ENROLLED_COLOR);
    }

    private void animateScrimColor(int color, long duration, int animationState) {
        if (duration <= 0) {
            return;
        }
        if (isAnimating(mScrimAnimator)) {
            mScrimAnimator.cancel();
        }
        mScrimAnimationState = animationState;
        ValueAnimator valueAnimator = ValueAnimator.ofArgb(mScrimPaint.getColor(), color);
        mScrimAnimator = valueAnimator;
        valueAnimator.addUpdateListener(mScrimAnimatorListener);
        mScrimAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animator) {
                        mScrimAnimationState = SCRIM_ANIMATION_NONE;
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        mScrimAnimationState = SCRIM_ANIMATION_NONE;
                    }
                });
        mScrimAnimator.setDuration(duration);
        mScrimAnimator.start();
    }

    public void fadeCursorNow() {
        handleFadeCursor();
    }

    private void handleFadeCursor() {
        mCursorState = CURSOR_STATE_FADING;
        if (isAnimating(mCursorAnimator)) {
            mCursorAnimator.cancel();
        }
        ValueAnimator valueAnimator =
                ValueAnimator.ofArgb(mCursorEdgePaint.getColor(), mCursorColorGone);
        mCursorAnimator = valueAnimator;
        valueAnimator.setDuration(200L);
        mCursorAnimator.addUpdateListener(mCursorAnimatorListener);
        mCursorAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        mCursorState = CURSOR_STATE_GONE;
                    }
                });
        mCursorAnimator.start();
    }

    private static boolean isAnimating(ValueAnimator valueAnimator) {
        return valueAnimator != null && valueAnimator.isRunning();
    }

    private static long getRemainingAnimationTime(ValueAnimator valueAnimator) {
        return Math.round(
                (1.0f - valueAnimator.getAnimatedFraction()) * valueAnimator.getDuration());
    }
}
