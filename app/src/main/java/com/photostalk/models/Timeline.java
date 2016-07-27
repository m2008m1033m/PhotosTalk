package com.photostalk.models;

import com.photostalk.utils.MiscUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mohammed on 3/7/16.
 */
public class Timeline extends Model {

    private Story mStory;

    public Timeline(JSONObject jsonObject) {

        setId(MiscUtils.getString(jsonObject, "timeline_id", ""));

        try {
            mStory = new Story(jsonObject.getJSONObject("story"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public Story getStory() {
        return mStory;
    }

    @Override
    public void copyFrom(Model model) {
        Timeline other = ((Timeline) model);
        setId(other.getId());
        mStory.copyFrom(other.getStory());
    }
}
