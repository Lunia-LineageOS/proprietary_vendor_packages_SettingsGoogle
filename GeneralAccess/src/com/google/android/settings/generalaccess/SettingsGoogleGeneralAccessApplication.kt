package com.google.android.settings.generalaccess

import com.google.android.settings.SettingsGoogleApplication
import com.google.android.settings.generalaccess.overlay.SettingsGoogleGeneralAccessFeatureFactoryImpl

open class SettingsGoogleGeneralAccessApplication : SettingsGoogleApplication() {
    override fun getFeatureFactory() = SettingsGoogleGeneralAccessFeatureFactoryImpl()
}
