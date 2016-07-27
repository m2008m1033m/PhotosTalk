package com.photostalk.models;

import com.photostalk.utils.MiscUtils;

import org.json.JSONObject;

/**
 * Created by mohammed on 6/12/16.
 */
public class Country extends Model {

    private String mName;

    public Country(JSONObject jsonObject) {
        setId(MiscUtils.getString(jsonObject, "id", ""));
        setName(MiscUtils.getString(jsonObject, "name", ""));
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    @Override
    public void copyFrom(Model model) {
        if (!(model instanceof Country)) return;
        setId(model.getId());
        setName(((Country) model).getName());
    }
}
