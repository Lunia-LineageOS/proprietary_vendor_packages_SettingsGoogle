package com.google.android.settings.localepicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewTreeObserver;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.app.LocaleStore;

import com.google.android.settings.R;
import com.google.android.setupdesign.GlifRecyclerLayout;
import com.google.android.setupdesign.items.RecyclerItemAdapter;
import com.google.android.setupdesign.util.ThemeHelper;

public class SetupRegionAndNumberingSystemActivity extends Activity {
    private static final int REQUEST_LOCALE_PICKER = 0;

    private RecyclerItemAdapter mAdapter;
    private ViewTreeObserver.OnGlobalFocusChangeListener mFocusChangeListener = null;
    private boolean mIsNumberingMode;
    private LocaleStore.LocaleInfo mLocaleInfo;
    private RecyclerView mRecyclerView;

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
        setContentView(R.layout.setup_region_and_numbering_system_picker);
        setupContent();
    }

    private void setupContent() {
        String headerText;
        Bundle extras = getIntent().getExtras();
        mLocaleInfo = (LocaleStore.LocaleInfo) extras.getSerializable("extra_target_locale");
        mIsNumberingMode = extras.getBoolean("extra_is_numbering_system");
        GlifRecyclerLayout layout =
                findViewById(R.id.setup_wizard_region_and_numbering_system_picker_layout);
        if (mIsNumberingMode) {
            headerText = mLocaleInfo.getFullNameNative();
        } else {
            headerText =
                    getApplicationContext()
                            .getString(com.android.settings.R.string.region_picker_title);
        }
        String descriptionText =
                getApplicationContext()
                        .getString(com.android.settings.R.string.region_picker_sub_title);
        if (mIsNumberingMode) {
            descriptionText = "";
        }
        layout.setHeaderText(headerText);
        layout.setDescriptionText(descriptionText);
        mRecyclerView = layout.getRecyclerView();
        mAdapter = (RecyclerItemAdapter) layout.getAdapter();
        LocaleSelectedListener listener =
                localeInfo -> {
                    Intent intent = new Intent();
                    intent.putExtra("localeInfo", localeInfo);
                    setResult(RESULT_OK, intent);
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
        SetupLocalePickerListController controller =
                new SetupLocalePickerListController(
                        getApplicationContext(), this, mLocaleInfo, mIsNumberingMode);
        controller.setLocaleSelectedListener(listener);
        controller.displayScreen(mAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOCALE_PICKER && resultCode == RESULT_OK) {
            if (data != null) {
                LocaleStore.LocaleInfo localeInfo =
                        (LocaleStore.LocaleInfo) data.getSerializableExtra("localeInfo");
                Intent intent = new Intent();
                intent.putExtra("localeInfo", localeInfo);
                setResult(RESULT_OK, intent);
            }
            finish();
        }
    }
}
