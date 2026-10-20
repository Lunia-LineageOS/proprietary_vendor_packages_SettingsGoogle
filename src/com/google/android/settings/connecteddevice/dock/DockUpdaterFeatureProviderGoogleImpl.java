package com.google.android.settings.connecteddevice.dock;

import android.content.Context;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.overlay.DockUpdaterFeatureProvider;

/** Shows Pixel Stand docks in Connected devices through the Dreamliner dock provider. */
public class DockUpdaterFeatureProviderGoogleImpl implements DockUpdaterFeatureProvider {
    @Override
    public DockUpdater getConnectedDockUpdater(
            Context context, DevicePreferenceCallback devicePreferenceCallback) {
        return new ConnectedDockUpdater(context, devicePreferenceCallback);
    }

    @Override
    public DockUpdater getSavedDockUpdater(
            Context context, DevicePreferenceCallback devicePreferenceCallback) {
        return new SavedDockUpdater(context, devicePreferenceCallback);
    }
}
