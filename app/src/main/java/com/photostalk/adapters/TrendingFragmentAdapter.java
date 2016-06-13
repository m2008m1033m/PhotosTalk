package com.photostalk.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.photostalk.HomeActivity;
import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.core.User;
import com.photostalk.models.Model;
import com.photostalk.models.Story;
import com.photostalk.models.Trending;
import com.photostalk.models.UserModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class TrendingFragmentAdapter extends RefreshAdapter {


    public interface OnActionListener {
        void onStoryClicked(int position);

        void onFollowButtonClicked(int position);

        void onUserClicked(int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView mPhoto;
        private ImageView mUserPhoto;
        private TextView mBig;
        private ImageButton mFollowButton;

        private int mPosition;

        public ViewHolder(View itemView) {
            super(itemView);

            mPhoto = ((ImageView) itemView.findViewById(R.id.photo));
            mUserPhoto = ((ImageView) itemView.findViewById(R.id.user_photo));
            mBig = ((TextView) itemView.findViewById(R.id.big));
            mFollowButton = ((ImageButton) itemView.findViewById(R.id.follow_button));
        }
    }

    private ArrayList<Model> mItems = new ArrayList<>();
    private OnActionListener mOnActionListener;

    public TrendingFragmentAdapter(OnActionListener onActionListener) {
        mOnActionListener = onActionListener;
    }

    @Override
    public ArrayList getItems() {
        return mItems;
    }

    @Override
    public int getItemViewType(int position) {
        return HomeActivity.TRENDING;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.trending_item, parent, false);
        final ViewHolder vh = new ViewHolder(v);

        vh.mUserPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onUserClicked(vh.mPosition);
            }
        });

        vh.mFollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onFollowButtonClicked(vh.mPosition);
            }
        });

        vh.mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onStoryClicked(vh.mPosition);
            }
        });

        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder).mPosition = position;
        Story story = getStory(position);
        if (story == null) return;

        Glide.with(PhotosTalkApplication.getContext())
                .load(story.getImageUrl())
                .into(((ViewHolder) holder).mPhoto);

        UserModel user = getUser(position);
        if (user.getPhoto().isEmpty())
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(R.drawable.no_avatar)
                    .into(((ViewHolder) holder).mUserPhoto);
        else
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(user.getPhoto())
                    .placeholder(R.drawable.no_avatar)
                    .into(((ViewHolder) holder).mUserPhoto);

        ((ViewHolder) holder).mBig.setText(user.getName());

        if (user.isFollowRequestSent() || User.getInstance().getId().equals(user.getId()))
            ((ViewHolder) holder).mFollowButton.setVisibility(View.GONE);
        else {
            ((ViewHolder) holder).mFollowButton.setVisibility(View.VISIBLE);
            ((ViewHolder) holder).mFollowButton.setImageResource(user.isFollowingUser() ? R.drawable.unfollow : R.drawable.follow);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }


    public void removeStory(String storyId) {
        int len = mItems.size();
        for (int i = 0; i < len; i++) {
            Story story = getStory(i);
            if (story == null) return;
            if (story.getId().equals(storyId)) {
                mItems.remove(i);
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void clearAll() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public UserModel getUser(int position) {
        if (position >= mItems.size()) return null;
        return ((Trending) mItems.get(position)).getUser();
    }

    public Story getStory(int position) {
        if (position >= mItems.size()) return null;
        return ((Trending) mItems.get(position)).getStory();
    }

    public void updateUserPhoto() {
        for (int i = 0; i < mItems.size(); i++) {
            if (!User.getInstance().getId().equals(getUser(i).getId())) continue;
            getUser(i).setPhoto(User.getInstance().getPhoto());
            notifyItemChanged(i);
        }
    }

    public void updateUser(String userId, boolean request, boolean follow) {
        int len = mItems.size();
        for (int i = 0; i < len; i++) {
            UserModel user = getUser(i);
            if (user == null) continue;
            if (user.getId().equals(userId)) {
                user.setIsFollowRequestSent(request);
                user.setIsFollowingUser(follow);
            }
        }
        notifyDataSetChanged();
    }
}
