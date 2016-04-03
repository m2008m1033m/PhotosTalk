package com.photostalk.models;

import com.photostalk.utils.MiscUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mohammed on 3/7/16.
 */
public class Followship extends Model {
    private UserModel mUser;

    public Followship(JSONObject jsonObject) {
        setId(MiscUtils.getString(jsonObject, "relation_id", ""));
        try {
            mUser = new UserModel(jsonObject.getJSONObject("user"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public UserModel getUser() {
        return mUser;
    }

    public void setUser(UserModel user) {
        mUser = user;
    }

    @Override
    public void copyFrom(Model model) {
        Followship other = ((Followship) model);
        setId(other.getId());
        mUser.copyFrom(other.getUser());
    }
}
