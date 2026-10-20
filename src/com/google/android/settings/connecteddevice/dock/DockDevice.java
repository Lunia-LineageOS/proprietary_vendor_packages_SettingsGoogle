package com.google.android.settings.connecteddevice.dock;

/** A dock reported by the Dreamliner dock provider. */
public class DockDevice {
    private final String mId;
    private final String mName;

    DockDevice(String id, String name) {
        mId = id;
        mName = name;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }
}
