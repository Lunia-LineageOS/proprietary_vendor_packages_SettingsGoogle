package com.google.android.settings.biometrics.face.anim;

import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_NOT_DETECTED;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_TOO_CLOSE;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_TOO_FAR;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_TOO_HIGH;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_TOO_LEFT;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_TOO_LOW;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_TOO_RIGHT;

import android.animation.TimeAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.ImageView;

import com.google.android.settings.R;
import com.google.android.settings.biometrics.face.Debouncer;
import com.google.android.settings.biometrics.face.FaceEnrollSidecar;
import com.google.android.settings.biometrics.face.FaceUtils;
import com.google.android.settings.biometrics.face.Utils;

public abstract class FaceEnrollAnimationBase extends Drawable
        implements FaceEnrollSidecar.Listener {
    private static final String TAG = "FaceEnroll/AnimationBase";
    private static final int FOV_STATE_IN = 1;
    private static final int FOV_STATE_OUT = 2;
    private static final int FOV_STATE_TURNED_TOO_FAR = 3;
    private static final AudioAttributes SONIFICATION_AUDIO_ATTRIBUTES =
            new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build();
    private boolean mCenterAcquired;
    private final Paint mCircleCutoutPaint;
    private final Context mContext;
    private final Debouncer mDebouncer;
    private final ImageView mFaceIcon;
    private final FaceOutlineIndicatorController mFaceOutlineIndicatorController;
    private boolean mFinishing;
    private boolean mFromSetupWizard;
    private Bitmap mInverseCutoutBitmap;
    private final AnimationListener mListener;
    private final Paint mScrimPaint;
    private final Paint mSquarePaint;
    private TimeAnimator mTimeAnimator;
    private final Vibrator mVibrator;
    private int mFOVState = FOV_STATE_OUT;
    private final VibrationEffect mVibrationEffect =
            VibrationEffect.get(VibrationEffect.EFFECT_TICK);

    public interface AnimationListener {
        void clearHelp();

        void onEnrollAnimationFinished();

        void onEnrollAnimationStarted();

        void showHelp(CharSequence help);
    }

    protected void bucketAcquiredWhileScrimShowing(int acquiredInfo) {}

    protected boolean isBucket(int acquiredInfo) {
        return acquiredInfo >= 1101 && acquiredInfo <= 1125;
    }

    public void onSaveInstanceState(Bundle outState) {}

    protected void update(long currentTimeMs, long deltaTimeMs) {}

    public FaceEnrollAnimationBase(
            Context context,
            AnimationListener animationListener,
            ImageView indicatorView,
            boolean fromSetupWizard) {
        mContext = context;
        mListener = animationListener;
        mFromSetupWizard = fromSetupWizard;
        mVibrator = context.getSystemService(Vibrator.class);
        mSquarePaint = new Paint();
        mSquarePaint.setColor(Color.WHITE);
        mSquarePaint.setAntiAlias(true);
        mCircleCutoutPaint = new Paint();
        mCircleCutoutPaint.setColor(Color.TRANSPARENT);
        mCircleCutoutPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mCircleCutoutPaint.setAntiAlias(true);
        mScrimPaint = new Paint();
        mScrimPaint.setAntiAlias(true);
        mScrimPaint.setStyle(Paint.Style.FILL);
        mScrimPaint.setColor(Color.TRANSPARENT);
        mFaceIcon = ((Activity) context).findViewById(R.id.face_smiley);
        mFaceIcon.setImageDrawable(context.getDrawable(R.drawable.face_smiley));
        mFaceOutlineIndicatorController =
                new FaceOutlineIndicatorController(context, indicatorView);
        SparseIntArray debounceWindows = new SparseIntArray();
        debounceWindows.append(FOV_STATE_IN, 6);
        debounceWindows.append(FOV_STATE_OUT, 10);
        debounceWindows.append(FOV_STATE_TURNED_TOO_FAR, 10);
        mDebouncer = new Debouncer(debounceWindows);
    }

    protected void vibrate() {
        mVibrator.vibrate(mVibrationEffect, SONIFICATION_AUDIO_ATTRIBUTES);
    }

    protected AnimationListener getListener() {
        return mListener;
    }

    protected boolean isCenterAcquired() {
        return mCenterAcquired;
    }

    protected void onUserEnterGood() {
        Log.i(TAG, "onUserEnterGood");
        getListener().clearHelp();
        mFaceOutlineIndicatorController.clear();
    }

    protected void onUserLeaveGood(CharSequence help) {
        Log.i(TAG, "onUserLeaveGood");
        getListener().showHelp(help);
        mFaceOutlineIndicatorController.show();
    }

    public void onFirstFrameReceived() {
        onUserLeaveGood(null);
        mFOVState = FOV_STATE_OUT;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (mTimeAnimator == null) {
            mTimeAnimator = new TimeAnimator();
            mTimeAnimator.setTimeListener(
                    (animation, frameTime, deltaTime) -> {
                        update(frameTime, deltaTime);
                        invalidateSelf();
                    });
            mTimeAnimator.start();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        int margin = (int) Utils.dpToPx(mContext, 30);
        if (mInverseCutoutBitmap == null) {
            mInverseCutoutBitmap =
                    Cutout.createCutoutBitmap(
                            mContext, getBounds().width() + (margin * 2), getBounds().width() / 2);
        }
        canvas.drawBitmap(mInverseCutoutBitmap, -margin, -margin, null);
        canvas.drawCircle(
                canvas.getWidth() / 2, canvas.getHeight() / 2, canvas.getWidth() / 2, mScrimPaint);
        canvas.restore();
    }

    protected boolean outOfFOVScrimShowing() {
        return mFOVState != FOV_STATE_IN;
    }

    @Override
    public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        if (mFinishing) {
            return;
        }
        if (isBucket(helpMsgId) && mFOVState != FOV_STATE_IN) {
            mDebouncer.updateBuffer(FOV_STATE_IN);
            if (!mDebouncer.passesDebounce(FOV_STATE_IN)) {
                if (!FaceUtils.isOneOfCenterBuckets(helpMsgId)) {
                    bucketAcquiredWhileScrimShowing(helpMsgId);
                }
            } else {
                mFOVState = FOV_STATE_IN;
                onUserEnterGood();
            }
        } else {
            switch (helpMsgId) {
                case FACE_ACQUIRED_NOT_DETECTED:
                case FACE_ACQUIRED_TOO_CLOSE:
                case FACE_ACQUIRED_TOO_FAR:
                case FACE_ACQUIRED_TOO_HIGH:
                case FACE_ACQUIRED_TOO_LOW:
                case FACE_ACQUIRED_TOO_RIGHT:
                case FACE_ACQUIRED_TOO_LEFT:
                    handleOutOfFovState(FOV_STATE_OUT, helpString);
                    break;
                case 1126:
                case 1127:
                case 1128:
                case 1129:
                case 1130:
                case 1131:
                case 1132:
                case 1133:
                    handleOutOfFovState(FOV_STATE_TURNED_TOO_FAR, helpString);
                    break;
                default:
                    break;
            }
        }
        if (mCenterAcquired || !FaceUtils.isOneOfCenterBuckets(helpMsgId)) {
            return;
        }
        mCenterAcquired = true;
    }

    private void handleOutOfFovState(int state, CharSequence help) {
        mDebouncer.updateBuffer(state);
        if (mFOVState == state || !mDebouncer.passesDebounce(state)) {
            return;
        }
        onUserLeaveGood(help);
        mFOVState = state;
    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
        if (remaining == 0) {
            if (mFOVState != FOV_STATE_IN) {
                mFOVState = FOV_STATE_IN;
                onUserEnterGood();
            }
            mListener.onEnrollAnimationStarted();
            startFinishing();
            mListener.clearHelp();
        }
    }

    protected void startFinishing() {
        mFinishing = true;
    }

    protected boolean isFinishing() {
        return mFinishing;
    }
}
