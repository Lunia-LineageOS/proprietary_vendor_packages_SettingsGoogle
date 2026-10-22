package com.google.android.settings.overlay

import com.google.android.settings.search.SearchFeatureProviderGoogleImpl

abstract class FeatureFactoryImpl : com.android.settings.overlay.FeatureFactoryImpl() {

    override val searchFeatureProvider = SearchFeatureProviderGoogleImpl
}
