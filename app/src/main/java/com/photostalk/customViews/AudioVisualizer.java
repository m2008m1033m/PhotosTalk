package com.photostalk.customViews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.photostalk.R;

/**
 * Created by mohammed on 2/27/16.
 */
public class AudioVisualizer extends View {

    private int mMaxHeight;
    private int mMaxWidth;
    private int mMaxRadius;
    private int mCurrentRadius;
    private Paint mCirclePaint;
    private int mMinRadius;

    private float mRadiusOffset = 0;

    public AudioVisualizer(Context context) {
        super(context);
        init();
    }

    public AudioVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AudioVisualizer, 0, 0);
        mRadiusOffset = ta.getDimension(R.styleable.AudioVisualizer_centerY, 0);
        ta.recycle();
        init();
    }

    public AudioVisualizer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setColor(Color.BLACK);
        mCirclePaint.setAlpha(170);
        mCurrentRadius = 0;
        mMinRadius = dpToPx(28);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(mMaxWidth / 2, (mMaxHeight / 2) + mRadiusOffset, mCurrentRadius, mCirclePaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mMaxHeight = MeasureSpec.getSize(heightMeasureSpec);
        mMaxWidth = MeasureSpec.getSize(widthMeasureSpec);
        mMaxRadius = Math.min(mMaxWidth, mMaxHeight);
        setMeasuredDimension(mMaxWidth, mMaxHeight);
    }

    public void update(int amplitude) {
        /**
         *
         * maxRadius => 32000
         * ?         => amplitude
         *
         */

        mCurrentRadius = (mMaxRadius * amplitude / 22000) + mMinRadius;
        invalidate();
    }

    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }


}
