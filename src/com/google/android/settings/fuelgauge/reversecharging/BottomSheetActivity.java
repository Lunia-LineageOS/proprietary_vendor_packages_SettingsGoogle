package com.google.android.settings.fuelgauge.reversecharging;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.google.android.settings.R;

public class BottomSheetActivity extends FragmentActivity {
    static final String REVERSE_CHARGING_SETTINGS = "android.settings.REVERSE_CHARGING_SETTINGS";
    ReverseChargingManager mReverseChargingManager;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (mReverseChargingManager == null) {
            mReverseChargingManager = ReverseChargingManager.getInstance(this);
        }
        if (!mReverseChargingManager.isSupportedReverseCharging()) {
            finish();
            return;
        }
        setContentView(R.layout.reverse_charging_bottom_sheet);
        Button okBtn = findViewById(R.id.ok_btn);
        Button learnMoreBtn = findViewById(R.id.learn_more_btn);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        TextView headerSubtitle = findViewById(R.id.header_subtitle);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new VideoPreferenceFragment())
                .commit();
        okBtn.setOnClickListener(view -> onOkClick());
        learnMoreBtn.setOnClickListener(view -> onLearnMoreClick());
        int titleRes = R.string.reverse_charging_title;
        setTitle(getString(titleRes));
        toolbarTitle.setText(getString(titleRes));
        headerSubtitle.setText(getString(R.string.reverse_charging_instructions_title));
    }

    private void onLearnMoreClick() {
        Intent intent = new Intent(REVERSE_CHARGING_SETTINGS);
        intent.setPackage("com.android.settings");
        startActivity(intent);
        finish();
    }

    private void onOkClick() {
        finish();
    }
}
