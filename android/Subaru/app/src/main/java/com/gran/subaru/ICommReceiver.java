package com.gran.subaru;

/**
 * Created by grisha on 3/5/2018.
 */

public interface ICommReceiver {
    void onReceive(byte[] buff);
    void onDeviceReady();
    void onDeviceNotFound();
}
