package com.photostalk.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.models.Photo;
import com.photostalk.utils.MiscUtils;

import java.util.ArrayList;

/**
 * Created by mohammed on 3/6/16.
 */
public class ProfileActivityPhotosAdapter extends RefreshAdapter {

    public interface OnActionListener {
        void onPlayStopButtonClicked(int position);

        void onPhotoClicked(int position);

        void onHashtagClicked(String hashtag);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView mPhoto;
        private TextView mIsLiveTextView;
        private TextView mHashTagsTextView;
        private ImageView mPlayStopButton;
        private ProgressBar mProgressBar;
        private int mPosition;


        public ViewHolder(View itemView) {
            super(itemView);
            mPhoto = ((ImageView) itemView.findViewById(R.id.photo));
            mPlayStopButton = ((ImageView) itemView.findViewById(R.id.play_stop_button));
            mHashTagsTextView = ((TextView) itemView.findViewById(R.id.hash_tags_text_view));
            mIsLiveTextView = ((TextView) itemView.findViewById(R.id.is_live_text_view));
            mProgressBar = ((ProgressBar) itemView.findViewById(R.id.progress));
        }
    }


    private ArrayList<Photo> mPhotos = new ArrayList<>();
    OnActionListener mOnActionListener;
    private int mCurrentlyPlaying = -1;
    private boolean mIsLoading = false;

    public ProfileActivityPhotosAdapter(OnActionListener onActionListener) {
        mOnActionListener = onActionListener;
    }

    @Override
    public ArrayList getItems() {
        return mPhotos;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(PhotosTalkApplication.getContext()).inflate(R.layout.photo_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);

        viewHolder.mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onPhotoClicked(viewHolder.mPosition);
            }
        });

        viewHolder.mPlayStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onPlayStopButtonClicked(viewHolder.mPosition);
            }
        });

        viewHolder.mHashTagsTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    String hashtag = MiscUtils.getWordFromCharPosition(viewHolder.mHashTagsTextView.getOffsetForPosition(motionEvent.getX(), motionEvent.getY()), viewHolder.mHashTagsTextView.getText().toString());
                    if (hashtag == null) return true;
                    if (hashtag.startsWith("#")) hashtag = hashtag.substring(1);
                    mOnActionListener.onHashtagClicked(hashtag);
                    return true;
                }
                return false;
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        Photo photo = mPhotos.get(position);
        ((ViewHolder) holder).mPosition = position;
        Glide.with(PhotosTalkApplication.getContext())
                .load(photo.getImageUrl())
                .into(((ViewHolder) holder).mPhoto);
        ((ViewHolder) holder).mIsLiveTextView.setVisibility(photo.isLive() ? View.VISIBLE : View.GONE);
        ((ViewHolder) holder).mHashTagsTextView.setText(photo.getHashTagsConcatenated());

        if (photo.getAudioUrl().isEmpty()) {
            ((ViewHolder) holder).mPlayStopButton.setVisibility(View.GONE);
            ((ViewHolder) holder).mProgressBar.setVisibility(View.GONE);
        } else {
            ((ViewHolder) holder).mPlayStopButton.setVisibility(View.VISIBLE);
            ((ViewHolder) holder).mPlayStopButton.setImageResource(position != mCurrentlyPlaying ? R.drawable.play_blue : mIsLoading ? R.drawable.crystal_button : R.drawable.stop_blue);
            ((ViewHolder) holder).mProgressBar.setVisibility(position == mCurrentlyPlaying && mIsLoading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mPhotos.size();
    }

    public void setCurrentlyPlaying(int currentlyPlaying) {
        mCurrentlyPlaying = currentlyPlaying;
    }

    public void setIsLoading(boolean isLoading) {
        mIsLoading = isLoading;
    }
}
