package com.photostalk;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.photostalk.core.User;
import com.photostalk.models.AccessToken;
import com.photostalk.models.FacebookUser;
import com.photostalk.models.Model;
import com.photostalk.services.AuthApi;
import com.photostalk.services.Result;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Notifications;
import com.photostalk.utils.Validations;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

import java.util.Arrays;

import io.fabric.sdk.android.Fabric;

/**
 * Created by mohammed on 2/19/16.
 */
public class LoginActivity extends AppCompatActivity {

    public static final int DISPLAY_REGISTER_SUCCESSFUL = 0;

    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private Button mLoginButton;
    private Button mFacebookButton;
    //private Button mTwitterButton;
    //private TextView mForgotPasswordTextView;
    private TextView mRegisterTextView;

    private CallbackManager mCallbackManager;
    private TwitterAuthClient mTwitterAuthClient;
    private ApiListeners.OnItemLoadedListener mLoginListener;


    AlertDialog mProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.login_activity);
        init();
    }

    private void init() {
        initReferences();
        initLoginListener();
        initFacebook();
        initTwitter();
        initEvents();
    }

    private void initLoginListener() {
        mLoginListener = new ApiListeners.OnItemLoadedListener() {
            @Override
            public void onLoaded(Result result, Model item) {
                if (result.isSucceeded()) {
                    User.getInstance().login(((AccessToken) item), new ApiListeners.OnActionExecutedListener() {
                        @Override
                        public void onExecuted(Result result) {
                            if (result.isSucceeded()) {
                                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                startActivity(new Intent(LoginActivity.this, CameraActivity.class));
                                finish();
                            } else
                                Notifications.showSnackbar(LoginActivity.this, result.getMessages().get(0));
                            dismissProgress();
                        }
                    });
                } else {
                    Notifications.showListAlertDialog(LoginActivity.this, getString(R.string.error), result.getMessages());
                    dismissProgress();
                }

            }
        };
    }

    private void initFacebook() {
        FacebookSdk.sdkInitialize(getApplicationContext());
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AuthApi.getFacebookUser(loginResult.getAccessToken().getToken(), new ApiListeners.OnItemLoadedListener() {
                    @Override
                    public void onLoaded(Result result, Model item) {
                        if (result.isSucceeded()) {
                            FacebookUser user = ((FacebookUser) item);
                            AuthApi.facebookLogin(user.getFacebookId(), user.getEmail(), user.getFullName(), user.getEmail(), mLoginListener);

                        } else {
                            Notifications.showListAlertDialog(LoginActivity.this, getString(R.string.error), result.getMessages());
                            if (mProgressDialog != null) {
                                mProgressDialog.dismiss();
                                mProgressDialog = null;
                            }
                        }

                    }
                });
                LoginManager.getInstance().logOut();
            }

            @Override
            public void onCancel() {
                LoginManager.getInstance().logOut();
            }

            @Override
            public void onError(FacebookException e) {
                LoginManager.getInstance().logOut();
            }
        });
    }

    private void initTwitter() {
        TwitterAuthConfig authConfig =
                new TwitterAuthConfig("q0kRqSuNAxIUgTYVkqzQzTPl6", "kIImNPfGxdww6lpIoeeKbwItvHIKPRnuBcFaYtfDYlOeiM5Nja");
        Fabric.with(this, new TwitterCore(authConfig));
        mTwitterAuthClient = new TwitterAuthClient();
    }

    private void initReferences() {
        mUsernameEditText = ((EditText) findViewById(R.id.username));
        mPasswordEditText = ((EditText) findViewById(R.id.password));
        mLoginButton = ((Button) findViewById(R.id.login_button));
        mFacebookButton = ((Button) findViewById(R.id.facebook_button));
        //mTwitterButton = ((Button) findViewById(R.id.twitter_button));
        //mForgotPasswordTextView = ((TextView) findViewById(R.id.forgot_password));
        mRegisterTextView = ((TextView) findViewById(R.id.register_button));
    }

    private void initEvents() {
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isEverythingOkay()) {
                    mProgressDialog = Notifications.showLoadingDialog(LoginActivity.this, getString(R.string.please_wait));
                    AuthApi.normalLogin(mUsernameEditText.getText().toString().trim(), mPasswordEditText.getText().toString(), mLoginListener);
                }
            }
        });

        mFacebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProgressDialog = Notifications.showLoadingDialog(LoginActivity.this, getString(R.string.please_wait));
                LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this, Arrays.asList("public_profile", "email"));
            }
        });

        /*mTwitterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProgressDialog = Notifications.showLoadingDialog(LoginActivity.this, getString(R.string.please_wait));
                mTwitterAuthClient.authorize(LoginActivity.this, new Callback<TwitterSession>() {
                    @Override
                    public void success(com.twitter.sdk.android.core.Result<TwitterSession> result) {
                        mTwitterAuthClient.requestEmail(result.data, new Callback<String>() {
                            @Override
                            public void success(com.twitter.sdk.android.core.Result<String> result) {
                                if (mProgressDialog != null) {
                                    mProgressDialog.dismiss();
                                    mProgressDialog = null;
                                }
                                //TODO: perform login twitter after you get whitelisted
                            }

                            @Override
                            public void failure(TwitterException e) {
                                if (mProgressDialog != null) {
                                    mProgressDialog.dismiss();
                                    mProgressDialog = null;
                                }
                                Notifications.showAlertDialog(LoginActivity.this, getString(R.string.error), e.getMessage());
                            }
                        });

                    }

                    @Override
                    public void failure(TwitterException e) {
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                            mProgressDialog = null;
                        }
                        Notifications.showAlertDialog(LoginActivity.this, getString(R.string.error), e.getMessage());
                    }
                });

            }
        });*/

        mRegisterTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivityForResult(i, DISPLAY_REGISTER_SUCCESSFUL);
            }
        });


    }

    private boolean isEverythingOkay() {
        if (!Validations.notEmptyOrWhiteSpaces(mUsernameEditText.getText().toString().trim())) {
            Notifications.showAlertDialog(this, getString(R.string.error), getString(R.string.username_cannot_be_empty));
            return false;
        }

        if (!Validations.notEmptyOrWhiteSpaces(mPasswordEditText.getText().toString())) {
            Notifications.showAlertDialog(this, getString(R.string.error), getString(R.string.password_cannot_be_empty));
            return false;
        }

        return true;
    }

    private void dismissProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == DISPLAY_REGISTER_SUCCESSFUL && data != null) {
            String message = data.getStringExtra("message");
            if (message != null)
                Notifications.showSnackbar(this, message);

        }

        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        mTwitterAuthClient.onActivityResult(requestCode, resultCode, data);
    }

}
