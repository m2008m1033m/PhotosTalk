package com.photostalk.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.models.Photo;
import com.photostalk.models.Story;
import com.photostalk.utils.MiscUtils;
import com.squareup.picasso.Picasso;

/**
 * Created by mohammed on 3/4/16.
 */
public class StoryActivityAdapter extends RecyclerView.Adapter {

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

    private OnActionListener mOnActionListener;
    private Story mStory;
    private int mItemPlaying = -1;
    private boolean mIsLoading = true;

    public StoryActivityAdapter(OnActionListener listener) {
        mOnActionListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_item, parent, false);
        final ViewHolder vh = new ViewHolder(v);

        if (mOnActionListener != null) {
            vh.mPlayStopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnActionListener.onPlayStopButtonClicked(vh.mPosition);
                }
            });
            vh.mPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnActionListener.onPhotoClicked(vh.mPosition);
                }
            });
            vh.mHashTagsTextView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        String word = MiscUtils.getWordFromCharPosition(vh.mHashTagsTextView.getOffsetForPosition(motionEvent.getX(), motionEvent.getY()), vh.mHashTagsTextView.getText().toString());
                        if (word != null && word.startsWith("#"))
                            mOnActionListener.onHashtagClicked(word.substring(1));
                        return true;
                    }
                    return false;
                }
            });
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Photo photo = mStory.getPhotos().get(position);
        if (!photo.getImageUrl().equals(""))
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(photo.getImageUrl())
                    .into(((ViewHolder) holder).mPhoto);
        ((ViewHolder) holder).mHashTagsTextView.setText(photo.getHashTagsConcatenated());
        ((ViewHolder) holder).mIsLiveTextView.setVisibility(photo.isLive() ? View.VISIBLE : View.GONE);

        if (photo.getAudioUrl().isEmpty()) {
            ((ViewHolder) holder).mPlayStopButton.setVisibility(View.GONE);
            ((ViewHolder) holder).mProgressBar.setVisibility(View.GONE);
        } else {
            ((ViewHolder) holder).mPlayStopButton.setVisibility(View.VISIBLE);
            if (position == mItemPlaying) {
                ((ViewHolder) holder).mPlayStopButton.setImageResource(mIsLoading ? R.drawable.crystal_button : R.drawable.stop_blue);
                ((ViewHolder) holder).mProgressBar.setVisibility(mIsLoading ? View.VISIBLE : View.GONE);
            } else {
                ((ViewHolder) holder).mPlayStopButton.setImageResource(R.drawable.play_blue);
                ((ViewHolder) holder).mProgressBar.setVisibility(View.GONE);
            }
        }

        ((ViewHolder) holder).mPosition = position;
    }

    public void setItemPlaying(int itemPlaying) {
        mItemPlaying = itemPlaying;
    }

    public void setIsLoading(boolean isLoading) {
        mIsLoading = isLoading;
    }

    @Override
    public int getItemCount() {
        if (mStory == null) return 0;
        return mStory.getPhotos().size();
    }

    public void setStory(Story story) {
        mStory = story;
        notifyDataSetChanged();
    }
}
