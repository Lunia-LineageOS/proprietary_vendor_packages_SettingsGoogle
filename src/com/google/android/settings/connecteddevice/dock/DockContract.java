package com.google.android.settings.connecteddevice.dock;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

/** Contract of the Dreamliner dock content provider. */
public final class DockContract {
    private static final String AUTHORITY = "com.google.android.apps.dreamliner.provider";
    private static final String PATH_CONNECTED = "connected";
    private static final String PATH_SAVED = "saved";

    private static final ComponentName DOCK_COMPONENT =
            new ComponentName(
                    "com.google.android.apps.dreamliner",
                    "com.google.android.apps.dreamliner.settings.DockDetailSettings");

    static final String DOCK_ID = "dockId";
    static final String DOCK_NAME = "dockName";

    static final int DOCK_PROVIDER_CONNECTED_TOKEN = 1;
    static final int DOCK_PROVIDER_SAVED_TOKEN = 2;

    public static final String[] DOCK_PROJECTION = {DOCK_ID, DOCK_NAME};

    public static final Uri DOCK_PROVIDER_CONNECTED_URI =
            new Uri.Builder()
                    .scheme("content")
                    .authority(AUTHORITY)
                    .appendPath(PATH_CONNECTED)
                    .build();

    public static final Uri DOCK_PROVIDER_SAVED_URI =
            new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(PATH_SAVED).build();

    private DockContract() {}

    public static Intent buildDockSettingIntent(String dockId) {
        return new Intent().setComponent(DOCK_COMPONENT).putExtra(DOCK_ID, dockId);
    }
}
