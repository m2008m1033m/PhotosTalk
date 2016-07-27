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
 * Created by mohammed on 2/18/16.
 */
public class Story extends Model {

    private String mImageUrl;
    private int mPhotosCount;
    private int mViewCount;
    private Date mStoryDate;
    private String mShareUrl;
    boolean mIsAuthor;
    private UserModel mUser;

    private ArrayList<Photo> mPhotos = new ArrayList<>();

    public Story() {
        mUser = new UserModel();
    }

    public Story(JSONObject jsonObject) {
        /**
         * populating the story
         */
        setId(MiscUtils.getString(jsonObject, "id", ""));
        setImageUrl(MiscUtils.getString(jsonObject, "image", ""));
        setPhotosCount(MiscUtils.getInt(jsonObject, "photos_count", 0));
        setViewCount(MiscUtils.getInt(jsonObject, "views_count", 0));
        setStoryDateAsString(MiscUtils.getString(jsonObject, "story_date", ""));
        setShareUrl(MiscUtils.getString(jsonObject, "share_url", ""));

        try {
            mUser = new UserModel(jsonObject.getJSONObject("user"));
        } catch (JSONException e) {
            mUser = new UserModel();
            e.printStackTrace();
        }

        try {
            JSONArray jsonArray = jsonObject.getJSONArray("photos");
            int len = jsonArray.length();
            for (int i = 0; i < len; i++)
                mPhotos.add(new Photo(jsonArray.getJSONObject(i)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        mImageUrl = imageUrl;
    }

    public int getPhotosCount() {
        return mPhotosCount;
    }

    public void setPhotosCount(int photosCount) {
        mPhotosCount = photosCount;
    }

    public int getViewCount() {
        return mViewCount;
    }

    public void setViewCount(int viewCount) {
        mViewCount = viewCount;
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

    public void setStoryDateAsString(String storyDate) {
        try {
            mStoryDate = new SimpleDateFormat("yyyy-MM-dd").parse(storyDate);
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

    public boolean isAuthor() {
        return mIsAuthor;
    }

    public void setIsAuthor(boolean isAuthor) {
        mIsAuthor = isAuthor;
    }

    public UserModel getUser() {
        return mUser;
    }

    public void setUser(UserModel user) {
        mUser = user;
    }

    public ArrayList<Photo> getPhotos() {
        return mPhotos;
    }

    public void setPhotos(ArrayList<Photo> photos) {
        mPhotos = photos;
    }

    @Override
    public void copyFrom(Model model) {
        Story other = ((Story) model);
        setId(other.getId());
        setImageUrl(other.getImageUrl());
        setPhotosCount(other.getPhotosCount());
        setViewCount(other.getViewCount());
        setStoryDate(other.getStoryDate());
        setShareUrl(other.getShareUrl());
        setIsAuthor(other.isAuthor());

        mUser.copyFrom(other.getUser());
        mPhotos = other.mPhotos;
    }
}
