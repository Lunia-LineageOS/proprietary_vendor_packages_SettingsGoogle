package com.google.android.settings.connecteddevice.dock;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.widget.GearPreference;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Shows the saved docks that are not connected in Previously connected devices. */
public class SavedDockUpdater implements DockUpdater, DockAsyncQueryHandler.OnQueryListener {
    private static final String TAG = "SavedDockUpdater";

    private final Context mContext;
    private final DevicePreferenceCallback mDevicePreferenceCallback;
    private final DockObserver mConnectedDockObserver;
    private final DockObserver mSavedDockObserver;
    private final DockAsyncQueryHandler mAsyncQueryHandler;

    @VisibleForTesting boolean mIsObserverRegistered;
    @VisibleForTesting final Map<String, GearPreference> mPreferenceMap = new ArrayMap<>();
    @VisibleForTesting Executor mExecutor = Executors.newSingleThreadExecutor();

    private Context mPreferenceContext;
    private Map<String, String> mSavedDevices;
    private String mConnectedDockId;

    public SavedDockUpdater(Context context, DevicePreferenceCallback devicePreferenceCallback) {
        mContext = context;
        mDevicePreferenceCallback = devicePreferenceCallback;
        Handler handler = new Handler(Looper.getMainLooper());
        mConnectedDockObserver =
                new DockObserver(
                        handler,
                        DockContract.DOCK_PROVIDER_CONNECTED_TOKEN,
                        DockContract.DOCK_PROVIDER_CONNECTED_URI);
        mSavedDockObserver =
                new DockObserver(
                        handler,
                        DockContract.DOCK_PROVIDER_SAVED_TOKEN,
                        DockContract.DOCK_PROVIDER_SAVED_URI);
        if (isRunningOnMainThread()) {
            mAsyncQueryHandler = new DockAsyncQueryHandler(context.getContentResolver());
            mAsyncQueryHandler.setOnQueryListener(this);
        } else {
            mAsyncQueryHandler = null;
        }
    }

    @Override
    public void registerCallback() {
        mExecutor.execute(
                () -> {
                    try {
                        mContext.getContentResolver()
                                .registerContentObserver(
                                        DockContract.DOCK_PROVIDER_CONNECTED_URI,
                                        false,
                                        mConnectedDockObserver);
                        mContext.getContentResolver()
                                .registerContentObserver(
                                        DockContract.DOCK_PROVIDER_SAVED_URI,
                                        false,
                                        mSavedDockObserver);
                        mIsObserverRegistered = true;
                        forceUpdate();
                    } catch (Exception e) {
                        Log.e(TAG, "registerCallback failed in background thread", e);
                    }
                });
    }

    @Override
    public void unregisterCallback() {
        mExecutor.execute(
                () -> {
                    if (mIsObserverRegistered) {
                        mContext.getContentResolver()
                                .unregisterContentObserver(mConnectedDockObserver);
                        mContext.getContentResolver().unregisterContentObserver(mSavedDockObserver);
                        mIsObserverRegistered = false;
                    }
                });
    }

    @Override
    public void forceUpdate() {
        startQuery(
                DockContract.DOCK_PROVIDER_CONNECTED_TOKEN,
                DockContract.DOCK_PROVIDER_CONNECTED_URI);
        startQuery(DockContract.DOCK_PROVIDER_SAVED_TOKEN, DockContract.DOCK_PROVIDER_SAVED_URI);
    }

    @Override
    public void setPreferenceContext(Context preferenceContext) {
        mPreferenceContext = preferenceContext;
    }

    @Override
    public void onQueryComplete(int token, List<DockDevice> devices) {
        if (devices == null) {
            return;
        }
        if (token == DockContract.DOCK_PROVIDER_SAVED_TOKEN) {
            updateSavedDevicesList(devices);
        } else if (token == DockContract.DOCK_PROVIDER_CONNECTED_TOKEN) {
            updateConnectedDevice(devices);
        }
    }

    private void startQuery(int token, Uri uri) {
        if (isRunningOnMainThread()) {
            mAsyncQueryHandler.startQuery(
                    token, mContext, uri, DockContract.DOCK_PROJECTION, null, null, null);
            return;
        }
        try (Cursor cursor =
                mContext.getApplicationContext()
                        .getContentResolver()
                        .query(uri, DockContract.DOCK_PROJECTION, null, null, null)) {
            onQueryComplete(token, DockAsyncQueryHandler.parseCursorToDockDevice(cursor));
        } catch (Exception e) {
            Log.w(TAG, "Query dockProvider fail", e);
        }
    }

    private void updateConnectedDevice(List<DockDevice> devices) {
        if (devices.isEmpty()) {
            mConnectedDockId = null;
            updateDevices();
            return;
        }
        mConnectedDockId = devices.get(0).getId();
        if (mPreferenceMap.containsKey(mConnectedDockId)) {
            mDevicePreferenceCallback.onDeviceRemoved(mPreferenceMap.get(mConnectedDockId));
            mPreferenceMap.remove(mConnectedDockId);
        }
    }

    private void updateSavedDevicesList(List<DockDevice> devices) {
        if (mSavedDevices == null) {
            mSavedDevices = new ArrayMap<>();
        }
        mSavedDevices.clear();
        for (DockDevice device : devices) {
            String name = device.getName();
            if (!TextUtils.isEmpty(name)) {
                mSavedDevices.put(device.getId(), name);
            }
        }
        updateDevices();
    }

    private void updateDevices() {
        if (mSavedDevices == null) {
            return;
        }
        for (String dockId : mSavedDevices.keySet()) {
            if (TextUtils.equals(dockId, mConnectedDockId)) {
                continue;
            }
            String dockName = mSavedDevices.get(dockId);
            if (mPreferenceMap.containsKey(dockId)) {
                mPreferenceMap.get(dockId).setTitle(dockName);
            } else {
                mPreferenceMap.put(dockId, initPreference(dockId, dockName));
                mDevicePreferenceCallback.onDeviceAdded(mPreferenceMap.get(dockId));
            }
        }
        mPreferenceMap.keySet().removeIf(this::hasDeviceBeenRemoved);
    }

    private boolean hasDeviceBeenRemoved(String dockId) {
        if (mSavedDevices.containsKey(dockId)) {
            return false;
        }
        mDevicePreferenceCallback.onDeviceRemoved(mPreferenceMap.get(dockId));
        return true;
    }

    private GearPreference initPreference(String dockId, String dockName) {
        GearPreference preference = new GearPreference(mPreferenceContext, null);
        preference.setIcon(DockUtils.buildRainbowIcon(mPreferenceContext, dockId));
        preference.setTitle(dockName);
        if (!TextUtils.isEmpty(dockId)) {
            preference.setIntent(DockContract.buildDockSettingIntent(dockId));
            preference.setSelectable(true);
        }
        return preference;
    }

    private boolean isRunningOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private class DockObserver extends ContentObserver {
        private final int mToken;
        private final Uri mUri;

        DockObserver(Handler handler, int token, Uri uri) {
            super(handler);
            mToken = token;
            mUri = uri;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            startQuery(mToken, mUri);
        }
    }
}
