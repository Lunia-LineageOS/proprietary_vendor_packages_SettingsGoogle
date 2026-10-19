package com.google.android.settings.fuelgauge.reversecharging

import android.content.Context
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.TopIntroPreference
import com.google.android.settings.R

class ReverseChargingTopIntroPreference :
    PreferenceMetadata, PreferenceBinding, PreferenceAvailabilityProvider {

    override val key: String
        get() = "reverse_charging_summary"

    override val title: Int
        get() = R.string.reverse_charging_switch_title

    override val purpose: Int
        get() = R.string.reverse_charging_summary_purpose

    override val indexable: Boolean
        get() = false

    override fun tags(context: Context): Array<String> = arrayOf("ui_only_preference")

    override fun createWidget(context: Context): TopIntroPreference = TopIntroPreference(context)

    override val availabilityDescription: String
        get() = "ui_only_preference"

    override fun getAvailabilityStability(): PreconditionStability =
        PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean =
        ReverseChargingManager.getInstance(context).isSupportedReverseCharging()
}
