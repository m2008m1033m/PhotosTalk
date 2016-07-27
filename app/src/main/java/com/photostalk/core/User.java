package com.photostalk.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.photostalk.PhotosTalkApplication;
import com.photostalk.apis.Result;
import com.photostalk.apis.UserApi;
import com.photostalk.models.AccessToken;
import com.photostalk.models.Model;
import com.photostalk.models.UserModel;
import com.photostalk.utils.ApiListeners;

/**
 * Created by mohammed on 2/28/16.
 */
public class User {

    private final static String SHARED_PREFS_FILE_NAME = "photostalk_settings";

    public static class Settings {
        public boolean getCommentNotifications() {
            return PhotosTalkApplication.getContext().getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE).getBoolean("comment_notification", true);
        }

        public void setCommentNotifications(boolean enable) {
            PhotosTalkApplication.getContext().getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE).edit().putBoolean("comment_notification", enable).apply();
        }

        public void setLikeNotifications(boolean enable) {
            PhotosTalkApplication.getContext().getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE).edit().putBoolean("like_notification", enable).apply();
        }

        public boolean getLikeNotifications() {
            return PhotosTalkApplication.getContext().getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE).getBoolean("like_notification", true);
        }

        public void setFollowNotifications(boolean enable) {
            PhotosTalkApplication.getContext().getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE).edit().putBoolean("follow_notification", enable).apply();
        }

        public boolean getFollowNotifications() {
            return PhotosTalkApplication.getContext().getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE).getBoolean("follow_notification", true);
        }

        public void setFollowRequestNotifications(boolean enable) {
            PhotosTalkApplication.getContext().getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE).edit().putBoolean("follow_request_notification", enable).apply();
        }

        public boolean getFollowRequestNotifications() {
            return PhotosTalkApplication.getContext().getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE).getBoolean("follow_request_notification", true);
        }

        public void setAcceptFollowRequestNotifications(boolean enable) {
            PhotosTalkApplication.getContext().getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE).edit().putBoolean("accept_follow_request_notification", enable).apply();
        }

        public boolean getAcceptFollowRequestNotifications() {
            return PhotosTalkApplication.getContext().getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE).getBoolean("accept_follow_request_notification", true);
        }

        public void putPhotoToCache(String photoUrl, String photoPath) {
            PhotosTalkApplication.getContext().getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE).edit().putString("photo_cache_" + photoUrl, photoPath).apply();
        }

        public String getPhotoFromCache(String photoUrl) {
            return PhotosTalkApplication.getContext().getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE).getString("photo_cache_" + photoUrl, null);
        }
    }

    private String mAccessToken;
    private String mRefreshToken;
    private long mAccessTokenExpiresAt;

    private String mId;
    private String mUsername;
    private String mName;
    private String mEmail;
    private String mPhoto;
    private String mWebsite;
    private String mBio;
    private String mGender;
    private String mMobile;
    private boolean mIsPrivate;

    private boolean mIsLoggedIn;

    private Context mContext;
    private Settings mSettings;

    private static User mInstance = null;

    public static User getInstance() {
        if (mInstance == null)
            mInstance = new User(PhotosTalkApplication.getContext());
        return mInstance;
    }

    private User(Context context) {
        mContext = context;
        mIsLoggedIn = false;
        checkLoggedIn();
    }

    public void login(AccessToken accessToken, final ApiListeners.OnActionExecutedListener listener) {
        /**
         * set to logged in since the access token is opbtained
         */
        mIsLoggedIn = true;

        /**
         * figure out the time in which the
         * access token expires:
         */
        mAccessTokenExpiresAt = (System.currentTimeMillis() / 1000) + accessToken.getExpiration() - 10;

        mAccessToken = accessToken.getAccessToken();
        mRefreshToken = accessToken.getRefreshToken();


        /**
         * get the user info and populate it
         */
        UserApi.get(null, new ApiListeners.OnItemLoadedListener() {
            @Override
            public void onLoaded(Result result, Model item) {

                if (result.isSucceeded()) {
                    UserModel um = ((UserModel) item);

                    setId(um.getId());
                    setUsername(um.getUsername());
                    setName(um.getName());
                    setEmail(um.getEmail());
                    setPhoto(um.getPhoto());
                    setWebsite(um.getWebsite());
                    setBio(um.getBio());
                    setGender(um.getGender());
                    setMobile(um.getMobile());
                    setIsPrivate(um.isPrivate());

                    /**
                     * once we get everything without problems
                     * we need to store these information in the
                     * shared preferences
                     */
                    fillSharedPrefs();

                    listener.onExecuted(result);
                } else {
                    listener.onExecuted(result);
                    mIsLoggedIn = false;
                }
            }
        });


    }

    public void logout() {
        /**
         * remove shared preferences
         */
        mIsLoggedIn = false;
        fillSharedPrefs();
    }

    public void update() {
        fillSharedPrefs();
    }

    public boolean isValidAccessToken() {
        return mAccessTokenExpiresAt > System.currentTimeMillis();
    }

    private void checkLoggedIn() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("photostalk_settings", Context.MODE_PRIVATE);
        mIsLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (mIsLoggedIn) {
            mAccessToken = sharedPreferences.getString("accessToken", "");
            mRefreshToken = sharedPreferences.getString("refreshToken", "");
            mAccessTokenExpiresAt = sharedPreferences.getLong("accessTokenExpiresAt", 0);
            mId = sharedPreferences.getString("id", "");
            mUsername = sharedPreferences.getString("username", "");
            mName = sharedPreferences.getString("name", "");
            mEmail = sharedPreferences.getString("email", "");
            mPhoto = sharedPreferences.getString("photo", "");
            mWebsite = sharedPreferences.getString("website", "");
            mBio = sharedPreferences.getString("bio", "");
            mGender = sharedPreferences.getString("gender", "");
            mMobile = sharedPreferences.getString("mobile", "");
            mIsPrivate = sharedPreferences.getBoolean("private", false);
        }
    }

    private void fillSharedPrefs() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(SHARED_PREFS_FILE_NAME, Context.MODE_PRIVATE);
        sharedPreferences
                .edit()
                .putString("accessToken", mIsLoggedIn ? mAccessToken : "")
                .putString("refreshToken", mIsLoggedIn ? mRefreshToken : "")
                .putLong("accessTokenExpiresAt", mIsLoggedIn ? mAccessTokenExpiresAt : 0)
                .putString("id", mIsLoggedIn ? mId : "")
                .putString("username", mIsLoggedIn ? mUsername : "")
                .putString("name", mIsLoggedIn ? mName : "")
                .putString("email", mIsLoggedIn ? mEmail : "")
                .putString("photo", mIsLoggedIn ? mPhoto : "")
                .putString("website", mIsLoggedIn ? mWebsite : "")
                .putString("bio", mIsLoggedIn ? mBio : "")
                .putString("gender", mIsLoggedIn ? mGender : "")
                .putString("mobile", mIsLoggedIn ? mMobile : "")
                .putBoolean("private", mIsPrivate)
                .putBoolean("isLoggedIn", mIsLoggedIn)
                .apply();
    }

    public long getAccessTokenExpiresAt() {
        return mAccessTokenExpiresAt;
    }

    public void setAccessTokenExpiresAt(long duration) {
        mAccessTokenExpiresAt = System.currentTimeMillis() + duration - 10000;
    }

    public String getRefreshToken() {
        return mRefreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        mRefreshToken = refreshToken;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String accessToken) {
        mAccessToken = accessToken;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        mUsername = username;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public String getPhoto() {
        return mPhoto;
    }

    public void setPhoto(String photo) {
        mPhoto = photo;
    }

    public String getWebsite() {
        return mWebsite;
    }

    public void setWebsite(String website) {
        mWebsite = website;
    }

    public String getBio() {
        return mBio;
    }

    public void setBio(String bio) {
        mBio = bio;
    }

    public String getGender() {
        return mGender;
    }

    public void setGender(String gender) {
        mGender = gender;
    }

    public String getMobile() {
        return mMobile;
    }

    public void setMobile(String mobile) {
        mMobile = mobile;
    }

    public boolean isLoggedIn() {
        return mIsLoggedIn;
    }

    public void setIsLoggedIn(boolean isLoggedIn) {
        mIsLoggedIn = isLoggedIn;
    }

    public boolean isPrivate() {
        return mIsPrivate;
    }

    public void setIsPrivate(boolean isPrivate) {
        mIsPrivate = isPrivate;
    }

    public Settings getSettings() {
        if (mSettings == null) mSettings = new Settings();
        return mSettings;
    }

}
