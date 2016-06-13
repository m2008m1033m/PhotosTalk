package com.photostalk.models;

import com.photostalk.utils.MiscUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by mohammed on 3/13/16.
 */
public class Notification extends Model {

    public enum Type {
        FOLLOW,
        REQUEST,
        COMMENT,
        REQUEST_ACCEPTANCE,
        UNKNOWN
    }

    private UserModel mUser;
    private int mTypeId;
    private String mTypeName;
    private Type mType;
    private boolean mSeen;
    private Date mNotificationDate;
    private Photo mPhoto;


    public Notification(JSONObject jsonObject) {

        setId(MiscUtils.getString(jsonObject, "id", ""));
        setTypeId(MiscUtils.getInt(jsonObject, "type_id", 0));
        setTypeName(MiscUtils.getString(jsonObject, "type_name", ""));
        setSeen(MiscUtils.getBoolean(jsonObject, "seen", false));
        setNotificationDateAsString(MiscUtils.getString(jsonObject, "notification_date", "1970-01-01"));

        try {
            mUser = new UserModel(jsonObject.getJSONObject("user"));
        } catch (JSONException e) {
            e.printStackTrace();
            mUser = new UserModel();
        }

        if (mType == Type.COMMENT) {
            try {
                mPhoto = new Photo(jsonObject.getJSONObject("photo"));
            } catch (JSONException e) {
                e.printStackTrace();
                mPhoto = new Photo();
            }
        }
    }


    @Override
    public void copyFrom(Model model) {
        if (!(model instanceof Notification)) return;
        setId(model.getId());
        setTypeId(((Notification) model).getTypeId());
        setTypeName(((Notification) model).getTypeName());
        setSeen(((Notification) model).isSeen());
        setNotificationDate(((Notification) model).getNotificationDate());
        mUser.copyFrom(((Notification) model).getUser());
    }


    public UserModel getUser() {
        return mUser;
    }

    public Photo getPhoto() {
        return mPhoto;
    }

    public int getTypeId() {
        return mTypeId;
    }

    public void setTypeId(int typeId) {
        mTypeId = typeId;
        switch (mTypeId) {
            case 1:
                mType = Type.FOLLOW;
                break;
            case 3:
                mType = Type.REQUEST;
                break;
            case 4:
                mType = Type.COMMENT;
                break;
            case 5:
                mType = Type.REQUEST_ACCEPTANCE;
                break;
            default:
                mType = Type.UNKNOWN;
        }
    }

    public String getTypeName() {
        return mTypeName;
    }

    public void setTypeName(String typeName) {
        mTypeName = typeName;
    }

    public Type getType() {
        return mType;
    }

    public boolean isSeen() {
        return mSeen;
    }

    public void setSeen(boolean seen) {
        mSeen = seen;
    }

    public Date getNotificationDate() {
        return mNotificationDate;
    }

    public void setNotificationDate(Date notificationDate) {
        mNotificationDate = notificationDate;
    }

    public String getNotificationDateAsString(String format) {
        return new SimpleDateFormat(format).format(mNotificationDate);
    }

    public void setNotificationDateAsString(String notificationDate) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            mNotificationDate = simpleDateFormat.parse(notificationDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
