package com.google.android.settings.fuelgauge.reversecharging;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.TogglePreferenceController;

import com.google.android.settings.experiments.PhenotypeProxy;

public class ReverseChargingAutoTurnOnSwitchPreferenceController
        extends TogglePreferenceController {

    public static final int AUTO_TURN_ON_DISABLED = 0;
    public static final int AUTO_TURN_ON_ENABLED = 1;
    private static final String TAG = "AutoTurnOnSwitchPreferenceController";

    ReverseChargingManager mReverseChargingManager;
    private final boolean mSupportAutoTurnOn;

    public ReverseChargingAutoTurnOnSwitchPreferenceController(Context context, String key) {
        super(context, key);
        mReverseChargingManager = ReverseChargingManager.getInstance(context);
        mSupportAutoTurnOn =
                PhenotypeProxy.getBooleanFlagByPackageAndKey(
                        context,
                        context.getString(R.string.config_settingsintelligence_package_name),
                        "BatterySettings__is_reverse_charging_auto_turn_on_supported",
                        true);
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(
                        mContext.getContentResolver(),
                        "settings_key_reverse_charging_auto_turn_on",
                        0)
                == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        boolean success =
                Settings.Global.putInt(
                        mContext.getContentResolver(),
                        "settings_key_reverse_charging_auto_turn_on",
                        isChecked ? 1 : 0);
        if (!success) {
            Log.w(TAG, "set auto turn-on failed. isChecked=" + isChecked);
        }
        return success;
    }

    @Override
    public int getAvailabilityStatus() {
        if (mSupportAutoTurnOn) {
            return ReverseChargingUtils.getAvailability(mContext, mReverseChargingManager);
        }
        return BasePreferenceController.UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_battery;
    }
}
