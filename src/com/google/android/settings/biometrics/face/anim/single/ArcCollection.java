package com.google.android.settings.biometrics.face.anim.single;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;

import com.google.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class ArcCollection {
    private static final int ARC_COUNT = 4;
    private static final int STATE_STOPPED = 1;
    private static final int STATE_ROTATING = 2;
    private static final int STATE_STOPPING = 3;
    private static final int STATE_FINISHING = 4;
    private static final int STATE_FINISHED = 5;

    private final List<RotatingArc> mArcs;
    private final Handler mHandler;
    private float mSpeed;
    private ValueAnimator mSpeedAnimator;
    private int mState = 0;
    private float mSweepAngle;
    private ValueAnimator mSweepAnimator;

    public ArcCollection(Context context, Handler handler) {
        mHandler = handler;
        int[] colors = {
            context.getResources().getColor(R.color.face_enroll_single_capture_rotating_4),
            context.getResources().getColor(R.color.face_enroll_single_capture_rotating_3),
            context.getResources().getColor(R.color.face_enroll_single_capture_rotating_2),
            context.getResources().getColor(R.color.face_enroll_single_capture_rotating_1)
        };
        mArcs = new ArrayList<>();
        for (int i = 0; i < ARC_COUNT; i++) {
            mArcs.add(new RotatingArc(i, ARC_COUNT, colors));
        }
    }

    public void update(long currentTimeMs, long deltaTimeMs) {
        for (RotatingArc arc : mArcs) {
            arc.update(currentTimeMs, deltaTimeMs);
        }
    }

    public void draw(Canvas canvas) {
        for (RotatingArc arc : mArcs) {
            arc.draw(canvas);
        }
    }

    public void setSweepAngle(float sweepAngle) {
        mSweepAngle = sweepAngle;
        for (RotatingArc arc : mArcs) {
            arc.setSweepAngle(sweepAngle);
        }
    }

    public void setSpeed(float speed) {
        mSpeed = speed;
        for (RotatingArc arc : mArcs) {
            arc.setRotateSpeed(speed);
        }
    }

    public void stopCurrentAnimation() {
        if (mSweepAnimator != null && mSweepAnimator.isRunning()) {
            mSweepAnimator.cancel();
        }
        if (mSpeedAnimator != null && mSpeedAnimator.isRunning()) {
            mSpeedAnimator.cancel();
        }
        for (RotatingArc arc : mArcs) {
            arc.stopCurrentAnimation();
        }
    }

    public void stopRotating() {
        if (mState == STATE_STOPPED || mState == STATE_STOPPING) {
            return;
        }
        stopCurrentAnimation();
        mState = STATE_STOPPING;
        mSweepAnimator = ValueAnimator.ofFloat(mSweepAngle, 0.0f);
        mSweepAnimator.setDuration(1100L);
        mSweepAnimator.addUpdateListener(
                valueAnimator -> setSweepAngle((Float) valueAnimator.getAnimatedValue()));
        mSweepAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        mState = STATE_STOPPED;
                    }
                });
        mSpeedAnimator = ValueAnimator.ofFloat(mSpeed, 0.0f);
        mSpeedAnimator.setDuration(1100L);
        mSpeedAnimator.addUpdateListener(
                valueAnimator -> setSpeed((Float) valueAnimator.getAnimatedValue()));
        mSweepAnimator.start();
        for (RotatingArc arc : mArcs) {
            arc.stopRotating(1100L);
        }
    }

    public void startRotating() {
        if (mState == STATE_ROTATING) {
            return;
        }
        stopCurrentAnimation();
        mState = STATE_ROTATING;
        mSweepAnimator = ValueAnimator.ofFloat(mSweepAngle, 90.0f);
        mSweepAnimator.setDuration(800L);
        mSweepAnimator.addUpdateListener(
                valueAnimator -> setSweepAngle((Float) valueAnimator.getAnimatedValue()));
        mSpeedAnimator = ValueAnimator.ofFloat(mSpeed, 200.0f);
        mSpeedAnimator.setDuration(800L);
        mSpeedAnimator.addUpdateListener(
                valueAnimator -> setSpeed((Float) valueAnimator.getAnimatedValue()));
        mSweepAnimator.start();
        mSpeedAnimator.start();
        for (RotatingArc arc : mArcs) {
            arc.startRotating(800L);
        }
    }

    public void startFinishing(final Runnable runnable) {
        if (mState == STATE_FINISHING || mState == STATE_FINISHED) {
            return;
        }
        stopCurrentAnimation();
        mState = STATE_FINISHING;
        mSweepAnimator = ValueAnimator.ofFloat(mSweepAngle, 360.0f);
        mSweepAnimator.setDuration(800L);
        mSweepAnimator.addUpdateListener(
                valueAnimator -> setSweepAngle((Float) valueAnimator.getAnimatedValue()));
        mSweepAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        mHandler.post(runnable);
                    }
                });
        mSpeedAnimator = ValueAnimator.ofFloat(mSpeed, 200.0f);
        mSpeedAnimator.setDuration(800L);
        mSpeedAnimator.addUpdateListener(
                valueAnimator -> setSpeed((Float) valueAnimator.getAnimatedValue()));
        mSweepAnimator.start();
        mSpeedAnimator.start();
        for (RotatingArc arc : mArcs) {
            arc.startFinishing(800L);
        }
    }
}
