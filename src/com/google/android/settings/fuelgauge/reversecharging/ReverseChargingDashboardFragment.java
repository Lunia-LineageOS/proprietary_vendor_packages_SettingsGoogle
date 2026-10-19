package com.google.android.settings.fuelgauge.reversecharging;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.google.android.settings.R;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ReverseChargingDashboardFragment extends RestrictedDashboardFragment {

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.reverse_charging);

    public ReverseChargingDashboardFragment() {
        super("ReverseChargingDashboardFragment");
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.OPEN_BATTERY_SHARE;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.reverse_charging;
    }

    @Override
    protected String getLogTag() {
        return "ReverseChargingDashboardFragment";
    }

    @Override
    public String getPreferenceScreenBindingKey(Context context) {
        return "reverse_charging";
    }
}
