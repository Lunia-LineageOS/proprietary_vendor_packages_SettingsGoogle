package com.google.android.settings.fuelgauge.reversecharging;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.Utils;

import com.google.android.settings.R;

public class ReverseChargingPreferenceController extends ReverseChargingBasePreferenceController {

    Preference mPreference;

    public ReverseChargingPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void updateState() {
        updateState(mPreference);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        int thresholdLevel = getThresholdLevel();
        boolean isReverseChargingOn = mReverseChargingManager.isReverseChargingOn();
        if (isOverheat()) {
            preference.setSummary(
                    mContext.getString(R.string.reverse_charging_detail_charging_overheat_message));
            return;
        }
        if (mIsUnsupportedCharger) {
            preference.setSummary(
                    mContext.getString(
                            R.string.reverse_charging_detail_unsupported_charger_message));
            return;
        }
        if (isPowerSaveMode()) {
            preference.setSummary(
                    mContext.getString(R.string.reverse_charging_power_save_mode_is_on_message));
            return;
        }
        if (mReverseChargingManager.isOnWirelessCharge()) {
            preference.setSummary(
                    mContext.getString(R.string.reverse_charging_charging_wirelessly_message));
            return;
        }
        if (mIsUsbPlugIn) {
            preference.setSummary(
                    mContext.getString(R.string.reverse_charging_unplug_usb_cable_message));
            return;
        }
        if (mLevel < LOW_BATTERY_LEVEL) {
            preference.setSummary(
                    mContext.getString(
                            R.string.reverse_charging_warning_summary,
                            Utils.formatPercentage(LOW_BATTERY_LEVEL)));
            return;
        }
        if (thresholdLevel >= mLevel) {
            preference.setSummary(
                    mContext.getString(R.string.reverse_charging_sharing_level_message));
            return;
        }
        int summaryRes =
                isReverseChargingOn
                        ? R.string.reverse_charging_is_on_summary
                        : R.string.reverse_charging_is_off_summary;
        preference.setSummary(mContext.getString(summaryRes));
    }
}
