package com.google.android.settings.overlay

import com.google.android.settings.privatespace.PrivateSpaceLoginFeatureProviderGoogleImpl
import com.google.android.settings.search.SearchFeatureProviderGoogleImpl
import com.google.android.settings.vpn2.AdvancedVpnFeatureProviderGoogleImpl
import com.google.android.settings.wifi.factory.WifiFeatureProviderGoogleImpl

abstract class FeatureFactoryImpl : com.android.settings.overlay.FeatureFactoryImpl() {

    override val searchFeatureProvider = SearchFeatureProviderGoogleImpl

    override val advancedVpnFeatureProvider by lazy { AdvancedVpnFeatureProviderGoogleImpl() }

    override val wifiFeatureProvider by lazy { WifiFeatureProviderGoogleImpl(appContext) }

    override val privateSpaceLoginFeatureProvider by lazy { PrivateSpaceLoginFeatureProviderGoogleImpl() }
}
