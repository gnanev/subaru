package com.gran.subaru;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cardinalsolutions.android.arch.autowire.AndroidLayout;
import com.cardinalsolutions.android.arch.autowire.AndroidView;

@AndroidLayout(R.layout.activity_key_pad)

public class KeyPadActivity extends BaseActivity implements View.OnClickListener,
        View.OnTouchListener {

	private static final String TAG = "KeyPadActivity";

	@AndroidView(R.id.activity_login_access_code_value)
    private EditText mUserAccessCode;

    @AndroidView(R.id.one_button)
    private TextView mOneButton;

    @AndroidView(R.id.two_button)
    private TextView mTwoButton;

    @AndroidView(R.id.three_button)
    private TextView mThreeButton;

    @AndroidView(R.id.four_button)
    private TextView mFourButton;

    @AndroidView(R.id.five_button)
    private TextView mFiveButton;

    @AndroidView(R.id.six_button)
    private TextView mSixButton;

    @AndroidView(R.id.seven_button)
    private TextView mSevenButton;

    @AndroidView(R.id.eight_button)
    private TextView mEightButton;

    @AndroidView(R.id.nine_button)
    private TextView mNineButton;

    @AndroidView(R.id.zero_button)
    private TextView mZeroButton;

    @AndroidView(R.id.activity_login_access_code_delete)
    private TextView mDeleteButton;

    @AndroidView(R.id.button_Cancel)
    private TextView mButtonCancel;

    @AndroidView(R.id.activity_login_access_code)
    private TextView mEnterCodeLabel;

    private boolean mDeleteIsShowing = false;

	public static final String USER_PIN = "USER_PIN";
	public static final int USER_PIN_MAX_CHAR = 4;

    public static final String ACTION = "action";
    public static final String ACTION_ACCESS_CODE_CHANGE = "change_code";
    public static final String ACTION_UNCLOCK = "unlock";

    private String mActionCode = "";

    private String mFirstCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        mActionCode = bundle.getString(ACTION);

        configureViews();
        setEditTextListener();
    }

    private void attemptLogin(String userPinIn) {
        /*
         Details left out for brevity and the numerous was one can attempt a login, but...
         attempt your login here and grab the response...

	    If your login is a success, pass the user pin to save to shared prefs and close your
	    activity

	    saveUserPinToSharedPrefs("success", userPinIn);
	    closeActivity();

	    else run animation and reset the text field...

	    Wrapping the failed response in a handler to pause execution so we can demo the animation.
	    You do not need the handler in real world use!!
	    */

	    Handler handler = new Handler();
	    handler.postDelayed(new Runnable() {
		    @Override
		    public void run() {
			    mUserAccessCode.setText("");
		    }
	    }, 3000);

    }

    private void configureViews() {
        this.mOneButton.setOnClickListener(this);
        this.mOneButton.setOnTouchListener(this);
        this.mTwoButton.setOnClickListener(this);
        this.mTwoButton.setOnTouchListener(this);
        this.mThreeButton.setOnClickListener(this);
        this.mThreeButton.setOnTouchListener(this);
        this.mFourButton.setOnClickListener(this);
        this.mFourButton.setOnTouchListener(this);
        this.mFiveButton.setOnClickListener(this);
        this.mFiveButton.setOnTouchListener(this);
        this.mSixButton.setOnClickListener(this);
        this.mSixButton.setOnTouchListener(this);
        this.mSevenButton.setOnClickListener(this);
        this.mSevenButton.setOnTouchListener(this);
        this.mEightButton.setOnClickListener(this);
        this.mEightButton.setOnTouchListener(this);
        this.mNineButton.setOnClickListener(this);
        this.mNineButton.setOnTouchListener(this);
        this.mZeroButton.setOnClickListener(this);
        this.mZeroButton.setOnTouchListener(this);
        this.mDeleteButton.setVisibility(View.INVISIBLE);
        this.mDeleteButton.setOnClickListener(this);
        this.mDeleteButton.setOnTouchListener(this);

        this.mButtonCancel.setOnTouchListener(this);
        this.mButtonCancel.setOnClickListener(this);

    }

    private void setEditTextListener() {
        this.mUserAccessCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    @Override
    public void onClick(View vIn) {
        if (!this.mDeleteIsShowing && (vIn.getId() != R.id.button_Cancel)) {
            mDeleteButton.setVisibility(View.VISIBLE);
            this.mDeleteIsShowing = true;
        }

        switch (vIn.getId()) {
            case R.id.one_button:
                if (this.mUserAccessCode.getText().length() < USER_PIN_MAX_CHAR) {
                    this.mUserAccessCode.append(this.mOneButton.getText());
                }
                break;
            case R.id.two_button:
                if (this.mUserAccessCode.getText().length() < USER_PIN_MAX_CHAR) {
                    this.mUserAccessCode.append(this.mTwoButton.getText());
                }
                break;
            case R.id.three_button:
                if (this.mUserAccessCode.getText().length() < USER_PIN_MAX_CHAR) {
                    this.mUserAccessCode.append(this.mThreeButton.getText());
                }
                break;
            case R.id.four_button:
                if (this.mUserAccessCode.getText().length() < USER_PIN_MAX_CHAR) {
                    this.mUserAccessCode.append(this.mFourButton.getText());
                }
                break;
            case R.id.five_button:
                if (this.mUserAccessCode.getText().length() < USER_PIN_MAX_CHAR) {
                    this.mUserAccessCode.append(this.mFiveButton.getText());
                }
                break;
            case R.id.six_button:
                if (this.mUserAccessCode.getText().length() < USER_PIN_MAX_CHAR) {
                    this.mUserAccessCode.append(this.mSixButton.getText());
                }
                break;
            case R.id.seven_button:
                if (this.mUserAccessCode.getText().length() < USER_PIN_MAX_CHAR) {
                    this.mUserAccessCode.append(this.mSevenButton.getText());
                }
                break;
            case R.id.eight_button:
                if (this.mUserAccessCode.getText().length() < USER_PIN_MAX_CHAR) {
                    this.mUserAccessCode.append(this.mEightButton.getText());
                }
                break;
            case R.id.nine_button:
                if (this.mUserAccessCode.getText().length() < USER_PIN_MAX_CHAR) {
                    this.mUserAccessCode.append(this.mNineButton.getText());
                }
                break;
            case R.id.zero_button:
                if (this.mUserAccessCode.getText().length() < USER_PIN_MAX_CHAR) {
                    this.mUserAccessCode.append(this.mZeroButton.getText());
                }
                break;

            case R.id.activity_login_access_code_delete:
                this.mUserAccessCode.dispatchKeyEvent(new KeyEvent(
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                if (isEditTextEmpty(this.mUserAccessCode)) {
                    mDeleteButton.setVisibility(View.GONE);
                    this.mDeleteIsShowing = false;
                }
                break;

            case R.id.button_Cancel:
                exit(Activity.RESULT_CANCELED);
                break;
        }

        if ((vIn.getId() != R.id.button_Cancel) && (mUserAccessCode.getText().length() == USER_PIN_MAX_CHAR))
            processAccessCode();
    }

    private static boolean isEditTextEmpty(EditText etText) {
        return etText.getText().toString().trim().length() == 0;
    }

    @Override
    public boolean onTouch(View vIn, MotionEvent eventIn) {
        switch (vIn.getId()) {
            case R.id.one_button:
                toggleNumberColor(vIn, eventIn);
                break;
            case R.id.two_button:
                toggleNumberColor(vIn, eventIn);
                break;
            case R.id.three_button:
                toggleNumberColor(vIn, eventIn);
                break;
            case R.id.four_button:
                toggleNumberColor(vIn, eventIn);
                break;
            case R.id.five_button:
                toggleNumberColor(vIn, eventIn);
                break;
            case R.id.six_button:
                toggleNumberColor(vIn, eventIn);
                break;
            case R.id.seven_button:
                toggleNumberColor(vIn, eventIn);
                break;
            case R.id.eight_button:
                toggleNumberColor(vIn, eventIn);
                break;
            case R.id.nine_button:
                toggleNumberColor(vIn, eventIn);
                break;
            case R.id.zero_button:
                toggleNumberColor(vIn, eventIn);
                break;
            case R.id.button_Cancel:
                toggleNumberColor(vIn, eventIn);
                break;
            case R.id.activity_login_access_code_delete:
                toggleNumberColor(vIn, eventIn);
                break;
        }
        return false;
    }

    private void toggleNumberColor(View viewIn, MotionEvent eventIn) {
        if (eventIn.getAction() == MotionEvent.ACTION_DOWN) {
            ((TextView) viewIn).setTextColor(getResources().getColor(
                    R.color.blue));
        } else if (eventIn.getAction() == MotionEvent.ACTION_UP) {
            ((TextView) viewIn).setTextColor(getResources().getColor(
                    R.color.white));
        }
    }

    void exit(int code) {
        Intent returnIntent = new Intent();
        setResult(code, returnIntent);
        finish();
    }

    void processAccessCode() {
        if (mActionCode.equals(ACTION_ACCESS_CODE_CHANGE)) {
            processCodeChange();
        }
        else if (mActionCode.equals(ACTION_UNCLOCK)) {
            processUnlockCode();
        }
    }

    void processUnlockCode() {
        String code = mUserAccessCode.getText().toString();
        String unlockCode =  mSharedPreferences.getString(USER_PIN, "1111");

        if (!code.equals(unlockCode))
            wrongCode();
        else
            exit(Activity.RESULT_OK);
    }

    void processCodeChange() {
        String code = mUserAccessCode.getText().toString();

        if (mFirstCode.isEmpty()) {
            mFirstCode = code;
            enterAgain();
        }
        else {
            if (!mFirstCode.equals(code)) {
                wrongCode();
            }
            else {
                saveCode(code);
            }
        }
    }

    void enterAgain() {
        mUserAccessCode.setText("");
        mEnterCodeLabel.setText(R.string.activity_login_again);
        mDeleteButton.setVisibility(View.GONE);
        mDeleteIsShowing = false;
    }

    void wrongCode() {
        Toast.makeText(this, "!!! WRONG !!!", Toast.LENGTH_SHORT).show();
        mFirstCode = "";
        mUserAccessCode.setText("");
        mEnterCodeLabel.setText(R.string.activity_login_enter_access_code);
        mDeleteButton.setVisibility(View.GONE);
        mDeleteIsShowing = false;
    }

    void saveCode(String code) {
        mPreferencesEditor.putString(USER_PIN, code);
        mPreferencesEditor.commit();

        exit(Activity.RESULT_OK);
    }
}
