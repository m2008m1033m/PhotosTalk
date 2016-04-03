package com.photostalk;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.photostalk.services.Result;
import com.photostalk.services.UserApi;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Notifications;
import com.photostalk.utils.Validations;

/**
 * Created by mohammed on 2/19/16.
 */
public class RegistrationActivity extends AppCompatActivity {

    public static final String INTENT_MODE = "mode";
    public static final String INTENT_FACEBOOK_ID = "facebook_id";
    public static final String INTENT_TWITTER_ID = "twitter_id";
    public static final String INTENT_EMAIL = "email";

    public static final int MODE_NORMAL = 0;
    public static final int MODE_FACEBOOK = 1;
    public static final int MODE_TWITTER = 2;

    private int mMode;
    private String mFacebookId;
    private String mTwitterId;
    private String mEmail;

    private EditText mFullNameEditText;
    private EditText mUserNameEditText;
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private Button mRegisterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * setting the title
         */
        setTitle(getString(R.string.register));

        /**
         * setting the mode and getting the facebook or twitter id (if any)
         */
        mMode = getIntent().getIntExtra(INTENT_MODE, MODE_NORMAL);
        if (mMode == MODE_FACEBOOK)
            mFacebookId = getIntent().getStringExtra(INTENT_FACEBOOK_ID);
        else if (mMode == MODE_TWITTER)
            mTwitterId = getIntent().getStringExtra(INTENT_TWITTER_ID);
        if (mMode != MODE_NORMAL)
            mEmail = getIntent().getStringExtra(INTENT_EMAIL);


        /**
         * setting the layout
         */
        setContentView(R.layout.registeration_activity);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        init();
    }

    private void init() {
        initReferences();
        refreshView();
        initEvents();
    }

    private void initEvents() {
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isEverythingOk())
                    switch (mMode) {
                        case MODE_FACEBOOK:
                            registerFacebook();
                            break;
                        case MODE_TWITTER:
                            registerTwitter();
                            break;
                        default:
                            registerNormal();
                    }
            }
        });
    }

    private void registerNormal() {
        final AlertDialog ad = Notifications.showLoadingDialog(this, getString(R.string.loading));
        UserApi.registerNormal(
                mFullNameEditText.getText().toString().trim(),
                mUserNameEditText.getText().toString().trim(),
                mEmailEditText.getText().toString().trim(),
                mPasswordEditText.getText().toString().trim(),
                new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {
                        ad.dismiss();
                        if (result.isSucceeded()) {
                            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                            intent.putExtra("message", result.getMessages().get(0));
                            setResult(LoginActivity.DISPLAY_REGISTER_SUCCESSFUL, intent);
                            finish();
                        } else {
                            Notifications.showListAlertDialog(RegistrationActivity.this, getString(R.string.error), result.getMessages());
                        }
                    }
                }
        );
    }

    private void registerFacebook() {

    }

    private void registerTwitter() {

    }

    private void initReferences() {
        mFullNameEditText = ((EditText) findViewById(R.id.full_name));
        mUserNameEditText = ((EditText) findViewById(R.id.username));
        mEmailEditText = ((EditText) findViewById(R.id.email));
        mPasswordEditText = ((EditText) findViewById(R.id.password));
        mRegisterButton = ((Button) findViewById(R.id.register_button));
    }

    /**
     * hides the email and password field in case
     * of facebook or twitter registration
     */
    private void refreshView() {
        if (mMode != MODE_NORMAL) {
            mEmailEditText.setVisibility(View.GONE);
            mPasswordEditText.setVisibility(View.GONE);
        }
    }

    /**
     * checks the validity of the fields
     * before submitting them
     *
     * @return
     */
    private boolean isEverythingOk() {
        if (!Validations.notEmptyOrWhiteSpaces(mFullNameEditText.getText().toString())) {
            Notifications.showAlertDialog(this, getString(R.string.error), getString(R.string.full_name_cannot_be_empty));
            return false;
        }

        if (!Validations.notEmptyOrWhiteSpaces(mUserNameEditText.getText().toString())) {
            Notifications.showAlertDialog(this, getString(R.string.error), getString(R.string.username_cannot_be_empty));
            return false;
        }

        if (!Validations.noSpaces(mUserNameEditText.getText().toString())) {
            Notifications.showAlertDialog(this, getString(R.string.error), getString(R.string.username_cannot_have_whitespaces));
            return false;
        }

        if (mMode == MODE_NORMAL) {

            if (!Validations.notEmptyOrWhiteSpaces(mEmailEditText.getText().toString())) {
                Notifications.showAlertDialog(this, getString(R.string.error), getString(R.string.email_cannot_be_empty));
                return false;
            }

            if (!Validations.isValidEmail(mEmailEditText.getText().toString())) {
                Notifications.showAlertDialog(this, getString(R.string.error), getString(R.string.invalid_email_address));
                return false;
            }

            if (!Validations.notEmptyOrWhiteSpaces(mPasswordEditText.getText().toString())) {
                Notifications.showAlertDialog(this, getString(R.string.error), getString(R.string.password_cannot_be_empty));
                return false;
            }

        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
