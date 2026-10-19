package com.google.android.settings.fuelgauge.reversecharging;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

abstract class ReverseChargingUtils {
    static int getAvailability(Context context, ReverseChargingManager reverseChargingManager) {
        if (reverseChargingManager == null && context != null) {
            reverseChargingManager = ReverseChargingManager.getInstance(context);
        }
        return (reverseChargingManager == null
                        || !reverseChargingManager.isSupportedReverseCharging())
                ? BasePreferenceController.UNSUPPORTED_ON_DEVICE
                : BasePreferenceController.AVAILABLE;
    }
}
