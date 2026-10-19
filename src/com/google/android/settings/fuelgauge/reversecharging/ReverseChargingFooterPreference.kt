package com.google.android.settings.fuelgauge.reversecharging

import android.content.Context
import com.android.settings.widget.FooterPreferenceBinding
import com.android.settings.widget.FooterPreferenceMetadata
import com.google.android.settings.R

class ReverseChargingFooterPreference : FooterPreferenceMetadata, FooterPreferenceBinding {

    override val key: String
        get() = "reverse_charging_detail_footer"

    override val purpose: Int
        get() = R.string.reverse_charging_detail_footer_purpose

    override val title: Int
        get() = R.string.reverse_charging_detail_footer_title_advanced

    override fun tags(context: Context): Array<String> = arrayOf("ui_only_preference")
}
