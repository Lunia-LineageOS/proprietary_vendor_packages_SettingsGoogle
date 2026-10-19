package com.google.android.settings.fuelgauge.reversecharging

import android.content.Context
import androidx.preference.Preference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.IllustrationPreference
import com.google.android.settings.R

class ReverseChargingIllustrationPreference : PreferenceMetadata, PreferenceBinding {

    override val indexable: Boolean
        get() = false

    override val key: String
        get() = "reverse_charging_illustration"

    override val purpose: Int
        get() = R.string.reverse_charging_illustration_purpose

    override fun tags(context: Context): Array<String> = arrayOf("ui_only_preference")

    override fun createWidget(context: Context): IllustrationPreference {
        val preference = IllustrationPreference(context)
        preference.setLottieAnimationResId(R.raw.lottie_reverse_charging)
        return preference
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isSelectable = false
    }
}
