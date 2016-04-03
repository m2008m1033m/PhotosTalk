package com.photostalk.services;

import android.support.annotation.Nullable;

import com.loopj.android.http.RequestParams;
import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.core.Communicator;
import com.photostalk.models.Comment;
import com.photostalk.models.Model;
import com.photostalk.models.Photo;
import com.photostalk.utils.ApiListeners;
import com.photostalk.utils.MiscUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by mohammed on 2/19/16.
 */
public class PhotosApi {
    public static void add(final File photo, final File audio, final String audioDuration, final String description, final String isLive, final ApiListeners.OnItemLoadedListener listener) {

        try {

            /**
             * add hash tags to the description:
             */
            String finalDescription = MiscUtils.convertStringToHashTag(description);

            final RequestParams requestParams = new RequestParams();
            requestParams.put("image", photo);
            if (audio != null) requestParams.put("audio", audio);
            if (audio != null) requestParams.put("audio_time", audioDuration);
            requestParams.put("description", finalDescription);
            requestParams.put("is_live", isLive);


            Stub.post("photo/add", listener, new Stub.ModelParser() {
                @Override
                Model parseItem(JSONObject jsonObject) throws JSONException {
                    Photo photo = new Photo();
                    photo.setId(MiscUtils.getString(jsonObject, "photo_id", ""));
                    photo.getStory().setId(MiscUtils.getString(jsonObject, "story_id", ""));
                    return photo;
                }
            }, requestParams, "photo_add");

        } catch (IOException e) {
            Result result = new Result();
            result.setIsSucceeded(false);
            result.getMessages().clear();
            result.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.error_loading_files));
            listener.onLoaded(result, null);
        }
    }

    public static void get(final String photoId, final ApiListeners.OnItemLoadedListener listener) {
        final RequestParams params = new RequestParams("id", photoId);
        Stub.get("photo/get", listener, new Stub.ModelParser() {
            @Override
            Model parseItem(JSONObject jsonObject) throws JSONException {
                return new Photo(jsonObject.getJSONObject("photo"));
            }
        }, params, "photo_get");
    }

    public static void like(final String photoId, boolean like, final ApiListeners.OnActionExecutedListener listener) {
        Stub.post(
                like ? "photo/like" : "photo/unlike",
                listener,
                null,
                new RequestParams("id", photoId),
                "photo_like"
        );
    }

    public static void comment(File audio, String photoId, String audioDuration, ApiListeners.OnActionExecutedListener listener) {
        try {
            RequestParams params = new RequestParams();
            params.put("photo_id", photoId);
            params.put("audio", audio);
            params.put("audio_time", audioDuration);


            Stub.post(
                    "comment/add",
                    listener,
                    null,
                    params,
                    "photo_comment"
            );

        } catch (IOException e) {
            Result r = new Result();
            r.setIsSucceeded(false);
            r.getMessages().add(PhotosTalkApplication.getContext().getString(R.string.could_not_load_the_recording_file));

        }
    }

    public static void comments(String photoId, ApiListeners.OnItemsArrayLoadedListener listener, @Nullable String maxId, @Nullable String sinceId) {

        RequestParams params = new RequestParams("photo_id", photoId);
        if (maxId != null) params.put("max_id", maxId);
        if (sinceId != null) params.put("since_id", sinceId);

        Stub.get(
                "photo/comments",
                listener,
                new Stub.ModelParser() {
                    @Override
                    ArrayList<Model> extractArray(JSONObject jsonObject) throws JSONException {
                        ArrayList<Model> comments = new ArrayList<>();
                        JSONArray jsonArray = jsonObject.getJSONArray("comments");
                        int len = jsonArray.length();
                        for (int i = 0; i < len; i++)
                            comments.add(new Comment(jsonArray.getJSONObject(i)));
                        return comments;
                    }
                },
                params,
                "photo_comments"
        );
    }

    public static void report(String photoId, ApiListeners.OnActionExecutedListener listener) {
        Stub.post("photo/report", listener, null, new RequestParams("id", photoId), "photo_report");
    }

    public static void delete(String photoId, ApiListeners.OnActionExecutedListener listener) {
        Stub.post("photo/delete", listener, null, new RequestParams("id", photoId), "photo_report");
    }

    public static void deleteComment(String photoId, String commentId, ApiListeners.OnActionExecutedListener listener) {
        RequestParams requestParams = new RequestParams();
        requestParams.put("photo_id", photoId);
        requestParams.put("comment_id", commentId);
        Stub.post("comment/delete", listener, null, requestParams, "photo_deleteComment");
    }

    public static void searchHashtag(String hashtag, @Nullable String maxId, @Nullable String sinceId, ApiListeners.OnItemsArrayLoadedListener listener) {
        Communicator.getInstance().cancelByTag("user_search");
        RequestParams params = new RequestParams("hashtag", hashtag);
        if (maxId != null) params.put("max_id", maxId);
        if (sinceId != null) params.put("since_id", sinceId);
        Stub.get("photo/hashtag", listener, new Stub.ModelParser() {
            @Override
            ArrayList<Model> extractArray(JSONObject jsonObject) throws JSONException {
                ArrayList<Model> items = new ArrayList<>();
                JSONArray jsonArray = jsonObject.getJSONArray("photos");
                int len = jsonArray.length();
                for (int i = 0; i < len; i++)
                    items.add(new Photo(jsonArray.getJSONObject(i)));

                return items;
            }
        }, params, "photo_hashtag");

    }
}
