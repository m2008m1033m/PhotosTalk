package com.photostalk.models;

import com.photostalk.utils.MiscUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by mohammed on 2/29/16.
 */
public class Photo extends Model {

    private ArrayList<String> mHashTags = new ArrayList<>();
    private ArrayList<Comment> mComments = new ArrayList<>();
    private UserModel mUser;
    private Story mStory;

    private String mDescription;
    private String mImageUrl;
    private String mAudioUrl;
    private String mAudioTime;
    private boolean mIsLive;
    private int mCommentsCount;
    private int mViewsCount;
    private int mLikesCount;
    private Date mCreatedAt;
    private String mShareUrl;
    private boolean mIsAuthor;
    private boolean mIsLiked;

    public Photo() {
        mStory = new Story();
        mUser = new UserModel();
    }

    public Photo(JSONObject jsonObject) {
        setId(MiscUtils.getString(jsonObject, "id", ""));
        setDescription(MiscUtils.getString(jsonObject, "description", ""));
        setImageUrl(MiscUtils.getString(jsonObject, "image", ""));
        setAudioUrl(MiscUtils.getString(jsonObject, "audio", ""));
        setAudioTime(MiscUtils.getString(jsonObject, "audio_time", ""));
        setIsLive(MiscUtils.getString(jsonObject, "is_live", "").equals("1"));
        setCommentsCount(MiscUtils.getInt(jsonObject, "comments_count", 0));
        setViewsCount(MiscUtils.getInt(jsonObject, "views_count", 0));
        setLikesCount(MiscUtils.getInt(jsonObject, "likes_count", 0));
        setCreatedAtAsString(MiscUtils.getString(jsonObject, "created_at", ""));
        setShareUrl(MiscUtils.getString(jsonObject, "share_url", ""));
        setIsAuthor(MiscUtils.getBoolean(jsonObject, "is_author", false));
        setIsLiked(MiscUtils.getBoolean(jsonObject, "is_liked", false));

        try {
            if (!jsonObject.getString("hashtags").trim().equals("null")) {
                JSONArray hashTags = jsonObject.getJSONArray("hashtags");
                for (int i = 0; i < hashTags.length(); i++)
                    getHashTags().add(hashTags.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject jObj;

        /**
         * populating the story
         */
        try {
            jObj = jsonObject.getJSONObject("story");
            mStory = new Story(jObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /**
         * populating the user
         */
        try {
            jObj = jsonObject.getJSONObject("user");
            mUser = new UserModel(jObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Story getStory() {
        return mStory;
    }

    public void setStory(Story story) {
        mStory = story;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        mImageUrl = imageUrl;
    }

    public String getAudioUrl() {
        return mAudioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        mAudioUrl = audioUrl;
    }

    public String getAudioTime() {
        return mAudioTime;
    }

    public void setAudioTime(String audioTime) {
        mAudioTime = audioTime;
    }

    public boolean isLive() {
        return mIsLive;
    }

    public void setIsLive(boolean isLive) {
        mIsLive = isLive;
    }

    public int getCommentsCount() {
        return mCommentsCount;
    }

    public void setCommentsCount(int commentsCount) {
        mCommentsCount = commentsCount;
    }

    public int getViewsCount() {
        return mViewsCount;
    }

    public void setViewsCount(int viewsCount) {
        mViewsCount = viewsCount;
    }

    public int getLikesCount() {
        return mLikesCount;
    }

    public void setLikesCount(int likesCount) {
        mLikesCount = likesCount;
    }

    public ArrayList<String> getHashTags() {
        return mHashTags;
    }

    public void setHashTags(ArrayList<String> hashTags) {
        mHashTags = hashTags;
    }

    public String getCreatedAtAsString() {
        return new SimpleDateFormat("dd-mm-yyyy").format(mCreatedAt);
    }

    public void setCreatedAtAsString(String createdAt) {
        try {
            mCreatedAt = new SimpleDateFormat("yyyy-MM-dd").parse(createdAt);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public Date getCreatedAt() {
        return mCreatedAt;
    }

    public void setCreatedAt(Date createdAt) {
        mCreatedAt = createdAt;
    }

    public String getShareUrl() {
        return mShareUrl;
    }

    public void setShareUrl(String shareUrl) {
        mShareUrl = shareUrl;
    }

    public boolean isAuthor() {
        return mIsAuthor;
    }

    public void setIsAuthor(boolean isAuthor) {
        mIsAuthor = isAuthor;
    }

    public String getHashTagsConcatenated() {
        String hashTagStr = "";
        for (String hashTag : mHashTags)
            hashTagStr += "#" + hashTag + " ";
        return hashTagStr;
    }

    public UserModel getUser() {
        return mUser;
    }

    public ArrayList<Comment> getComments() {
        return mComments;
    }

    public boolean isLiked() {
        return mIsLiked;
    }

    public void setIsLiked(boolean isLiked) {
        mIsLiked = isLiked;
    }

    @Override
    public void copyFrom(Model model) {
        Photo other = ((Photo) model);
        setId(other.getId());
        setDescription(other.getDescription());
        setImageUrl(other.getImageUrl());
        setAudioUrl(other.getAudioUrl());
        setAudioTime(other.getAudioTime());
        setIsLive(other.isLive());
        setCommentsCount(other.getCommentsCount());
        setViewsCount(other.getViewsCount());
        setLikesCount(other.getLikesCount());
        setCreatedAt(other.getCreatedAt());
        setShareUrl(other.getShareUrl());
        setIsAuthor(other.isAuthor());
        setIsLiked(other.isLiked());
        mHashTags = other.mHashTags;
        mStory.copyFrom(other.getStory());
        mUser.copyFrom(other.getUser());
    }
}
