package com.photostalk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import com.photostalk.core.User;
import com.photostalk.utils.Broadcasting;


public class LoggedInActivity extends AppCompatActivity {
    private BroadcastReceiver mBroadcastReceiver;
    private boolean mIsCurrentActivity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBroadcastReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsCurrentActivity = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsCurrentActivity = true;
    }

    private void setupBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Broadcasting.LOGOUT) || intent.getAction().equals(Broadcasting.EXPIRED_REFRESH_TOKEN)) {
                    /**
                     * check if this activity is the active one.
                     */
                    if (mIsCurrentActivity) {
                        User.getInstance().logout();
                        Intent i = new Intent(LoggedInActivity.this, LoginActivity.class);
                        if (intent.getAction().equals(Broadcasting.EXPIRED_REFRESH_TOKEN)) {
                            i.putExtra(LoginActivity.DISPLAY_MESSAGE, getString(R.string.authorization_error));
                        }
                        startActivity(i);
                    }
                    finish();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Broadcasting.LOGOUT);
        intentFilter.addAction(Broadcasting.EXPIRED_REFRESH_TOKEN);

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
    }
}
