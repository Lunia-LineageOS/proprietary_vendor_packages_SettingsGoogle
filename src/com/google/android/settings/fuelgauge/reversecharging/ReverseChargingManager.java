package com.google.android.settings.fuelgauge.reversecharging;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.android.settings.overlay.FeatureFactory;

import com.google.android.settings.R;
import com.google.android.systemui.reversecharging.ReverseWirelessCharger;

import vendor.google.wireless_charger.RtxStatusInfo;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReverseChargingManager {

    private static final String TAG = "ReverseChargingManager";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    private static ReverseChargingManager sInstance;

    final Context mContext;
    final ReverseWirelessCharger mReverseWirelessCharger;
    final Collection<ReverseChargingCallback> mReverseChargingCallbacks =
            new CopyOnWriteArrayList<>();
    final RtxInformationCallback mRtxInformationCallback = new RtxInformationCallback();
    final RtxStatusCallback mRtxStatusCallback = new RtxStatusCallback();
    final boolean mIsRtxSupported;

    public interface ReverseChargingCallback {
        void onReverseChargingStateChanged(int mode, int reason);
    }

    public static ReverseChargingManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ReverseChargingManager(context);
        }
        return sInstance;
    }

    private ReverseChargingManager(Context context) {
        mContext = context;
        mReverseWirelessCharger = new ReverseWirelessCharger(context, "settings_callback");
        mIsRtxSupported = isRtxSupported();
        mReverseWirelessCharger.addRtxInformationCallback(mRtxInformationCallback);
        mReverseWirelessCharger.addRtxStatusCallback(mRtxStatusCallback);
    }

    public void refreshRtxState() {
        mReverseWirelessCharger.getRtxInformation();
    }

    private boolean isRtxSupported() {
        if (mContext.getResources().getBoolean(R.bool.config_show_reverse_charging)) {
            return mReverseWirelessCharger.isRtxSupported();
        }
        return false;
    }

    public void registerCallback(ReverseChargingCallback reverseChargingCallback) {
        mReverseChargingCallbacks.add(reverseChargingCallback);
    }

    public void unregisterCallback(ReverseChargingCallback reverseChargingCallback) {
        mReverseChargingCallbacks.remove(reverseChargingCallback);
    }

    public boolean isReverseChargingOn() {
        if (!isOnWirelessCharge() && mIsRtxSupported) {
            return mReverseWirelessCharger.isRtxModeOn();
        }
        return false;
    }

    public void setReverseChargingState(boolean enabled) {
        mReverseWirelessCharger.setRtxMode(enabled);
        FeatureFactory.getFeatureFactory()
                .getMetricsFeatureProvider()
                .action(mContext, SettingsEnums.ACTION_REVERSE_CHARGING_TOGGLE, enabled);
    }

    public boolean isOnWirelessCharge() {
        Intent batteryIntent =
                mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return batteryIntent != null
                && batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                        == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    }

    boolean isSupportedReverseCharging() {
        return mIsRtxSupported;
    }

    void dispatchReverseChargingStateChanged(int mode, int reason) {
        for (ReverseChargingCallback callback :
                new CopyOnWriteArrayList<>(mReverseChargingCallbacks)) {
            callback.onReverseChargingStateChanged(mode, reason);
        }
    }

    class RtxInformationCallback implements ReverseWirelessCharger.RtxInformationCallback {
        @Override
        public void onRtxInformationChanged(RtxStatusInfo rtxStatusInfo) {
            if (ReverseChargingManager.DEBUG) {
                Log.d(TAG, "RtxInformationCallback() onValues : " + rtxStatusInfo.toString());
            }
            dispatchReverseChargingStateChanged(rtxStatusInfo.mode, rtxStatusInfo.reason);
        }
    }

    class RtxStatusCallback implements ReverseWirelessCharger.RtxStatusCallback {
        @Override
        public void onRtxStatusChanged(RtxStatusInfo rtxStatusInfo) {
            if (ReverseChargingManager.DEBUG) {
                Log.d(TAG, "rtxStatusInfoChanged() rtxStatusInfo : " + rtxStatusInfo.toString());
            }
            dispatchReverseChargingStateChanged(rtxStatusInfo.mode, rtxStatusInfo.reason);
        }
    }
}
