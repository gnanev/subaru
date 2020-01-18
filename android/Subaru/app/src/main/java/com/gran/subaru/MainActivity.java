package com.gran.subaru;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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


public class MainActivity extends BaseActivity implements  IBoardEventsReceiver {
    private final static String TAG = "MainActivity";

    private final static int OVERLAY_SETTINGS_PERMISSION = 123;
    private final static int ACCESS_CODE_CHANGED = 124;
    private final static int SUBARU_UNLOCKED = 125;

    private final static int ADC_SUNLIGHT = 0;
    private final static int ADC_THROTTLE = 1;
    private final static int ADC_BATT     = 2;

    private final static int ADC_SUN_OVERSAMPLE = 10;

    private final static float V_FRACTION = 0.082f;

    private final static String SHARED_PREFS_NAME = "Subaru_int_settings";

    private Board mBoard;
    private TextView mTextView;
    private Messenger mMessenger = null;
    private boolean mIsBound = false;
    private ImageButton mBtnLock;
    private ImageButton mBtnChangePass;
    private ImageButton mBtnRemoveProtection;
    private ImageButton mBtnDRL;

    private EditText mEditTextAdcMin;
    private EditText mEditTextAdcMax;
    private EditText mEditTextILLMin;
    private EditText mEditTextILLMax;

    private EditText mEditTextBattMin;
    private EditText mEditTextCutoff;
    private EditText mEditTextHLon;
    private EditText mEditTextHLoff;

    private int mAdcMin;
    private int mAdcMax;
    private int mILLMin;
    private int mILLMax;

    private boolean mDimmerEnabled;
    private float mDimmerFraction;
    private float mBrightnessFraction;

    private boolean mIsLocked = false;
    private boolean mFirstRun = true;
    private boolean mButtonsRefreshed = true;

    private boolean mFirstUnlocked = true;

    private boolean mIsDrlOn = false;

    private long mSunAdcSum = 0;
    private int mSunAdcSamples = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        Intent intent = getIntent();
        int i = intent.getIntExtra("FromWidget", 0);

        if (i == 42) {
            moveTaskToBack(true);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirstRun = this.getIntent().getBooleanExtra(BootReceiver.RUN_FROM_BOOT, false);

        mTextView = (TextView)findViewById(R.id.textView);

        mEditTextAdcMin = (EditText)findViewById(R.id.editTextAdcMin);
        mEditTextAdcMax = (EditText)findViewById(R.id.editTextAdcMax);
        mEditTextILLMin = (EditText)findViewById(R.id.editTextILLMin);
        mEditTextILLMax = (EditText)findViewById(R.id.editTextILLMax);

        mEditTextBattMin = (EditText)findViewById(R.id.editTextBattMin);
        mEditTextCutoff = (EditText)findViewById(R.id.editTextCutoff);
        mEditTextHLon = (EditText)findViewById(R.id.editTextHLon);
        mEditTextHLoff = (EditText)findViewById(R.id.editTextHLoff);

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


        mBtnLock = (ImageButton)findViewById(R.id.btnLock);
        mBtnLock.setBackgroundResource(R.drawable.selector_btn_unlocked);
        mBtnLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lockUnlock();
            }
        });

        mBtnChangePass = (ImageButton)findViewById(R.id.btnChangePass);
        mBtnChangePass.setVisibility(View.GONE);
        mBtnChangePass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onChangeAccessCode();
            }
        });

        mBtnRemoveProtection = (ImageButton)findViewById(R.id.btnUnlock);
        mBtnRemoveProtection.setVisibility(View.GONE);
        mBtnRemoveProtection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSetProtection();
            }
        });

        mBtnDRL = (ImageButton)findViewById(R.id.btnDrl);
        mBtnDRL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSetDrl();
            }
        });

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        Init();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mBoard != null)
            mBoard.Resume();

        if (mFirstRun) {
            mFirstRun = false;
            moveTaskToBack (true);
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


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (Settings.System.canWrite(this)) {
//            }
//            else {
//                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
//                intent.setData(Uri.parse("package:" + getPackageName()));
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
//            }
//        }


        loadIll();
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

            case ACCESS_CODE_CHANGED:
                onAccessCodeChanged(resultCode);
                break;

            case SUBARU_UNLOCKED:
                if (resultCode == Activity.RESULT_OK)
                    onSubaruUnlocked();
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
        mTextView.post(new Runnable() {
            @Override
            public void run() {
                String s = "";
                String s1 = "";

                for(int i=0; i<values.length; i++) {
                    s += "ADC_" + i + " __ " + values[i] + "\n";
                }

                s += "\n";

                for(int i=2; i<values.length; i++) {
                    s1 = String.format("%.2f", (float)values[i] * V_FRACTION);
                    s += "V_" + i + " _____ " + s1 + "\n";
                }

                mTextView.setText(s);

                setIll(values[ADC_SUNLIGHT]+1); // evade zero
            }
        });
    }

    @Override
    public void onDeviceConfigReceived() {
        Board.DeviceConfig deviceConfig;
        deviceConfig = mBoard.getDeviceConfig();

        mEditTextBattMin.setText(Integer.toString(deviceConfig.minVoltage));
        mEditTextCutoff.setText(Integer.toString(deviceConfig.cutOffThreshold));
        mEditTextHLon.setText(Integer.toString(deviceConfig.lightsOnThreshold));
        mEditTextHLoff.setText(Integer.toString(deviceConfig.lightsOffThreshold));

        refreshButtons();
    }

    @Override
    public void onLockStatusRefreshed(boolean isLocked) {
        boolean prevState = mIsLocked;
        mIsLocked = isLocked;

        if (mButtonsRefreshed) {
            mButtonsRefreshed = false;
            refreshButtons();
        }

        if (prevState != isLocked)
            refreshButtons();
    }

    @Override
    public void onDeviceReady() {
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }

    void refreshButtons() {
        mBtnLock.setBackgroundResource(mIsLocked ? R.drawable.selector_btn_locked : R.drawable.selector_btn_unlocked);
        mBtnDRL.setBackgroundResource(mIsDrlOn ? R.drawable.selector_btn_drl_on : R.drawable.selector_btn_drl);
        mBtnChangePass.setVisibility(mIsLocked ? View.GONE : View.VISIBLE);
        mBtnRemoveProtection.setVisibility(mIsLocked ? View.GONE : View.VISIBLE);
        if (mBoard.getDeviceConfig().protectionEnabled == 0)
            mBtnRemoveProtection.setBackgroundResource(R.drawable.selector_btn_protect);
        else
            mBtnRemoveProtection.setBackgroundResource(R.drawable.selector_btn_unprotect);
    }

    void lockUnlock() {
        if (!mIsLocked) {
            mBoard.setLock(true);
            return;
        }

        onEnterAccessCode();
    }

    void onSetProtection() {
        int prot = mBoard.getDeviceConfig().protectionEnabled;
        prot = prot == 0 ? 1 : 0;
        mBoard.getDeviceConfig().protectionEnabled = prot;
        saveConfig();
    }

    void onSetDrl() {
        mIsDrlOn = !mIsDrlOn;
        mBoard.setDrl(mIsDrlOn);
        refreshButtons();
    }

    void onEnterAccessCode() {
        Intent intent = new Intent(this, KeyPadActivity.class);
        intent.putExtra(KeyPadActivity.ACTION, KeyPadActivity.ACTION_UNCLOCK);
        startActivityForResult(intent, SUBARU_UNLOCKED);
    }

    void onChangeAccessCode() {
        Intent intent = new Intent(this, KeyPadActivity.class);
        intent.putExtra(KeyPadActivity.ACTION, KeyPadActivity.ACTION_ACCESS_CODE_CHANGE);
        startActivityForResult(intent, ACCESS_CODE_CHANGED);
    }

    void onAccessCodeChanged(int code) {
        mBoard.setConfig();
    }

    void onSubaruUnlocked() {
        mBoard.setLock(false);
        if (mFirstUnlocked) {
            mFirstUnlocked = false;
            goHome();
        }
    }

    void setIll(int val) {
        if (!mDimmerEnabled)
            return;

        if (val <= mAdcMin) {
            val = mAdcMin;
        }
        else if (val >= mAdcMax) {
            val = mAdcMax;
        }

        val = val - mAdcMin;

        mSunAdcSum += val;

        if (++mSunAdcSamples == ADC_SUN_OVERSAMPLE) {
            val = (int)(mSunAdcSum / mSunAdcSamples);
            mSunAdcSamples = 0;
            mSunAdcSum = 0;
        }
        else {
            return;
        }

        int dimm = (int)((float)val * mDimmerFraction);
        dimm += mILLMin;

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

        mEditTextAdcMin.setText(Integer.toString(mAdcMin));
        mEditTextAdcMax.setText(Integer.toString(mAdcMax));
        mEditTextILLMin.setText(Integer.toString(mILLMin));
        mEditTextILLMax.setText(Integer.toString(mILLMax));

        calcFractions();
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
        mBoard.getDeviceConfig().cutOffThreshold = str2int(mEditTextCutoff.getText().toString());

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
