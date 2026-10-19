package com.google.android.settings.fuelgauge.reversecharging;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;

public class ReverseChargingTopIntroController extends BasePreferenceController {

    ReverseChargingManager mReverseChargingManager;

    public ReverseChargingTopIntroController(Context context, String key) {
        super(context, key);
        mReverseChargingManager = ReverseChargingManager.getInstance(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return ReverseChargingUtils.getAvailability(mContext, mReverseChargingManager);
    }
}
