package com.photostalk.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.photostalk.PhotosTalkApplication;
import com.photostalk.models.AccessToken;
import com.photostalk.models.Model;
import com.photostalk.models.UserModel;
import com.photostalk.services.Result;
import com.photostalk.services.UserApi;
import com.photostalk.utils.ApiListeners;

/**
 * Created by mohammed on 2/28/16.
 */
public class User {

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
        mAccessTokenExpiresAt = System.currentTimeMillis() + accessToken.getExpiration() - 10000;

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
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("com.photostalk", Context.MODE_PRIVATE);
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
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("com.photostalk", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("accessToken", mIsLoggedIn ? mAccessToken : "");
        editor.putString("refreshToken", mIsLoggedIn ? mRefreshToken : "");
        editor.putLong("accessTokenExpiresAt", mIsLoggedIn ? mAccessTokenExpiresAt : 0);
        editor.putString("id", mIsLoggedIn ? mId : "");
        editor.putString("username", mIsLoggedIn ? mUsername : "");
        editor.putString("name", mIsLoggedIn ? mName : "");
        editor.putString("email", mIsLoggedIn ? mEmail : "");
        editor.putString("photo", mIsLoggedIn ? mPhoto : "");
        editor.putString("website", mIsLoggedIn ? mWebsite : "");
        editor.putString("bio", mIsLoggedIn ? mBio : "");
        editor.putString("gender", mIsLoggedIn ? mGender : "");
        editor.putString("mobile", mIsLoggedIn ? mMobile : "");
        editor.putBoolean("private", mIsPrivate);
        editor.putBoolean("isLoggedIn", mIsLoggedIn);
        editor.apply();
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
}
