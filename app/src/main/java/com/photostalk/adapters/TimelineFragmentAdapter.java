package com.photostalk.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.core.User;
import com.photostalk.models.Story;
import com.photostalk.models.Timeline;
import com.photostalk.models.UserModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by mohammed on 3/10/16.
 */
public class TimelineFragmentAdapter extends RefreshAdapter {
    public interface OnActionListener {
        void onFollowButtonClicked(int position);

        void onUserClicked(int position);

        void onStoryClicked(int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private FrameLayout mWrapper;
        private ImageView mPhoto;
        private ImageView mUserPhoto;
        private TextView mUserName;
        private ImageButton mFollowButton;

        private int mPosition;

        public ViewHolder(View itemView) {
            super(itemView);

            mWrapper = ((FrameLayout) itemView.findViewById(R.id.wrapper));
            mPhoto = ((ImageView) itemView.findViewById(R.id.photo));
            mUserPhoto = ((ImageView) itemView.findViewById(R.id.user_photo));
            mUserName = ((TextView) itemView.findViewById(R.id.big));
            mFollowButton = ((ImageButton) itemView.findViewById(R.id.follow_button));
        }
    }

    private ArrayList<Timeline> mItems = new ArrayList<>();
    private OnActionListener mOnActionListener;

    public TimelineFragmentAdapter(OnActionListener onActionListener) {
        mOnActionListener = onActionListener;
    }

    @Override
    public ArrayList getItems() {
        return mItems;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.trending_item, parent, false);
        final ViewHolder vh = new ViewHolder(v);

        vh.mWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onStoryClicked(vh.mPosition);
            }
        });

        vh.mFollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onFollowButtonClicked(vh.mPosition);
            }
        });

        vh.mUserPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onUserClicked(vh.mPosition);
            }
        });

        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder).mPosition = position;

        Timeline timeline = mItems.get(position);
        if (timeline == null) return;

        Glide.with(PhotosTalkApplication.getContext())
                .load(timeline.getStory().getImageUrl())
                .into(((ViewHolder) holder).mPhoto);

        if (timeline.getStory().getUser().getPhoto().isEmpty())
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(R.drawable.no_avatar)
                    .into(((ViewHolder) holder).mUserPhoto);
        else
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(timeline.getStory().getUser().getPhoto())
                    .placeholder(R.drawable.no_avatar)
                    .into(((ViewHolder) holder).mUserPhoto);

        UserModel user = timeline.getStory().getUser();
        ((ViewHolder) holder).mUserName.setText(user.getName());
        ((ViewHolder) holder).mFollowButton.setVisibility(View.GONE);
        /*if (!timeline.getStory().getUser().isFollowRequestSent() && !User.getInstance().getId().equals(user.getId())) {
            ((ViewHolder) holder).mFollowButton.setVisibility(View.VISIBLE);
            ((ViewHolder) holder).mFollowButton.setImageResource(user.isFollowingUser() ? R.drawable.unfollow : R.drawable.follow);
        } else {
            ((ViewHolder) holder).mFollowButton.setVisibility(View.GONE);
        }*/
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void updateUser(String userId, boolean request, boolean follow) {
        UserModel u = null;
        for (Timeline tl : mItems) {
            if (tl.getStory().getUser().getId().equals(userId)) {
                u = tl.getStory().getUser();
                break;
            }
        }
        if (u == null) return;
        u.setIsFollowRequestSent(request);
        u.setIsFollowingUser(follow);
        notifyDataSetChanged();
    }

    public void removeStory(String storyId) {
        Timeline timeline = null;
        for (Timeline tl : mItems) {
            if (tl.getStory().getId().equals(storyId)) {
                timeline = tl;
                break;
            }
        }
        if (timeline == null) return;
        mItems.remove(timeline);
        notifyDataSetChanged();
    }

    public UserModel getUser(int position) {
        return mItems.get(position).getStory().getUser();
    }

    public Story getStory(int position) {
        return mItems.get(position).getStory();
    }

    public void updateUserPhoto() {
        for (int i = 0; i < mItems.size(); i++) {
            Timeline timeline = mItems.get(i);
            if (!User.getInstance().getId().equals(timeline.getStory().getUser().getId())) continue;
            timeline.getStory().getUser().setPhoto(User.getInstance().getPhoto());
            notifyItemChanged(i);
        }
    }

}
