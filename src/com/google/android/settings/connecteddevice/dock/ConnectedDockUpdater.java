package com.google.android.settings.connecteddevice.dock;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.connecteddevice.dock.DockUpdater;
import com.android.settings.widget.GearPreference;

import com.google.android.settings.R;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Shows the currently connected dock in Connected devices. */
public class ConnectedDockUpdater implements DockUpdater, DockAsyncQueryHandler.OnQueryListener {
    private static final String TAG = "ConnectedDockUpdater";

    private final Context mContext;
    private final DevicePreferenceCallback mDevicePreferenceCallback;
    private final DockAsyncQueryHandler mAsyncQueryHandler;
    private final Uri mDockProviderUri = DockContract.DOCK_PROVIDER_CONNECTED_URI;
    private final ConnectedDockObserver mConnectedDockObserver =
            new ConnectedDockObserver(new Handler(Looper.getMainLooper()));

    @VisibleForTesting boolean mIsObserverRegistered;
    @VisibleForTesting GearPreference mDockPreference;
    @VisibleForTesting Executor mExecutor = Executors.newSingleThreadExecutor();

    private Context mPreferenceContext;
    private String mDockId;
    private String mDockName;

    public ConnectedDockUpdater(
            Context context, DevicePreferenceCallback devicePreferenceCallback) {
        mContext = context;
        mDevicePreferenceCallback = devicePreferenceCallback;
        mAsyncQueryHandler = new DockAsyncQueryHandler(context.getContentResolver());
        mAsyncQueryHandler.setOnQueryListener(this);
    }

    @Override
    public void registerCallback() {
        mExecutor.execute(
                () -> {
                    try {
                        mContext.getContentResolver()
                                .registerContentObserver(
                                        mDockProviderUri, false, mConnectedDockObserver);
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
                        mIsObserverRegistered = false;
                    }
                });
    }

    @Override
    public void forceUpdate() {
        mAsyncQueryHandler.startQuery(
                DockContract.DOCK_PROVIDER_CONNECTED_TOKEN,
                mContext,
                mDockProviderUri,
                DockContract.DOCK_PROJECTION,
                null,
                null,
                null);
    }

    @Override
    public void setPreferenceContext(Context preferenceContext) {
        mPreferenceContext = preferenceContext;
    }

    @Override
    public void onQueryComplete(int token, List<DockDevice> devices) {
        if (devices != null && !devices.isEmpty()) {
            DockDevice device = devices.get(0);
            mDockId = device.getId();
            mDockName = device.getName();
            updatePreference();
        } else if (mDockPreference != null && mDockPreference.isVisible()) {
            mDockPreference.setVisible(false);
            mDevicePreferenceCallback.onDeviceRemoved(mDockPreference);
        }
    }

    @VisibleForTesting
    void initPreference() {
        if (mDockPreference == null) {
            mDockPreference = new GearPreference(mPreferenceContext, null);
            mDockPreference.setSummary(mContext.getString(R.string.dock_summary_charging_phone));
            mDockPreference.setSelectable(false);
            mDockPreference.setVisible(false);
        }
    }

    private void updatePreference() {
        if (mDockPreference == null) {
            initPreference();
        }
        if (TextUtils.isEmpty(mDockName)) {
            if (mDockPreference.isVisible()) {
                mDockPreference.setVisible(false);
                mDevicePreferenceCallback.onDeviceRemoved(mDockPreference);
            }
            return;
        }
        mDockPreference.setIcon(DockUtils.buildRainbowIcon(mPreferenceContext, mDockId));
        mDockPreference.setTitle(mDockName);
        if (!TextUtils.isEmpty(mDockId)) {
            mDockPreference.setIntent(DockContract.buildDockSettingIntent(mDockId));
            mDockPreference.setSelectable(true);
        }
        if (!mDockPreference.isVisible()) {
            mDockPreference.setVisible(true);
            mDevicePreferenceCallback.onDeviceAdded(mDockPreference);
        }
    }

    private class ConnectedDockObserver extends ContentObserver {
        ConnectedDockObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            forceUpdate();
        }
    }
}
