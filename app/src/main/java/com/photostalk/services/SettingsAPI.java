package com.photostalk.services;

import com.photostalk.models.Country;
import com.photostalk.models.Model;
import com.photostalk.utils.ApiListeners;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SettingsAPI {

    public static void getCountries(ApiListeners.OnItemsArrayLoadedListener listener) {
        Stub.get("settings/get-countries", listener, new Stub.ModelParser() {
            @Override
            ArrayList<Model> extractArray(JSONObject jsonObject) throws JSONException {
                ArrayList<Model> countries = new ArrayList<>();
                JSONArray jsonArray = jsonObject.getJSONArray("countries");
                int len = jsonArray.length();
                for (int i = 0; i < len; i++)
                    countries.add(new Country(jsonArray.getJSONObject(i)));
                return countries;
            }
        }, null, "settings_get_countries");
    }
}
