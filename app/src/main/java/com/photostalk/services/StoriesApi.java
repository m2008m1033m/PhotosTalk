package com.photostalk.services;

import com.loopj.android.http.RequestParams;
import com.photostalk.models.Model;
import com.photostalk.models.Story;
import com.photostalk.models.Trending;
import com.photostalk.utils.ApiListeners;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by mohammed on 2/19/16.
 */
public class StoriesApi {

    public static void get(String id, ApiListeners.OnItemLoadedListener listener) {
        Stub.get(
                "stories/get",
                listener,
                new Stub.ModelParser() {
                    @Override
                    Model parseItem(JSONObject jsonObject) throws JSONException {
                        return new Story(jsonObject.getJSONObject("story"));
                    }
                },
                new RequestParams("id", id),
                "stories_get");

    }

    public static void report(String id, ApiListeners.OnActionExecutedListener listener) {
        Stub.post("stories/report", listener, null, new RequestParams("id", id), "stories_report");
    }

    public static void delete(String id, ApiListeners.OnActionExecutedListener listener) {
        Stub.post("stories/delete", listener, null, new RequestParams("id", id), "stories_delete");
    }

    public static void trending(ApiListeners.OnItemsArrayLoadedListener listener) {
        Stub.get(
                "stories/trending",
                listener,
                new Stub.ModelParser() {
                    @Override
                    ArrayList<Model> extractArray(JSONObject jsonObject) throws JSONException {
                        ArrayList<Model> items = new ArrayList<>();
                        JSONArray jsonArray = jsonObject.getJSONArray("trending");
                        int len = jsonArray.length();
                        for (int i = 0; i < len; i++)
                            items.add(new Trending(jsonArray.getJSONObject(i)));
                        return items;
                    }
                },
                new RequestParams(),
                "story_trending"
        );
    }
}
