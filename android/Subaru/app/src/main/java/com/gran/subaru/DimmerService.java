package com.gran.subaru;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class DimmerService extends Service {
    private final static String TAG = "DimmerService";

    public static final int MSG_SET_ALPHA = 1;

    LinearLayout mDimmerView;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        super.onCreate();

        int LAYOUT_FLAG;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
//        } else {
//            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
//        }

        LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

        mDimmerView = new LinearLayout(this);
        mDimmerView.setBackgroundColor(Color.argb(0, 0, 0, 0));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.RIGHT | Gravity.TOP;

        View decorView = mDimmerView.getRootView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        wm.addView(mDimmerView, params);

//        ImageButton mBtnLock;
//        mBtnLock = new ImageButton(getApplicationContext());
//        mBtnLock.setBackgroundResource(R.drawable.selector_btn_locked);
//
//
//        WindowManager.LayoutParams params2 = new WindowManager.LayoutParams(
//                160,
//                160,
//                LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                PixelFormat.TRANSLUCENT);
//
//        params.gravity = Gravity.LEFT | Gravity.TOP;
//
//        wm.addView(mBtnLock, params2);

//        mBtnLock.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.d("xxx", "xxxxx");
//            }
//        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY; // run until explicitly stopped.
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        if(mDimmerView != null)
        {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mDimmerView);
            mDimmerView = null;
        }
    }

    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_ALPHA:
                    setAlpha(msg.arg1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void setAlpha(int a) {
        mDimmerView.setBackgroundColor(Color.argb(a, 0, 0, 0));
    }
}

