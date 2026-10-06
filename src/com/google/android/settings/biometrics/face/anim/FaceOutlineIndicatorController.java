package com.google.android.settings.biometrics.face.anim;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.widget.ImageView;

import com.google.android.settings.R;

public class FaceOutlineIndicatorController {
    private static final int STATE_CLEARED = 0;
    private static final int STATE_SHOWN = 1;

    private final Context mContext;
    private int mState = STATE_CLEARED;
    private final ImageView mView;

    public FaceOutlineIndicatorController(Context context, ImageView view) {
        mContext = context;
        mView = view;
    }

    public void show() {
        if (mState == STATE_SHOWN) {
            return;
        }
        AnimatedVectorDrawable animatedVectorDrawable =
                (AnimatedVectorDrawable) mContext.getDrawable(R.drawable.face_distance_fade_in);
        mView.setImageDrawable(animatedVectorDrawable);
        animatedVectorDrawable.start();
        mState = STATE_SHOWN;
    }

    public void clear() {
        if (mState == STATE_CLEARED) {
            return;
        }
        AnimatedVectorDrawable animatedVectorDrawable =
                (AnimatedVectorDrawable) mContext.getDrawable(R.drawable.face_distance_fade_out);
        mView.setImageDrawable(animatedVectorDrawable);
        animatedVectorDrawable.start();
        mState = STATE_CLEARED;
    }
}
