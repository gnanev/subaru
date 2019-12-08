package com.gran.subaru;

import android.content.Context;

/**
 * Created by grisha on 4/1/2018.
 */

public interface IComm {
    void Init(Context context);
    void Connect();
    void setReceiver(ICommReceiver receiver);
    void sendData(byte[] data);
    boolean canSendData();
}