package com.google.android.settings.search

import android.content.Context
import android.content.Intent
import com.android.settings.search.SearchFeatureProviderImpl
import com.google.android.settings.external.SignatureVerifier

/** Indexes the Google pages alongside AOSP and lets Google apps launch search results. */
object SearchFeatureProviderGoogleImpl : SearchFeatureProviderImpl() {
    private val lazySearchIndexableResources by lazy {
        CombinedSearchIndexableResources(
            listOf(
                super.getSearchIndexableResources(),
                SearchIndexableResourcesGoogleFactory.createSearchIndexableResourcesGoogle(),
            )
        )
    }

    override fun getSearchIndexableResources(): CombinedSearchIndexableResources =
        lazySearchIndexableResources

    override fun isSignatureAllowlisted(context: Context, callerPackage: String): Boolean =
        SignatureVerifier.isPackageAllowlisted(context, callerPackage)

    override fun sendPreIndexIntent(context: Context) {
        val intent = Intent("com.google.android.settings.PRE_INDEX_SEARCH")
        intent.setPackage(getSettingsIntelligencePkgName(context))
        context.sendBroadcast(intent)
    }
}
