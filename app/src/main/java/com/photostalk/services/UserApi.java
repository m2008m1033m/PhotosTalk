package com.photostalk.services;

import android.support.annotation.Nullable;
import android.util.Log;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.core.Communicator;
import com.photostalk.core.User;
import com.photostalk.models.Followship;
import com.photostalk.models.Model;
import com.photostalk.models.Notification;
import com.photostalk.models.Photo;
import com.photostalk.models.Story;
import com.photostalk.models.Timeline;
import com.photostalk.models.UserModel;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.Communication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.message.BasicHeader;

/**
 * Created by mohammed on 2/19/16.
 */
public class UserApi {

    public static void registerNormal(String fullName, String username, String email, String password, final ApiListeners.OnActionExecutedListener listener) {
        RequestParams params = new RequestParams();
        params.add("username", username);
        params.add("name", fullName);
        params.add("email", email);
        params.add("password", password);

        final Result result = new Result();

        Communication.post(
                "user/register",
                params,
                new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            result.setIsSucceeded(response.getString("success").trim().equals("1"));
                            if (result.isSucceeded()) {
                                result.setIsSucceeded(true);
                                result.getMessages().add(response.getString("message").trim());
                            } else {
                                JSONArray errors = response.getJSONArray("errors");
                                int len = errors.length();
                                for (int i = 0; i < len; i++)
                                    result.getMessages().add(errors.getString(i));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d("UserApi", response.toString());
                            result.setIsSucceeded(false);
                            result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.unknown_response_format));
                        }

                        if (listener != null)
                            listener.onExecuted(result);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        result.setIsSucceeded(false);
                        result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.unknown_error_has_occurred));
                        if (listener != null)
                            listener.onExecuted(result);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        result.setIsSucceeded(false);
                        result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.no_internet_connection));
                        if (listener != null)
                            listener.onExecuted(result);
                    }
                }
        );
    }

    public static void get(@Nullable final String id, final ApiListeners.OnItemLoadedListener listener) {

        final RequestParams requestParams = new RequestParams();
        if (id != null)
            requestParams.put("id", id);

        AuthApi.getAccessToken(new ApiListeners.OnActionExecutedListener() {
            @Override
            public void onExecuted(final Result result) {

                if (result.isSucceeded()) {
                    result.getMessages().clear();

                    Header[] headers = new BasicHeader[]{
                            new BasicHeader("Authorization", "Bearer " + User.getInstance().getAccessToken())
                    };

                    Communicator.getInstance().get(
                            "user",
                            requestParams,
                            new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    try {

                                        if (response.getString("success").trim().equals("1")) {
                                            result.setIsSucceeded(true);
                                            /**
                                             * fill in the user's info here:
                                             */
                                            listener.onLoaded(result, new UserModel(response.getJSONObject("user")));

                                        } else {
                                            result.setIsSucceeded(false);
                                            result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.an_error_occurred_while_getting_user_info));
                                            listener.onLoaded(result, null);
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        result.setIsSucceeded(false);
                                        result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.unknown_response_format));
                                        listener.onLoaded(result, null);
                                    }

                                }
                            },
                            "user_get",
                            headers
                    );

                } else {
                    listener.onLoaded(result, null);
                }

            }
        });
    }

    public static void stories(@Nullable String id, @Nullable String sinceId, @Nullable String maxId, final ApiListeners.OnItemsArrayLoadedListener listener) {

        RequestParams requestParams = new RequestParams();
        if (id != null) requestParams.put("user_id", id);
        if (sinceId != null) requestParams.put("since_id", sinceId);
        if (maxId != null) requestParams.put("max_id", maxId);

        Stub.get(
                "user/stories",
                listener,
                new Stub.ModelParser() {
                    @Override
                    ArrayList<Model> extractArray(JSONObject jsonObject) throws JSONException {
                        JSONArray jsonArray = jsonObject.getJSONArray("stories");
                        ArrayList<Model> out = new ArrayList<>();
                        int len = jsonArray.length();
                        for (int i = 0; i < len; i++)
                            out.add(new Story(jsonArray.getJSONObject(i)));
                        return out;
                    }
                },
                requestParams,
                "user_stories"
        );

    }

    public static void photos(@Nullable String id, @Nullable String sinceId, @Nullable String maxId, final ApiListeners.OnItemsArrayLoadedListener listener) {
        RequestParams requestParams = new RequestParams();
        if (id != null) requestParams.put("user_id", id);
        if (sinceId != null) requestParams.put("since_id", sinceId);
        if (maxId != null) requestParams.put("max_id", maxId);

        Stub.get(
                "user/photos",
                listener,
                new Stub.ModelParser() {
                    @Override
                    ArrayList<Model> extractArray(JSONObject jsonObject) throws JSONException {
                        JSONArray jsonArray = jsonObject.getJSONArray("photos");
                        ArrayList<Model> out = new ArrayList<>();
                        int len = jsonArray.length();
                        for (int i = 0; i < len; i++)
                            out.add(new Photo(jsonArray.getJSONObject(i)));
                        return out;
                    }
                },
                requestParams,
                "user_photos"
        );
    }

    public static void follow(String id, ApiListeners.OnActionExecutedListener listener) {
        Communicator.getInstance().cancelByTag("user_follow");
        Communicator.getInstance().cancelByTag("user_request");
        Communicator.getInstance().cancelByTag("user_unfollow");
        Stub.post("user/follow", listener, null, new RequestParams("id", id), "user_follow");
    }

    public static void request(String id, ApiListeners.OnActionExecutedListener listener) {
        Communicator.getInstance().cancelByTag("user_follow");
        Communicator.getInstance().cancelByTag("user_request");
        Communicator.getInstance().cancelByTag("user_unfollow");
        Stub.post("user/request-follow", listener, null, new RequestParams("id", id), "user_request");
    }

    public static void unfollow(String id, ApiListeners.OnActionExecutedListener listener) {
        Communicator.getInstance().cancelByTag("user_follow");
        Communicator.getInstance().cancelByTag("user_request");
        Communicator.getInstance().cancelByTag("user_unfollow");
        Stub.post("user/un-follow", listener, null, new RequestParams("id", id), "user_unfollow");
    }

    public static void cancel(String followId, ApiListeners.OnActionExecutedListener listener) {
        Stub.post("user/cancel-follow-request", listener, null, new RequestParams("followed_id", followId), "user_cancel_follow");
    }

    public static void block(String id, ApiListeners.OnActionExecutedListener listener) {
        Stub.post("user/block", listener, null, new RequestParams("id", id), "user_block");
    }

    public static void unblock(String id, ApiListeners.OnActionExecutedListener listener) {
        Stub.post("user/unblock", listener, null, new RequestParams("id", id), "user_block");
    }

    public static void report(String id, ApiListeners.OnActionExecutedListener listener) {
        Stub.post("user/report", listener, null, new RequestParams("id", id), "user_block");
    }

    public static void changePassword(String oldPassword, String newPassword, String confirmPassword, ApiListeners.OnActionExecutedListener listener) {
        RequestParams requestParams = new RequestParams();
        requestParams.put("old_password", oldPassword);
        requestParams.put("new_password", newPassword);
        requestParams.put("confirm_password", confirmPassword);
        Stub.post("user/update-password", listener, null, requestParams, "user_change_password");
    }

    public static void makePrivate(String isPrivate, ApiListeners.OnActionExecutedListener listener) {
        Stub.post("user/update-privacy", listener, null, new RequestParams("private", isPrivate), "make_private");
    }

    public static void update(String name, File photo, String bio, String website, String mobile, String gender, ApiListeners.OnItemLoadedListener listener) {
        try {
            User u = User.getInstance();
            RequestParams params = new RequestParams();
            params.put("username", u.getUsername());
            params.put("name", name);
            if (photo != null) params.put("photo", photo);
            params.put("bio", bio);
            params.put("website", website);
            params.put("mobile", mobile);
            if (gender != null) params.put("gender", gender);
            params.put("email", u.getEmail());
            Stub.post("user/update-profile", listener, new Stub.ModelParser() {
                @Override
                Model parseItem(JSONObject jsonObject) throws JSONException {
                    return new UserModel(jsonObject.getJSONObject("user"));
                }
            }, params, "user_update");

        } catch (IOException e) {
            e.printStackTrace();
            Result r = new Result();
            r.setIsSucceeded(false);
            r.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.error_loading_the_photo));
        }
    }

    public static void followers(@Nullable String id, @Nullable String maxId, @Nullable String sinceId, ApiListeners.OnItemsArrayLoadedListener listener) {
        RequestParams params = new RequestParams();
        if (id != null) params.put("user_id", id);
        if (maxId != null) params.put("max_id", maxId);
        if (sinceId != null) params.put("since_id", sinceId);
        Stub.get("user/followers", listener, new Stub.ModelParser() {
            @Override
            ArrayList<Model> extractArray(JSONObject jsonObject) throws JSONException {
                JSONArray jsonArray = jsonObject.getJSONArray("followers");
                ArrayList<Model> items = new ArrayList<>();
                int len = jsonArray.length();
                for (int i = 0; i < len; i++)
                    items.add(new Followship(jsonArray.getJSONObject(i)));

                return items;
            }
        }, params, "user_followers");
    }

    public static void followings(@Nullable String id, @Nullable String maxId, @Nullable String sinceId, ApiListeners.OnItemsArrayLoadedListener listener) {
        RequestParams params = new RequestParams();
        if (id != null) params.put("user_id", id);
        if (maxId != null) params.put("max_id", maxId);
        if (sinceId != null) params.put("since_id", sinceId);
        Stub.get("user/following", listener, new Stub.ModelParser() {
            @Override
            ArrayList<Model> extractArray(JSONObject jsonObject) throws JSONException {
                JSONArray jsonArray = jsonObject.getJSONArray("following");
                ArrayList<Model> items = new ArrayList<>();
                int len = jsonArray.length();
                for (int i = 0; i < len; i++)
                    items.add(new Followship(jsonArray.getJSONObject(i)));

                return items;
            }
        }, params, "user_followings");
    }

    public static void blocked(ApiListeners.OnItemsArrayLoadedListener listener) {
        Stub.get("user/block-list", listener, new Stub.ModelParser() {
            @Override
            ArrayList<Model> extractArray(JSONObject jsonObject) throws JSONException {
                ArrayList<Model> items = new ArrayList<>();
                JSONArray jsonArray = jsonObject.getJSONArray("users");
                int len = jsonArray.length();
                for (int i = 0; i < len; i++)
                    items.add(new UserModel(jsonArray.getJSONObject(i)));
                return items;
            }
        }, new RequestParams(), "user_blocked");
    }

    public static void timeline(@Nullable String maxId, @Nullable String sinceId, ApiListeners.OnItemsArrayLoadedListener listener) {
        RequestParams params = new RequestParams();
        if (maxId != null) params.put("max_id", maxId);
        if (sinceId != null) params.put("since_id", sinceId);

        Stub.get(
                "user/timeline",
                listener,
                new Stub.ModelParser() {
                    @Override
                    ArrayList<Model> extractArray(JSONObject jsonObject) throws JSONException {
                        ArrayList<Model> items = new ArrayList<>();
                        JSONArray jsonArray = jsonObject.getJSONArray("timeline");
                        int len = jsonArray.length();
                        for (int i = 0; i < len; i++)
                            items.add(new Timeline(jsonArray.getJSONObject(i)));
                        return items;
                    }
                },
                params,
                "user_timeline"
        );
    }

    public static void search(String username, ApiListeners.OnItemsArrayLoadedListener listener) {
        Communicator.getInstance().cancelByTag("user_search");
        RequestParams params = new RequestParams("username", username);
        Stub.get("user/search", listener, new Stub.ModelParser() {
            @Override
            ArrayList<Model> extractArray(JSONObject jsonObject) throws JSONException {
                JSONArray jsonArray = jsonObject.getJSONArray("users");
                int len = jsonArray.length();
                ArrayList<Model> items = new ArrayList<>();
                for (int i = 0; i < len; i++)
                    items.add(new UserModel(jsonArray.getJSONObject(i)));

                return items;
            }
        }, params, "user_search");
    }

    public static void notifications(@Nullable String unseen, @Nullable String maxId, @Nullable String sinceId, ApiListeners.OnItemsArrayLoadedListener listener) {
        RequestParams params = new RequestParams();
        if (maxId != null) params.put("max_id", maxId);
        if (sinceId != null) params.put("since_id", sinceId);
        if (unseen != null) params.put("unseen", unseen);

        Stub.get(
                "user/get-notifications",
                listener,
                new Stub.ModelParser() {
                    @Override
                    ArrayList<Model> extractArray(JSONObject jsonObject) throws JSONException {
                        ArrayList<Model> items = new ArrayList<>();
                        JSONArray jsonArray = jsonObject.getJSONArray("notifications");
                        int len = jsonArray.length();
                        for (int i = 0; i < len; i++)
                            items.add(new Notification(jsonArray.getJSONObject(i)));
                        return items;
                    }
                },
                params,
                "user_notification"
        );
    }

    public static void reject(String userId, ApiListeners.OnActionExecutedListener listener) {
        Stub.post("user/delete-follow-request", listener, new Stub.ModelParser(), new RequestParams("follower_id", userId), "user_reject");
    }

    public static void accept(String userId, ApiListeners.OnActionExecutedListener listener) {
        Stub.post("user/accept-follow-request", listener, new Stub.ModelParser(), new RequestParams("follower_id", userId), "user_accept");
    }

    public static void seenNotifications(ApiListeners.OnActionExecutedListener listener) {
        Stub.post("user/see-all-notifications", listener, new Stub.ModelParser(), new RequestParams(), "user_seen_notifications");
    }

}
