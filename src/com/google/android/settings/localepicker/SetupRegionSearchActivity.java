package com.google.android.settings.localepicker;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import com.android.settings.overlay.FeatureFactory;

import com.google.android.settings.R;
import com.google.android.setupcompat.partnerconfig.PartnerConfig;
import com.google.android.setupcompat.partnerconfig.PartnerConfigHelper;
import com.google.android.setupdesign.GlifRecyclerLayout;
import com.google.android.setupdesign.items.RecyclerItemAdapter;
import com.google.android.setupdesign.template.FloatingBackButtonMixin;
import com.google.android.setupdesign.template.HeaderMixin;
import com.google.android.setupdesign.util.ThemeHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetupRegionSearchActivity extends Activity implements TextWatcher {
    private static final String TAG = "SetupRegionSearchActivity";
    private static final int REQUEST_LOCALE_PICKER = 0;

    private RecyclerItemAdapter mAdapter;
    private AppCompatImageView mBackIcon;
    private AppCompatImageView mClearSearchQueryButton;
    private boolean mIsNumberingMode;
    private LocaleStore.LocaleInfo mLocaleInfo;
    private List<LocaleStore.LocaleInfo> mLocaleOptions;
    private List<LocaleStore.LocaleInfo> mOriginalLocaleInfos;
    private CharSequence mPrefix;
    private RecyclerView mRecyclerView;
    private AppCompatEditText mSearchActionBarText;
    private SetupLocalePickerListController mSetupLocalePickerListController;
    private SearchFilter mSearchFilter = null;
    private ViewTreeObserver.OnGlobalFocusChangeListener mFocusChangeListener = null;

    @Override
    public void afterTextChanged(Editable s) {}

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    protected void onCreate(Bundle bundle) {
        applySuwTheme();
        super.onCreate(bundle);
        setupLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRecyclerView != null) {
            mRecyclerView
                    .getViewTreeObserver()
                    .addOnGlobalFocusChangeListener(mFocusChangeListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRecyclerView != null) {
            mRecyclerView
                    .getViewTreeObserver()
                    .removeOnGlobalFocusChangeListener(mFocusChangeListener);
        }
    }

    private void applySuwTheme() {
        setTheme(R.style.Theme_Settings_LocalePicker);
        if (ThemeHelper.shouldApplyGlifExpressiveStyle(getApplicationContext())) {
            if (ThemeHelper.trySetSuwTheme(this)) {
                return;
            }
            setTheme(ThemeHelper.getSuwDefaultTheme(getApplicationContext()));
            ThemeHelper.trySetDynamicColor(this);
            return;
        }
        ThemeHelper.applyTheme(this);
        ThemeHelper.trySetDynamicColor(this);
    }

    private void setupLayout() {
        setContentView(R.layout.setup_search_region);
        setupContent();
    }

    private void setupContent() {
        Bundle extras = getIntent().getExtras();
        mLocaleInfo = (LocaleStore.LocaleInfo) extras.getSerializable("extra_target_locale");
        mIsNumberingMode = extras.getBoolean("extra_is_numbering_system");
        boolean isSuwUseModalDialogEnabled =
                PartnerConfigHelper.isSuwUseModalDialogEnabled(getApplicationContext());
        GlifRecyclerLayout layout = findViewById(R.id.setup_wizard_region_search_layout);
        layout.getMixin(HeaderMixin.class).getTextView().setVisibility(View.GONE);
        if (layout.getMixin(FloatingBackButtonMixin.class) != null) {
            layout.getMixin(FloatingBackButtonMixin.class).setVisibility(View.GONE);
        }
        LinearLayout header =
                layout.findManagedViewById(com.google.android.setupdesign.R.id.sud_layout_header);
        if (header != null) {
            header.setVisibility(View.GONE);
        }
        ViewGroup statusLayout =
                findViewById(com.google.android.setupdesign.R.id.suc_layout_status);
        if (isSuwUseModalDialogEnabled) {
            statusLayout =
                    findViewById(com.google.android.setupdesign.R.id.suc_intrinsic_size_layout);
        }
        if (statusLayout != null) {
            statusLayout.setClipChildren(true);
            View searchBarLayout = getLayoutInflater().inflate(R.layout.search_bar_layout, null);
            int childCount = statusLayout.getChildCount();
            if (statusLayout instanceof FrameLayout) {
                if (!ThemeHelper.shouldApplyGlifExpressiveStyle(getApplicationContext())) {
                    View contentArea =
                            layout.findManagedViewById(
                                    com.google.android.setupdesign.R.id.sud_landscape_content_area);
                    if (contentArea != null) {
                        contentArea.setPadding(0, 0, 0, 0);
                    }
                }
                View firstChild = statusLayout.getChildAt(0);
                FrameLayout.LayoutParams layoutParams =
                        (FrameLayout.LayoutParams) firstChild.getLayoutParams();
                View searchBar = searchBarLayout.findViewById(R.id.suw_locale_picker_search_bar);
                searchBar.post(
                        () -> {
                            layoutParams.topMargin =
                                    searchBar.getBottom() + (searchBar.getHeight() / 4);
                            firstChild.setLayoutParams(layoutParams);
                        });
                if (isSuwUseModalDialogEnabled) {
                    searchBarLayout.setPadding(
                            searchBarLayout.getPaddingLeft(),
                            (int)
                                    PartnerConfigHelper.get(getApplicationContext())
                                            .getDimension(
                                                    getApplicationContext(),
                                                    PartnerConfig.CONFIG_ICON_MARGIN_TOP),
                            searchBarLayout.getPaddingRight(),
                            searchBarLayout.getPaddingBottom());
                    View contentArea =
                            layout.findManagedViewById(
                                    com.google.android.setupdesign.R.id.sud_landscape_content_area);
                    if (contentArea != null) {
                        contentArea.setPadding(
                                contentArea.getPaddingLeft(),
                                searchBarLayout.getHeight(),
                                contentArea.getPaddingRight(),
                                contentArea.getPaddingBottom());
                    }
                }
                statusLayout.addView(searchBarLayout, childCount);
            } else {
                View[] children = new View[childCount + 1];
                children[0] = searchBarLayout;
                for (int i = 0; i < childCount; i++) {
                    children[i + 1] = statusLayout.getChildAt(i);
                }
                statusLayout.removeAllViews();
                View contentArea =
                        children[2].findViewById(
                                com.google.android.setupdesign.R.id.sud_landscape_content_area);
                if (contentArea != null) {
                    contentArea.setPadding(
                            contentArea.getPaddingStart(),
                            0,
                            contentArea.getPaddingEnd(),
                            contentArea.getPaddingBottom());
                }
                LinearLayout.LayoutParams layoutParams =
                        (LinearLayout.LayoutParams) children[2].getLayoutParams();
                View searchBar = searchBarLayout.findViewById(R.id.suw_locale_picker_search_bar);
                searchBar.post(
                        () -> {
                            layoutParams.bottomMargin = (searchBar.getHeight() * 3) / 2;
                            children[2].setLayoutParams(layoutParams);
                        });
                for (int i = 0; i < children.length; i++) {
                    statusLayout.addView(children[i], i);
                }
                ScrollView scrollView =
                        findViewById(com.google.android.setupdesign.R.id.sud_header_scroll_view);
                if (scrollView != null) {
                    scrollView.setContentDescription(
                            getApplicationContext().getString(R.string.search_region_page));
                }
            }
        }
        mBackIcon = findViewById(R.id.back_icon);
        if (mBackIcon != null) {
            mBackIcon.setOnClickListener(
                    view -> {
                        FeatureFactory.getFeatureFactory()
                                .getMetricsFeatureProvider()
                                .action(
                                        getApplicationContext(),
                                        SettingsEnums
                                                .ACTION_NO_PREFERRED_REGION_AFTER_SEARCH_REGION_IN_SUW);
                        finish();
                    });
        }
        mSearchActionBarText = findViewById(R.id.search_action_bar_text);
        if (mSearchActionBarText != null) {
            mSearchActionBarText.addTextChangedListener(this);
            mSearchActionBarText.requestFocus();
        }
        mClearSearchQueryButton = findViewById(R.id.clear_search_query);
        if (mClearSearchQueryButton != null) {
            mClearSearchQueryButton.setOnClickListener(view -> clearLanguageSearchText());
        }
        mRecyclerView = layout.getRecyclerView();
        mAdapter = (RecyclerItemAdapter) layout.getAdapter();
        LocaleSelectedListener localeSelectedListener =
                localeInfo -> {
                    Intent intent = new Intent();
                    intent.putExtra("localeInfo", localeInfo);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                };
        mFocusChangeListener =
                (oldFocus, newFocus) -> {
                    if (mRecyclerView == null
                            || mAdapter == null
                            || newFocus == null
                            || newFocus.getParent() != mRecyclerView) {
                        return;
                    }
                    if (mRecyclerView.getLayoutManager()
                            instanceof LinearLayoutManager linearLayoutManager) {
                        int position = mRecyclerView.getChildAdapterPosition(newFocus);
                        int itemCount = mAdapter.getItemCount();
                        if (position == RecyclerView.NO_POSITION || position >= itemCount) {
                            return;
                        }
                        int lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition();
                        int nextPosition = lastVisiblePosition + 1;
                        if (position != lastVisiblePosition || nextPosition >= itemCount) {
                            return;
                        }
                        mRecyclerView.smoothScrollToPosition(nextPosition);
                    }
                };
        mSetupLocalePickerListController =
                new SetupLocalePickerListController(
                        getApplicationContext(), this, mLocaleInfo, mIsNumberingMode);
        mSetupLocalePickerListController.setLocaleSelectedListener(localeSelectedListener);
        mSetupLocalePickerListController.displayScreen(mAdapter);
        if (mSetupLocalePickerListController != null) {
            mOriginalLocaleInfos = mSetupLocalePickerListController.getSupportedLocaleList();
            mOriginalLocaleInfos.addAll(mSetupLocalePickerListController.getSuggestedLocaleList());
        }
    }

    private void filterSearch(String query) {
        if (mSearchFilter == null) {
            mSearchFilter = new SearchFilter();
        }
        if (mOriginalLocaleInfos == null) {
            Log.w(TAG, "Locales haven't loaded completely yet, so nothing can be filtered");
        } else {
            mSearchFilter.filter(query);
        }
    }

    class SearchFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
            mPrefix = prefix;
            if (TextUtils.isEmpty(prefix)) {
                results.values = mOriginalLocaleInfos;
                results.count = mOriginalLocaleInfos.size();
                return results;
            }
            List<LocaleStore.LocaleInfo> newList = new ArrayList<>(mOriginalLocaleInfos);
            Locale locale = Locale.getDefault();
            String prefixString = LocaleHelper.normalizeForSearch(prefix.toString(), locale);
            final ArrayList<LocaleStore.LocaleInfo> newValues = new ArrayList<>();
            final int count = newList.size();
            for (int i = 0; i < count; i++) {
                final LocaleStore.LocaleInfo value = newList.get(i);
                final String nameToCheck =
                        LocaleHelper.normalizeForSearch(value.getFullNameInUiLanguage(), locale);
                final String nativeNameToCheck =
                        LocaleHelper.normalizeForSearch(value.getFullNameNative(), locale);
                if (wordMatches(nativeNameToCheck, prefixString)
                        || wordMatches(nameToCheck, prefixString)) {
                    if (!newValues.contains(value)) {
                        newValues.add(value);
                    }
                }
            }
            results.values = newValues;
            results.count = newValues.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (mSetupLocalePickerListController == null) {
                Log.d(TAG, "publishResults(), can not get item.");
                return;
            }
            mLocaleOptions = (List<LocaleStore.LocaleInfo>) results.values;
            mSetupLocalePickerListController.onSearchListChanged(mLocaleOptions, mPrefix);
        }

        private boolean wordMatches(String valueText, String prefixString) {
            if (valueText == null) {
                return false;
            }
            if (valueText.startsWith(prefixString)) {
                return true;
            }
            Matcher matcher = Pattern.compile("^.*?\\((.*)").matcher(valueText);
            if (matcher.find()) {
                return matcher.group(1).startsWith(prefixString);
            }
            return false;
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mClearSearchQueryButton != null) {
            mClearSearchQueryButton.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
        }
        filterSearch(s.toString());
    }

    private void clearLanguageSearchText() {
        Editable text;
        if (mSearchActionBarText != null && (text = mSearchActionBarText.getText()) != null) {
            text.clear();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOCALE_PICKER && resultCode == RESULT_OK) {
            if (data != null) {
                LocaleStore.LocaleInfo localeInfo =
                        (LocaleStore.LocaleInfo) data.getSerializableExtra("localeInfo");
                Intent intent = new Intent();
                intent.putExtra("localeInfo", localeInfo);
                setResult(Activity.RESULT_OK, intent);
            }
            finish();
        }
    }
}
