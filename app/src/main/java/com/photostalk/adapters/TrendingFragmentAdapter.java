package com.photostalk.adapters;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.photostalk.HomeActivity;
import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.core.User;
import com.photostalk.fragments.TrendingFragment;
import com.photostalk.models.Model;
import com.photostalk.models.Photo;
import com.photostalk.models.Story;
import com.photostalk.models.Trending;
import com.photostalk.models.UserModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by mohammed on 3/10/16.
 */
public class TrendingFragmentAdapter extends RefreshAdapter {


    public interface OnActionListener {
        void onStoryClicked(int position);

        void onFollowButtonClicked(int position);

        void onUserClicked(int position);
    }

    class ViewHolderTrending extends RecyclerView.ViewHolder {

        private FrameLayout mWrapper;
        private ImageView mPhoto;
        private ImageView mUserPhoto;
        private TextView mBig;
        private TextView mSmall;
        private ImageButton mFollowButton;

        private int mPosition;

        public ViewHolderTrending(View itemView) {
            super(itemView);

            mWrapper = ((FrameLayout) itemView.findViewById(R.id.wrapper));
            mPhoto = ((ImageView) itemView.findViewById(R.id.photo));
            mUserPhoto = ((ImageView) itemView.findViewById(R.id.user_photo));
            mBig = ((TextView) itemView.findViewById(R.id.big));
            mSmall = ((TextView) itemView.findViewById(R.id.small));
            mFollowButton = ((ImageButton) itemView.findViewById(R.id.follow_button));
        }
    }

    class ViewHolderUser extends RecyclerView.ViewHolder {

        private FrameLayout mWrapper;
        private ImageView mPhoto;
        private TextView mUserName;
        private TextView mUsername;
        private Button mFollowButton;

        private int mPosition;

        public ViewHolderUser(View itemView) {
            super(itemView);

            mWrapper = ((FrameLayout) itemView.findViewById(R.id.wrapper));
            mPhoto = ((ImageView) itemView.findViewById(R.id.photo));
            mUserName = ((TextView) itemView.findViewById(R.id.user_name));
            mUsername = ((TextView) itemView.findViewById(R.id.username));
            mFollowButton = ((Button) itemView.findViewById(R.id.follow_button));

        }
    }

    private ArrayList<Model> mItems = new ArrayList<>();
    private TrendingFragment mTrendingFragment;
    private OnActionListener mOnActionListener;

    public TrendingFragmentAdapter(TrendingFragment trendingFragment, OnActionListener onActionListener) {
        mTrendingFragment = trendingFragment;
        mOnActionListener = onActionListener;
    }

    @Override
    public ArrayList getItems() {
        return mItems;
    }

    @Override
    public int getItemViewType(int position) {
        if (mTrendingFragment.isSearching())
            return mTrendingFragment.getType();

        return HomeActivity.TRENDING;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == HomeActivity.USER)
            return createUserViewHolder(parent);
        else
            return createTrendingViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (mTrendingFragment.isSearching() && mTrendingFragment.getType() == HomeActivity.USER)
            bindUser((ViewHolderUser) holder, position);
        else
            bindTrending((ViewHolderTrending) holder, position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
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
        if (!mTrendingFragment.isSearching()) {
            return ((Trending) mItems.get(position)).getUser();
        } else {
            switch (mTrendingFragment.getType()) {
                case HomeActivity.USER:
                    return ((UserModel) mItems.get(position));
                case HomeActivity.TRENDING:
                    return ((Photo) mItems.get(position)).getUser();
            }
        }

        return null;
    }

    public Story getStory(int position) {
        if (position >= mItems.size()) return null;
        if (!mTrendingFragment.isSearching()) {
            return ((Trending) mItems.get(position)).getStory();
        } else if (mTrendingFragment.getType() == HomeActivity.TRENDING) {
            return ((Photo) mItems.get(position)).getStory();
        }

        return null;
    }

    public void updateUserPhoto() {
        for (int i = 0; i < mItems.size(); i++) {
            if (!User.getInstance().getId().equals(getUser(i).getId())) continue;
            getUser(i).setPhoto(User.getInstance().getPhoto());
            notifyItemChanged(i);
        }
    }

    private ViewHolderUser createUserViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
        final ViewHolderUser vh = new ViewHolderUser(v);

        vh.mWrapper.setOnClickListener(new View.OnClickListener() {
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

        return vh;
    }

    private ViewHolderTrending createTrendingViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.trending_item, parent, false);
        final ViewHolderTrending vh = new ViewHolderTrending(v);

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

    private void bindUser(ViewHolderUser vh, int position) {
        vh.mPosition = position;
        if (position >= mItems.size()) return;
        UserModel user = getUser(position);
        if (user == null) return;

        if (user.getPhoto().isEmpty())
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(R.drawable.no_avatar)
                    .into(vh.mPhoto);
        else
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(user.getPhoto())
                    .placeholder(R.drawable.no_avatar)
                    .into(vh.mPhoto);

        vh.mUserName.setText(user.getName());
        vh.mUsername.setText(user.getUsername());

        if (user.isFollowRequestSent() || User.getInstance().getId().equals(user.getId()))
            vh.mFollowButton.setVisibility(View.GONE);
        else {
            vh.mFollowButton.setVisibility(View.VISIBLE);
            vh.mFollowButton.setTextColor(ContextCompat.getColor(PhotosTalkApplication.getContext(), user.isFollowingUser() ? R.color.white : R.color.main));
            vh.mFollowButton.setText(user.isFollowingUser() ? R.string.following : R.string.follow);
            vh.mFollowButton.setBackgroundResource(user.isFollowingUser() ? R.drawable.main_button : R.drawable.bordered_button_main);
        }
    }

    private void bindTrending(ViewHolderTrending vh, int position) {
        vh.mPosition = position;
        Story story = getStory(position);
        if (story == null) return;

        Glide.with(PhotosTalkApplication.getContext())
                .load(story.getImageUrl())
                .into(vh.mPhoto);

        UserModel user = getUser(position);
        if (user.getPhoto().isEmpty())
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(R.drawable.no_avatar)
                    .into(vh.mUserPhoto);
        else
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(user.getPhoto())
                    .placeholder(R.drawable.no_avatar)
                    .into(vh.mUserPhoto);

        vh.mBig.setText(user.getName());

        if (user.isFollowRequestSent() || User.getInstance().getId().equals(user.getId()))
            vh.mFollowButton.setVisibility(View.GONE);
        else {
            vh.mFollowButton.setVisibility(View.VISIBLE);
            vh.mFollowButton.setImageResource(user.isFollowingUser() ? R.drawable.unfollow : R.drawable.follow);
        }
    }
}
