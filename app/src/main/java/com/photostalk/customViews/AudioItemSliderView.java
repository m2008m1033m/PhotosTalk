package com.photostalk.customViews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.photostalk.utils.MiscUtils;

import java.util.ArrayList;

public class AudioItemSliderView extends HorizontalScrollView {
    public static class AudioItem {
        private int mResId;
        private boolean mHasView;

        public AudioItem(int resId) {
            mResId = resId;
        }
    }

    public interface OnItemClickedListener {
        void onClicked(int position);
    }

    private LinearLayout mLinearLayout;

    private OnItemClickedListener mOnItemClickedListener;
    private ArrayList<AudioItem> mAudioItems = new ArrayList<>();
    private ArrayList<AudioItemView> mAudioItemViews = new ArrayList<>();

    private int mSelectedItemPosition = -1;


    public AudioItemSliderView(Context context) {
        super(context);
        init();
    }

    public AudioItemSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioItemSliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void addAudioItem(AudioItem item) {
        mAudioItems.add(item);
        recheck();
    }

    public void addAudioItems(ArrayList<AudioItem> items) {
        mAudioItems.addAll(items);
        recheck();
    }

    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mOnItemClickedListener = listener;
    }

    private void recheck() {
        for (int i = 0; i < mAudioItems.size(); i++) {
            AudioItem audioItem = mAudioItems.get(i);
            if (audioItem.mHasView) continue;

            /**
             * create a view for the audio item:
             */
            AudioItemView audioItemView = new AudioItemView(getContext());
            mLinearLayout.addView(audioItemView);
            LinearLayout.LayoutParams params = ((LinearLayout.LayoutParams) audioItemView.getLayoutParams());
            params.height = getLayoutParams().height;
            params.width = getLayoutParams().height;
            params.setMargins(0, 0, MiscUtils.convertDP2Pixel(10), 0);
            audioItemView.setLayoutParams(params);
            audioItemView.setImageResource(audioItem.mResId);
            audioItem.mHasView = true;
            mAudioItemViews.add(audioItemView);

            /**
             * keep track of the selected item
             */
            final int position = i;
            audioItemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    setSelectedItem(position);
                    if (mOnItemClickedListener != null) {
                        mOnItemClickedListener.onClicked(position);
                    }
                }
            });

            if (mSelectedItemPosition == -1) {
                setSelectedItem(i);
            }
        }
    }

    private void setSelectedItem(int position) {
        if (mSelectedItemPosition != -1)
            mAudioItemViews.get(mSelectedItemPosition).setItemSelected(false);
        mSelectedItemPosition = position;
        mAudioItemViews.get(mSelectedItemPosition).setItemSelected(true);

    }

    private void init() {
        /**
         * remove scrollbars
         */
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);

        /**
         * add the layout that will hold the items.
         */
        mLinearLayout = new LinearLayout(getContext());
        addView(mLinearLayout);
        FrameLayout.LayoutParams params = ((LayoutParams) mLinearLayout.getLayoutParams());
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
    }

}
