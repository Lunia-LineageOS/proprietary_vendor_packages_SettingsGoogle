package com.google.android.settings.search;

import com.android.settingslib.search.SearchIndexableResources;

/** Creates the generated Google search indexables for this form factor. */
abstract class SearchIndexableResourcesGoogleFactory {
    static SearchIndexableResources createSearchIndexableResourcesGoogle() {
        return new SearchIndexableResourcesMobile();
    }
}
