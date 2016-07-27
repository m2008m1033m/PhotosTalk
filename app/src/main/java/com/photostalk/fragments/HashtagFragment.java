package com.photostalk.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.photostalk.R;

import java.io.File;


public class HashtagFragment extends Fragment {

    private View mView;
    private EditText mEditText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.hashtag_fragment, container, false);
        mEditText = ((EditText) mView.findViewById(R.id.hash_tag_edit_text));
        return mView;
    }

    public void setPhoto(String photoPath) {
        Glide.with(getActivity())
                .load(new File(photoPath))
                .into(((ImageView) mView.findViewById(R.id.photo)));
    }

    public String getHashtag() {
        return mEditText.getText().toString();
    }

    public EditText getEditText() {
        return mEditText;
    }
}
