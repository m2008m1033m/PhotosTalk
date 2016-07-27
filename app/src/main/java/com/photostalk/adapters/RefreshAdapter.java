package com.photostalk.adapters;

import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;


abstract public class RefreshAdapter extends RecyclerView.Adapter {
    public abstract ArrayList getItems();
}
