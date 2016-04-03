package com.photostalk.core;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class Communicator {

    class Request {
        final static int GET = 0;
        final static int POST = 1;

        public Request(String url, RequestParams requestParams, JsonHttpResponseHandler handler, String tag, Header[] headers, int type) {
            this.url = url;
            this.requestParams = requestParams;
            this.handler = handler;
            this.tag = tag;
            this.headers = headers;
            this.type = type;
        }

        String url;
        RequestParams requestParams;
        JsonHttpResponseHandler handler;
        String tag;
        Header[] headers;
        int type;
    }

    public final static String BASE_URL = "http://photostalk.tsslabs.info/";
    public final static String API_URL = BASE_URL + "api/";
    private final static int TIME_OUT = 20 * 1000;
    private final static int MAX_NUMBER_OF_PARALLEL_REQUESTS = 10;

    private final AsyncHttpClient mAsyncHttpClient = new AsyncHttpClient();
    private final ArrayList<Request> mPendingRequests = new ArrayList<>();
    private int mNumberOfParallelRequests = 0;
    private final Context mContext;

    private static Communicator mInstance;

    public static Communicator getInstance() {
        if (mInstance == null)
            mInstance = new Communicator(PhotosTalkApplication.getContext());
        return mInstance;
    }

    private Communicator(Context context) {
        mContext = context;
    }

    public void get(String url, RequestParams requestParams, JsonHttpResponseHandler handler, String tag, Header[] headers) {
        if (!checkIfCanRun(url, requestParams, handler, tag, headers, Request.GET)) return;
        handler = wrapHandler(handler, tag);
        url = (url.startsWith("http")) ? url : getAbsoluteUrl(url);
        Log.d("Communicator", "The url is: " + url);
        Log.d("Communicator", "The params are: " + requestParams.toString());

        mAsyncHttpClient.setTimeout(TIME_OUT);
        mAsyncHttpClient.get(mContext, url, headers, requestParams, handler).setTag(tag);
    }

    public void get(String url, RequestParams requestParams, JsonHttpResponseHandler handler, Header[] headers) {
        get(url, requestParams, handler, System.nanoTime() + "", headers);
    }

    public void get(String url, RequestParams requestParams, JsonHttpResponseHandler handler) {
        get(url, requestParams, handler, System.nanoTime() + "", null);
    }

    public void post(String url, RequestParams requestParams, JsonHttpResponseHandler handler, String tag, Header[] headers) {
        if (!checkIfCanRun(url, requestParams, handler, tag, headers, Request.POST)) return;
        handler = wrapHandler(handler, tag);
        url = (url.startsWith("http")) ? url : getAbsoluteUrl(url);
        Log.d("Communicator", "The url is: " + url);
        Log.d("Communicator", "The params are: " + requestParams.toString());

        mAsyncHttpClient.setTimeout(TIME_OUT);
        mAsyncHttpClient.post(mContext, url, headers, requestParams, null, handler).setTag(tag);
    }

    public void post(String url, RequestParams requestParams, JsonHttpResponseHandler handler, Header[] headers) {
        post(url, requestParams, handler, System.nanoTime() + "", headers);
    }

    public void post(String url, RequestParams requestParams, JsonHttpResponseHandler handler) {
        post(url, requestParams, handler, System.nanoTime() + "", null);
    }

    public void cancelAll() {
        mAsyncHttpClient.cancelRequests(mContext, true);
    }

    public void cancelByTag(String tag) {
        mAsyncHttpClient.cancelRequestsByTAG(tag, true);
    }

    public boolean checkIfCanRun(String url, RequestParams requestParams, JsonHttpResponseHandler handler, String tag, Header[] headers, int type) {
        if (mNumberOfParallelRequests > MAX_NUMBER_OF_PARALLEL_REQUESTS) {
            mPendingRequests.add(new Request(url, requestParams, handler, tag, headers, type));
            return false;
        }
        mNumberOfParallelRequests++;
        return true;
    }

    private JsonHttpResponseHandler wrapHandler(final JsonHttpResponseHandler handler, final String tag) {
        JsonHttpResponseHandler wrapper = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                handler.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                handler.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                handler.onSuccess(statusCode, headers, responseString);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("message", PhotosTalkApplication.getContext().getString(R.string.unknown_error_has_occurred));
                    handler.onFailure(statusCode, headers, throwable, jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                handler.onFailure(statusCode, headers, throwable, errorResponse);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("message", PhotosTalkApplication.getContext().getString(R.string.unknown_error_has_occurred));
                    handler.onFailure(statusCode, headers, throwable, jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFinish() {
                super.onFinish();
                mNumberOfParallelRequests--;
                engageNewRequest();
            }
        };


        wrapper.setTag(tag);
        return wrapper;
    }

    private String getAbsoluteUrl(String url) {
        return API_URL + url;
    }

    private void engageNewRequest() {
        while (mPendingRequests.size() > 1 && mNumberOfParallelRequests <= MAX_NUMBER_OF_PARALLEL_REQUESTS) {
            Request r = mPendingRequests.remove(0);
            if (r.type == Request.GET) {
                get(r.url, r.requestParams, r.handler, r.tag, r.headers);
            } else {
                post(r.url, r.requestParams, r.handler, r.tag, r.headers);
            }
        }
    }
}
