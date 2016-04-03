package com.photostalk.adapters;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.models.BestPhoto;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by mohammed on 2/24/16.
 */
public class SelectBestPhotoAdapter extends RecyclerView.Adapter {

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mPhoto;
        FloatingActionButton mFAB;
        BestPhoto mItem;

        public ViewHolder(View itemView) {
            super(itemView);
            mPhoto = ((ImageView) itemView.findViewById(R.id.photo));
            mFAB = ((FloatingActionButton) itemView.findViewById(R.id.fab));
        }
    }

    public interface OnPhotoSelectedListener {
        void onSelected(BestPhoto selectedPhoto);
    }


    private ArrayList<BestPhoto> mItems;
    private OnPhotoSelectedListener mOnPhotoSelectedListener;
    private BestPhoto mSelectedItem;

    public SelectBestPhotoAdapter(ArrayList<BestPhoto> items, OnPhotoSelectedListener listener) {
        mItems = items;
        mOnPhotoSelectedListener = listener;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.select_best_photo_item, parent, false);
        final ViewHolder vh = new ViewHolder(v);
        vh.mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectedItem != null) {
                    mSelectedItem.setIsSelected(false);
                }

                vh.mItem.setIsSelected(true);
                mSelectedItem = vh.mItem;
                notifyDataSetChanged();
            }
        });

        vh.mFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!vh.mItem.isSelected()) return;
                mOnPhotoSelectedListener.onSelected(vh.mItem);
            }
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        BestPhoto item = mItems.get(position);


        Glide.with(PhotosTalkApplication.getContext())
                .load(new File(item.getPath()))
                .into(((ViewHolder) holder).mPhoto);


        if (item.isSelected())
            ((ViewHolder) holder).mFAB.setVisibility(View.VISIBLE);
        else
            ((ViewHolder) holder).mFAB.setVisibility(View.GONE);
        ((ViewHolder) holder).mItem = item;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public BestPhoto getSelectedItem() {
        return mSelectedItem;
    }
}
