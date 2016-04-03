package com.photostalk.models;

import com.photostalk.utils.MiscUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mohammed on 3/13/16.
 */
public class Notification extends Model {

    private UserModel mUser;
    private int mTypeId;
    private String mTypeName;
    private boolean mSeen;
    private Date mNotificationDate;


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

    public int getTypeId() {
        return mTypeId;
    }

    public void setTypeId(int typeId) {
        mTypeId = typeId;
    }

    public String getTypeName() {
        return mTypeName;
    }

    public void setTypeName(String typeName) {
        mTypeName = typeName;
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
            mNotificationDate = new SimpleDateFormat("yyyy-MM-dd").parse(notificationDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
