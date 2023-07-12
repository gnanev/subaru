package com.gran.subaru;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.drm.DrmStore;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

class FirstRunSingleton {

    private static FirstRunSingleton instance;
    private boolean val;

    public static FirstRunSingleton getInstance() {
        if (instance == null)
            instance = new FirstRunSingleton();
        return instance;
    }

    private FirstRunSingleton() {
        val = true;
    }

    public boolean getValue() {
        return val;
    }

    public void setValue(boolean value) {
        this.val = value;
    }
}

public class MainActivity extends BaseActivity implements  IBoardEventsReceiver {
    private final static String TAG = "MainActivity";

    private final static int OVERLAY_SETTINGS_PERMISSION = 123;

    private final static int ADC_SUNLIGHT = 0;
    private final static int ADC_BATT     = 1;

    private final static int ADC_SUN_OVERSAMPLE = 20;

    private final static float V_FRACTION_BATT = 0.08f;
    private final static float V_FRACTION_SUN =  0.0195f;

    private final static String SHARED_PREFS_NAME = "Subaru_int_settings";

    private Board mBoard;
    private TextView mTextView;
    private Messenger mMessenger = null;
    private boolean mIsBound = false;
    private ImageButton mBtnDRL;
    private ImageButton mBtnDimmer;

    private EditText mEditTextAdcMin;
    private EditText mEditTextAdcMax;
    private EditText mEditTextILLMin;
    private EditText mEditTextILLMax;

    private EditText mEditTextBattMin;
    private EditText mEditTextHLon;
    private EditText mEditTextHLoff;
    private EditText mEditTextHLTime;

    private int mAdcMin;
    private int mAdcMax;
    private int mILLMin;
    private int mILLMax;

    private boolean mDimmerEnabled;
    private float mDimmerFraction;
    private float mBrightnessFraction;

    private boolean mHaveGui = false;
    private boolean mButtonsRefreshed = true;

    private boolean mFirstRun = true;

    private boolean mIsDrlOn = false;

    private boolean mIsDimmerOn = true;

    private long mSunAdcSum = 0;
    private int mSunAdcSamples = 0;

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if ((action != null) && action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                mBoard.Kill();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filterDetached = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filterDetached);

        boolean isFirstRun = FirstRunSingleton.getInstance().getValue();
        FirstRunSingleton.getInstance().setValue(false);

        Intent intent = getIntent();
        boolean isFromBoot = intent.getBooleanExtra(BootReceiver.RUN_FROM_BOOT, false);
        intent.putExtra(BootReceiver.RUN_FROM_BOOT, false);

        if (isFirstRun || isFromBoot) {
            moveTaskToBack(true);
        }
        else {
            initGUI();
        }

        Init();
    }

    void initGUI() {
        setContentView(R.layout.activity_main);

        mTextView = (TextView)findViewById(R.id.textView);

        mEditTextAdcMin = (EditText)findViewById(R.id.editTextAdcMin);
        mEditTextAdcMax = (EditText)findViewById(R.id.editTextAdcMax);
        mEditTextILLMin = (EditText)findViewById(R.id.editTextILLMin);
        mEditTextILLMax = (EditText)findViewById(R.id.editTextILLMax);

        mEditTextBattMin = (EditText)findViewById(R.id.editTextBattMin);
        mEditTextHLon = (EditText)findViewById(R.id.editTextHLon);
        mEditTextHLoff = (EditText)findViewById(R.id.editTextHLoff);
        mEditTextHLTime = (EditText)findViewById(R.id.editTextHLTime);

        mEditTextAdcMin.clearFocus();
        mEditTextAdcMax.clearFocus();
        mEditTextILLMin.clearFocus();
        mEditTextILLMax.clearFocus();

        ImageButton btnSave = (ImageButton)findViewById(R.id.btnSaveDimmer);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveIll();
            }
        });

        btnSave = (ImageButton)findViewById(R.id.btnSaveConfig);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveConfig();
            }
        });

        mBtnDRL = (ImageButton)findViewById(R.id.btnDrl);
        mBtnDRL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSetDrl();
            }
        });

        mBtnDimmer = (ImageButton)findViewById(R.id.btnDimmer);
        mBtnDimmer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onChangeDimmer();
            }
        });

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mHaveGui = true;

        loadIll();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mBoard != null)
            mBoard.Resume();

        if (!mHaveGui) {
            initGUI();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mBoard != null)
            mBoard.Pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBoard != null)
            mBoard.Kill();

        mIsBound = false;
        unbindService(mDimmerConnection);
    }

    void Init() {
        if(Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_SETTINGS_PERMISSION);
            }
            else {
                InitDimmer();
            }
        }
        else
        {
            InitDimmer();
        }

        InitBoard();
    }

    void InitBoard() {
        mBoard = new Board();
        mBoard.Init(getApplicationContext(), this);
        mBoard.Resume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case OVERLAY_SETTINGS_PERMISSION:
                if (Settings.canDrawOverlays(this))
                    InitDimmer();
                break;
        }
    }

    private ServiceConnection mDimmerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mIsBound = true;
            mMessenger = new Messenger(service);
//            mMessenger = new Messenger(service);
//            try {
////                Message msg = Message.obtain(null, MyService.MSG_REGISTER_CLIENT);
////                msg.replyTo = null;
////                mMessenger.send(msg);
//            }
//            catch (RemoteException e) {
//                // In this case the service has crashed before we could even do anything with it
//            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mMessenger = null;
        }
    };

    void InitDimmer() {
        Intent serviceIntent = new Intent(getApplicationContext(), DimmerService.class);
        startService(serviceIntent);
        bindService(new Intent(this, DimmerService.class), mDimmerConnection, Context.BIND_AUTO_CREATE);
    }

    private void sendMessageToService(int message, int val) {
        if (mIsBound) {
            if (mMessenger != null) {
                try {
                    Message msg = Message.obtain(null, message, val, 0);
                    msg.replyTo = null;
                    mMessenger.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }

    @Override
    public void onAdcRefreshed(final int[] values) {
        if (!mHaveGui)
            return;

        mTextView.post(new Runnable() {
            @Override
            public void run() {
                String s = "";
                String s1 = "";

                for(int i=0; i<values.length; i++) {
                    s += "ADC_" + i + " __ " + values[i] + "\n";
                }

                s += "\n";

                s1 = String.format("%.2f", (float)values[0] * V_FRACTION_SUN);
                s += "V_1" + " _____ " + s1 + "\n";
                s1 = String.format("%.2f", (float)values[1] * V_FRACTION_BATT);
                s += "V_1" + " _____ " + s1 + "\n";

                mTextView.setText(s);

                setIll(values[ADC_SUNLIGHT]+1); // evade zero
            }
        });
    }

    @Override
    public void onDeviceConfigReceived() {
        Board.DeviceConfig deviceConfig;
        deviceConfig = mBoard.getDeviceConfig();

        if (!mHaveGui)
            return;

        mEditTextBattMin.setText(Integer.toString(deviceConfig.minVoltage));
        mEditTextHLon.setText(Integer.toString(deviceConfig.lightsOnThreshold));
        mEditTextHLoff.setText(Integer.toString(deviceConfig.lightsOffThreshold));
        mEditTextHLTime.setText(Integer.toString(deviceConfig.lightsTime));

        refreshButtons();
    }

    @Override
    public void onDeviceReady() {
        mBoard.getConfig();
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }

    void refreshButtons() {
        if (!mHaveGui)
            return;

        mBtnDRL.setBackgroundResource(mIsDrlOn ? R.drawable.selector_btn_drl_on : R.drawable.selector_btn_drl);
        mBtnDimmer.setBackgroundResource(mIsDimmerOn ? R.drawable.selector_btn_dimmer : R.drawable.selector_btn_dimmer_off);
    }

    void onSetDrl() {
        if (!mHaveGui)
            return;

        mIsDrlOn = !mIsDrlOn;
        mBoard.setDrl(mIsDrlOn);
        refreshButtons();
    }

    void onChangeDimmer() {
        mIsDimmerOn = !mIsDimmerOn;
        SharedPreferences.Editor e = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE).edit();
        e.putBoolean("mIsDimmerOn", mIsDimmerOn);
        e.apply();
        e.commit();
        refreshButtons();
    }

    void setIll(int val) {
        if (!mDimmerEnabled)
            return;

        int dimm = 255;

        if (mIsDimmerOn) {
            if (val <= mAdcMin) {
                val = mAdcMin;
            } else if (val >= mAdcMax) {
                val = mAdcMax;
            }

            val = val - mAdcMin;

            mSunAdcSum += val;

            if (++mSunAdcSamples == ADC_SUN_OVERSAMPLE) {
                val = (int) (mSunAdcSum / mSunAdcSamples);
                mSunAdcSamples = 0;
                mSunAdcSum = 0;
            } else {
                return;
            }

            dimm = (int) ((float) val * mDimmerFraction);
            dimm += mILLMin;
        }

        sendMessageToService(DimmerService.MSG_SET_ALPHA, 255-dimm);

//        int brightness = (int)(val * (float)mBrightnessFraction);
//        setBrightness(brightness);
    }

    void loadIll(){
        SharedPreferences sp = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
        mAdcMin = sp.getInt("mAdcMin", 0);
        mAdcMax = sp.getInt("mAdcMax", 0);
        mILLMin = sp.getInt("mILLMin", 0);
        mILLMax = sp.getInt("mILLMax", 0);

        mIsDimmerOn = sp.getBoolean("mIsDimmerOn", true);

        calcFractions();

        if (!mHaveGui)
            return;

        mEditTextAdcMin.setText(Integer.toString(mAdcMin));
        mEditTextAdcMax.setText(Integer.toString(mAdcMax));
        mEditTextILLMin.setText(Integer.toString(mILLMin));
        mEditTextILLMax.setText(Integer.toString(mILLMax));
    }

    void saveIll(){
        mAdcMin = str2int(mEditTextAdcMin.getText().toString());
        mAdcMax = str2int(mEditTextAdcMax.getText().toString());
        mILLMin = str2int(mEditTextILLMin.getText().toString());
        mILLMax = str2int(mEditTextILLMax.getText().toString());

        SharedPreferences.Editor e = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE).edit();
        e.putInt("mAdcMin", mAdcMin);
        e.putInt("mAdcMax", mAdcMax);
        e.putInt("mILLMin", mILLMin);
        e.putInt("mILLMax", mILLMax);
        e.apply();
        e.commit();

        calcFractions();
    }

    void saveConfig() {
        int on = str2int(mEditTextHLon.getText().toString());
        int off = str2int(mEditTextHLoff.getText().toString());

        if (off <= on) {
            Toast.makeText(this, "HL Off < HL On", Toast.LENGTH_SHORT).show();
            return;
        }

        mBoard.getDeviceConfig().lightsOnThreshold = on;
        mBoard.getDeviceConfig().lightsOffThreshold = off;
        mBoard.getDeviceConfig().minVoltage = str2int(mEditTextBattMin.getText().toString());
        mBoard.getDeviceConfig().lightsTime = str2int(mEditTextHLTime.getText().toString());

        mBoard.setConfig();

        final MainActivity This = this;

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBoard.getConfig();
            }
        }, 1000);
    }

    void calcFractions() {
        if ((mAdcMax == 0) || (mILLMax == 0) ||
            (mAdcMax <= mAdcMin) || (mILLMax <= mILLMin)){
            mDimmerEnabled = false;
            return;
        }

        int diffAdc = mAdcMax - mAdcMin;
        int diffIll = mILLMax - mILLMin;

        mBrightnessFraction = 255.0f / (float)diffAdc;

        mDimmerFraction = (float)diffIll / (float)diffAdc;
        mDimmerEnabled = true;
    }

    private void setBrightness(int b) {
        ContentResolver cr = getContentResolver();

        try
        {
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, b);
        }
        catch (Exception e)
        {
            //Throw an error case it couldn't be retrieved
            Log.e("Error", "Cannot access system brightness");
            e.printStackTrace();
        }
    }

    void goHome() {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
    }

    int str2int(String s) {
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            return 0;
        }
    }
}
