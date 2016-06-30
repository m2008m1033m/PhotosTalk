package com.photostalk.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.photostalk.apis.FCMApi;
import com.photostalk.apis.Result;
import com.photostalk.core.User;
import com.photostalk.utils.ApiListeners;

public class PhotosTalkFirebaseInstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        final String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d("InstanceIdService", "Refreshed token: " + refreshedToken);

        // make sure the user is logged in
        User user = User.getInstance();
        if (user == null || !user.isLoggedIn()) return;

        // send the token to the server
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                FCMApi.register(refreshedToken, new ApiListeners.OnActionExecutedListener() {
                    @Override
                    public void onExecuted(Result result) {

                    }
                });
            }
        });
    }
}
