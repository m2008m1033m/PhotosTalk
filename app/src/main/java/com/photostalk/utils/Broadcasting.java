package com.photostalk.utils;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.photostalk.PhotosTalkApplication;


public class Broadcasting {

    public static final String FOLLOW = "follow";
    public static final String BLOCK = "block";
    public static final String STORY_DELETE = "story_delete";
    public static final String PHOTO_DELETE = "photo_delete";
    public static final String PHOTO_POSTED = "terminate_camera";
    public static final String LOGOUT = "logout";
    public static final String COMMENT_DELETE = "comment_delete";
    public static final String PROFILE_UPDATED = "profile_updated";
    public static final String EXPIRED_REFRESH_TOKEN = "expired_refresh_token";

    public static void sendFollow(String userId, boolean request, boolean follow) {
        Intent intent = new Intent(FOLLOW);
        intent.putExtra("user_id", userId);
        intent.putExtra("request", request);
        intent.putExtra("follow", follow);
        LocalBroadcastManager.getInstance(PhotosTalkApplication.getContext()).sendBroadcast(intent);
    }


    public static void sendBlock(String userId, boolean block) {
        Intent intent = new Intent(BLOCK);
        intent.putExtra("user_id", userId);
        intent.putExtra("block", block);
        LocalBroadcastManager.getInstance(PhotosTalkApplication.getContext()).sendBroadcast(intent);
    }

    public static void sendPhotoDelete(String photoId) {
        Intent intent = new Intent(PHOTO_DELETE);
        intent.putExtra("photo_id", photoId);
        LocalBroadcastManager.getInstance(PhotosTalkApplication.getContext()).sendBroadcast(intent);
    }

    public static void sendStoryDelete(String storyId, String userId) {
        Intent intent = new Intent(STORY_DELETE);
        intent.putExtra("story_id", storyId);
        intent.putExtra("user_id", userId);
        LocalBroadcastManager.getInstance(PhotosTalkApplication.getContext()).sendBroadcast(intent);
    }

    public static void sendPhotoPosted() {
        Intent intent = new Intent(PHOTO_POSTED);
        LocalBroadcastManager.getInstance(PhotosTalkApplication.getContext()).sendBroadcast(intent);
    }

    public static void sendLogout() {
        Intent intent = new Intent(LOGOUT);
        LocalBroadcastManager.getInstance(PhotosTalkApplication.getContext()).sendBroadcast(intent);
    }

    public static void sendProfileUpdated() {
        Intent intent = new Intent(PROFILE_UPDATED);
        LocalBroadcastManager.getInstance(PhotosTalkApplication.getContext()).sendBroadcast(intent);
    }

    public static void sendCommentDelete(String commentId) {
        Intent intent = new Intent(COMMENT_DELETE);
        intent.putExtra("comment_id", commentId);
        LocalBroadcastManager.getInstance(PhotosTalkApplication.getContext()).sendBroadcast(intent);
    }

    public static void sendExpiredRefreshToken() {
        Intent intent = new Intent(EXPIRED_REFRESH_TOKEN);
        LocalBroadcastManager.getInstance(PhotosTalkApplication.getContext()).sendBroadcast(intent);
    }

}
