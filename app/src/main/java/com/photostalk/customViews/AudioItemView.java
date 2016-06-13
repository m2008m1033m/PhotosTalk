package com.photostalk.customViews;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.photostalk.R;
import com.photostalk.utils.MiscUtils;


public class AudioItemView extends FrameLayout {
    private static int IMAGE_LENGTH = MiscUtils.convertDP2Pixel(50);

    public AudioItemView(Context context) {
        super(context);
        init();
    }

    public AudioItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.transparent));

    }

    public void setItemSelected(boolean isSelected) {
        if (isSelected)
            setBackgroundResource(R.drawable.circle_red);
        else
            setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.transparent));
    }

    public void setImageResource(int resId) {
        RoundImageView roundImageView = new RoundImageView(getContext());
        roundImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(roundImageView);
        LayoutParams params = ((LayoutParams) roundImageView.getLayoutParams());
        params.height = (int) (getLayoutParams().height * 0.90f);
        params.width = (int) (getLayoutParams().width * 0.90f);
        params.gravity = Gravity.CENTER;
        roundImageView.setLayoutParams(params);
        roundImageView.setImageResource(resId);
    }
}
