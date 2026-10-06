package com.google.android.settings.biometrics.face.anim.curve;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.ImageView;

import com.google.android.settings.R;

public class DirectionIndicatorController {
    private static final AudioAttributes SONIFICATION_AUDIO_ATTRIBUTES =
            new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build();
    private Rect mBounds;
    private final Context mContext;
    private final ImageView mImageView;
    private final Paint mLargeAnglePaint;
    private boolean mShouldRepeat;
    private ValueAnimator mStrokeAnimator;
    private final VibrationEffect mVibrationEffect =
            VibrationEffect.get(VibrationEffect.EFFECT_DOUBLE_CLICK);
    private final Vibrator mVibrator;

    public void draw(Canvas canvas) {}

    public DirectionIndicatorController(Context context, ImageView imageView) {
        mContext = context;
        mImageView = imageView;
        mVibrator = context.getSystemService(Vibrator.class);
        mLargeAnglePaint = new Paint();
        mLargeAnglePaint.setAntiAlias(true);
        mLargeAnglePaint.setColor(context.getColor(R.color.blue_500));
        mLargeAnglePaint.setStrokeWidth(0.0f);
        mLargeAnglePaint.setStrokeCap(Paint.Cap.ROUND);
        mLargeAnglePaint.setStyle(Paint.Style.STROKE);
        mStrokeAnimator = ValueAnimator.ofFloat(0.0f, 20.0f, 0.0f);
        mStrokeAnimator.setDuration(1233L);
        mStrokeAnimator.addUpdateListener(
                valueAnimator ->
                        mLargeAnglePaint.setStrokeWidth((Float) valueAnimator.getAnimatedValue()));
    }

    public void stopCurrentIndication() {
        mShouldRepeat = false;
    }

    public void pulseForNoActivity(int angle, int times) {
        pulseAnimation(angle, times, false);
    }

    private void pulseAnimation(int angle, int times, boolean reverse) {
        if (mBounds == null) {
            return;
        }
        mShouldRepeat = true;
        AnimatedVectorDrawable animatedVectorDrawable =
                (AnimatedVectorDrawable) mImageView.getDrawable();
        if (animatedVectorDrawable == null || !animatedVectorDrawable.isRunning()) {
            AnimatedVectorDrawable animation =
                    (AnimatedVectorDrawable)
                            mContext.getDrawable(R.drawable.face_indicator_triangle);
            mImageView.setImageDrawable(animation);
            double radians = Math.toRadians(angle);
            float halfIndicatorWidth = (mImageView.getMeasuredWidth() * 0.15f) / 2.0f;
            int translationX = (int) ((mBounds.centerX() + halfIndicatorWidth) * Math.sin(radians));
            int translationY = (int) ((mBounds.centerY() + halfIndicatorWidth) * Math.cos(radians));
            mImageView.setScaleX(0.15f);
            mImageView.setScaleY(0.15f);
            mImageView.setRotation(reverse ? angle - 180 : angle);
            mImageView.setTranslationX(translationX);
            mImageView.setTranslationY(-translationY);
            animation.registerAnimationCallback(
                    new Animatable2.AnimationCallback() {
                        private int curPulses = 1;
                        private final int numPulses;

                        {
                            numPulses = times;
                        }

                        @Override
                        public void onAnimationEnd(Drawable drawable) {
                            super.onAnimationEnd(drawable);
                            if (!mShouldRepeat || curPulses >= numPulses) {
                                return;
                            }
                            animation.start();
                            curPulses++;
                        }
                    });
            animation.start();
        }
    }

    public void onBoundsChange(Rect bounds) {
        mBounds = bounds;
    }
}
