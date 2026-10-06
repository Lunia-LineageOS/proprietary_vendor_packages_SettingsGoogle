package com.google.android.settings.biometrics.face.anim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.widget.ImageView;

import com.google.android.settings.biometrics.face.anim.single.ArcCollection;

public class FaceEnrollAnimationSingleCaptureDrawable extends FaceEnrollAnimationBase {
    private final Handler mHandler;
    private final ArcCollection mRotatingArcs;

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void onEnrollmentError(int errMsgId, CharSequence errString) {}

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(ColorFilter colorFilter) {}

    public FaceEnrollAnimationSingleCaptureDrawable(
            Context context,
            FaceEnrollAnimationBase.AnimationListener animationListener,
            ImageView faceOutlineView,
            boolean fromSetupWizard) {
        super(context, animationListener, faceOutlineView, fromSetupWizard);
        mHandler = new Handler();
        mRotatingArcs = new ArcCollection(context, mHandler);
    }

    @Override
    protected void startFinishing() {
        super.startFinishing();
        mRotatingArcs.startFinishing(() -> getListener().onEnrollAnimationFinished());
    }

    @Override
    protected void update(long currentTimeMs, long deltaTimeMs) {
        mRotatingArcs.update(currentTimeMs, deltaTimeMs);
    }

    @Override
    protected void onUserLeaveGood(CharSequence help) {
        super.onUserLeaveGood(help);
        mRotatingArcs.stopRotating();
    }

    @Override
    protected void onUserEnterGood() {
        super.onUserEnterGood();
        mRotatingArcs.startRotating();
    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
        super.onEnrollmentProgressChange(steps, remaining);
        if (remaining == 0) {
            vibrate();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mRotatingArcs.draw(canvas);
    }
}
