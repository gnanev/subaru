package com.gran.subaru;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

public class Board implements IDataReceiver {
    private static String TAG = "SubaruBoard";

    public static final int MAX_COMMANDS = 2000;

    public static final int CMD_GET_ADC_DATA = 11;
    public static final int CMD_ADC_DATA = 12;
    public static final int CMD_GET_CONFIG = 13;
    public static final int CMD_CONFIG = 14;
    public static final int CMD_SET_CONFIG = 15;
    public static final int CMD_GET_LOCK = 16;
    public static final int CMD_LOCK = 17;
    public static final int CMD_SET_LOCK = 18;
    public static final int CMD_SET_DRL = 19;

    private Protocol mProtocol;
    private UsbComm mComm;
    private Context mContext;
    private static int empty[] = new int[0];
    private boolean mIsRunning;
    private boolean mDeviceValidated;
    private int mCurrentCommand = 0;
    private BlockingQueue mCommandsQueue = new ArrayBlockingQueue(MAX_COMMANDS);

    private IBoardEventsReceiver mEventsReceiver;

    public interface ICommandHandler {
        public void process(Protocol.DataFrame df);
    }

    HashMap<Integer, ICommandHandler> mCommandProcessors = new HashMap<Integer, ICommandHandler>();

    private Runnable cmdGetConfig = new Runnable() {
        @Override
        public void run() {
            mProtocol.sendData(Protocol.ADDR_CTRL, CMD_GET_CONFIG, empty);
        }
    };

    private Runnable cmdGetAdcData = new Runnable() {
        @Override
        public void run() {
            mProtocol.sendData(Protocol.ADDR_CTRL, CMD_GET_ADC_DATA, empty);
        }
    };

    private Runnable cmdGetLock = new Runnable() {
        @Override
        public void run() {
            mProtocol.sendData(Protocol.ADDR_CTRL, CMD_GET_LOCK, empty);
        }
    };

    ArrayList<Runnable> mCommands;

    public class DeviceConfig {
        public static final int SIZE = 5;
        public int protectionEnabled;
        public int minVoltage;
        public int lightsOnThreshold;
        public int lightsOffThreshold;
        public int cutOffThreshold;
    }

    private DeviceConfig mDeviceConfig = new DeviceConfig();

    public void Init(Context context, IBoardEventsReceiver eventsReceiver) {
        mContext = context;

        mEventsReceiver = eventsReceiver;

        mComm = new UsbComm();
        mComm.Init(context);

        mProtocol = new Protocol(mComm, this);

        mCommands = new ArrayList<Runnable>(2);
        mCommands.add(cmdGetAdcData);
        mCommands.add(cmdGetLock);

        initCommandProcessors();

        initThreads();

        mComm.Connect();
    }

    public void Kill() {
        mIsRunning = false;
    }

    private void initCommandProcessors() {
        mCommandProcessors.put(CMD_ADC_DATA, new ICommandHandler() {
            @Override
            public void process(Protocol.DataFrame df) {
                processAdcData(df);
            }
        });
        mCommandProcessors.put(CMD_CONFIG, new ICommandHandler() {
            @Override
            public void process(Protocol.DataFrame df) {
                processConfig(df);
            }
        });
        mCommandProcessors.put(CMD_LOCK, new ICommandHandler() {
            @Override
            public void process(Protocol.DataFrame df) {
                processLock(df);
            }
        });
    }

    private void initThreads() {
        mIsRunning = true;
        mDeviceValidated = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(mIsRunning) {
                    monitoringProc();
                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(mIsRunning) {
                    commProc();
                }
            }
        }).start();
    }

    @Override
    public void onDeviceReady() {
        mDeviceValidated = false;

        getConfig(); // Validate device

        new Thread(new Runnable() {
            @Override
            public void run() {
            try {
                Thread.sleep(2000);

                // if device is not validated for 2 sec
                // search for another one
                if (!mDeviceValidated) {
                    String deviceToIgnore = mComm.getConnectedDevice();
                    mComm.AddInvalidDevice(deviceToIgnore);
                    mComm.Reconnect();
                }
                else if (mEventsReceiver != null){
                    // device is validated, inform host
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            mEventsReceiver.onDeviceReady();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            }
        }).start();
    }

    @Override
    public void onNewData() {
        Protocol.DataFrame df = mProtocol.nextFrame();
        processData(df);
    }

    private void processData(Protocol.DataFrame df) {
        if (df == null)
            return;

        ICommandHandler h = mCommandProcessors.get(df.cmd);
        if (h != null)
            h.process(df);
    }

    public void commProc() {
        if (!mIsRunning)
            return;

        try {
            Runnable r = (Runnable)mCommandsQueue.take();
            r.run();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void monitoringProc() {
        if (!mIsRunning)
            return;

        Runnable r = mCommands.get(mCurrentCommand++);
        r.run();

        if (mCurrentCommand == mCommands.size())
            mCurrentCommand = 0;
    }

    private void sendCmd(Runnable r) {
        mCommandsQueue.add(r);
    }

    public void getConfig() {
        sendCmd(cmdGetConfig);
    }

    public void setConfig() {
        sendCmd(new Runnable() {
            @Override
            public void run() {
                int a[] = new int[DeviceConfig.SIZE];
                a[0] = mDeviceConfig.protectionEnabled;
                a[1] = mDeviceConfig.minVoltage;
                a[2] = mDeviceConfig.lightsOnThreshold;
                a[3] = mDeviceConfig.lightsOffThreshold;
                a[4] = mDeviceConfig.cutOffThreshold;
                mProtocol.sendData(Protocol.ADDR_CTRL, CMD_SET_CONFIG, a);
            }
        });
    }

    public void setLock(final boolean isLocked) {
        sendCmd(new Runnable() {
            @Override
            public void run() {
                int a[] = new int[1];
                a[0] = isLocked ? 1 : 0;
                mProtocol.sendData(Protocol.ADDR_CTRL, CMD_SET_LOCK, a);
            }
        });
    }

    public void setDrl(final boolean drlOn) {
        sendCmd(new Runnable() {
            @Override
            public void run() {
                int a[] = new int[1];
                a[0] = drlOn ? 1 : 0;
                mProtocol.sendData(Protocol.ADDR_CTRL, CMD_SET_DRL, a);
            }
        });
    }

    private void processConfig(Protocol.DataFrame df) {
        if (df.data.length != DeviceConfig.SIZE)
            return;

        mDeviceConfig.protectionEnabled = df.data[0];
        mDeviceConfig.minVoltage = df.data[1];
        mDeviceConfig.lightsOnThreshold = df.data[2];
        mDeviceConfig.lightsOffThreshold = df.data[3];
        mDeviceConfig.cutOffThreshold = df.data[4];

        mDeviceValidated = true;

        if (mEventsReceiver == null)
            return;

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mEventsReceiver.onDeviceConfigReceived();
            }
        });
    }

    private void processAdcData(final Protocol.DataFrame df) {
        if (mEventsReceiver == null)
            return;

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mEventsReceiver.onAdcRefreshed(df.data);
            }
        });
    }


    private void processLock(final Protocol.DataFrame df) {
        if ((mEventsReceiver == null) || (df.data.length != 1))
            return;

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mEventsReceiver.onLockStatusRefreshed(df.data[0]==1);
            }
        });
    }

    public DeviceConfig getDeviceConfig() {
        return mDeviceConfig;
    }

    public void Pause() {
    }

    public void Resume() {
    }
}
