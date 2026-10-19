package com.google.android.settings.fuelgauge.reversecharging

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R as AR
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.metadata.preferenceHierarchy
import com.google.android.settings.R
import kotlinx.coroutines.CoroutineScope

class ReverseChargingScreen : PreferenceScreenMixin {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.reverse_charging_title

    override val purpose: Int
        get() = R.string.reverse_charging_purpose

    override val keywords: Int
        get() = R.string.keywords_battery_share_settings

    override val highlightMenuKey: Int
        get() = AR.string.menu_key_battery

    override fun getMetricsCategory(): Int = SettingsEnums.OPEN_BATTERY_SHARE

    override fun isFlagEnabled(context: Context): Boolean = false

    override fun hasCompleteHierarchy(): Boolean = false

    override fun fragmentClass(): Class<out Fragment> = ReverseChargingDashboardFragment::class.java

    override fun getPreferenceHierarchy(
        context: Context,
        coroutineScope: CoroutineScope,
    ): PreferenceHierarchy =
        preferenceHierarchy(context) {
            +ReverseChargingTopIntroPreference()
            +ReverseChargingIllustrationPreference()
            +ReverseChargingFooterPreference()
        }

    companion object {
        const val KEY = "reverse_charging"
    }
}
