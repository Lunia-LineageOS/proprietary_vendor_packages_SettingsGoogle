package com.google.android.settings.fuelgauge.reversecharging;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.core.SettingsBaseActivity;

public class ReverseChargingToolTrampoline extends SettingsBaseActivity {
    static final String EXTRA_REVERSE_CHARGING_IS_SUPPORTED = "extra_reverse_charging_is_supported";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getIntent() == null) {
            Log.w("RCToolTrampoline", "Intent is null!");
            finish();
        }
        handleIntent(getIntent());
    }

    void handleIntent(Intent intent) {
        if (intent.getBooleanExtra(EXTRA_REVERSE_CHARGING_IS_SUPPORTED, false)) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(
                    EXTRA_REVERSE_CHARGING_IS_SUPPORTED,
                    ReverseChargingManager.getInstance(getBaseContext())
                            .isSupportedReverseCharging());
            setResult(10, resultIntent);
            finish();
        }
    }
}
