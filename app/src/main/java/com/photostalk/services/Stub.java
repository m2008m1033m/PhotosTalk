package com.photostalk.services;

import android.support.annotation.Nullable;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.core.Communicator;
import com.photostalk.core.User;
import com.photostalk.models.Model;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Broadcasting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.message.BasicHeader;

/**
 * Created by mohammed on 2/29/16.
 */
public class Stub {

    public static class ModelParser {
        Model parseItem(JSONObject jsonObject) throws JSONException {
            return null;
        }

        ArrayList<Model> extractArray(JSONObject jsonObject) throws JSONException {
            return null;
        }

        ArrayList<Model> parseArray(JSONArray array) throws JSONException {
            return null;
        }
    }

    public static void get(final String url, final Object listener, final @Nullable ModelParser parser, final @Nullable RequestParams params, final @Nullable String tag) {
        perform(0, url, listener, parser, params, tag);
    }

    public static void post(final String url, final Object listener, final @Nullable ModelParser parser, final @Nullable RequestParams params, final @Nullable String tag) {
        perform(2, url, listener, parser, params, tag);
    }

    private static void perform(final int type, final String url, final Object listener, final @Nullable ModelParser parser, final @Nullable RequestParams params, final @Nullable String tag) {

        AuthApi.getAccessToken(new ApiListeners.OnActionExecutedListener() {


            @Override
            public void onExecuted(final Result result) {
                if (result.isSucceeded()) {
                    JsonHttpResponseHandler handler = new JsonHttpResponseHandler() {

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            try {
                                if (response.getString("success").trim().equals("1")) {
                                    result.setIsSucceeded(true);
                                    executeListener(listener, result, (parser != null) ? parser.parseItem(response) : null, (parser != null) ? parser.extractArray(response) : null);
                                } else {
                                    result.setIsSucceeded(false);
                                    result.getMessages().clear();
                                    JSONArray errors = response.getJSONArray("errors");
                                    for (int i = 0; i < errors.length(); i++)
                                        result.getMessages().add(errors.getString(i));
                                    executeListener(listener, result, null, null);
                                }


                            } catch (JSONException e) {
                                e.printStackTrace();
                                result.setIsSucceeded(false);
                                result.getMessages().clear();
                                result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.unknown_response_format));
                                executeListener(listener, result, null, null);
                            }

                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                            result.setIsSucceeded(true);
                            try {
                                executeListener(listener, result, null, (parser != null) ? parser.parseArray(response) : null);
                            } catch (JSONException e) {
                                result.setIsSucceeded(false);
                                result.getMessages().clear();
                                result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.unknown_response_format));
                                executeListener(listener, result, null, null);
                            }

                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                            result.setIsSucceeded(false);
                            result.getMessages().clear();
                            try {
                                result.getMessages().add(errorResponse.getString("message"));
                            } catch (Exception e) {
                                e.printStackTrace();
                                result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.unknown_error_has_occurred));
                            }

                            executeListener(listener, result, null, null);
                        }
                    };

                    BasicHeader[] headers = new BasicHeader[]{new BasicHeader("Authorization", "Bearer " + User.getInstance().getAccessToken())};

                    if (type == 0)
                        Communicator.getInstance().get(
                                url,
                                params,
                                handler,
                                tag,
                                headers
                        );
                    else
                        Communicator.getInstance().post(
                                url,
                                params,
                                handler,
                                tag,
                                headers
                        );

                } else {
                    // user is no longer authenticated:
                    // need to send logout signal
                    executeListener(listener, result, null, null);
                }
            }
        });

    }

    private static void executeListener(Object listener, Result result, @Nullable Model model, @Nullable ArrayList<Model> models) {
        if (listener instanceof ApiListeners.OnActionExecutedListener) {
            ((ApiListeners.OnActionExecutedListener) listener).onExecuted(result);
        } else if (listener instanceof ApiListeners.OnItemLoadedListener) {
            ((ApiListeners.OnItemLoadedListener) listener).onLoaded(result, model);
        } else if (listener instanceof ApiListeners.OnItemsArrayLoadedListener) {
            ((ApiListeners.OnItemsArrayLoadedListener) listener).onLoaded(result, models);
        }
    }

}
