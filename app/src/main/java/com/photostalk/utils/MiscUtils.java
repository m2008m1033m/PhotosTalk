package com.photostalk.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;

import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by mohammed on 3/4/16.
 */
public class MiscUtils {

    /**
     * json getters:
     */
    public static String getString(JSONObject jsonObject, String name, String defaultValue) {
        try {
            String string = jsonObject.getString(name).trim();
            return string.equals("null") ? "" : string;
        } catch (JSONException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static int getInt(JSONObject jsonObject, String name, int defaultValue) {
        try {
            return Integer.parseInt(jsonObject.getString(name).trim());
        } catch (JSONException e) {
            e.printStackTrace();
            return defaultValue;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static boolean getBoolean(JSONObject jsonObject, String name, boolean defaultValue) {
        try {
            return jsonObject.getString(name).trim().equals("1");
        } catch (JSONException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static String getDurationFormatted(Date notificationDate) {

        int currentSeconds = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
        int seconds = (int) (notificationDate.getTime() / 1000);

        seconds = currentSeconds - seconds;

        if (seconds < 60)
            return PhotosTalkApplication.getContext().getString(seconds == 1 ? R.string.s_second_ago : R.string.s_seconds_ago, seconds);

        seconds = seconds / 60;
        if (seconds < 60)
            return PhotosTalkApplication.getContext().getString(seconds == 1 ? R.string.s_minute_ago : R.string.s_minutes_ago, seconds);

        seconds = seconds / 60;
        if (seconds < 24)
            return PhotosTalkApplication.getContext().getString(seconds == 1 ? R.string.s_hour_ago : R.string.s_hours_ago, seconds);

        seconds = seconds / 24;
        if (seconds < 30)
            return PhotosTalkApplication.getContext().getString(seconds == 1 ? R.string.s_day_ago : R.string.s_days_ago, seconds);

        seconds = seconds / 30;
        if (seconds < 12)
            return PhotosTalkApplication.getContext().getString(seconds == 1 ? R.string.s_month_ago : R.string.s_months_ago, seconds);

        seconds = seconds / 12;
        return PhotosTalkApplication.getContext().getString(seconds == 1 ? R.string.s_year_ago : R.string.s_years_ago, seconds);

    }

    // Decodes image and scales it to reduce memory consumption
    public static Bitmap decodeFile(String photoPath) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(photoPath), null, o);

            // The new size we want to scale to
            final int REQUIRED_SIZE = 1000;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE ||
                    o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2;
            }

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(photoPath), null, o2);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String convertStringToHashTag(String str) {
        String[] bits = str.split(" ");
        String finalDescription = "";
        for (String bit : bits) {
            if (bit.trim().isEmpty()) continue;
            if (!bit.startsWith("#"))
                bit = "#" + bit;
            finalDescription += bit + " ";
        }

        return finalDescription;
    }

    public static int convertDP2Pixel(int dp) {
        Resources r = PhotosTalkApplication.getContext().getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }
}
