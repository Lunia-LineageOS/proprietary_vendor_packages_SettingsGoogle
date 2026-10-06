package com.google.android.settings.biometrics.face.anim.curve;

import android.graphics.Path;

public class CellConfig {
    final boolean mFlipVertical;
    final Path mPath;
    final int mRotation;

    CellConfig(Path path, int rotation) {
        this(path, rotation, false);
    }

    CellConfig(Path path, int rotation, boolean flipVertical) {
        mPath = path;
        mRotation = rotation;
        mFlipVertical = flipVertical;
    }
}
