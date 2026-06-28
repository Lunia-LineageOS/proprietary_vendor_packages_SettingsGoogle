package com.google.android.settings.localepicker;

import android.content.ContentResolver;
import android.provider.Settings;
import android.text.TextUtils;

import com.google.common.base.Splitter;

import java.util.HashSet;
import java.util.Set;

public abstract class AccessibilityStateUtils {
    private static final String TALKBACK_SERVICE = "TalkBackService";
    private static final String SELECT_TO_SPEAK_SERVICE = "SelectToSpeakService";

    private static void setEnabledAccessibilityServices(
            ContentResolver contentResolver, Set<String> services) {
        String enabledServices =
                Settings.Secure.getString(
                        contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabledServices)) {
            return;
        }
        for (String service : Splitter.on(':').split(enabledServices)) {
            if (service.startsWith("com.google.") || service.startsWith("com.googlecode.")) {
                if (service.endsWith(TALKBACK_SERVICE)) {
                    services.add(TALKBACK_SERVICE);
                }
                if (service.endsWith(SELECT_TO_SPEAK_SERVICE)) {
                    services.add(SELECT_TO_SPEAK_SERVICE);
                }
            }
        }
    }

    public static boolean isTtsEnabled(ContentResolver contentResolver) {
        Set<String> services = new HashSet<>();
        setEnabledAccessibilityServices(contentResolver, services);
        return services.contains(TALKBACK_SERVICE) || services.contains(SELECT_TO_SPEAK_SERVICE);
    }
}
