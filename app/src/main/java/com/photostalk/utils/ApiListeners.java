package com.photostalk.utils;

import com.photostalk.models.Model;
import com.photostalk.apis.Result;

import java.util.ArrayList;

/**
 * Created by mohammed on 2/19/16.
 */
public class ApiListeners {
    public interface OnActionExecutedListener {
        void onExecuted(Result result);
    }

    public interface OnItemLoadedListener {
        void onLoaded(Result result, Model item);
    }

    public interface OnItemsArrayLoadedListener {
        void onLoaded(Result result, ArrayList<Model> items);
    }
}
