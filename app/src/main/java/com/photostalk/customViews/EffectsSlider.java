package com.photostalk.customViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.photostalk.R;
import com.photostalk.customListeners.OnSwipeTouchListener;

/**
 * Created by mohammed on 2/29/16.
 */
public class EffectsSlider extends FrameLayout {

    private ImageView mImageView;
    private Paint mFillPaint;
    private Paint mStrokePaint;
    private int mColors[];
    private int mMaxHeight;
    private int mMaxWidth;
    private int mMarginBottom = 30; //dp
    private int mMarginBetween = 4; //dp
    private int mRadius = 5; //dp
    private int mStrokeWidth = 1; //dp
    private int mCircleStokeSpacing = 2; //dp
    private int mCurrentSelection = 0;
    private boolean mIsEditing = false;

    public EffectsSlider(Context context) {
        super(context);
    }

    public EffectsSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EffectsSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setEditing(boolean isEditing) {
        mIsEditing = isEditing;
        invalidate();
    }

    public int getCurrentColor() {
        return mColors[mCurrentSelection];
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws Exception {
        /**
         * make sure that this has only one
         * child and that child is an ImageView
         */
        if (getChildCount() != 1) {
            throw new Exception("There should be only one child and it should an ImageView");
        } else if (!(getChildAt(0) instanceof ImageView)) {
            throw new Exception("The child held up by the EffectSlider should be ImageView");
        }

        /**
         * getting the image view
         */
        mImageView = ((ImageView) getChildAt(0));

        /**
         * loading the colors
         */
        mColors = getResources().getIntArray(R.array.filter_colors);
        /*int ii = R.color.color_filter_1;
        for (int i = 0; i < mColors.length; i++)
            mColors[i] = ContextCompat.getColor(getContext(), mColors[i]);*/

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        /**
         * calculating the margin bottom
         */
        mMarginBottom = Math.round(mMarginBottom * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

        /**
         * calculating the margin between
         */
        mMarginBetween = Math.round(mMarginBetween * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

        /**
         * calculating the radius
         */
        mRadius = Math.round(mRadius * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

        /**
         * calculating the stroke
         */
        mStrokeWidth = Math.round(mStrokeWidth * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

        /**
         * calculating the space between the circle and the stroke
         */
        mCircleStokeSpacing = Math.round(mCircleStokeSpacing * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));

        mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFillPaint.setStyle(Paint.Style.FILL);

        mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(mStrokeWidth);
        mStrokePaint.setColor(ContextCompat.getColor(getContext(), R.color.white));

        setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeLeft() {
                changeFilter(1);
            }

            @Override
            public void onSwipeRight() {
                changeFilter(-1);
            }
        });
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mMaxHeight = MeasureSpec.getSize(heightMeasureSpec);
        mMaxWidth = MeasureSpec.getSize(widthMeasureSpec);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        drawCircles(canvas);
    }

    private void drawCircles(Canvas canvas) {
        if (!mIsEditing) return;
        float cy = mMaxHeight - (2 * mRadius) - mMarginBottom;
        float totalX = (mRadius + mMarginBetween) * 2 * mColors.length;
        float cx = (mMaxWidth - totalX) / 2;
        cx += mMarginBetween + mRadius;

        for (int i = 0; i < mColors.length; i++) {
            int color = mColors[i];
            if (mCurrentSelection == i)
                canvas.drawCircle(cx, cy, mRadius + mCircleStokeSpacing, mStrokePaint);

            mFillPaint.setColor(color);
            canvas.drawCircle(cx, cy, mRadius, mFillPaint);
            cx += 2 * (mRadius + mMarginBetween);
        }
    }

    private void changeFilter(int incDec) {
        if (!mIsEditing) return;
        mCurrentSelection += incDec;
        if (mCurrentSelection >= mColors.length) mCurrentSelection = mColors.length - 1;
        else if (mCurrentSelection < 0) mCurrentSelection = 0;
        int color = mColors[mCurrentSelection];
        mImageView.setColorFilter(color, PorterDuff.Mode.LIGHTEN);
        invalidate();
    }
}
