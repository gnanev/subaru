package com.gran.subaru;

public interface IBoardEventsReceiver {
    void onAdcRefreshed(int[] values);
    void onLockStatusRefreshed(boolean isLocked);
    void onDeviceConfigReceived();
    void onDeviceReady();
}
