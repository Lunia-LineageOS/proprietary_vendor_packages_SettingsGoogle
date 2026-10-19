package com.google.android.settings.fuelgauge.reversecharging;

import android.content.Context;
import android.os.BatteryManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.Utils;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.TopIntroPreference;

import com.google.android.settings.R;
import com.google.android.systemui.reversecharging.ReverseChargingMetrics;

public class ReverseChargingSwitchPreferenceController
        extends ReverseChargingBasePreferenceController {

    private static final String TAG = "RCSwitchPrefController";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String KEY_INTRO_PREFERENCE = "reverse_charging_summary";
    static final int NO_ERROR = -1;

    MainSwitchPreference mPreference;
    TopIntroPreference mTopIntroPreference;

    public ReverseChargingSwitchPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void updateState() {
        updateState(mPreference);
    }

    @Override
    public boolean isChecked() {
        return mReverseChargingManager.isReverseChargingOn();
    }

    int checkLaunchRequirements() {
        if (mReverseChargingManager.isOnWirelessCharge()) {
            return 101;
        }
        if (isLowBattery()) {
            return 100;
        }
        if (isPowerSaveMode()) {
            return 104;
        }
        if (mIsUsbPlugIn) {
            return 107;
        }
        if (isOverheat()) {
            return 109;
        }
        if (mIsUnsupportedCharger) {
            return 111;
        }
        return NO_ERROR;
    }

    private void logLaunchFailEvent(int reason) {
        Log.d(TAG, "checkLaunchRequirements() = " + reason);
        int batteryLevel =
                mContext.getSystemService(BatteryManager.class)
                        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        if (reason != 100 || mLevelChanged) {
            ReverseChargingMetrics.logStopEvent(reason, batteryLevel, 0L);
        }
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mPreference.setChecked(isChecked);
        if (isChecked == mReverseChargingManager.isReverseChargingOn()) {
            return true;
        }
        if (DEBUG) {
            Log.d(TAG, "isChecked : " + isChecked);
        }
        mReverseChargingManager.setReverseChargingState(isChecked);
        return true;
    }

    boolean isLowBattery() {
        int thresholdLevel = getThresholdLevel();
        return thresholdLevel >= mLevel || mLevel < LOW_BATTERY_LEVEL;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (MainSwitchPreference) screen.findPreference(getPreferenceKey());
        mTopIntroPreference = (TopIntroPreference) screen.findPreference(KEY_INTRO_PREFERENCE);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        int thresholdLevel = getThresholdLevel();
        String title = mContext.getString(R.string.reverse_charging_switch_title);
        if (isOverheat()) {
            title = mContext.getString(R.string.reverse_charging_detail_charging_overheat_message);
        } else if (mIsUnsupportedCharger) {
            title =
                    mContext.getString(
                            R.string.reverse_charging_detail_unsupported_charger_message);
        } else if (isPowerSaveMode()) {
            title =
                    mContext.getString(
                            R.string.reverse_charging_detail_power_save_mode_is_on_message);
        } else if (mReverseChargingManager.isOnWirelessCharge()) {
            title =
                    mContext.getString(
                            R.string.reverse_charging_detail_charging_wirelessly_message);
        } else if (mIsUsbPlugIn) {
            title = mContext.getString(R.string.reverse_charging_detail_unplug_usb_cable_message);
        } else {
            if (mLevel != NO_ERROR && mLevel < LOW_BATTERY_LEVEL) {
                title =
                        mContext.getString(
                                R.string.reverse_charging_warning_title,
                                Utils.formatPercentage(LOW_BATTERY_LEVEL));
                Log.d(TAG, "updateState() phone is low battery ! level : " + mLevel);
            } else if (mLevel != NO_ERROR && thresholdLevel >= mLevel) {
                title =
                        mContext.getString(
                                R.string.reverse_charging_warning_title,
                                Utils.formatPercentage(thresholdLevel));
                Log.d(
                        TAG,
                        "updateState() phone is low battery ! level : "
                                + mLevel
                                + ", thresholdLevel : "
                                + thresholdLevel);
            }
        }
        if (!TextUtils.isEmpty(title)) {
            mTopIntroPreference.setTitle(title);
        }
        int failReason = checkLaunchRequirements();
        if (failReason != NO_ERROR) {
            logLaunchFailEvent(failReason);
        }
        preference.setEnabled(failReason == NO_ERROR);
    }
}
