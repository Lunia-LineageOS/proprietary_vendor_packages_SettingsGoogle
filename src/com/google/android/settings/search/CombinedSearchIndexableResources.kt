package com.google.android.settings.search

import com.android.settingslib.search.SearchIndexableData
import com.android.settingslib.search.SearchIndexableResources

/** Merges several [SearchIndexableResources] into one set of providers. */
class CombinedSearchIndexableResources(resources: List<SearchIndexableResources>) :
    SearchIndexableResources {
    private val providers = LinkedHashSet<SearchIndexableData>()

    init {
        for (resource in resources) {
            providers.addAll(resource.providerValues)
        }
    }

    override fun getProviderValues(): Collection<SearchIndexableData> = providers

    override fun addIndex(indexBundle: SearchIndexableData) {
        providers.add(indexBundle)
    }
}
