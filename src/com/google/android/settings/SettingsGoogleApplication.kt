package com.google.android.settings

import com.android.settings.SettingsApplication
import com.android.settingslib.metadata.FixedArrayMap
import com.android.settingslib.metadata.PreferenceScreenMetadataFactory

// Stock is abstract only because R8 saw no direct instantiation.
open class SettingsGoogleApplication : SettingsApplication() {

    override fun preferenceScreenFactories():
        FixedArrayMap<String, PreferenceScreenMetadataFactory> =
        super.preferenceScreenFactories().merge(SettingsGoogleScreenCollector.get())
}
