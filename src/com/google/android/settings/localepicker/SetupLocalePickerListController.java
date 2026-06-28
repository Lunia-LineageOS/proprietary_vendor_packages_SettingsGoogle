package com.google.android.settings.localepicker;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate;

import com.android.internal.app.LocaleCollectorBase;
import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.internal.app.SystemLocaleCollector;
import com.android.settings.overlay.FeatureFactory;

import com.google.android.settings.R;
import com.google.android.setupdesign.items.IItem;
import com.google.android.setupdesign.items.ItemViewHolder;
import com.google.android.setupdesign.items.RecyclerItemAdapter;
import com.google.android.setupdesign.items.SectionItem;
import com.google.android.setupdesign.util.ThemeHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class SetupLocalePickerListController implements RecyclerItemAdapter.OnItemSelectedListener {
    private static final String TAG = "SetupLocalePickerListController";
    private static final int PSKU_JAPAN = 1;
    private static final int PSKU_UNITED_STATES = 2;
    private static final ImmutableMap<String, Integer> PSKU_MAP =
            ImmutableMap.of(
                    "UVZ", PSKU_UNITED_STATES,
                    "UAT", PSKU_UNITED_STATES,
                    "UTM", PSKU_UNITED_STATES,
                    "UGS", PSKU_UNITED_STATES,
                    "AJP", PSKU_JAPAN);
    private static final ImmutableList<String> REGION_SEARCH_SUPPORTED_LANGUAGES =
            ImmutableList.of("ar", "en", "es", "fr");

    private Activity mActivity;
    private RecyclerItemAdapter mAdapter;
    private SectionItem mAllListGroup;
    private Context mContext;
    private SectionItem mDividerItem;
    private boolean mIsCountryMode;
    private boolean mIsNumberingSystemMode;
    private LocaleSelectedListener mListener;
    private Set<LocaleStore.LocaleInfo> mLocaleList;
    private List<LocaleStore.LocaleInfo> mLocaleOptions;
    private LocaleStore.LocaleInfo mParentLocale;
    private String mPskuString;
    private SectionItem mSearchBarItem;
    private SectionItem mSuggestedListGroup;
    private boolean mIsFromSearch = false;
    private List<LocaleStore.LocaleInfo> mSuggestedLocaleOptions = new ArrayList<>();

    public SetupLocalePickerListController(
            Context context,
            Activity activity,
            LocaleStore.LocaleInfo localeInfo,
            boolean isNumberingSystemMode) {
        mPskuString = "";
        mParentLocale = localeInfo;
        mIsNumberingSystemMode = isNumberingSystemMode;
        mLocaleList = LocaleStore.getLevelLocales(context, new HashSet<>(), null, true);
        mContext = context;
        mActivity = activity;
        mLocaleOptions = new ArrayList<>(mLocaleList.size());
    }

    void setLocaleSelectedListener(LocaleSelectedListener listener) {
        mListener = listener;
    }

    void setPsku(String psku) {
        mPskuString = psku;
    }

    protected void displayScreen(RecyclerItemAdapter adapter) {
        mAdapter = adapter;
        mSuggestedListGroup = (SectionItem) adapter.findItemById(R.id.suggested_list);
        mAllListGroup = (SectionItem) mAdapter.findItemById(R.id.all_list);
        mSearchBarItem = (SectionItem) mAdapter.findItemById(R.id.setup_search_bar_section);
        mDividerItem = (SectionItem) mAdapter.findItemById(R.id.setup_divider_section);
        RecyclerView recyclerView =
                mActivity.findViewById(com.google.android.setupdesign.R.id.sud_recycler_view);
        if (recyclerView != null) {
            recyclerView.setAccessibilityDelegateCompat(
                    new LocalePickerAccessibilityDelegate(recyclerView));
        }
        if (mSearchBarItem != null && (mIsNumberingSystemMode || !isRegionSearchSupported())) {
            ((SearchBarItem) mSearchBarItem.getItemAt(0)).setVisible(false);
        }
        if (mDividerItem != null
                && (ThemeHelper.shouldApplyGlifExpressiveStyle(mActivity)
                        || AccessibilityStateUtils.isTtsEnabled(mContext.getContentResolver()))) {
            ((DividerItem) mDividerItem.getItemAt(0)).setVisible(false);
        }
        updateScreen();
        mAdapter.setOnItemSelectedListener(this);
    }

    private void updateScreen() {
        mIsFromSearch = false;
        updateHeaders();
        updateGroupItems();
    }

    private void updateHeaders() {
        if (mParentLocale != null) {
            mSuggestedListGroup.setHeaderTitle(
                    mContext.getString(com.android.settings.R.string.suggested_locales_title));
            String title;
            if (mIsNumberingSystemMode) {
                title =
                        mContext.getString(
                                com.android.settings.R.string.all_supported_numbering_system_title);
            } else {
                title =
                        mContext.getString(
                                com.android.settings.R.string.all_supported_locales_regions_title);
            }
            mAllListGroup.setHeaderTitle(title);
            if (ThemeHelper.shouldApplyGlifExpressiveStyle(mActivity)) {
                return;
            }
            mSuggestedListGroup
                    .getHeader()
                    .setTitleColor(
                            mContext.getColor(com.android.internal.R.color.materialColorPrimary));
            mAllListGroup
                    .getHeader()
                    .setTitleColor(
                            mContext.getColor(com.android.internal.R.color.materialColorPrimary));
            return;
        }
        boolean shouldApplyGlifExpressiveStyle =
                ThemeHelper.shouldApplyGlifExpressiveStyle(mActivity);
        if (shouldApplyGlifExpressiveStyle) {
            mSuggestedListGroup.setHeaderTitle("");
            mAllListGroup.setHeaderTitle("");
        } else {
            mSuggestedListGroup.removeChild(mSuggestedListGroup.getHeader());
            mAllListGroup.removeChild(mAllListGroup.getHeader());
        }
    }

    protected void updateGroupItems() {
        List<LocaleStore.LocaleInfo> suggestedLocaleListByPsku;
        List<LocaleStore.LocaleInfo> sortedLocaleList;
        if (mParentLocale != null) {
            mIsCountryMode = true;
            mLocaleList =
                    getLocaleCollectorController(mContext)
                            .getSupportedLocaleList(mParentLocale, false, mIsCountryMode);
            mLocaleOptions = new ArrayList<>(mLocaleList.size());
            suggestedLocaleListByPsku = getSortedLocaleList(getSuggestedLocaleList());
            sortedLocaleList = getSortedLocaleList(getSupportedLocaleList());
        } else if (AccessibilityStateUtils.isTtsEnabled(mContext.getContentResolver())) {
            suggestedLocaleListByPsku = getTtsLocaleList();
            sortedLocaleList = new ArrayList<>();
        } else {
            LocaleStore.updateSimCountries(mContext);
            mLocaleList = LocaleStore.getLevelLocales(mContext, new HashSet<>(), null, true);
            if (LocaleStore.isSimOrNwCountryAvailable()) {
                suggestedLocaleListByPsku = getSortedLocaleList(getLanguageSuggestedLocaleList());
                sortedLocaleList = getSortedLocaleList(getSupportedLocaleList());
            } else {
                suggestedLocaleListByPsku = getSuggestedLocaleListByPsku();
                sortedLocaleList = getSortedLocaleList(getSupportedLocaleListByPsku());
            }
        }
        setupItem(suggestedLocaleListByPsku, sortedLocaleList);
    }

    protected void onSearchListChanged(
            List<LocaleStore.LocaleInfo> localeInfoList, CharSequence prefix) {
        mSuggestedListGroup.clear();
        mAllListGroup.clear();
        List<LocaleStore.LocaleInfo> allSupportedLocaleList = getAllSupportedLocaleList();
        if (prefix == null || prefix.toString().isEmpty()) {
            updateScreen();
            return;
        }
        List<LocaleStore.LocaleInfo> sortedSuggestedRegionFromSearchList =
                getSortedSuggestedRegionFromSearchList(
                        prefix, localeInfoList, allSupportedLocaleList);
        Collections.sort(
                sortedSuggestedRegionFromSearchList,
                Comparator.comparing(localeInfo -> localeInfo.getLocale().getDisplayName()));
        setupItemWithSearchResult(sortedSuggestedRegionFromSearchList);
    }

    private void setupItemWithSearchResult(List<LocaleStore.LocaleInfo> localeInfoList) {
        if (mSuggestedListGroup.getHeader() != null) {
            mSuggestedListGroup.removeChild(mSuggestedListGroup.getHeader());
        }
        if (mAllListGroup.getHeader() != null) {
            mAllListGroup.removeChild(mAllListGroup.getHeader());
        }
        mIsFromSearch = true;
        localeInfoList.stream().forEach(locale -> addLocaleItem(locale, true, mSuggestedListGroup));
    }

    private void addLocaleItem(
            LocaleStore.LocaleInfo localeInfo, boolean isSuggested, SectionItem sectionItem) {
        LocaleItem localeItem = new LocaleItem();
        localeItem.setCountryMode(mIsCountryMode);
        localeItem.setSuggestedState(isSuggested);
        localeItem.setTitle(
                mIsCountryMode
                        ? localeInfo.getFullCountryNameNative()
                        : localeInfo.getFullNameNative());
        localeItem.setLocaleInfo(localeInfo);
        sectionItem.addChild(localeItem);
    }

    protected void setupItem(
            List<LocaleStore.LocaleInfo> suggestedLocales,
            List<LocaleStore.LocaleInfo> allLocales) {
        Log.d(TAG, "setupItem: isNumberingMode = " + mIsNumberingSystemMode);
        suggestedLocales.stream()
                .forEach(locale -> addLocaleItem(locale, true, mSuggestedListGroup));
        allLocales.stream().forEach(locale -> addLocaleItem(locale, false, mAllListGroup));
        if (mSuggestedListGroup.getCount() == 0) {
            mSuggestedListGroup.clear();
        }
        if (mAllListGroup.getCount() == 0) {
            mAllListGroup.clear();
        }
    }

    @Override
    public void onItemSelected(IItem item) {
        if (item instanceof SearchBarItem) {
            Bundle extras = mActivity.getIntent().getExtras();
            extras.putSerializable("extra_target_locale", mParentLocale);
            extras.putBoolean("extra_is_numbering_system", mParentLocale.hasNumberingSystems());
            Intent intent =
                    new Intent("com.google.android.settings.localepicker.SETUP_REGION_SEARCH");
            intent.putExtras(extras);
            mActivity.startActivityForResult(intent, 0);
            return;
        }
        LocaleItem localeItem = (LocaleItem) item;
        if (mParentLocale == null) {
            FeatureFactory.getFeatureFactory()
                    .getMetricsFeatureProvider()
                    .action(
                            mContext,
                            localeItem.isSuggested()
                                    ? SettingsEnums
                                            .ACTION_CHOOSE_PREFERRED_LANGUAGE_FROM_SUGGESTED_LIST_IN_SUW
                                    : SettingsEnums
                                            .ACTION_CHOOSE_PREFERRED_LANGUAGE_FROM_ALL_LIST_IN_SUW);
            Settings.Global.putInt(
                    mContext.getContentResolver(),
                    "is_suggested_locale",
                    localeItem.isSuggested() ? 1 : 0);
        } else if (!mIsNumberingSystemMode) {
            if (mIsFromSearch) {
                FeatureFactory.getFeatureFactory()
                        .getMetricsFeatureProvider()
                        .action(
                                mContext,
                                SettingsEnums.ACTION_CHOOSE_REGION_AFTER_SEARCH_REGION_IN_SUW);
            } else {
                FeatureFactory.getFeatureFactory()
                        .getMetricsFeatureProvider()
                        .action(
                                mContext,
                                localeItem.isSuggested()
                                        ? SettingsEnums
                                                .ACTION_CHOOSE_PREFERRED_REGION_FROM_SUGGESTED_LIST_IN_SUW
                                        : SettingsEnums
                                                .ACTION_CHOOSE_PREFERRED_REGION_FROM_ALL_LIST_IN_SUW);
            }
        }
        LocaleStore.LocaleInfo localeInfo = localeItem.getLocaleInfo();
        if (isFinalSelectedLocale(localeInfo)) {
            if (localeInfo.getLocale().getCountry().isEmpty()) {
                localeInfo =
                        LocaleStore.getLevelLocales(mContext, new HashSet<>(), localeInfo, true)
                                .iterator()
                                .next();
            }
            mListener.onLocaleSelected(localeInfo);
            LocalePicker.updateLocales(new LocaleList(localeInfo.getLocale()));
            Settings.Global.putInt(mContext.getContentResolver(), "is_locale_set", 1);
            return;
        }
        if (localeInfo.getParent() == null) {
            LocalePicker.updateLocales(new LocaleList(localeInfo.getLocale()));
        }
        Bundle extras = mActivity.getIntent().getExtras();
        extras.putSerializable("extra_target_locale", localeInfo);
        extras.putBoolean("extra_is_numbering_system", localeInfo.hasNumberingSystems());
        Intent intent = new Intent("com.google.android.settings.localepicker.REGIONAND_PICKER");
        intent.putExtras(extras);
        mActivity.startActivityForResult(intent, 0);
    }

    protected void initSuggestedListSectionItem(SectionItem suggestedListGroup) {
        mSuggestedListGroup = suggestedListGroup;
    }

    protected void initAllListSectionItem(SectionItem allListGroup) {
        mAllListGroup = allListGroup;
    }

    protected void initLocaleList(Set<LocaleStore.LocaleInfo> localeList) {
        mLocaleList = localeList;
    }

    protected SectionItem getSuggestedListSectionItem() {
        return mSuggestedListGroup;
    }

    protected SectionItem getAllListSectionItem() {
        return mAllListGroup;
    }

    protected List<LocaleStore.LocaleInfo> getTtsLocaleList() {
        for (String languageTag : getTtsLocalesFromStringArray()) {
            mSuggestedLocaleOptions.add(
                    LocaleStore.getLocaleInfo(Locale.forLanguageTag(languageTag)));
        }
        return mSuggestedLocaleOptions;
    }

    private String[] getTtsLocalesFromStringArray() {
        return mContext.getResources().getStringArray(R.array.tts_locales);
    }

    private List<LocaleStore.LocaleInfo> getAllSupportedLocaleList() {
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            mLocaleOptions.addAll(mLocaleList);
        } else {
            Log.d(TAG, "Can not get locales because the locale list is null or empty.");
        }
        return mLocaleOptions;
    }

    protected List<LocaleStore.LocaleInfo> getSuggestedLocaleListByPsku() {
        for (String languageTag : getSuggestedLocalesFromStringArray()) {
            mSuggestedLocaleOptions.add(
                    LocaleStore.getLocaleInfo(Locale.forLanguageTag(languageTag)));
        }
        return mSuggestedLocaleOptions;
    }

    protected List<LocaleStore.LocaleInfo> getSupportedLocaleListByPsku() {
        List<LocaleStore.LocaleInfo> result = new ArrayList<>();
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            HashMap<Locale, LocaleStore.LocaleInfo> locales = new HashMap<>(mLocaleList.size());
            for (LocaleStore.LocaleInfo localeInfo : mLocaleList) {
                if (!isPskuSuggestedLocale(localeInfo)) {
                    if (localeInfo.getLocale().getCountry().isEmpty()) {
                        if (localeInfo.isTranslated()) {
                            locales.put(localeInfo.getLocale(), localeInfo);
                        }
                    } else if (localeInfo.isTranslated()) {
                        locales.put(
                                localeInfo.getParent(),
                                LocaleStore.getLocaleInfo(localeInfo.getParent()));
                    }
                }
            }
            result.addAll(locales.values());
            return result;
        }
        Log.d(TAG, "Can not get supported locales because the locale list is null or empty.");
        return result;
    }

    private boolean isPskuSuggestedLocale(LocaleStore.LocaleInfo localeInfo) {
        for (LocaleStore.LocaleInfo suggested : mSuggestedLocaleOptions) {
            if (suggested.getLocale().equals(localeInfo.getLocale())) {
                return true;
            }
            if (suggested.getLocale().getCountry().isEmpty()
                    && !localeInfo.getLocale().getCountry().isEmpty()
                    && suggested.getLocale().equals(localeInfo.getParent())) {
                return true;
            }
        }
        return false;
    }

    private String[] getSuggestedLocalesFromStringArray() {
        int psku = PSKU_MAP.getOrDefault(mPskuString, 0);
        if (psku == PSKU_JAPAN) {
            return mContext.getResources().getStringArray(R.array.japan_locales);
        }
        if (psku == PSKU_UNITED_STATES) {
            return mContext.getResources().getStringArray(R.array.united_states_locales);
        }
        return mContext.getResources().getStringArray(R.array.global_locales);
    }

    protected List<LocaleStore.LocaleInfo> getLanguageSuggestedLocaleList() {
        List<LocaleStore.LocaleInfo> result = new ArrayList<>();
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            HashMap<Locale, LocaleStore.LocaleInfo> locales = new HashMap<>(mLocaleList.size());
            for (LocaleStore.LocaleInfo localeInfo : mLocaleList) {
                if (localeInfo.isSuggested()) {
                    Locale locale =
                            new Locale.Builder()
                                    .setLocale(localeInfo.getLocale().stripExtensions())
                                    .build();
                    locales.put(locale, LocaleStore.getLocaleInfo(locale));
                }
            }
            result.addAll(locales.values());
            return result;
        }
        Log.d(TAG, "Can not get suggested locales because the locale list is null or empty.");
        return result;
    }

    protected List<LocaleStore.LocaleInfo> getSuggestedLocaleList() {
        List<LocaleStore.LocaleInfo> result = new ArrayList<>();
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            result.addAll(
                    mLocaleList.stream()
                            .filter(LocaleStore.LocaleInfo::isSuggested)
                            .collect(Collectors.toList()));
            return result;
        }
        Log.d(TAG, "Can not get suggested locales because the locale list is null or empty.");
        return result;
    }

    protected List<LocaleStore.LocaleInfo> getSupportedLocaleList() {
        List<LocaleStore.LocaleInfo> result = new ArrayList<>();
        if (mLocaleList != null && !mLocaleList.isEmpty()) {
            result.addAll(
                    mLocaleList.stream()
                            .filter(localeInfo -> !localeInfo.isSuggested())
                            .collect(Collectors.toList()));
            return result;
        }
        Log.d(TAG, "Can not get supported locales because the locale list is null or empty.");
        return result;
    }

    private List<LocaleStore.LocaleInfo> getSortedSuggestedRegionFromSearchList(
            CharSequence prefix,
            List<LocaleStore.LocaleInfo> searchResults,
            List<LocaleStore.LocaleInfo> supportedLocales) {
        List<LocaleStore.LocaleInfo> result = new ArrayList<>();
        if (prefix == null || prefix.toString().isEmpty()) {
            return getSortedLocaleList(supportedLocales);
        }
        for (LocaleStore.LocaleInfo localeInfo : searchResults) {
            if (supportedLocales.contains(localeInfo)) {
                result.add(localeInfo);
            }
        }
        return getSortedLocaleList(result);
    }

    protected List<LocaleStore.LocaleInfo> getSortedLocaleList(List<LocaleStore.LocaleInfo> list) {
        Collections.sort(
                list, new LocaleHelper.LocaleInfoComparator(Locale.getDefault(), mIsCountryMode));
        return list;
    }

    private LocaleCollectorBase getLocaleCollectorController(Context context) {
        return new SystemLocaleCollector(context, (LocaleList) null);
    }

    private boolean isRegionSearchSupported() {
        if (!mContext.getResources()
                .getBoolean(R.bool.config_show_setuplocalepicker_region_searchbox)) {
            return false;
        }
        String languageTag = mParentLocale.getLocale().toLanguageTag();
        if (REGION_SEARCH_SUPPORTED_LANGUAGES.contains(languageTag)) {
            return true;
        }
        Log.d(TAG, "Region search is not supported for " + languageTag);
        return false;
    }

    private boolean isFinalSelectedLocale(LocaleStore.LocaleInfo localeInfo) {
        boolean isSystemLocale = localeInfo.isSystemLocale();
        boolean isRegionLocale = localeInfo.getParent() != null;
        boolean hasNumberingSystems = localeInfo.hasNumberingSystems();
        mLocaleList = LocaleStore.getLevelLocales(mContext, new HashSet<>(), localeInfo, true);
        Log.d(
                TAG,
                "isFinalSelectedLocale: isSystemLocale = "
                        + isSystemLocale
                        + ", isRegionLocale = "
                        + isRegionLocale
                        + ", mayHaveDifferentNumberingSystem = "
                        + hasNumberingSystems
                        + ", isSuggested = "
                        + localeInfo.isSuggested()
                        + ", isNumberingMode = "
                        + mIsNumberingSystemMode);
        return mLocaleList.size() == 1
                || isSystemLocale
                || localeInfo.isSuggested()
                || (isRegionLocale && !hasNumberingSystems)
                || mIsNumberingSystemMode;
    }

    class LocalePickerAccessibilityDelegate extends RecyclerViewAccessibilityDelegate {
        private final RecyclerViewAccessibilityDelegate.ItemDelegate mItemDelegate;

        LocalePickerAccessibilityDelegate(final RecyclerView recyclerView) {
            super(recyclerView);
            mItemDelegate =
                    new RecyclerViewAccessibilityDelegate.ItemDelegate(this) {
                        @Override
                        public void onInitializeAccessibilityNodeInfo(
                                View host, AccessibilityNodeInfoCompat info) {
                            super.onInitializeAccessibilityNodeInfo(host, info);
                            RecyclerView.ViewHolder childViewHolder =
                                    recyclerView.getChildViewHolder(host);
                            if (childViewHolder instanceof ItemViewHolder) {
                                IItem item = ((ItemViewHolder) childViewHolder).getItem();
                                if (item instanceof LocaleItem) {
                                    SectionItem section = getSectionForItem(item);
                                    if (section != null) {
                                        info.setCollectionItemInfo(
                                                AccessibilityNodeInfoCompat.CollectionItemInfoCompat
                                                        .obtain(
                                                                getIndexInSection(section, item),
                                                                1,
                                                                0,
                                                                1,
                                                                false));
                                    }
                                    return;
                                }
                                info.setCollectionItemInfo(null);
                            }
                        }
                    };
        }

        @Override
        public AccessibilityDelegateCompat getItemDelegate() {
            return mItemDelegate;
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            View focusedChild = findAccessibilityFocusedChild(host);
            if (focusedChild != null) {
                RecyclerView.ViewHolder childViewHolder =
                        ((RecyclerView) host).getChildViewHolder(focusedChild);
                if (childViewHolder instanceof ItemViewHolder) {
                    SectionItem section =
                            getSectionForItem(((ItemViewHolder) childViewHolder).getItem());
                    if (section != null) {
                        info.setCollectionInfo(
                                AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(
                                        getTotalLocaleItemsInSection(section), 1, false));
                        return;
                    }
                }
            }
            info.setCollectionInfo(null);
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(
                ViewGroup host, View child, AccessibilityEvent event) {
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
                host.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
            }
            return super.onRequestSendAccessibilityEvent(host, child, event);
        }

        private View findAccessibilityFocusedChild(View view) {
            if (!(view instanceof ViewGroup)) {
                return null;
            }
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child.isAccessibilityFocused()
                        || ((child instanceof ViewGroup)
                                && findAccessibilityFocusedChild(child) != null)) {
                    return child;
                }
            }
            return null;
        }
    }

    public SectionItem getSectionForItem(IItem item) {
        if (mSuggestedListGroup != null && isChildOf(mSuggestedListGroup, item)) {
            return mSuggestedListGroup;
        }
        if (mAllListGroup != null && isChildOf(mAllListGroup, item)) {
            return mAllListGroup;
        }
        return null;
    }

    private boolean isChildOf(SectionItem sectionItem, IItem item) {
        return getIndexInSection(sectionItem, item) >= 0;
    }

    public int getIndexInSection(SectionItem sectionItem, IItem item) {
        int localeIndex = 0;
        for (int i = 0; i < sectionItem.getCount(); i++) {
            IItem child = sectionItem.getItemAt(i);
            if (child == item) {
                return localeIndex;
            }
            if (child instanceof LocaleItem) {
                localeIndex++;
            }
        }
        return -1;
    }

    public int getTotalLocaleItemsInSection(SectionItem sectionItem) {
        int count = 0;
        for (int i = 0; i < sectionItem.getCount(); i++) {
            if (sectionItem.getItemAt(i) instanceof LocaleItem) {
                count++;
            }
        }
        return count;
    }
}
