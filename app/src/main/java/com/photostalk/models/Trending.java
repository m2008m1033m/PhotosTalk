package com.photostalk.models;

import com.photostalk.utils.MiscUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mohammed on 3/7/16.
 */
public class Trending extends Model {

    private UserModel mUser;
    private Story mStory;

    private String mImage;
    private int mPhotosCount;
    private int mViewsCount;
    private Date mStoryDate;
    private String mShareUrl;

    public Trending(JSONObject jsonObject) {
        setId(MiscUtils.getString(jsonObject, "id", ""));
        setImage(MiscUtils.getString(jsonObject, "image", ""));
        setPhotosCount(MiscUtils.getInt(jsonObject, "photos_count", 0));
        setViewsCount(MiscUtils.getInt(jsonObject, "views_count", 0));
        setStoryDateFromString(MiscUtils.getString(jsonObject, "story_date", "1970-01-01"));
        setShareUrl(MiscUtils.getString(jsonObject, "share_url", ""));

        try {
            mUser = new UserModel(jsonObject.getJSONObject("user"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mStory = new Story();
        mStory.setId(mId);
        mStory.setImageUrl(mImage);
        mStory.setPhotosCount(mPhotosCount);
        mStory.setViewCount(mViewsCount);
        mStory.setStoryDate(mStoryDate);
        mStory.setShareUrl(mShareUrl);
        mStory.getUser().copyFrom(mUser);
    }

    public UserModel getUser() {
        return mUser;
    }

    public Story getStory() {
        return mStory;
    }

    public String getImage() {
        return mImage;
    }

    public void setImage(String image) {
        mImage = image;
    }

    public int getPhotosCount() {
        return mPhotosCount;
    }

    public void setPhotosCount(int photosCount) {
        mPhotosCount = photosCount;
    }

    public int getViewsCount() {
        return mViewsCount;
    }

    public void setViewsCount(int viewsCount) {
        mViewsCount = viewsCount;
    }

    public Date getStoryDate() {
        return mStoryDate;
    }

    public void setStoryDate(Date storyDate) {
        mStoryDate = storyDate;
    }

    public String getStoryDateAsString(String format) {
        return new SimpleDateFormat(format).format(mStoryDate);
    }

    public void setStoryDateFromString(String storyDate) {
        try {
            mStoryDate = new SimpleDateFormat("yyyy-mm-dd").parse(storyDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String getShareUrl() {
        return mShareUrl;
    }

    public void setShareUrl(String shareUrl) {
        mShareUrl = shareUrl;
    }

    @Override
    public void copyFrom(Model model) {
        Trending other = ((Trending) model);
        setImage(other.getId());
        setPhotosCount(other.getPhotosCount());
        setViewsCount(other.getViewsCount());
        setStoryDate(other.getStoryDate());
        setShareUrl(other.getShareUrl());
        mUser.copyFrom(other.getUser());
    }
}
