package com.photostalk.models;

import com.photostalk.utils.MiscUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mohammed on 3/1/16.
 */
public class Comment extends Model {

    private UserModel mUser;

    private String mPhotoId;
    private String mAudioUrl;
    private String mAudioTime;
    private Date mCreatedAt;
    private boolean mIsAuthor;

    public Comment(JSONObject jsonObject) {

        setId(MiscUtils.getString(jsonObject, "id", ""));
        setPhotoId(MiscUtils.getString(jsonObject, "photo_id", ""));
        setAudioUrl(MiscUtils.getString(jsonObject, "audio", ""));
        setAudioTime(MiscUtils.getString(jsonObject, "audio_time", ""));
        setCreatedAtAsString(MiscUtils.getString(jsonObject, "created_at", ""));
        setIsAuthor(MiscUtils.getBoolean(jsonObject, "is_author", false));

        try {
            jsonObject = jsonObject.getJSONObject("user");
            mUser = new UserModel(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getPhotoId() {
        return mPhotoId;
    }

    public void setPhotoId(String photoId) {
        mPhotoId = photoId;
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

    public Date getCreatedAt() {
        return mCreatedAt;
    }

    public void setCreatedAt(Date createdAt) {
        mCreatedAt = createdAt;
    }

    public String getCreatedAtAsString(String format) {
        return new SimpleDateFormat(format).format(mCreatedAt);
    }

    public void setCreatedAtAsString(String createdAt) {
        try {
            mCreatedAt = new SimpleDateFormat("yyyy-MM-dd").parse(createdAt);
        } catch (ParseException e) {
            e.printStackTrace();
        }
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

    @Override
    public void copyFrom(Model model) {
        Comment comment = ((Comment) model);
        setId(comment.getId());
        setPhotoId(comment.getPhotoId());
        setAudioUrl(comment.getAudioUrl());
        setAudioTime(comment.getAudioTime());
        setCreatedAt(comment.getCreatedAt());
        setIsAuthor(comment.isAuthor());
        mUser.copyFrom(comment.getUser());
    }
}
