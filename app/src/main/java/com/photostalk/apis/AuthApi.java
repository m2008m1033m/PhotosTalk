package com.photostalk.apis;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.core.Communicator;
import com.photostalk.core.User;
import com.photostalk.models.AccessToken;
import com.photostalk.models.FacebookUser;
import com.photostalk.utils.ApiListeners;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by mohammed on 2/19/16.
 */
public class AuthApi {

    private static final String clientId = "testclient";
    private static final String clientSecret = "testpass";

    public static void getFacebookUser(String accessToken, final ApiListeners.OnItemLoadedListener listener) {
        RequestParams requestParams = new RequestParams();
        requestParams.put("fields", "id,email,name");
        requestParams.put("access_token", accessToken);

        final Result result = new Result();

        Communicator.getInstance().get("https://graph.facebook.com/me", requestParams, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    FacebookUser facebookUser = new FacebookUser();
                    facebookUser.setFacebookId(response.getString("id").trim());
                    facebookUser.setFullName(response.getString("name").trim());
                    facebookUser.setEmail(response.getString("email").trim());
                    result.setIsSucceeded(true);
                    if (listener != null)
                        listener.onLoaded(result, facebookUser);
                } catch (JSONException e) {
                    result.setIsSucceeded(false);
                    if (listener != null)
                        listener.onLoaded(result, null);
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                result.setIsSucceeded(false);
                result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.unknown_error_has_occurred));
                if (listener != null)
                    listener.onLoaded(result, null);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                result.setIsSucceeded(false);
                result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.no_internet_connection));
                if (listener != null)
                    listener.onLoaded(result, null);
            }
        });
    }

    private static void performLogin(String type, RequestParams requestParams, final ApiListeners.OnItemLoadedListener listener) {
        final Result result = new Result();
        result.setIsSucceeded(false);

        Communicator.getInstance().post(
                type,
                requestParams,
                new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            result.setIsSucceeded(response.getString("success").trim().equals("1"));
                            if (result.isSucceeded()) {

                                AccessToken accessToken = new AccessToken();
                                accessToken.setAccessToken(response.getString("access_token"));
                                accessToken.setRefreshToken(response.getString("refresh_token"));
                                accessToken.setExpiration(response.getLong("expires_in"));
                                if (listener != null)
                                    listener.onLoaded(result, accessToken);

                            } else {

                                JSONArray errors = response.getJSONArray("errors");
                                int length = errors.length();
                                for (int i = 0; i < length; i++)
                                    result.getMessages().add(errors.getString(i));
                                if (listener != null)
                                    listener.onLoaded(result, null);
                            }

                        } catch (JSONException e) {
                            result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.unknown_response_format));
                            if (listener != null)
                                listener.onLoaded(result, null);
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.unknown_error_has_occurred));
                        if (listener != null)
                            listener.onLoaded(result, null);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        try {
                            if (errorResponse != null) {
                                result.getMessages().add(errorResponse.getString("message"));
                            } else {
                                result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.no_internet_connection));
                            }

                        } catch (Exception e) {
                            result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.unknown_error_has_occurred));
                        }

                        if (listener != null)
                            listener.onLoaded(result, null);
                    }
                }
        );

    }

    public static void facebookLogin(String facebookId, String username, String name, String email, final ApiListeners.OnItemLoadedListener listener) {

        RequestParams requestParams = new RequestParams();
        requestParams.add("fb_id", facebookId);
        requestParams.add("username", username);
        requestParams.add("email", email);
        requestParams.add("name", name);
        requestParams.add("client_id", PhotosTalkApplication.getContext().getString(R.string.client_id));
        requestParams.add("client_secret", PhotosTalkApplication.getContext().getString(R.string.client_secret));

        performLogin("user/fb-login", requestParams, listener);

    }

    public static void normalLogin(String username, String password, final ApiListeners.OnItemLoadedListener listener) {
        RequestParams requestParams = new RequestParams();
        requestParams.add("username", username);
        requestParams.add("password", password);
        requestParams.add("grant_type", "password");
        requestParams.add("client_id", PhotosTalkApplication.getContext().getString(R.string.client_id));
        requestParams.add("client_secret", PhotosTalkApplication.getContext().getString(R.string.client_secret));

        performLogin("oauth2/token", requestParams, listener);
    }


    /*
        codes: 0x00 normal error
               0x01 not logged in
     */
    public static void getAccessToken(final ApiListeners.OnActionExecutedListener listener) {
        final Result result = new Result();
        result.setIsSucceeded(false);
        result.setCode("0x00");

        if (User.getInstance().isLoggedIn()) {
            /**
             * if there is a valid access token
             * just use it no need for a new one
             */
            if (User.getInstance().isValidAccessToken()) {
                result.setIsSucceeded(true);
                listener.onExecuted(result);
            }

            /**
             * else request a newer one using the refresh
             * token you have
             */
            else {

                RequestParams requestParams = new RequestParams();
                requestParams.add("grant_type", "refresh_token");
                requestParams.add("refresh_token", User.getInstance().getRefreshToken());
                requestParams.add("client_id", clientId);
                requestParams.add("client_secret", clientSecret);

                Communicator.getInstance().post(
                        "oauth2/token",
                        requestParams,
                        new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                try {
                                    if (response.getString("success").trim().equals("1")) {
                                        User.getInstance().setAccessToken(response.getString("access_token"));
                                        User.getInstance().setAccessTokenExpiresAt(Long.parseLong(response.getString("expires_in")));
                                        User.getInstance().update();
                                        result.setIsSucceeded(true);
                                    } else {
                                        result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.failed_to_obtain_new_access_token));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.unknown_response_format));
                                }
                                listener.onExecuted(result);
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                try {
                                    result.getMessages().add(errorResponse.getString("message") + " (" + errorResponse.getString("status") + ")");
                                } catch (JSONException | NullPointerException e) {
                                    e.printStackTrace();
                                    result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.unknown_response_format));
                                }
                                listener.onExecuted(result);
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                                result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.unknown_error_has_occurred));
                                listener.onExecuted(result);
                            }
                        }
                );

            }
        } else {
            result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.user_not_logged_in));
            result.setCode("0x01");
            listener.onExecuted(result);
        }
    }
}
