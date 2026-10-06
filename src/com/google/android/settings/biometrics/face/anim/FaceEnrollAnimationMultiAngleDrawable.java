package com.google.android.settings.biometrics.face.anim;

import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_GOOD;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.settings.R;
import com.google.android.settings.biometrics.face.anim.curve.DirectionIndicationHelper;
import com.google.android.settings.biometrics.face.anim.curve.DirectionIndicatorController;
import com.google.android.settings.biometrics.face.anim.curve.GridController;

public class FaceEnrollAnimationMultiAngleDrawable extends FaceEnrollAnimationBase {
    private static final String TAG = "FaceEnroll/AnimationDrawable";
    private static final String KEY_BUCKET_STATUS = "key_bucket_status";
    private static final int MSG_USER_NO_ACTIVITY = 1;
    private static final int FIRST_BUCKET = 1101;
    private static final int BUCKET_COUNT = 25;
    private final BucketListener mBucketListener;
    private boolean[] mBucketsCompleted;
    private final Context mContext;
    private final DirectionIndicationHelper mDirectionIndicationHelper;
    private final DirectionIndicatorController mDirectionIndicatorController;
    private Paint mFinishingArcPaint;
    private final GridController mGridController;
    private final Handler mHandler;
    private long mLastVibrationMs;

    public interface BucketListener {
        void onNoActivityAnimationFinished();

        void onStartFinishing();
    }

    private boolean isLargeAngle(int acquiredInfo) {
        return acquiredInfo >= 1126 && acquiredInfo <= 1133;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(ColorFilter colorFilter) {}

    private void handleUserNoActivityAnimation() {
        mGridController.pulseForNoActivity(
                mDirectionIndicationHelper.getNoProgressBucket(mBucketsCompleted),
                Integer.MAX_VALUE);
        mDirectionIndicatorController.pulseForNoActivity(
                mDirectionIndicationHelper.getNoProgressPulseAngle(mBucketsCompleted),
                Integer.MAX_VALUE);
        getListener().showHelp(mContext.getString(R.string.face_enrolling_turn_head_to_arrow));
    }

    public FaceEnrollAnimationMultiAngleDrawable(
            Context context,
            FaceEnrollAnimationBase.AnimationListener animationListener,
            ImageView directionIndicatorView,
            ImageView faceOutlineView,
            boolean fromSetupWizard,
            Bundle savedInstanceState) {
        super(context, animationListener, faceOutlineView, fromSetupWizard);
        mBucketsCompleted = new boolean[BUCKET_COUNT];
        mHandler =
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message message) {
                        if (message.what == MSG_USER_NO_ACTIVITY) {
                            handleUserNoActivityAnimation();
                            return;
                        }
                        Log.w(TAG, "Unknown message: " + message.what);
                    }
                };
        BucketListener bucketListener =
                new BucketListener() {
                    @Override
                    public void onStartFinishing() {
                        boolean shouldVibrate;
                        synchronized (this) {
                            if (SystemClock.uptimeMillis() - mLastVibrationMs > 50) {
                                mLastVibrationMs = SystemClock.uptimeMillis();
                                shouldVibrate = true;
                            } else {
                                shouldVibrate = false;
                            }
                        }
                        if (shouldVibrate) {
                            vibrate();
                        }
                    }

                    @Override
                    public void onNoActivityAnimationFinished() {
                        mHandler.removeMessages(MSG_USER_NO_ACTIVITY);
                        getListener().clearHelp();
                    }
                };
        mBucketListener = bucketListener;
        mContext = context;
        mFinishingArcPaint = new Paint();
        mFinishingArcPaint.setAntiAlias(true);
        mFinishingArcPaint.setStyle(Paint.Style.STROKE);
        mFinishingArcPaint.setStrokeCap(Paint.Cap.ROUND);
        mFinishingArcPaint.setStrokeWidth(20.0f);
        mFinishingArcPaint.setColor(
                context.getResources().getColor(R.color.face_enroll_single_capture_rotating_1));
        mDirectionIndicationHelper = new DirectionIndicationHelper();
        mGridController = new GridController(context, bucketListener);
        mDirectionIndicatorController =
                new DirectionIndicatorController(context, directionIndicatorView);
        if (savedInstanceState == null) {
            return;
        }
        mBucketsCompleted = savedInstanceState.getBooleanArray(KEY_BUCKET_STATUS);
        for (int i = 0; i < mBucketsCompleted.length; i++) {
            mGridController.restoreState(i, mBucketsCompleted[i]);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBooleanArray(KEY_BUCKET_STATUS, mBucketsCompleted);
    }

    private boolean isNewBucketAcquired(int acquiredInfo) {
        return isBucket(acquiredInfo) && !mBucketsCompleted[acquiredInfo - FIRST_BUCKET];
    }

    private void stopCurrentDirectionIndication() {
        mDirectionIndicatorController.stopCurrentIndication();
        mGridController.stopPulseForNoActivity();
    }

    @Override
    protected void onUserLeaveGood(CharSequence help) {
        super.onUserLeaveGood(help);
        mGridController.onUserLeaveGood();
        stopCurrentDirectionIndication();
        mHandler.removeMessages(MSG_USER_NO_ACTIVITY);
    }

    @Override
    protected void onUserEnterGood() {
        super.onUserEnterGood();
        mGridController.onUserEnterGood();
    }

    @Override
    protected void bucketAcquiredWhileScrimShowing(int acquiredInfo) {
        int bucket = acquiredInfo - FIRST_BUCKET;
        mBucketsCompleted[bucket] = true;
        mGridController.setEarlyDone(bucket);
    }

    @Override
    public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        super.onEnrollmentHelp(helpMsgId, helpString);
        if (isFinishing() || outOfFOVScrimShowing()) {
            return;
        }
        if (isCenterAcquired() && isBucket(helpMsgId) && !isNewBucketAcquired(helpMsgId)) {
            addDelayedUserNoActivityAnimation();
        } else if (isNewBucketAcquired(helpMsgId)
                || isLargeAngle(helpMsgId)
                || helpMsgId != FACE_ACQUIRED_GOOD) {
            mHandler.removeMessages(MSG_USER_NO_ACTIVITY);
            stopCurrentDirectionIndication();
        }
        if (isBucket(helpMsgId)) {
            int bucket = helpMsgId - FIRST_BUCKET;
            mBucketsCompleted[bucket] = true;
            if (isCenterAcquired()) {
                mGridController.onAcquired(bucket);
            }
        }
    }

    @Override
    public void onEnrollmentError(int errMsgId, CharSequence errString) {
        mHandler.removeMessages(MSG_USER_NO_ACTIVITY);
    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
        super.onEnrollmentProgressChange(steps, remaining);
        if (remaining == 0) {
            mHandler.removeMessages(MSG_USER_NO_ACTIVITY);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.save();
        mGridController.draw(canvas);
        mDirectionIndicatorController.draw(canvas);
        canvas.restore();
    }

    @Override
    public void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mGridController.onBoundsChange(bounds);
        mDirectionIndicatorController.onBoundsChange(bounds);
    }

    private void addDelayedUserNoActivityAnimation() {
        if (mHandler.hasMessages(MSG_USER_NO_ACTIVITY)) {
            return;
        }
        mHandler.sendEmptyMessageDelayed(MSG_USER_NO_ACTIVITY, 4000L);
    }
}
