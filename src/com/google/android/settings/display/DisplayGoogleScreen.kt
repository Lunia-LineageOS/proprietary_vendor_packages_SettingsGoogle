package com.google.android.settings.display

import android.content.Context
import com.android.settings.display.DisplayScreen
import com.android.settings.dream.ScreensaverScreen
import com.android.settingslib.metadata.PreferenceHierarchy
import kotlinx.coroutines.CoroutineScope

class DisplayGoogleScreen : DisplayScreen() {

    override fun getPreferenceHierarchy(
        context: Context,
        coroutineScope: CoroutineScope,
    ): PreferenceHierarchy =
        super.getPreferenceHierarchy(context, coroutineScope).apply {
            -ScreensaverScreen.KEY
            onGroup("category_key_appearance") { +ScreensaverScreen.KEY }
            // Stock also adds category_touch (touch_sensitivity_settings_page) at order -160,
            // left out until touch sensitivity is ported.
        }
}
