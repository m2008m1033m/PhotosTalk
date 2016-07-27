package com.photostalk.apis;

import com.loopj.android.http.RequestParams;
import com.photostalk.utils.ApiListeners;

/**
 * Created by mohammed on 6/19/16.
 */
public class FCMApi {

    public static void register(String token, ApiListeners.OnActionExecutedListener listener) {
        Stub.post("fcm/register", listener, null, new RequestParams("token", token), "fcm_register");
    }

}
