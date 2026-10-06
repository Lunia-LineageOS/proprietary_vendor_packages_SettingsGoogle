package com.google.android.settings.biometrics.face.anim.curve;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.ArraySet;

import com.android.internal.graphics.ColorUtils;

import com.google.android.settings.R;
import com.google.android.settings.biometrics.face.anim.FaceEnrollAnimationMultiAngleDrawable;

public class GridController {
    private static final int[] SCRIM_OPACITY_THRESHOLDS = {0, 5, 10, 15, 20};
    private static final float[] SCRIM_OPACITY_VALUES = {0.55f, 0.6f, 0.65f, 0.7f, 0.75f};
    private final FaceEnrollAnimationMultiAngleDrawable.BucketListener mBucketListener;
    private CellConfig[] mCellConfigs;
    private final CellState[] mCellStates;
    private final GridState mGridState;
    private final ScrimState mNoActivityScrimState;
    private final ArraySet<Integer> mPrimaryCellIndices;
    private final int mScrimNotEnrolledDefaultColor;
    private final int mScrimNotEnrolledPrimaryColor;
    private final int mScrimNotEnrolledSecondaryColor;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int mEnrolledCount = 0;

    public void onBoundsChange(Rect bounds) {}

    public GridController(
            Context context, FaceEnrollAnimationMultiAngleDrawable.BucketListener bucketListener) {
        mBucketListener = bucketListener;
        mScrimNotEnrolledDefaultColor = context.getColor(R.color.face_enroll_cell_not_enrolled);
        mScrimNotEnrolledPrimaryColor =
                context.getColor(R.color.face_enroll_cell_not_enrolled_primary);
        mScrimNotEnrolledSecondaryColor =
                context.getColor(R.color.face_enroll_cell_not_enrolled_secondary);
        int[] primaryBuckets =
                context.getResources().getIntArray(R.array.face_enroll_primary_buckets);
        mPrimaryCellIndices = new ArraySet<>(primaryBuckets.length);
        for (int bucket : primaryBuckets) {
            mPrimaryCellIndices.add(bucket);
        }
        mNoActivityScrimState =
                new ScrimState(
                        context.getColor(R.color.face_enroll_no_activity_gone),
                        context.getColor(R.color.face_enroll_no_activity_showing));
        mGridState = new GridState(context, mHandler);
        mCellStates = new CellState[25];
        for (int i = 0; i < mCellStates.length; i++) {
            mCellStates[i] =
                    new CellState(context, i, mBucketListener, getScrimNotEnrolledColor(0, i));
        }
    }

    public void stopPulseForNoActivity() {
        for (CellState cellState : mCellStates) {
            cellState.stopPulseForNoActivity();
        }
    }

    public void pulseForNoActivity(int bucket, int times) {
        mCellStates[bucket].pulseForNoActivity(times);
    }

    public void onUserLeaveGood() {
        for (CellState cellState : mCellStates) {
            cellState.fadeScrimOut(CellState.SCRIM_FADE_REASON_USER_LEFT);
            cellState.fadeCursorNow();
        }
        mGridState.fadeOut(null);
    }

    public void onUserEnterGood() {
        mGridState.fadeIn();
        for (CellState cellState : mCellStates) {
            cellState.fadeScrimIn();
        }
    }

    public void onAcquired(int bucket) {
        boolean isDone = mCellStates[bucket].isDone();
        if (mNoActivityScrimState.isShowing() && !isDone) {
            mNoActivityScrimState.fadeOut();
        }
        mCellStates[bucket].onAcquired();
        if (isDone) {
            return;
        }
        mEnrolledCount++;
        updateColor(true);
    }

    public void restoreState(int bucket, boolean done) {
        if (done) {
            mCellStates[bucket].setEarlyDone();
            mEnrolledCount++;
        }
    }

    private void updateColor(boolean animate) {
        for (int i = 0; i < mCellStates.length; i++) {
            CellState cellState = mCellStates[i];
            if (!cellState.isDone()) {
                cellState.updateScrimNotEnrolledColor(
                        getScrimNotEnrolledColor(mEnrolledCount, i), animate);
            }
        }
    }

    public void setEarlyDone(int bucket) {
        if (mCellStates[bucket].isDone()) {
            return;
        }
        mCellStates[bucket].setEarlyDone();
        mEnrolledCount++;
        updateColor(false);
    }

    public void draw(Canvas canvas) {
        canvas.save();
        if (mCellConfigs == null) {
            initializeCells(canvas.getWidth(), canvas.getHeight());
            for (int i = 0; i < mCellStates.length; i++) {
                mCellStates[i].updateConfig(mCellConfigs[i]);
            }
        }
        canvas.translate(canvas.getWidth() / 2, canvas.getHeight() / 2);
        mNoActivityScrimState.draw(canvas);
        for (CellState cellState : mCellStates) {
            cellState.draw(canvas);
        }
        mGridState.draw(canvas);
        for (CellState cellState : mCellStates) {
            cellState.drawCursor(canvas);
        }
        canvas.restore();
    }

    private void initializeCells(int width, int height) {
        float halfInner = (width * 0.32f) / 2.0f;
        float halfOuter = (height * 0.78f) / 2.0f;
        float halfWidth = width / 2;
        float halfHeight = height / 2;
        RectF innerVertical = new RectF(-halfInner, -halfHeight, halfInner, halfHeight);
        RectF innerHorizontal = new RectF(-halfWidth, -halfInner, halfWidth, halfInner);
        RectF outerVertical = new RectF(-halfOuter, -halfHeight, halfOuter, halfHeight);
        RectF outerHorizontal = new RectF(-halfWidth, -halfOuter, halfWidth, halfOuter);
        RectF bounds = new RectF(-halfWidth, -halfHeight, halfWidth, halfHeight);
        float[][] startAngles = {
            {72.26f, 165.41f, 252.3f, 342.3f},
            {78.0f, 131.2f, 107.5f, 17.8f},
            {52.0f, 48.8f, 72.0f, 12.0f},
            {50.0f, 102.05f},
            {0.0f, 38.0f, 40.0f},
            {0.0f, 90.0f, 52.0f}
        };
        float[][] sweepAngles = {
            {35.45f, 30.07f, 35.0f, 35.4f},
            {24.0f, 31.0f, -35.0f, 31.6f},
            {26.0f, -31.0f, -31.0f, 26.0f},
            {81.0f, -23.85f},
            {52.0f, -26.0f, -40.0f},
            {90.0f, -52.0f, -52.0f}
        };
        Path path = new Path();
        path.arcTo(innerHorizontal, startAngles[0][0], sweepAngles[0][0]);
        path.arcTo(innerVertical, startAngles[0][1], sweepAngles[0][1]);
        path.arcTo(innerHorizontal, startAngles[0][2], sweepAngles[0][2]);
        path.arcTo(innerVertical, startAngles[0][3], sweepAngles[0][3]);
        Path path2 = new Path();
        path2.arcTo(outerHorizontal, startAngles[1][0], sweepAngles[1][0]);
        path2.arcTo(innerVertical, startAngles[1][1], sweepAngles[1][1]);
        path2.arcTo(innerHorizontal, startAngles[1][2], sweepAngles[1][2]);
        path2.arcTo(innerVertical, startAngles[1][3], sweepAngles[1][3]);
        Path path3 = new Path();
        path3.arcTo(outerHorizontal, startAngles[2][0], sweepAngles[2][0]);
        path3.arcTo(innerVertical, startAngles[2][1], sweepAngles[2][1]);
        path3.arcTo(innerHorizontal, startAngles[2][2], sweepAngles[2][2]);
        path3.arcTo(outerVertical, startAngles[2][3], sweepAngles[2][3]);
        Path path4 = new Path();
        path4.arcTo(innerVertical, startAngles[3][0], sweepAngles[3][0]);
        path4.arcTo(outerHorizontal, startAngles[3][1], sweepAngles[3][1]);
        Path path5 = new Path();
        path5.arcTo(outerHorizontal, startAngles[4][0], sweepAngles[4][0]);
        path5.arcTo(outerVertical, startAngles[4][1], sweepAngles[4][1]);
        path5.arcTo(innerHorizontal, startAngles[4][2], sweepAngles[4][2]);
        Path path6 = new Path();
        path6.arcTo(bounds, startAngles[5][0], sweepAngles[5][0]);
        path6.arcTo(outerVertical, startAngles[5][1], sweepAngles[5][1]);
        path6.arcTo(outerHorizontal, startAngles[5][2], sweepAngles[5][2]);
        mCellConfigs =
                new CellConfig[] {
                    new CellConfig(path6, 180),
                    new CellConfig(path5, 90, true),
                    new CellConfig(path4, 180),
                    new CellConfig(path5, 270),
                    new CellConfig(path6, 270),
                    new CellConfig(path5, 180),
                    new CellConfig(path3, 180),
                    new CellConfig(path2, 180),
                    new CellConfig(path3, 270),
                    new CellConfig(path5, 0, true),
                    new CellConfig(path4, 90),
                    new CellConfig(path2, 90),
                    new CellConfig(path, 0),
                    new CellConfig(path2, 270),
                    new CellConfig(path4, 270),
                    new CellConfig(path5, 180, true),
                    new CellConfig(path3, 90),
                    new CellConfig(path2, 0),
                    new CellConfig(path3, 0),
                    new CellConfig(path5, 0),
                    new CellConfig(path6, 90),
                    new CellConfig(path5, 90),
                    new CellConfig(path4, 0),
                    new CellConfig(path5, 270, true),
                    new CellConfig(path6, 0)
                };
    }

    private int getScrimNotEnrolledColor(int enrolledCount, int index) {
        if (mPrimaryCellIndices.isEmpty()) {
            return getScrimNotEnrolledColorWithoutPrimaryCells(enrolledCount);
        }
        return getScrimNotEnrolledColorWithPrimaryCells(index);
    }

    private int getScrimNotEnrolledColorWithoutPrimaryCells(int enrolledCount) {
        return ColorUtils.setAlphaComponent(
                mScrimNotEnrolledDefaultColor,
                Math.round(getScrimNotEnrolledOpacity(enrolledCount) * 255.0f));
    }

    private int getScrimNotEnrolledColorWithPrimaryCells(int index) {
        if (mPrimaryCellIndices.contains(index)) {
            return mScrimNotEnrolledPrimaryColor;
        }
        return mScrimNotEnrolledSecondaryColor;
    }

    private static float getScrimNotEnrolledOpacity(int enrolledCount) {
        for (int i = SCRIM_OPACITY_THRESHOLDS.length - 1; i >= 0; i--) {
            if (enrolledCount >= SCRIM_OPACITY_THRESHOLDS[i]) {
                return SCRIM_OPACITY_VALUES[i];
            }
        }
        return SCRIM_OPACITY_VALUES[0];
    }
}
