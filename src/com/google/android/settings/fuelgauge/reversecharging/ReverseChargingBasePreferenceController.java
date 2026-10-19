package com.google.android.settings.fuelgauge.reversecharging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IThermalService;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Temperature;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.utils.ThreadUtils;

public abstract class ReverseChargingBasePreferenceController extends TogglePreferenceController
        implements PreferenceControllerMixin,
                LifecycleObserver,
                OnStart,
                OnStop,
                ReverseChargingManager.ReverseChargingCallback {

    private static final String TAG = "RCBasePrefController";
    private static final int AUDIO_CLASS = 1;
    private static final long DELAY_TIME = 300;
    protected static final int LOW_BATTERY_LEVEL = 10;
    private static final int MAX_POWER = 100;

    protected final Context mContext;
    Handler mHandler;
    final IntentFilter mIntentFilter;
    protected boolean mCharging;
    protected boolean mIsUnsupportedCharger;
    protected boolean mIsUsbPlugIn;
    protected int mLevel;
    protected boolean mLevelChanged;
    protected boolean mPluggedIn;
    PowerManager mPowerManager;
    BatteryLevelReceiver mReceiver;
    ReverseChargingManager mReverseChargingManager;
    IThermalService mThermalService;
    final ContentObserver mThresholdObserver;
    final Runnable mUpdateStateRunnable;
    UsbManager mUsbManager;

    public ReverseChargingBasePreferenceController(Context context, String key) {
        super(context, key);
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mLevel = MAX_POWER;
        mIsUsbPlugIn = false;
        mIsUnsupportedCharger = false;

        mThresholdObserver =
                new ContentObserver(mHandler) {
                    @Override
                    public void onChange(boolean selfChange) {
                        mHandler.removeCallbacks(mUpdateStateRunnable);
                        mHandler.postDelayed(mUpdateStateRunnable, DELAY_TIME);
                    }
                };

        mUpdateStateRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        int thresholdLevel = getThresholdLevel();
                        if (thresholdLevel >= mLevel) {
                            mReverseChargingManager.setReverseChargingState(false);
                        } else {
                            notifyChanged();
                        }
                    }
                };

        mReverseChargingManager = ReverseChargingManager.getInstance(context);
        mUsbManager = context.getSystemService(UsbManager.class);
        mPowerManager = context.getSystemService(PowerManager.class);
        mThermalService =
                IThermalService.Stub.asInterface(ServiceManager.getService("thermalservice"));
        mReceiver = new BatteryLevelReceiver();

        IntentFilter filter = new IntentFilter();
        mIntentFilter = filter;
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    }

    public abstract void updateState();

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    @Override
    public boolean isChecked() {
        return false;
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return false;
    }

    protected int getThresholdLevel() {
        return Settings.Global.getInt(
                        mContext.getContentResolver(), "advanced_battery_usage_amount", 2)
                * 5;
    }

    protected boolean isPowerSaveMode() {
        return mPowerManager.isPowerSaveMode();
    }

    protected boolean isOverheat() {
        try {
            for (Temperature temperature : mThermalService.getCurrentTemperaturesWithType(3)) {
                if (temperature.getStatus() >= 4) {
                    Log.w(
                            TAG,
                            "isOverheat() current skin status = "
                                    + temperature.getStatus()
                                    + ", temperature = "
                                    + temperature.getValue());
                    return true;
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, "isOverheat() : " + e);
        }
        return false;
    }

    @Override
    public int getAvailabilityStatus() {
        return ReverseChargingUtils.getAvailability(mContext, mReverseChargingManager);
    }

    @Override
    public void onStart() {
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mReverseChargingManager.registerCallback(this);
        mContext.getContentResolver()
                .registerContentObserver(
                        Settings.Global.getUriFor("advanced_battery_usage_amount"),
                        false,
                        mThresholdObserver);
        updateUsbDevice();
        ThreadUtils.postOnBackgroundThread(() -> mReverseChargingManager.refreshRtxState());
    }

    private void updateUsbDevice() {
        mIsUsbPlugIn = false;
        for (UsbDevice device : mUsbManager.getDeviceList().values()) {
            setIsUsbPlugin(device);
        }
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
        mReverseChargingManager.unregisterCallback(this);
        mContext.getContentResolver().unregisterContentObserver(mThresholdObserver);
    }

    class BatteryLevelReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, Intent.ACTION_BATTERY_CHANGED)) {
                int level =
                        (int)
                                ((intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100.0f)
                                        / intent.getIntExtra(
                                                BatteryManager.EXTRA_SCALE, MAX_POWER));
                mLevelChanged = mLevel != level;
                mLevel = level;
                mPluggedIn = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
                int status =
                        intent.getIntExtra(
                                BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
                boolean isFull = status == BatteryManager.BATTERY_STATUS_FULL;
                mCharging = isFull || status == BatteryManager.BATTERY_STATUS_CHARGING;
                notifyChanged();
                return;
            }
            if (TextUtils.equals(action, PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)) {
                notifyChanged();
                return;
            }
            if (TextUtils.equals(action, UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                setIsUsbPlugin((UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
                notifyChanged();
            } else if (TextUtils.equals(action, UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                mIsUsbPlugIn = false;
                notifyChanged();
            }
        }
    }

    void setIsUsbPlugin(UsbDevice usbDevice) {
        if (usbDevice != null) {
            boolean hasAudioInterface = false;
            for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
                if (usbDevice.getInterface(i).getInterfaceClass() == AUDIO_CLASS) {
                    hasAudioInterface = true;
                    break;
                }
            }
            boolean lowPowerConfig = false;
            for (int i = 0; i < usbDevice.getConfigurationCount(); i++) {
                if (usbDevice.getConfiguration(i).getMaxPower() < MAX_POWER) {
                    lowPowerConfig = true;
                    break;
                }
            }
            mIsUsbPlugIn = !(hasAudioInterface && lowPowerConfig);
        }
    }

    @Override
    public void onReverseChargingStateChanged(int mode, int reason) {
        mIsUnsupportedCharger = mode == 0 && (reason == 4 || reason == 5);
        notifyChanged();
    }

    private void notifyChanged() {
        mContext.getMainThreadHandler().post(this::updateState);
    }
}
