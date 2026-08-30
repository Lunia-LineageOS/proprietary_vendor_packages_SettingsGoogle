package com.google.android.settings.simplemode;

import android.app.Activity;
import android.app.WallpaperManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManagerGlobal;
import android.view.WindowManagerPolicyConstants;
import android.window.ConfigurationChangeSetting;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava2.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava2.RxDataStore;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.display.DisplayDensityUtils;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import io.reactivex.Single;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class SimpleModeUtils {
    private static final String TAG = "SimpleModeUtils";
    private static final String SETTING_SIMPLE_VIEW_ENABLED = "matcha_enable";
    private static final String SETTING_LAUNCHER_LAYOUT_PROVIDER = "launcher3.layout.provider";
    private static final String SIMPLE_VIEW_LAUNCHER_AUTHORITY =
            "com.android.settings.matcha.accessibilityappearanceprovider";
    private static final String DATA_STORE_FEATURE_TOGGLE =
            "simple_mode_data_store_data_for_feature_toggle";
    private static final String DATA_STORE_BACKUP_RESTORE =
            "simple_mode_data_store_data_for_backup_restore";
    private static final Executor sExecutor = Executors.newSingleThreadExecutor();
    private static final ImmutableList<String> ONE_GRID_OPTION =
            ImmutableList.of("small", "medium", "large", "xl");
    protected static final Preferences.Key<Integer> KEY_USER_DISPLAY_SIZE =
            PreferencesKeys.intKey("user_display_size");
    protected static final Preferences.Key<Float> KEY_USER_FONT_SIZE =
            PreferencesKeys.floatKey("user_font_size");
    protected static final Preferences.Key<String> KEY_USER_NAVIGATION_MODE =
            PreferencesKeys.stringKey("user_navigation_mode");
    protected static final Preferences.Key<Integer> KEY_USER_NAVIGATION_MODE_CONFIG =
            PreferencesKeys.intKey("user_navigation_mode_config");
    protected static final Preferences.Key<Integer> KEY_USER_TOUCH_TIME =
            PreferencesKeys.intKey("user_touch_time");
    protected static final Preferences.Key<String> KEY_USER_GRID_OPTION =
            PreferencesKeys.stringKey("user_grid_option");
    protected static final Preferences.Key<String> KEY_PREVIOUS_DEVICE_SETTINGS =
            PreferencesKeys.stringKey("previous_device_settings");
    protected static final Preferences.Key<Long> KEY_INIT_TIME =
            PreferencesKeys.longKey("init_time");
    private static final Preferences.Key<String> KEY_DEVICE_LAUNCHER_AUTHORITY =
            PreferencesKeys.stringKey("device_launcher_authority");
    private static final long FIVE_MINUTES = Duration.ofMinutes(5).getSeconds();
    private static final long ONE_HOUR = Duration.ofHours(1).getSeconds();
    private static final long ONE_DAY = Duration.ofDays(1).getSeconds();
    private static final long ONE_WEEK = Duration.ofDays(7).getSeconds();

    private static int getSimpleViewDisplaySizeIndex() {
        return 2;
    }

    private static int getSimpleViewNavigationModeConfig() {
        return WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON;
    }

    private static boolean isFoldable(Context context) {
        return context.getResources()
                        .getIntArray(com.android.internal.R.array.config_foldedDeviceStates)
                        .length
                > 0;
    }

    private static boolean isTablet() {
        return Arrays.asList(SystemProperties.get("ro.build.characteristics").split(","))
                .contains("tablet");
    }

    private static boolean isDesktop() {
        return Arrays.asList(SystemProperties.get("ro.build.characteristics").split(","))
                .contains("desktop");
    }

    private static int getNavigationModeConfig(Context context) {
        return context.getResources()
                .getInteger(com.android.internal.R.integer.config_navBarInteractionMode);
    }

    private static String getNavigationMode(Context context) {
        if (getNavigationModeConfig(context) == WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON) {
            return WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;
        }
        return WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;
    }

    private static Uri getUriForGridOption(String path) {
        return new Uri.Builder()
                .scheme("content")
                .authority("com.google.android.apps.nexuslauncher.grid_control")
                .appendPath(path)
                .build();
    }

    private static String getSimpleViewGridOption() {
        return "medium";
    }

    private static String getSimpleViewNavigationMode() {
        return WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;
    }

    private static float getSimpleViewFontSize(Context context) {
        return Float.parseFloat(
                context.getResources()
                        .getStringArray(com.android.settingslib.R.array.entryvalues_font_size)[2]);
    }

    private static int getSimpleViewTouchTime(Context context) {
        return Integer.parseInt(
                context.getResources()
                        .getStringArray(R.array.long_press_timeout_selector_values)[1]);
    }

    private static Uri getUriForSetDefaultGrid() {
        return getUriForGridOption("default_grid");
    }

    protected static void setFeatureToggleStatus(Context context, int status) {
        Settings.Secure.putInt(context.getContentResolver(), SETTING_SIMPLE_VIEW_ENABLED, status);
    }

    private static String getGridOption(Context context) {
        String gridOption = "small";
        try (Cursor cursor =
                context.getContentResolver()
                        .query(getUriForGridOption("list_options"), null, null, null, null)) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex("name"));
                if (Boolean.parseBoolean(cursor.getString(cursor.getColumnIndex("is_default")))) {
                    gridOption = name;
                    break;
                }
            }
            return gridOption;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get list options", e);
            return "small";
        }
    }

    private static ContentValues getGridOptionContentValue(String gridOption) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", gridOption);
        contentValues.put("enable_apply_button", Boolean.FALSE);
        return contentValues;
    }

    protected static void setSimpleViewConfigsWithoutGridSize(Context context) {
        setupAccessibilityAppearance(
                context,
                getSimpleViewDisplaySizeIndex(),
                getSimpleViewFontSize(context),
                null,
                -1,
                getSimpleViewTouchTime(context),
                null);
    }

    private static void setSimpleViewConfigs(Context context) {
        setupAccessibilityAppearance(
                context,
                getSimpleViewDisplaySizeIndex(),
                getSimpleViewFontSize(context),
                getSimpleViewNavigationMode(),
                getSimpleViewNavigationModeConfig(),
                getSimpleViewTouchTime(context),
                getSimpleViewGridOption());
    }

    protected static String getDataForFeatureToggle(Context context) {
        RxDataStore<Preferences> rxDataStore =
                new RxPreferenceDataStoreBuilder(context, DATA_STORE_FEATURE_TOGGLE).build();
        Preferences preferences = rxDataStore.data().blockingFirst();
        int displaySize = preferences.get(KEY_USER_DISPLAY_SIZE);
        float fontSize = preferences.get(KEY_USER_FONT_SIZE);
        String navMode = preferences.get(KEY_USER_NAVIGATION_MODE);
        int navModeConfig = preferences.get(KEY_USER_NAVIGATION_MODE_CONFIG);
        int touchTime = preferences.get(KEY_USER_TOUCH_TIME);
        String gridOption = preferences.get(KEY_USER_GRID_OPTION);
        long initTime = preferences.get(KEY_INIT_TIME);
        rxDataStore.dispose();
        rxDataStore.shutdownComplete().blockingAwait();
        return String.valueOf(displaySize)
                + ","
                + String.valueOf(fontSize)
                + ","
                + navMode
                + ","
                + String.valueOf(navModeConfig)
                + ","
                + String.valueOf(touchTime)
                + ","
                + gridOption
                + ","
                + String.valueOf(initTime);
    }

    private static void restoreUserConfigs(Context context, String[] configs) {
        setupAccessibilityAppearance(
                context,
                Integer.parseInt(configs[0]),
                Float.parseFloat(configs[1]),
                configs[2],
                Integer.parseInt(configs[3]),
                Integer.parseInt(configs[4]),
                configs[5]);
    }

    private static void backupDeviceLauncherAuthority(Context context, String authority) {
        RxDataStore<Preferences> rxDataStore =
                new RxPreferenceDataStoreBuilder(context, DATA_STORE_FEATURE_TOGGLE).build();
        rxDataStore
                .updateDataAsync(
                        preferences -> {
                            MutablePreferences mutablePreferences =
                                    preferences.toMutablePreferences();
                            mutablePreferences.set(KEY_DEVICE_LAUNCHER_AUTHORITY, authority);
                            return Single.just(mutablePreferences);
                        })
                .blockingGet();
        rxDataStore.dispose();
        rxDataStore.shutdownComplete().blockingAwait();
    }

    private static void backupUserConfigs(Context context) {
        RxDataStore<Preferences> rxDataStore =
                new RxPreferenceDataStoreBuilder(context, DATA_STORE_FEATURE_TOGGLE).build();
        DisplayDensityUtils displayDensityUtils = new DisplayDensityUtils(context);
        ContentResolver contentResolver = context.getContentResolver();
        String[] fontSizeArray =
                context.getResources()
                        .getStringArray(com.android.settingslib.R.array.entryvalues_font_size);
        String[] touchTimeArray =
                context.getResources().getStringArray(R.array.long_press_timeout_selector_values);
        final int displaySize = displayDensityUtils.getCurrentIndex();
        final float fontSize =
                Settings.System.getFloat(
                        contentResolver,
                        Settings.System.FONT_SCALE,
                        Float.parseFloat(fontSizeArray[1]));
        final String navMode = getNavigationMode(context);
        final int navModeConfig = getNavigationModeConfig(context);
        final int touchTime =
                Settings.Secure.getInt(
                        contentResolver,
                        Settings.Secure.LONG_PRESS_TIMEOUT,
                        Integer.parseInt(touchTimeArray[0]));
        final String gridOption = getGridOption(context);
        final long initTime = System.currentTimeMillis();
        rxDataStore
                .updateDataAsync(
                        preferences -> {
                            MutablePreferences mutablePreferences =
                                    preferences.toMutablePreferences();
                            mutablePreferences.set(
                                    KEY_USER_DISPLAY_SIZE, Integer.valueOf(displaySize));
                            mutablePreferences.set(KEY_USER_FONT_SIZE, Float.valueOf(fontSize));
                            mutablePreferences.set(KEY_USER_NAVIGATION_MODE, navMode);
                            mutablePreferences.set(
                                    KEY_USER_NAVIGATION_MODE_CONFIG,
                                    Integer.valueOf(navModeConfig));
                            mutablePreferences.set(KEY_USER_TOUCH_TIME, Integer.valueOf(touchTime));
                            mutablePreferences.set(KEY_USER_GRID_OPTION, gridOption);
                            mutablePreferences.set(KEY_INIT_TIME, Long.valueOf(initTime));
                            return Single.just(mutablePreferences);
                        })
                .blockingGet();
        rxDataStore.dispose();
        rxDataStore.shutdownComplete().blockingAwait();
    }

    protected static boolean isSupportedDevice(Context context) {
        return !isFoldable(context) && !isTablet() && !isDesktop();
    }

    protected static void enableSimpleView(final Context context, final boolean isFromSuw) {
        setFeatureToggleStatus(context, 1);
        sExecutor.execute(
                () -> {
                    Trace.beginSection("enableSimpleView");
                    backupUserConfigs(context);
                    setSimpleViewConfigs(context);
                    if (isFromSuw) {
                        setComponentsStatusForSuw(context, false);
                        String authority =
                                Settings.Secure.getString(
                                        context.getContentResolver(),
                                        SETTING_LAUNCHER_LAYOUT_PROVIDER);
                        if (!SIMPLE_VIEW_LAUNCHER_AUTHORITY.equals(authority)) {
                            backupDeviceLauncherAuthority(context, authority);
                        }
                        setLauncherLayout(context, SIMPLE_VIEW_LAUNCHER_AUTHORITY);
                    }
                    Trace.endSection();
                    Log.d(TAG, "enableSimpleViewFromSetupWizard:" + isFromSuw);
                    FeatureFactory.getFeatureFactory()
                            .getMetricsFeatureProvider()
                            .action(
                                    context,
                                    isFromSuw
                                            ? SettingsEnums.ACTION_EASY_MODE_CHANGED_VIA_SETUPWIZARD
                                            : SettingsEnums.ACTION_EASY_MODE_CHANGED,
                                    0);
                });
    }

    protected static void disableSimpleView(final Context context, final boolean isFromSuw) {
        setFeatureToggleStatus(context, 0);
        sExecutor.execute(
                () -> {
                    Trace.beginSection("disableSimpleView");
                    String[] configs = getDataForFeatureToggle(context).split(",");
                    restoreUserConfigs(context, configs);
                    long initTime = Long.parseLong(configs[6]);
                    if (isFromSuw) {
                        setComponentsStatusForSuw(context, true);
                        String authority =
                                Settings.Secure.getString(
                                        context.getContentResolver(),
                                        SETTING_LAUNCHER_LAYOUT_PROVIDER);
                        RxDataStore<Preferences> rxDataStore =
                                new RxPreferenceDataStoreBuilder(context, DATA_STORE_FEATURE_TOGGLE)
                                        .build();
                        String launcherAuthority =
                                rxDataStore
                                        .data()
                                        .blockingFirst()
                                        .get(KEY_DEVICE_LAUNCHER_AUTHORITY);
                        if (SIMPLE_VIEW_LAUNCHER_AUTHORITY.equals(authority)) {
                            setLauncherLayout(context, launcherAuthority);
                        }
                        rxDataStore.dispose();
                        rxDataStore.shutdownComplete().blockingAwait();
                    }
                    Trace.endSection();
                    Log.d(TAG, "disableSimpleViewFromSetupWizard:" + isFromSuw);
                    FeatureFactory.getFeatureFactory()
                            .getMetricsFeatureProvider()
                            .action(
                                    context,
                                    isFromSuw
                                            ? SettingsEnums.ACTION_EASY_MODE_CHANGED_VIA_SETUPWIZARD
                                            : SettingsEnums.ACTION_EASY_MODE_CHANGED,
                                    isFromSuw ? 1 : getAccumulatedTimeLevel(initTime));
                });
    }

    protected static void setLauncherLayout(Context context, String authority) {
        Settings.Secure.putString(
                context.getContentResolver(), SETTING_LAUNCHER_LAYOUT_PROVIDER, authority);
    }

    protected static boolean isFontOrDisplaySmaller(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        return new DisplayDensityUtils(context).getCurrentIndex() > getSimpleViewDisplaySizeIndex()
                || Settings.System.getFloat(
                                contentResolver,
                                Settings.System.FONT_SCALE,
                                Float.parseFloat(
                                        context.getResources()
                                                .getStringArray(
                                                        com.android.settingslib.R.array
                                                                .entryvalues_font_size)[1]))
                        > getSimpleViewFontSize(context);
    }

    protected static boolean isSimpleViewEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), SETTING_SIMPLE_VIEW_ENABLED, 0)
                == 1;
    }

    protected static boolean isCallingFromSUWEntryPoint(Activity activity) {
        if (activity.getCallingPackage() == null) {
            return false;
        }
        return activity.getCallingPackage().equals("com.google.android.pixel.setupwizard");
    }

    protected static void setComponentsStatusForSuw(Context context, boolean enabled) {
        setTextReadingPackageComponent(context, enabled);
        setWallpaperSuggestionPackageComponent(context, enabled);
    }

    private static void setTextReadingPackageComponent(Context context, boolean enabled) {
        setComponentEnabledSetting(
                context,
                new ComponentName(
                        "com.android.settings",
                        "com.android.settings.TextReadingForSetupWizardActivity"),
                enabled);
    }

    private static void setWallpaperSuggestionPackageComponent(Context context, boolean enabled) {
        setComponentEnabledSetting(
                context,
                new ComponentName(
                        "com.android.settings",
                        "com.android.settings.wallpaper.WallpaperSuggestionActivity"),
                enabled);
    }

    private static void setComponentEnabledSetting(
            Context context, ComponentName componentName, boolean enabled) {
        context.getPackageManager()
                .setComponentEnabledSetting(
                        componentName,
                        enabled
                                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
    }

    protected static void setWallpaper(Context context) {
        try {
            WallpaperManager.getInstance(context)
                    .setResource(com.google.android.settings.R.raw.simple_mode_wallpaper);
        } catch (IOException e) {
            Log.e(TAG, "exception: ", e);
        }
    }

    protected static void overrideDataStore(Context context, String data) {
        String[] configs = data.split(",");
        if (configs.length == 8 && configs[0].equals("true")) {
            Log.d(TAG, "overrideDataStore");
            RxDataStore<Preferences> rxDataStore =
                    new RxPreferenceDataStoreBuilder(context, DATA_STORE_FEATURE_TOGGLE).build();
            final int displaySize = Integer.parseInt(configs[1]);
            final float fontSize = Float.parseFloat(configs[2]);
            final String navMode = configs[3];
            final int navModeConfig = Integer.parseInt(configs[4]);
            final int touchTime = Integer.parseInt(configs[5]);
            final String gridOption = configs[6];
            final long initTime = Long.parseLong(configs[7]);
            Log.d(TAG, "overrideDataStore-initTimeFromBackup:" + initTime);
            rxDataStore
                    .updateDataAsync(
                            preferences -> {
                                MutablePreferences mutablePreferences =
                                        preferences.toMutablePreferences();
                                mutablePreferences.set(
                                        KEY_USER_DISPLAY_SIZE, Integer.valueOf(displaySize));
                                mutablePreferences.set(KEY_USER_FONT_SIZE, Float.valueOf(fontSize));
                                mutablePreferences.set(KEY_USER_NAVIGATION_MODE, navMode);
                                mutablePreferences.set(
                                        KEY_USER_NAVIGATION_MODE_CONFIG,
                                        Integer.valueOf(navModeConfig));
                                mutablePreferences.set(
                                        KEY_USER_TOUCH_TIME, Integer.valueOf(touchTime));
                                mutablePreferences.set(KEY_USER_GRID_OPTION, gridOption);
                                mutablePreferences.set(KEY_INIT_TIME, Long.valueOf(initTime));
                                return Single.just(mutablePreferences);
                            })
                    .blockingGet();
            rxDataStore.dispose();
            rxDataStore.shutdownComplete().blockingAwait();
        }
    }

    protected static String getCurrentAccessibilityAppearanceConfigs(Context context) {
        DisplayDensityUtils displayDensityUtils = new DisplayDensityUtils(context);
        ContentResolver contentResolver = context.getContentResolver();
        String[] fontSizeArray =
                context.getResources()
                        .getStringArray(com.android.settingslib.R.array.entryvalues_font_size);
        String[] touchTimeArray =
                context.getResources().getStringArray(R.array.long_press_timeout_selector_values);
        boolean isSimpleViewEnabled = isSimpleViewEnabled(context);
        int displaySize = displayDensityUtils.getCurrentIndex();
        float fontSize =
                Settings.System.getFloat(
                        contentResolver,
                        Settings.System.FONT_SCALE,
                        Float.parseFloat(fontSizeArray[1]));
        String navMode = getNavigationMode(context);
        int navModeConfig = getNavigationModeConfig(context);
        int touchTime =
                Settings.Secure.getInt(
                        contentResolver,
                        Settings.Secure.LONG_PRESS_TIMEOUT,
                        Integer.parseInt(touchTimeArray[0]));
        return String.valueOf(isSimpleViewEnabled)
                + ","
                + String.valueOf(displaySize)
                + ","
                + String.valueOf(fontSize)
                + ","
                + navMode
                + ","
                + String.valueOf(navModeConfig)
                + ","
                + String.valueOf(touchTime)
                + ","
                + getGridOption(context);
    }

    protected static void restoreCurrentAccessibilityAppearance(Context context, String data) {
        if (Strings.isNullOrEmpty(data)) {
            Log.d(TAG, "Skipping restore: no any backup data");
            return;
        }
        String[] configs = data.split(",");
        if (configs.length != 7) {
            Log.d(TAG, "Skipping restore: data loss");
            return;
        }
        FeatureFactory.getFeatureFactory()
                .getMetricsFeatureProvider()
                .action(
                        context,
                        SettingsEnums.ACTION_RESTORE_SIMPLE_VIEW_RESULT_IN_SUW,
                        Boolean.parseBoolean(configs[0]));
        RxDataStore<Preferences> rxDataStore =
                new RxPreferenceDataStoreBuilder(context, DATA_STORE_BACKUP_RESTORE).build();
        rxDataStore
                .updateDataAsync(
                        preferences -> {
                            MutablePreferences mutablePreferences =
                                    preferences.toMutablePreferences();
                            mutablePreferences.set(KEY_PREVIOUS_DEVICE_SETTINGS, data);
                            return Single.just(mutablePreferences);
                        })
                .blockingGet();
        rxDataStore.dispose();
        rxDataStore.shutdownComplete().blockingAwait();
    }

    protected static void setupAccessibilityAppearance(
            Context context,
            int displaySize,
            float fontScale,
            String navMode,
            int navModeConfig,
            int touchTime,
            String gridOption) {
        DisplayDensityUtils displayDensityUtils = new DisplayDensityUtils(context);
        ContentResolver contentResolver = context.getContentResolver();
        IOverlayManager overlayManager =
                IOverlayManager.Stub.asInterface(ServiceManager.getService("overlay"));
        List<ConfigurationChangeSetting> forcedDisplayDensitySetting =
                displayDensityUtils.getForcedDisplayDensitySetting(displaySize);
        forcedDisplayDensitySetting.add(new ConfigurationChangeSetting.FontScaleSetting(fontScale));
        try {
            WindowManagerGlobal.getWindowManagerService()
                    .setConfigurationChangeSettingsForUser(
                            forcedDisplayDensitySetting, UserHandle.myUserId());
        } catch (RemoteException unused) {
            Log.w(TAG, "Unable to save forced display density and font size settings");
        }
        if (navMode != null && navModeConfig != -1) {
            try {
                overlayManager.setEnabledExclusiveInCategory(navMode, UserHandle.USER_CURRENT);
                Settings.Secure.putInt(
                        contentResolver, Settings.Secure.NAVIGATION_MODE, navModeConfig);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Settings.Secure.putInt(contentResolver, Settings.Secure.LONG_PRESS_TIMEOUT, touchTime);
        if (gridOption != null) {
            if (!isOneGridOption(gridOption)) {
                gridOption = remapToOneGridOption(gridOption);
                Log.d(TAG, "remapToOneGridOption: " + gridOption);
            }
            contentResolver.update(
                    getUriForSetDefaultGrid(), getGridOptionContentValue(gridOption), null, null);
        }
    }

    protected static String getPreviousDeviceSettings(Context context) {
        RxDataStore<Preferences> rxDataStore =
                new RxPreferenceDataStoreBuilder(context, DATA_STORE_BACKUP_RESTORE).build();
        String previousDeviceSettings =
                rxDataStore.data().blockingFirst().get(KEY_PREVIOUS_DEVICE_SETTINGS);
        rxDataStore.dispose();
        rxDataStore.shutdownComplete().blockingAwait();
        return previousDeviceSettings;
    }

    protected static int getAccumulatedTimeLevel(long initTime) {
        long duration = (System.currentTimeMillis() - initTime) / 1000;
        Log.d(TAG, "getAccumulatedTime:" + duration);
        if (duration > ONE_WEEK) {
            return 5;
        }
        if (duration > ONE_DAY) {
            return 4;
        }
        if (duration > ONE_HOUR) {
            return 3;
        }
        return duration > FIVE_MINUTES ? 2 : 1;
    }

    private static boolean isOneGridOption(String gridOption) {
        return ONE_GRID_OPTION.contains(gridOption);
    }

    private static String remapToOneGridOption(String gridOption) {
        switch (gridOption) {
            case "practical":
            case "reasonable":
                return "medium";
            case "big":
                return "large";
            case "crazy_big":
                return "xl";
            case "normal":
            default:
                return "small";
        }
    }
}
