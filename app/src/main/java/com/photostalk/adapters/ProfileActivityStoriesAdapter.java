package com.photostalk.adapters;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.models.Story;

import java.util.ArrayList;

public class ProfileActivityStoriesAdapter extends RefreshAdapter {


    public interface OnActionListener {
        void onStoryClicked(int position);

        void onPlayStopButtonClicked(int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private CardView mCardView;
        private TextView mStoryName;
        private TextView mIsLive;
        private ImageView mPhoto;
        private ImageView mPlayStopButton;
        private TextView mHashTags;

        private int mPosition;

        public ViewHolder(View itemView) {
            super(itemView);

            mCardView = ((CardView) itemView.findViewById(R.id.card_view));
            mStoryName = ((TextView) itemView.findViewById(R.id.story_name));
            mIsLive = ((TextView) itemView.findViewById(R.id.is_live_text_view));
            mPhoto = ((ImageView) itemView.findViewById(R.id.photo));
            mPlayStopButton = ((ImageView) itemView.findViewById(R.id.play_stop_button));
            mHashTags = ((TextView) itemView.findViewById(R.id.hash_tags_text_view));

        }
    }

    private OnActionListener mOnActionListener;
    private ArrayList<Story> mStories = new ArrayList<>();

    public ProfileActivityStoriesAdapter(OnActionListener onActionListener) {
        mOnActionListener = onActionListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.story_item, parent, false);
        final ViewHolder vh = new ViewHolder(v);

        vh.mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onStoryClicked(vh.mPosition);
            }
        });

        vh.mPlayStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onPlayStopButtonClicked(vh.mPosition);
            }
        });

        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Story story = mStories.get(position);

        Glide.with(PhotosTalkApplication.getContext())
                .load(story.getImageUrl())
                .into(((ViewHolder) holder).mPhoto);

        ((ViewHolder) holder).mStoryName.setText(PhotosTalkApplication.getContext().getString(R.string.s_story, story.getStoryDateAsString("dd - MMMM")));
        ((ViewHolder) holder).mIsLive.setVisibility(View.GONE);
        ((ViewHolder) holder).mHashTags.setVisibility(View.GONE);
        ((ViewHolder) holder).mPlayStopButton.setVisibility(View.GONE);

        ((ViewHolder) holder).mPosition = position;
    }

    @Override
    public int getItemCount() {
        return mStories.size();
    }

    @Override
    public ArrayList getItems() {
        return mStories;
    }
}
