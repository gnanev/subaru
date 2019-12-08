package com.gran.subaru;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class UsbComm implements IComm{
    private static final String TAG = "UsbComm";

    private static final String ACTION_USB_PERMISSION = "com.gran.subaru.USB_PERMISSION";
    private static final String META_DEVICE_NAME = "UsbDeviceName";
    private static final int BAUD_RATE = 19200;

    private ICommReceiver mReceiver;
    private Context mContext;
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbSerialDevice mSerialPort;
    private UsbDeviceConnection mConnection;
    private HashSet<String> mInvalidDevices = new HashSet<String>();
    private String mDeviceToConnect = "";
    private boolean mSerialPortConnected = false;
    private boolean mReadThreadRunning = false;

    @Override
    public void Init(Context context) {
        mContext = context;

        setFilter();
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        try {
            ApplicationInfo ai = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
            mDeviceToConnect = (String)ai.metaData.get(META_DEVICE_NAME);
        }
        catch (Exception e) {
            mDeviceToConnect = "";
        }
    }

    @Override
    public void setReceiver(ICommReceiver receiver)
    {
        mReceiver = receiver;
    }

    @Override
    public void Connect() {
        findSerialPortDevice();
    }

    public void Disconnect() {
        if (mSerialPortConnected) {
            mSerialPortConnected = false;
            try {
                while (mReadThreadRunning)
                    Thread.sleep(5);
            }
            catch (Exception e) {
            }

            mSerialPort.syncClose();
            mSerialPort = null;
        }

        if (mConnection != null) {
            mConnection.close();
            mConnection = null;
        }

        mUsbDevice = null;
    }

    public void Reconnect() {
        Disconnect();
        Connect();
    }

    @Override
    public boolean canSendData() {
        return mSerialPort != null;
    }

    private void findSerialPortDevice() {
        // This snippet will try to open the first encountered usb mDevice connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = mUsbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean found = false;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                mUsbDevice = entry.getValue();

                String productName = mUsbDevice.getProductName();

                if (!mDeviceToConnect.isEmpty() && !productName.equals(mDeviceToConnect))
                    continue;

                if (mInvalidDevices.contains(mUsbDevice.getDeviceName()))
                    continue;

                int mDeviceVID = mUsbDevice.getVendorId();
                int mDevicePID = mUsbDevice.getProductId();

                if (mDeviceVID != 0x1d6b && (mDevicePID != 0x0001 && mDevicePID != 0x0002 && mDevicePID != 0x0003)) {
                    requestUserPermission();
                    found = true;
                } else {
                    mConnection = null;
                    mUsbDevice = null;
                }

                if (found)
                    break;
            }
            if (!found) {
                mReceiver.onDeviceNotFound();
            }
        } else {
            mReceiver.onDeviceNotFound();
        }
    }

    public void setupConnection() {
        if (mUsbDevice == null) {
            showToast("USB Device not set");
            return;
        }
        mConnection = mUsbManager.openDevice(mUsbDevice);
        mSerialPort = UsbSerialDevice.createUsbSerialDevice(mUsbDevice, mConnection);
        if (mSerialPort != null) {
            if (mSerialPort.syncOpen()) {
                mSerialPort.setBaudRate(BAUD_RATE);
                mSerialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                mSerialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                mSerialPort.setParity(UsbSerialInterface.PARITY_NONE);
                /**
                 * Current flow control Options:
                 * UsbSerialInterface.FLOW_CONTROL_OFF
                 * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                 * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                 */
                mSerialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

                mSerialPortConnected = true;
                new ReadThread().start();
                mReceiver.onDeviceReady();
            }
        } else {
            // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
            // Send an Intent to Main Activity
            boolean b = mSerialPort instanceof CDCSerialDevice;
            showToast("mSerialPort instanceof CDCSerialDevice = " + b);
        }
    }

    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        mUsbManager.requestPermission(mUsbDevice, mPendingIntent);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case ACTION_USB_PERMISSION: // USB PERMISSION GRANTED
//                    String msg = "USB Ready";
//                    if (device != null) {
//                        msg = msg + "\n DeviceName: " + device.getDeviceName() +
//                                "\n ManufacturerName: " + device.getManufacturerName() +
//                                "\n ProductName: " + device.getProductName() +
//                                "\n SerialNumber: " + device.getSerialNumber() +
//                                "\n getDeviceId: " + device.getDeviceId();
//                    }
//                    showToast(msg);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(mUsbDevice != null){
                            setupConnection();
                        }
                    }
                    else {
                        String msg = "permission denied for device " + mUsbDevice;
                        Log.d(TAG, msg);
                        showToast(msg);
                    }


                    break;
            }
        }
    };

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
//        filter.addAction(ACTION_USB_DETACHED);
//        filter.addAction(ACTION_USB_ATTACHED);
        mContext.registerReceiver(mUsbReceiver, filter);
    }

    public void AddInvalidDevice(String deviceName) {
        mInvalidDevices.add(deviceName);
    }

    public String getConnectedDevice() {
        if (mUsbDevice == null)
            return "";

        return mUsbDevice.getDeviceName();
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            mReadThreadRunning = true;
            byte[] buffer = new byte[30];

            try {
                while (mSerialPortConnected) {
                    //if (mHandler != null) {
                        int n = mSerialPort.syncRead(buffer, 5);
                        if (!mSerialPortConnected)
                            break;
                        if (n > 0) {
                            byte[] received = new byte[n];
                            System.arraycopy(buffer, 0, received, 0, n);
                            mReceiver.onReceive(buffer);
                            //mHandler.obtainMessage(SYNC_READ, received).sendToTarget();
                        }
                    //}
                }
            }
            catch (Exception e) {
            }

            mReadThreadRunning = false;
        }
    }

    @Override
    public void sendData(byte[] data) {
        if (mSerialPort != null)
            mSerialPort.syncWrite(data, 0);
    }

    private void showToast(final String msg) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
