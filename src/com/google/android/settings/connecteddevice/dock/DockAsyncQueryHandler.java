package com.google.android.settings.connecteddevice.dock;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

/** Queries the Dreamliner dock provider off the main thread. */
public class DockAsyncQueryHandler extends AsyncQueryHandler {

    public interface OnQueryListener {
        void onQueryComplete(int token, List<DockDevice> devices);
    }

    private OnQueryListener mListener;

    public DockAsyncQueryHandler(ContentResolver contentResolver) {
        super(contentResolver);
    }

    public void setOnQueryListener(OnQueryListener listener) {
        mListener = listener;
    }

    @Override
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        super.onQueryComplete(token, cookie, cursor);
        if (mListener != null) {
            mListener.onQueryComplete(token, parseCursorToDockDevice(cursor));
        }
    }

    public static List<DockDevice> parseCursorToDockDevice(Cursor cursor) {
        List<DockDevice> devices = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                devices.add(
                        new DockDevice(
                                cursor.getString(cursor.getColumnIndex(DockContract.DOCK_ID)),
                                cursor.getString(cursor.getColumnIndex(DockContract.DOCK_NAME))));
            }
        }
        return devices;
    }
}
