package com.photostalk.adapters;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.photostalk.FollowshipAndBlockagesActivity;
import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.core.User;
import com.photostalk.models.Followship;
import com.photostalk.models.Model;
import com.photostalk.models.UserModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by mohammed on 3/7/16.
 */
public class FollowshipAndBlockagesActivityAdapter extends RefreshAdapter {


    public interface OnActionListener {
        void onItemClicked(int position);

        void onFollowButtonClicked(int position, Button button);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private FrameLayout mWrapper;
        private ImageView mPhoto;
        private TextView mUserName;
        private TextView mUsername;
        private Button mFollowButton;

        private int mPosition;

        public ViewHolder(View itemView) {
            super(itemView);

            mWrapper = ((FrameLayout) itemView.findViewById(R.id.wrapper));
            mPhoto = ((ImageView) itemView.findViewById(R.id.photo));
            mUserName = ((TextView) itemView.findViewById(R.id.user_name));
            mUsername = ((TextView) itemView.findViewById(R.id.username));
            mFollowButton = ((Button) itemView.findViewById(R.id.follow_button));

        }
    }

    private ArrayList<Model> mItems = new ArrayList<>();
    private OnActionListener mOnActionListener;
    private int mType;

    public FollowshipAndBlockagesActivityAdapter(OnActionListener onActionListener, int type) {
        mOnActionListener = onActionListener;
        mType = type;
    }

    @Override
    public ArrayList getItems() {
        return mItems;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
        final ViewHolder vh = new ViewHolder(v);

        vh.mWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onItemClicked(vh.mPosition);
            }
        });

        vh.mFollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onFollowButtonClicked(vh.mPosition, ((Button) view));
            }
        });

        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder).mPosition = position;
        UserModel user;
        if (mType != FollowshipAndBlockagesActivity.TYPE_BLOCKED)
            user = ((Followship) mItems.get(position)).getUser();
        else
            user = ((UserModel) mItems.get(position));


        if (!user.getPhoto().isEmpty())
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(user.getPhoto())
                    .placeholder(R.drawable.no_avatar)
                    .into(((ViewHolder) holder).mPhoto);
        else
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(R.drawable.no_avatar)
                    .into(((ViewHolder) holder).mPhoto);

        ((ViewHolder) holder).mUserName.setText(user.getName());
        ((ViewHolder) holder).mUsername.setText(user.getUsername());

        if (user.getId().equals(User.getInstance().getId()))
            ((ViewHolder) holder).mFollowButton.setVisibility(View.GONE);
        else {
            if (mType != FollowshipAndBlockagesActivity.TYPE_BLOCKED) {
                ((ViewHolder) holder).mFollowButton.setText(user.isFollowingUser() ? R.string.following : user.isFollowRequestSent() ? R.string.requested : R.string.follow);
                ((ViewHolder) holder).mFollowButton.setTextColor(user.isFollowingUser() ? ContextCompat.getColor(PhotosTalkApplication.getContext(), R.color.white) : ContextCompat.getColor(PhotosTalkApplication.getContext(), R.color.main));
                ((ViewHolder) holder).mFollowButton.setBackgroundResource(user.isFollowingUser() ? R.drawable.main_button : R.drawable.bordered_button_main);
            } else {
                ((ViewHolder) holder).mFollowButton.setText(R.string.unblock);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public UserModel getUser(int position) {
        UserModel u;
        if (mType != FollowshipAndBlockagesActivity.TYPE_BLOCKED)
            u = ((Followship) mItems.get(position)).getUser();
        else
            u = (UserModel) mItems.get(position);
        return u;
    }

    public void removeUser(UserModel user) {
        int position = getUserPosition(user);
        if (position == -1) return;
        mItems.remove(position);
    }

    public int getUserPosition(String userId) {
        int len = mItems.size();
        for (int i = 0; i < len; i++) {
            if (userId.equals(getUser(i).getId()))
                return i;
        }

        return -1;
    }

    public int getUserPosition(UserModel user) {
        return getUserPosition(user.getId());
    }

    public UserModel getUserById(String userId) {
        int len = mItems.size();
        for (int i = 0; i < len; i++) {
            UserModel user = getUser(i);
            if (user.getId().equals(userId))
                return user;
        }
        return null;
    }
}
