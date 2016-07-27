package com.photostalk.adapters;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.models.Notification;
import com.photostalk.utils.MiscUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by mohammed on 3/13/16.
 */
public class NotificationsFragmentAdapter extends RefreshAdapter {

    public interface OnActionListener {
        void onAcceptButtonClicked(int position);

        void onRejectButtonClicked(int position);

        void onFollowButtonClicked(int position);

        void onUserClicked(int position);

        void onItemClicked(int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private CardView mCardView;
        private ImageView mUserPhoto;
        private TextView mText;
        private TextView mDate;
        private Button mFollowButton;
        private ImageButton mAcceptButton;
        private ImageButton mRejectButton;
        private LinearLayout mAcceptReject;

        private int mPosition;

        public ViewHolder(View itemView) {
            super(itemView);

            mCardView = ((CardView) itemView.findViewById(R.id.card_view));
            mUserPhoto = ((ImageView) itemView.findViewById(R.id.photo));
            mText = ((TextView) itemView.findViewById(R.id.text));
            mDate = ((TextView) itemView.findViewById(R.id.date));
            mFollowButton = ((Button) itemView.findViewById(R.id.follow_button));
            mAcceptButton = ((ImageButton) itemView.findViewById(R.id.accept));
            mRejectButton = ((ImageButton) itemView.findViewById(R.id.reject));
            mAcceptReject = ((LinearLayout) itemView.findViewById(R.id.accept_reject));
        }
    }

    private ArrayList<Notification> mItems = new ArrayList<>();
    private OnActionListener mOnActionListener;


    public NotificationsFragmentAdapter(OnActionListener onActionListener) {
        mOnActionListener = onActionListener;
    }

    @Override
    public ArrayList getItems() {
        return mItems;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        final ViewHolder vh = new ViewHolder(v);

        vh.mAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onAcceptButtonClicked(vh.mPosition);
            }
        });


        vh.mRejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onRejectButtonClicked(vh.mPosition);
            }
        });

        vh.mFollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onFollowButtonClicked(vh.mPosition);
            }
        });

        vh.mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onItemClicked(vh.mPosition);
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
        Notification item = mItems.get(position);
        String text;

        switch (item.getType()) {
            case REQUEST:
                ((ViewHolder) holder).mFollowButton.setVisibility(View.GONE);
                ((ViewHolder) holder).mAcceptReject.setVisibility(View.VISIBLE);
                text = PhotosTalkApplication.getContext().getString(R.string.has_requested_to_follow_you, item.getUser().getName());
                break;
            case FOLLOW:
                ((ViewHolder) holder).mFollowButton.setVisibility(View.VISIBLE);
                ((ViewHolder) holder).mAcceptReject.setVisibility(View.GONE);
                ((ViewHolder) holder).mFollowButton.setTextColor(ContextCompat.getColor(PhotosTalkApplication.getContext(), item.getUser().isFollowingUser() ? R.color.white : R.color.main));
                ((ViewHolder) holder).mFollowButton.setBackgroundResource(item.getUser().isFollowingUser() ? R.drawable.main_button : R.drawable.bordered_button_main);
                ((ViewHolder) holder).mFollowButton.setText(item.getUser().isFollowingUser() ? R.string.following : item.getUser().isFollowRequestSent() ? R.string.requested : R.string.follow);
                ((ViewHolder) holder).mFollowButton.setEnabled(!item.getUser().isFollowRequestSent());
                text = PhotosTalkApplication.getContext().getString(R.string.has_started_following_you, item.getUser().getName());
                break;
            case REQUEST_ACCEPTANCE:
                ((ViewHolder) holder).mFollowButton.setVisibility(View.VISIBLE);
                ((ViewHolder) holder).mAcceptReject.setVisibility(View.GONE);
                ((ViewHolder) holder).mFollowButton.setTextColor(ContextCompat.getColor(PhotosTalkApplication.getContext(), item.getUser().isFollowingUser() ? R.color.white : R.color.main));
                ((ViewHolder) holder).mFollowButton.setBackgroundResource(item.getUser().isFollowingUser() ? R.drawable.main_button : R.drawable.bordered_button_main);
                ((ViewHolder) holder).mFollowButton.setText(item.getUser().isFollowingUser() ? R.string.following : item.getUser().isFollowRequestSent() ? R.string.requested : R.string.follow);
                ((ViewHolder) holder).mFollowButton.setEnabled(!item.getUser().isFollowRequestSent());
                text = PhotosTalkApplication.getContext().getString(R.string.has_accepted_your_follow_request, item.getUser().getName());
                break;
            case COMMENT:
                ((ViewHolder) holder).mFollowButton.setVisibility(View.GONE);
                ((ViewHolder) holder).mAcceptReject.setVisibility(View.GONE);
                text = PhotosTalkApplication.getContext().getString(R.string.s_has_commented_on_your_photo, item.getUser().getName());
                break;
            case LIKE:
                ((ViewHolder) holder).mFollowButton.setVisibility(View.GONE);
                ((ViewHolder) holder).mAcceptReject.setVisibility(View.GONE);
                text = PhotosTalkApplication.getContext().getString(R.string.s_has_liked_your_photo, item.getUser().getName());
                break;
            default:
                ((ViewHolder) holder).mFollowButton.setVisibility(View.GONE);
                ((ViewHolder) holder).mAcceptReject.setVisibility(View.GONE);
                text = "unknown notification type: " + item.getTypeName() + " with id " + item.getTypeId();
                break;
        }

        if (item.getUser().getPhoto().isEmpty() || item.getType() == Notification.Type.UNKNOWN) {
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(R.drawable.no_avatar)
                    .into(((ViewHolder) holder).mUserPhoto);
        } else {
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(item.getUser().getPhoto())
                    .placeholder(R.drawable.no_avatar)
                    .into(((ViewHolder) holder).mUserPhoto);
        }

        ((ViewHolder) holder).mText.setText(text);
        ((ViewHolder) holder).mDate.setText(MiscUtils.getDurationFormatted(item.getNotificationDate()));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void updateUser(String userId, boolean request, boolean follow) {
        for (int i = 0; i < mItems.size(); i++) {
            Notification item = mItems.get(i);
            if (item.getUser().getId().equals(userId)) {
                item.getUser().setIsFollowRequestSent(request);
                item.getUser().setIsFollowingUser(follow);
                this.notifyItemChanged(i);
            }
        }
    }
}
