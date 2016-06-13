package com.photostalk.models;

import com.photostalk.utils.MiscUtils;

import org.json.JSONObject;

/**
 * Created by mohammed on 2/28/16.
 */
public class UserModel extends Model {
    private String mUsername;
    private String mName;
    private String mEmail;
    private String mPhoto;
    private String mWebsite;
    private String mBio;
    private String mGender;
    private String mMobile;
    private String mCountry; //id
    private String mCity;
    private boolean mIsPrivate;
    private boolean mIsVerified;
    private int mFollowersCount;
    private int mFollowingCount;
    private int mStoriesCount;
    private int mPhotosCount;
    private boolean mIsFollowingUser;
    private boolean mIsFollowRequestSent;
    private boolean mIsBlocked;

    public UserModel() {

    }

    public UserModel(JSONObject jsonObject) {
        setId(MiscUtils.getString(jsonObject, "id", ""));
        setUsername(MiscUtils.getString(jsonObject, "username", ""));
        setName(MiscUtils.getString(jsonObject, "name", ""));
        setPhoto(MiscUtils.getString(jsonObject, "photo", ""));
        setBio(MiscUtils.getString(jsonObject, "bio", ""));
        setWebsite(MiscUtils.getString(jsonObject, "website", ""));
        setMobile(MiscUtils.getString(jsonObject, "mobile", ""));
        setCountry(MiscUtils.getString(jsonObject, "country", ""));
        setCity(MiscUtils.getString(jsonObject, "city", ""));
        setGender(MiscUtils.getString(jsonObject, "gender", ""));
        setIsPrivate(MiscUtils.getBoolean(jsonObject, "private", false));
        setIsVerified(MiscUtils.getBoolean(jsonObject, "verified", false));
        setFollowersCount(MiscUtils.getInt(jsonObject, "followers_count", 0));
        setFollowingCount(MiscUtils.getInt(jsonObject, "following_count", 0));
        setStoriesCount(MiscUtils.getInt(jsonObject, "stories_count", 0));
        setPhotosCount(MiscUtils.getInt(jsonObject, "photos_count", 0));
        setIsFollowingUser(MiscUtils.getBoolean(jsonObject, "is_following_user", false));
        setIsFollowRequestSent(MiscUtils.getBoolean(jsonObject, "is_follow_request_sent", false));
        setIsBlocked(MiscUtils.getBoolean(jsonObject, "is_blocked", false));
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

    public String getCountry() {
        return mCountry;
    }

    public void setCountry(String country) {
        mCountry = country;
    }

    public String getCity() {
        return mCity;
    }

    public void setCity(String city) {
        mCity = city;
    }

    public boolean isPrivate() {
        return mIsPrivate;
    }

    public void setIsPrivate(boolean isPrivate) {
        mIsPrivate = isPrivate;
    }

    public boolean isVerified() {
        return mIsVerified;
    }

    public void setIsVerified(boolean isVerfied) {
        mIsVerified = isVerfied;
    }

    public int getFollowersCount() {
        return mFollowersCount;
    }

    public void setFollowersCount(int followersCount) {
        mFollowersCount = followersCount;
    }

    public int getFollowingCount() {
        return mFollowingCount;
    }

    public void setFollowingCount(int followingCount) {
        mFollowingCount = followingCount;
    }

    public int getStoriesCount() {
        return mStoriesCount;
    }

    public void setStoriesCount(int storiesCount) {
        mStoriesCount = storiesCount;
    }

    public int getPhotosCount() {
        return mPhotosCount;
    }

    public void setPhotosCount(int photosCount) {
        mPhotosCount = photosCount;
    }

    public boolean isFollowingUser() {
        return mIsFollowingUser;
    }

    public void setIsFollowingUser(boolean isFollowingUser) {
        mIsFollowingUser = isFollowingUser;
    }

    public boolean isFollowRequestSent() {
        return mIsFollowRequestSent;
    }

    public void setIsFollowRequestSent(boolean isFollowRequestSent) {
        mIsFollowRequestSent = isFollowRequestSent;
    }

    public boolean isBlocked() {
        return mIsBlocked;
    }

    public void setIsBlocked(boolean isBlocked) {
        mIsBlocked = isBlocked;
    }

    @Override
    public void copyFrom(Model model) {
        UserModel other = ((UserModel) model);
        setId(other.getId());
        setUsername(other.getUsername());
        setName(other.getName());
        setEmail(other.getEmail());
        setPhoto(other.getPhoto());
        setWebsite(other.getWebsite());
        setBio(other.getBio());
        setGender(other.getGender());
        setMobile(other.getMobile());
        setIsPrivate(other.isPrivate());
        setIsVerified(other.isVerified());
        setFollowersCount(other.getFollowersCount());
        setFollowingCount(other.getFollowingCount());
        setStoriesCount(other.getStoriesCount());
        setPhotosCount(other.getPhotosCount());
        setIsFollowingUser(other.isFollowingUser());
        setIsFollowRequestSent(other.isFollowRequestSent());
        setIsBlocked(other.isBlocked());
    }

}
