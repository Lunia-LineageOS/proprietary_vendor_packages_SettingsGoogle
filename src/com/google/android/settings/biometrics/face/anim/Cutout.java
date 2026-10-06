package com.google.android.settings.biometrics.face.anim;

import android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.TypedValue;

public abstract class Cutout {
    public static Bitmap createCutoutBitmap(Context context, int size, int radius) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        RectF rect = new RectF(0.0f, 0.0f, size, size);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(getColorAttr(context, R.attr.windowBackground));
        canvas.drawRect(rect, paint);
        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        float center = size / 2.0f;
        canvas.drawCircle(center, center, radius, paint);
        return bitmap;
    }

    private static int getColorAttr(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
