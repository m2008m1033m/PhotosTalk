package com.photostalk.services;

import java.util.ArrayList;

/**
 * Created by mohammed on 2/19/16.
 */
public class Result {
    private boolean isSucceeded;
    private String mCode;
    private ArrayList<String> mMessages = new ArrayList<>();
    private ArrayList<Object> mExtra = new ArrayList<>();

    public boolean isSucceeded() {
        return isSucceeded;
    }

    public void setIsSucceeded(boolean isSucceeded) {
        this.isSucceeded = isSucceeded;
    }

    public String getCode() {
        return mCode;
    }

    public void setCode(String code) {
        mCode = code;
    }

    public ArrayList<String> getMessages() {
        return mMessages;
    }

    public ArrayList<Object> getExtra() {
        return mExtra;
    }

}
