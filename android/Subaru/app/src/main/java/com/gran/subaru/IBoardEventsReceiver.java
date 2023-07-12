package com.gran.subaru;

public interface IBoardEventsReceiver {
    void onAdcRefreshed(int[] values);
    void onDeviceConfigReceived();
    void onDeviceReady();
}
