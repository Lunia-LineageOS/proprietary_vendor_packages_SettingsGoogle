package com.google.android.settings.fuelgauge.reversecharging;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.settings.R;

public class VideoPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        addPreferencesFromResource(R.xml.reverse_charging_preference);
    }

    @Override
    public RecyclerView onCreateRecyclerView(
            LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        RecyclerView recyclerView = super.onCreateRecyclerView(layoutInflater, viewGroup, bundle);
        recyclerView.setImportantForAccessibility(2);
        return recyclerView;
    }
}
