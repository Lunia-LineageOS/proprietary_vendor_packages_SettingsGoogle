package com.google.android.settings.fuelgauge.reversecharging;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;

import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.core.SubSettingLauncher;

import com.google.android.settings.R;

public class ReverseChargingTrampoline extends SettingsBaseActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        startFragmentIfNecessary(ReverseChargingManager.getInstance(this));
    }

    void startFragmentIfNecessary(ReverseChargingManager reverseChargingManager) {
        if (reverseChargingManager.isSupportedReverseCharging()) {
            new SubSettingLauncher(getApplicationContext())
                    .setDestination(ReverseChargingDashboardFragment.class.getName())
                    .setTitleRes(R.string.reverse_charging_title)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setSourceMetricsCategory(SettingsEnums.FUELGAUGE_ADVANCED_BATTERY_OPTION)
                    .launch();
        }
        finish();
    }
}
