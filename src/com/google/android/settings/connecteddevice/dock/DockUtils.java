package com.google.android.settings.connecteddevice.dock;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.android.settingslib.widget.AdaptiveIcon;

import com.google.android.settings.R;

/** Utilities for dock preferences. */
public final class DockUtils {
    private DockUtils() {}

    /** Builds the dock icon, colored by a hash of the dock ID like Bluetooth device icons. */
    static Drawable buildRainbowIcon(Context context, String dockId) {
        Resources resources = context.getResources();
        Drawable drawable = context.getDrawable(R.drawable.ic_dock_24dp);
        int[] iconColors = resources.getIntArray(R.array.wlc_icon_fg_colors);
        int[] backgroundColors = resources.getIntArray(R.array.wlc_icon_bg_colors);
        int index =
                Math.abs(dockId.hashCode() % Math.min(iconColors.length, backgroundColors.length));
        drawable.setTint(iconColors[index]);
        AdaptiveIcon adaptiveIcon = new AdaptiveIcon(context, drawable);
        adaptiveIcon.setBackgroundColor(backgroundColors[index]);
        return adaptiveIcon;
    }
}
