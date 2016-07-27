package com.photostalk.utils;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.photostalk.PhotosTalkApplication;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.entity.ByteArrayEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HTTP;

/**
 * Created by mohammed on 2/19/16.
 */
public class Communication {

    public final static String BASE_URL = "http://photostalk.tsslabs.info/";
    public final static String API_URL = BASE_URL + "api/";
    private final static int TIME_OUT = 20 * 1000;
    private static AsyncHttpClient mAsyncHttpClient = new AsyncHttpClient();

    public static void get(String url, RequestParams requestParams, AsyncHttpResponseHandler asyncHttpResponseHandler) {
        mAsyncHttpClient.setTimeout(TIME_OUT);
        url = (url.startsWith("http")) ? url : getAbsoluteUrl(url);
        mAsyncHttpClient.get(url, requestParams, asyncHttpResponseHandler);
    }

    public static void post(String url, RequestParams requestParams, AsyncHttpResponseHandler asyncHttpResponseHandler) {
        mAsyncHttpClient.setTimeout(TIME_OUT);
        url = (url.startsWith("http")) ? url : getAbsoluteUrl(url);
        Log.d("RESTfull", "Link: " + url);
        mAsyncHttpClient.post(url, requestParams, asyncHttpResponseHandler);
    }

    public static void postJson(String url, JSONObject requestParams, AsyncHttpResponseHandler asyncHttpResponseHandler) {
        try {
            ByteArrayEntity entity = new ByteArrayEntity(requestParams.toString().getBytes("UTF-8"));
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded"));
            entity.setContentEncoding("UTF-8");

            mAsyncHttpClient.setTimeout(TIME_OUT);
            url = (url.startsWith("http")) ? url : getAbsoluteUrl(url);
            Log.d("RESTfull", "Link: " + url);
            Log.d("RESTfull", "Params: " + requestParams.toString());
            mAsyncHttpClient.post(PhotosTalkApplication.getContext(), url, entity, "application/x-www-form-urlencoded", asyncHttpResponseHandler);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static String getAbsoluteUrl(String url) {
        return API_URL + url;
    }

}
