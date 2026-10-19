package com.google.android.settings.fuelgauge.reversecharging;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.SliderPreference;

import com.google.android.systemui.reversecharging.ReverseChargingMetrics;

public class ReverseChargingSeekBarController extends SliderPreferenceController
        implements LifecycleObserver, OnStop {

    public static final int BASE_LEVEL_TIMES = 5;
    public static final int MAX_SEEKBAR_VALUE = 10;
    public static final int MIN_SEEKBAR_VALUE = 2;

    boolean mIsPreferenceChanged;
    boolean mIsSliderPositionChanged;
    int mLastSliderPosition;
    SliderPreference mPreference;
    ReverseChargingManager mReverseChargingManager;

    public ReverseChargingSeekBarController(Context context, String key) {
        super(context, key);
        mIsSliderPositionChanged = false;
        mReverseChargingManager = ReverseChargingManager.getInstance(context);
    }

    @Override
    public int getMax() {
        return MAX_SEEKBAR_VALUE;
    }

    @Override
    public int getMin() {
        return MIN_SEEKBAR_VALUE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        SliderPreference sliderPreference = screen.findPreference(getPreferenceKey());
        mPreference = sliderPreference;
        sliderPreference.setUpdatesContinuously(true);
        mPreference.setMax(getMax());
        mPreference.setMin(getMin());
        mPreference.setSliderIncrement(1);
        mPreference.setTickVisible(false);
        mPreference.setHapticFeedbackMode(1);
        int sliderPosition = getSliderPosition();
        mLastSliderPosition = sliderPosition;
        mPreference.setValue(sliderPosition);
        updateSliderSummary();
    }

    @Override
    public int getSliderPosition() {
        return Settings.Global.getInt(
                mContext.getContentResolver(), "advanced_battery_usage_amount", 2);
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (position != mLastSliderPosition) {
            mLastSliderPosition = position;
            mIsSliderPositionChanged = true;
            mPreference.setValue(position);
            updateSliderSummary();
            Settings.Global.putInt(
                    mContext.getContentResolver(),
                    "advanced_battery_usage_amount",
                    mLastSliderPosition);
        }
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return ReverseChargingUtils.getAvailability(mContext, mReverseChargingManager);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mIsPreferenceChanged = true;
        return super.onPreferenceChange(preference, newValue);
    }

    @Override
    public void onStop() {
        if (mIsPreferenceChanged) {
            ReverseChargingMetrics.logLowBatteryThresholdChange(
                    getSliderPosition() * BASE_LEVEL_TIMES);
            mIsPreferenceChanged = false;
        }
        if (mIsSliderPositionChanged) {
            mIsSliderPositionChanged = false;
            FeatureFactory.getFeatureFactory()
                    .getMetricsFeatureProvider()
                    .action(
                            mContext,
                            SettingsEnums.ACTION_REVERSE_CHARGING_THRESHOLD,
                            getSliderPosition());
        }
    }

    private void updateSliderSummary() {
        String percentage = Utils.formatPercentage(mLastSliderPosition * BASE_LEVEL_TIMES);
        mPreference.setSummary(percentage);
        mPreference.setSliderStateDescription(percentage);
    }
}
