package com.photostalk.adapters;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.photostalk.PhotosTalkApplication;
import com.photostalk.R;
import com.photostalk.models.Comment;
import com.photostalk.utils.MiscUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * for this adapter there will three types of items:
 * the header always at the top
 * the footer is gonna hold the load more button and is gonna be at the bottom
 * the rest are the comments items
 */
public class PhotoActivityAdapter extends RefreshAdapter {


    public interface OnActionListener {

        void onPlayStopComment(Comment comment, int position, boolean isStopping);

        void onSeek(int progress);

        void onDeleteCommentClicked(int position);

        void onUserClicked(String userId);
    }


    /**
     * A ViewHolder for the comment view
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        private FrameLayout mWrapper;
        private FrameLayout mContainer;
        private ImageView mPhoto;
        private TextView mUserNameTextView;
        private TextView mDateTextView;
        private FrameLayout mPlayStopButton;
        private ImageView mPlayStopIcon;
        private ProgressBar mProgressBar;
        private AppCompatSeekBar mSeekBar;
        private View mSeparator;
        private Comment mComment;

        public ViewHolder(View itemView) {
            super(itemView);
            mWrapper = ((FrameLayout) itemView.findViewById(R.id.wrapper));
            mContainer = ((FrameLayout) itemView.findViewById(R.id.container));
            mPhoto = ((ImageView) itemView.findViewById(R.id.photo));
            mUserNameTextView = ((TextView) itemView.findViewById(R.id.full_name));
            mDateTextView = ((TextView) itemView.findViewById(R.id.date));
            mPlayStopButton = ((FrameLayout) itemView.findViewById(R.id.play_stop_button));
            mPlayStopIcon = ((ImageView) itemView.findViewById(R.id.button_icon));
            mProgressBar = ((ProgressBar) itemView.findViewById(R.id.progress));
            mSeekBar = ((AppCompatSeekBar) itemView.findViewById(R.id.seek_bar));
            mSeparator = itemView.findViewById(R.id.separator);
        }
    }

    private ArrayList<Comment> mItems = new ArrayList<>();
    private OnActionListener mOnActionListener;

    private int mPlayingComments = -1;
    private boolean mIsCommentLoading = false;


    public PhotoActivityAdapter(OnActionListener listener) {
        mOnActionListener = listener;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment, parent, false);

        final ViewHolder viewHolderComment = new ViewHolder(v);
        viewHolderComment.mPlayStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isStopping = mPlayingComments == viewHolderComment.getAdapterPosition();
                mOnActionListener.onPlayStopComment(viewHolderComment.mComment, viewHolderComment.getAdapterPosition(), isStopping);
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
                mOnActionListener.onDeleteCommentClicked(viewHolderComment.getAdapterPosition());
            }
        });

        viewHolderComment.mPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnActionListener.onUserClicked(mItems.get(viewHolderComment.getAdapterPosition()).getUser().getId());
            }
        });

        return viewHolderComment;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Comment comment = mItems.get(position);
        ((ViewHolder) holder).mUserNameTextView.setText(PhotosTalkApplication.getContext().getString(R.string.s_commented, comment.getUser().getName()));
        ((ViewHolder) holder).mDateTextView.setText("on " + comment.getCreatedAtAsString("MMM-dd") + " at " + comment.getCreatedAtAsString("hh:mm a"));
        playComment((ViewHolder) holder, position == mPlayingComments, mIsCommentLoading, position + 1 == mPlayingComments);
        changeLayout((ViewHolder) holder, position);

        ((ViewHolder) holder).mComment = comment;

        /**
         * load the user's photo
         */

        if (!comment.getUser().getPhoto().equals(""))
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(comment.getUser().getPhoto())
                    .into(((ViewHolder) holder).mPhoto);
        else
            Picasso.with(PhotosTalkApplication.getContext())
                    .load(R.drawable.no_avatar)
                    .into(((ViewHolder) holder).mPhoto);

    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public ArrayList<Comment> getItems() {
        return mItems;
    }

    /**
     * All the  methods here
     * belongs to the header
     */

    public void setPlayingComments(int playingComment) {
        if (playingComment == -1 && mPlayingComments != -1) {
            int currentlyPlaying = mPlayingComments;
            mPlayingComments = -1;
            notifyItemChanged(currentlyPlaying - 1);
            notifyItemChanged(currentlyPlaying);
        } else if (playingComment == -1)
            mPlayingComments = -1;
        else {
            mPlayingComments = playingComment;
            notifyItemChanged(mPlayingComments - 1);
            notifyItemChanged(mPlayingComments);
        }
    }

    private void playComment(ViewHolder holder, boolean play, boolean loading, boolean nextPlaying) {
        holder.mWrapper.setBackgroundResource(nextPlaying ? R.color.light : R.color.white);
        holder.mContainer.setBackgroundResource(play ? R.color.light : R.color.white);
        holder.mUserNameTextView.setTextColor(ContextCompat.getColor(PhotosTalkApplication.getContext(), play ? R.color.main : R.color.black));
        holder.mPlayStopIcon.setImageResource(play ? R.drawable.stop : R.drawable.play);
        holder.mPlayStopIcon.setVisibility(play && loading ? View.GONE : View.VISIBLE);
        holder.mProgressBar.setVisibility(play && loading ? View.VISIBLE : View.GONE);
        holder.mSeekBar.setVisibility(play ? View.VISIBLE : View.GONE);
    }

    private void changeLayout(ViewHolder holder, int position) {
        int shift = position == 0 ? 15 : 0;
        int shiftPx = MiscUtils.convertDP2Pixel(shift);

        holder.mWrapper.getLayoutParams().height = MiscUtils.convertDP2Pixel(87 + shift);

        int paddingPx = MiscUtils.convertDP2Pixel(15);
        holder.mContainer.setPadding(paddingPx, shiftPx, paddingPx, 0);
        holder.mContainer.getLayoutParams().height = MiscUtils.convertDP2Pixel(71 + shift);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.mSeekBar.getLayoutParams();
        params.setMargins(0, MiscUtils.convertDP2Pixel(55 + shift), 0, 0);
        holder.mSeekBar.setLayoutParams(params);

        params = (FrameLayout.LayoutParams) holder.mSeparator.getLayoutParams();
        params.setMargins(0, MiscUtils.convertDP2Pixel(71 + shift), 0, 0);
        holder.mSeparator.setLayoutParams(params);
    }

    public void setIsCommentLoading(boolean loading) {
        mIsCommentLoading = loading;
    }

    public AppCompatSeekBar getActiveSeekBar(RecyclerView recyclerView) {
        AppCompatSeekBar appCompatSeekBar = null;
        int firstVisible = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        int lastVisible = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
        for (int i = firstVisible; i <= lastVisible; i++) {
            RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(i);
            if (!(vh instanceof ViewHolder)) continue;
            if (mPlayingComments == i) {
                appCompatSeekBar = ((ViewHolder) vh).mSeekBar;
                break;
            }
        }
        return appCompatSeekBar;
    }
}
