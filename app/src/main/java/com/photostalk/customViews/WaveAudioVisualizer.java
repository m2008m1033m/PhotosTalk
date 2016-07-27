package com.photostalk.customViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;


public class WaveAudioVisualizer extends View {

    private short[] mData;
    private Paint mPaint;
    private int mHeight;
    private int mWidth;
    private int mDataLength;
    private Path mPath = new Path();

    public WaveAudioVisualizer(Context context) {
        super(context);
        init();
    }

    public WaveAudioVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveAudioVisualizer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(0xFFAAAAAA);
        mPaint.setStrokeWidth(5);
    }

    public void update(short[] data, int len) {
        mData = data;
        mDataLength = len;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int numberOfPointsToDraw = mDataLength;
        int pointStep = 1;

        if (numberOfPointsToDraw > 250) {
            numberOfPointsToDraw = 250;
            pointStep = mDataLength / numberOfPointsToDraw;
        }


        if (numberOfPointsToDraw == 0) return;
        float step = (float) mWidth / numberOfPointsToDraw;

        float previousX = 0;
        int previousY = mHeight / 2;


        float currentX = previousX + step;


        for (int i = 0; i < mDataLength; i += pointStep) {
            short value = mData[i];
            int currentY = value + 32767;
            currentY = (currentY * (mHeight / 2)) / 32767;


            canvas.drawLine(previousX, previousY, currentX, currentY, mPaint);
            previousX = currentX;
            previousY = currentY;
            currentX = previousX + step;
        }
    }
}
