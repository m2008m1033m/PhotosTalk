package com.photostalk.adapters;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.core.User;
import com.photostalk.models.Comment;
import com.photostalk.models.Photo;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * for this adapter there will three types of items:
 * the header always at the top
 * the footer is gonna hold the load more button and is gonna be at the bottom
 * the rest are the comments items
 */
public class PhotosActivityAdapter extends RefreshAdapter {

    /**
     * there are three types of items for
     * this adapter:
     * The header in which the actual image will be diaplayed
     * The footer in which the show more comments will appear
     * The regular items which will be the comments
     */
    private final int HEADER = 0;
    private final int ITEM = 1;
    private final int FOOTER = 2;


    /**
     * the state of the play button:
     * - Stopped
     * - Playing
     * - Loading
     */
    public final static int PLAY = 0;
    public final static int STOP = 1;
    public final static int LOAD = 2;

    public interface OnActionListener {
        void onBackButtonClicked();

        void onMenuButtonClicked(View v);

        void onLikeButtonClicked();

        void onShareButtonClicked();

        void onRecordButtonClicked();

        void onPlayButtonClicked();

        void onPlayStopComment(Comment comment, int position, boolean isStopping);

        void onSeek(int progress);

        void onDeleteCommentClicked(int position);

        void onUserClicked(String userId);
    }

    /**
     * A ViewHolderTrending for the header view
     */
    class ViewHolderHeader extends RecyclerView.ViewHolder {
        private ImageButton mBackButton;
        private ImageButton mMenuButton;
        private ImageView mUserPhoto;
        private ImageView mPhoto;
        private ImageView mPlayStopButton;
        private ImageButton mLikeButton;
        private ImageButton mMicButton;
        private ImageButton mShareButton;
        private TextView mStoryTitle;
        private TextView mUsername;
        private TextView mLiveTextView;
        private TextView mHashTagsTextView;
        private TextView mLikeNumberTextView;
        private TextView mCommentsNumber;
        private ProgressBar mProgressBar;

        public ViewHolderHeader(View itemView) {
            super(itemView);
            mUserPhoto = ((ImageView) itemView.findViewById(R.id.user_photo));
            mPhoto = ((ImageView) itemView.findViewById(R.id.photo));
            mPlayStopButton = ((ImageView) itemView.findViewById(R.id.play_stop_button));
            mBackButton = ((ImageButton) itemView.findViewById(R.id.back_button));
            mMenuButton = ((ImageButton) itemView.findViewById(R.id.menu_button));
            mLikeButton = ((ImageButton) itemView.findViewById(R.id.like_button));
            mShareButton = ((ImageButton) itemView.findViewById(R.id.share_button));
            mMicButton = ((ImageButton) itemView.findViewById(R.id.mic_button));

            mStoryTitle = ((TextView) itemView.findViewById(R.id.date));
            mUsername = ((TextView) itemView.findViewById(R.id.username));
            mLiveTextView = ((TextView) itemView.findViewById(R.id.is_live_text_view));
            mHashTagsTextView = ((TextView) itemView.findViewById(R.id.hash_tags_text_view));
            mLikeNumberTextView = ((TextView) itemView.findViewById(R.id.likes_number_text_view));
            mCommentsNumber = ((TextView) itemView.findViewById(R.id.comments_number_text_view));
            mProgressBar = ((ProgressBar) itemView.findViewById(R.id.progress));
        }
    }


    /**
     * A ViewHolderTrending for the comment view
     */
    class ViewHolderComment extends RecyclerView.ViewHolder {

        private FrameLayout mWrapper;
        private FrameLayout mContainer;
        private ImageView mPhoto;
        private TextView mUserNameTextView;
        private FrameLayout mPlayStopButton;
        private ImageView mPlayStopIcon;
        private ProgressBar mProgressBar;
        private AppCompatSeekBar mSeekBar;
        private Comment mComment;
        private int mPosition;

        public ViewHolderComment(View itemView) {
            super(itemView);
            mWrapper = ((FrameLayout) itemView.findViewById(R.id.wrapper));
            mContainer = ((FrameLayout) itemView.findViewById(R.id.container));
            mPhoto = ((ImageView) itemView.findViewById(R.id.photo));
            mUserNameTextView = ((TextView) itemView.findViewById(R.id.full_name));
            mPlayStopButton = ((FrameLayout) itemView.findViewById(R.id.play_stop_button));
            mPlayStopIcon = ((ImageView) itemView.findViewById(R.id.button_icon));
            mProgressBar = ((ProgressBar) itemView.findViewById(R.id.progress));
            mSeekBar = ((AppCompatSeekBar) itemView.findViewById(R.id.seek_bar));

            /*itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    *//**
             * setting the color of the progress bar
             *//*
                    mProgressBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                    return false;
                }
            });*/
        }
    }

    private Photo mPhoto;

    private ViewHolderHeader mViewHolderHeader;
    private OnActionListener mOnActionListener;

    private int mPlayingComments = -1;
    private boolean mIsCommentLoading = false;


    public PhotosActivityAdapter(OnActionListener listener) {
        mOnActionListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return HEADER;
        else
            return ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == HEADER) return setUpHeaderViewHolder(parent);
        else if (viewType == ITEM) return setUpItemViewHolder(parent);
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position == 0)
            bindHeaderToPhoto(((ViewHolderHeader) holder));
        else
            bindComments((ViewHolderComment) holder, position - 1);
    }

    @Override
    public int getItemCount() {
        return 1 + ((mPhoto != null) ? mPhoto.getComments().size() : 0);
    }

    @Override
    public ArrayList getItems() {
        return mPhoto.getComments();
    }

    /**
     * All the  methods here
     * belongs to the header
     */

    public void setPhoto(Photo photo) {
        mPhoto = photo;
        notifyDataSetChanged();
    }

    public void setHeaderState(int state) {
        switch (state) {
            case PLAY:
                mViewHolderHeader.mPlayStopButton.setImageResource(R.drawable.stop_blue);
                mViewHolderHeader.mProgressBar.setVisibility(View.GONE);
                break;
            case STOP:
                mViewHolderHeader.mPlayStopButton.setImageResource(R.drawable.play_blue);
                mViewHolderHeader.mProgressBar.setVisibility(View.GONE);
                break;
            case LOAD:
                mViewHolderHeader.mPlayStopButton.setImageResource(R.drawable.crystal_button);
                mViewHolderHeader.mProgressBar.setVisibility(View.VISIBLE);
                break;
        }
    }

    private ViewHolderHeader setUpHeaderViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_screen_header, parent, false);


        mViewHolderHeader = new ViewHolderHeader(v);

        if (mOnActionListener != null) {
            /**
             * setup an event when the back button is clicked
             */
            mViewHolderHeader.mBackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnActionListener.onBackButtonClicked();
                }
            });

            /**
             * setup an event when the menu button is clicked
             */
            mViewHolderHeader.mMenuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnActionListener.onMenuButtonClicked(view);
                }
            });

            /**
             * setup an event when the like button is clicked
             */
            mViewHolderHeader.mLikeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnActionListener.onLikeButtonClicked();
                }
            });

            /**
             * setup an event when the share button is clicked
             */
            mViewHolderHeader.mShareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnActionListener.onShareButtonClicked();
                }
            });

            /**
             * setup an event when the record button is clicked
             */
            mViewHolderHeader.mMicButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnActionListener.onRecordButtonClicked();
                }
            });

            /**
             * setup an event when the play button is clicked
             */
            mViewHolderHeader.mPlayStopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnActionListener.onPlayButtonClicked();
                }
            });

            /**
             * setup an event when the user photo is clicked
             */
            mViewHolderHeader.mUserPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnActionListener.onUserClicked(mPhoto.getUser().getId());
                }
            });
        }

        return mViewHolderHeader;
    }

    private void bindHeaderToPhoto(ViewHolderHeader holder) {
        if (mPhoto == null) return;

        /**
         * the actual photo
         */
        Glide.with(PhotosTalkApplication.getContext())
                .load(mPhoto.getImageUrl())
                .into(holder.mPhoto);

        /**
         * the user's photo
         */
        if (!mPhoto.getUser().getPhoto().equals(""))
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(mPhoto.getUser().getPhoto())
                    .placeholder(R.drawable.no_avatar)
                    .into(holder.mUserPhoto);
        else
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(R.drawable.no_avatar)
                    .into(holder.mUserPhoto);

        holder.mStoryTitle.setText(mPhoto.getStory().getStoryDateAsString("dd - MMMM"));
        holder.mUsername.setText(mPhoto.getUser().getName());
        holder.mLiveTextView.setVisibility(mPhoto.isLive() ? View.VISIBLE : View.GONE);
        holder.mHashTagsTextView.setText(mPhoto.getHashTagsConcatenated());
        holder.mLikeNumberTextView.setText(mPhoto.getLikesCount() + "");
        holder.mCommentsNumber.setText(mPhoto.getCommentsCount() + "");
        holder.mLikeButton.setImageResource(mPhoto.isLiked() ? R.drawable.like_blue : R.drawable.empty_heart);

        holder.mPlayStopButton.setVisibility(mPhoto.getAudioUrl().isEmpty() ? View.GONE : View.VISIBLE);
    }


    /**
     * All the  methods here
     * belongs to the items
     */

    private ViewHolderComment setUpItemViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment, parent, false);

        final ViewHolderComment viewHolderComment = new ViewHolderComment(v);
        viewHolderComment.mPlayStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isStopping = mPlayingComments == viewHolderComment.mPosition;
                mOnActionListener.onPlayStopComment(viewHolderComment.mComment, viewHolderComment.mPosition, isStopping);
            }
        });


        viewHolderComment.mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                if (!b) return;
                mOnActionListener.onSeek(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        viewHolderComment.mWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onDeleteCommentClicked(viewHolderComment.mPosition);
            }
        });

        viewHolderComment.mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onUserClicked(mPhoto.getComments().get(viewHolderComment.mPosition).getUser().getId());
            }
        });

        return viewHolderComment;
    }

    public void setPlayingComments(int playingComment) {
        mPlayingComments = playingComment;
    }

    private void bindComments(ViewHolderComment holder, int position) {
        Comment comment = mPhoto.getComments().get(position);
        holder.mUserNameTextView.setText(comment.getUser().getName() + PhotosTalkApplication.getContext().getString(R.string.commented));
        playComment(holder, position == mPlayingComments, mIsCommentLoading, position + 1 == mPlayingComments);

        holder.mPosition = position;
        holder.mComment = comment;

        /**
         * load the user's photo
         */
        if (!comment.getUser().getPhoto().equals(""))
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(comment.getUser().getPhoto())
                    .into(holder.mPhoto);
    }

    private void playComment(ViewHolderComment holder, boolean play, boolean loading, boolean nextPlaying) {
        holder.mWrapper.setBackgroundResource(nextPlaying ? R.color.light : R.color.white);
        holder.mContainer.setBackgroundResource(play ? R.color.light : R.color.white);
        holder.mUserNameTextView.setTextColor(ContextCompat.getColor(PhotosTalkApplication.getContext(), play ? R.color.main : R.color.black));
        holder.mPlayStopIcon.setImageResource(play ? R.drawable.stop : R.drawable.play);
        holder.mPlayStopIcon.setVisibility(play && loading ? View.GONE : View.VISIBLE);
        holder.mProgressBar.setVisibility(play && loading ? View.VISIBLE : View.GONE);
        holder.mSeekBar.setVisibility(play ? View.VISIBLE : View.GONE);
    }

    public void setIsCommentLoading(boolean loading) {
        mIsCommentLoading = loading;
    }

    public AppCompatSeekBar getActiveSeekbar(RecyclerView recyclerView) {
        AppCompatSeekBar appCompatSeekBar = null;
        int firstVisible = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        int lastVisible = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
        for (int i = firstVisible; i <= lastVisible; i++) {
            RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(i);
            if (!(vh instanceof ViewHolderComment)) continue;
            if (mPlayingComments + 1 == i) {
                Log.d("PhotosActivityAdapter", "Position is: " + i);
                appCompatSeekBar = ((ViewHolderComment) vh).mSeekBar;
                break;
            }
        }
        return appCompatSeekBar;
    }

    public void updateUserPhoto() {
        mPhoto.getUser().setPhoto(User.getInstance().getPhoto());
        notifyItemRemoved(0);
    }

}
