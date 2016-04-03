package com.photostalk.utils;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by mohammed on 3/7/16.
 */
public class Broadcasting {

    public static final String FOLLOW = "follow";
    public static final String BLOCK = "block";
    public static final String STORY_DELETE = "story_delete";
    public static final String PHOTO_DELETE = "photo_delete";
    public static final String TERMINATE_CAMERA = "terminate_camera";
    public static final String LOGOUT = "logout";
    public static final String COMMENT_DELETE = "comment_delete";
    public static final String PROFILE_UPDATED = "profile_updated";

    public static void sendFollow(Activity activity, String userId, boolean request, boolean follow) {
        Intent intent = new Intent(FOLLOW);
        intent.putExtra("user_id", userId);
        intent.putExtra("request", request);
        intent.putExtra("follow", follow);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }


    public static void sendBlock(Activity activity, String userId, boolean block) {
        Intent intent = new Intent(BLOCK);
        intent.putExtra("user_id", userId);
        intent.putExtra("block", block);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }

    public static void sendPhotoDelete(Activity activity, String photoId) {
        Intent intent = new Intent(PHOTO_DELETE);
        intent.putExtra("photo_id", photoId);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }

    public static void sendStoryDelete(Activity activity, String storyId, String userId) {
        Intent intent = new Intent(STORY_DELETE);
        intent.putExtra("story_id", storyId);
        intent.putExtra("user_id", userId);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }

    public static void sendTerminatCamera(Activity activity) {
        Intent intent = new Intent(TERMINATE_CAMERA);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }

    public static void sendLogout(Activity activity) {
        Intent intent = new Intent(LOGOUT);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }

    public static void sendProfileUpdated(Activity activity) {
        Intent intent = new Intent(PROFILE_UPDATED);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }

    public static void sendCommentDelete(Activity activity, String commentId) {
        Intent intent = new Intent(COMMENT_DELETE);
        intent.putExtra("comment_id", commentId);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }

}
