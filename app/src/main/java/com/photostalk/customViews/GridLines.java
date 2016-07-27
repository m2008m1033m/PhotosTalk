package com.photostalk.customViews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;


public class GridLines extends View {

    private class Line {
        float startX, startY, stopX, stopY;

        public void setLine(float startX, float startY, float stopX, float stopY) {
            this.startX = startX;
            this.startY = startY;
            this.stopX = stopX;
            this.stopY = stopY;
        }
    }

    private Paint mLinePaint;
    private Line[] mLines;
    private float mMaxWidth = 0;
    private float mMaxHeight = 0;

    public GridLines(Context context) {
        super(context);
        init();
    }

    public GridLines(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GridLines(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStrokeWidth(4f);
        mLinePaint.setColor(Color.WHITE);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);


        mLines = new Line[4];
        for (int i = 0; i < mLines.length; i++)
            mLines[i] = new Line();


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        mMaxHeight = MeasureSpec.getSize(heightMeasureSpec);
        mMaxWidth = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension((int) mMaxWidth, (int) mMaxHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /**
         * drawing the main grid lines:
         */
        mLines[0].setLine(mMaxWidth / 3, 0, mMaxWidth / 3, mMaxHeight);
        mLines[1].setLine(2 * mMaxWidth / 3, 0, 2 * mMaxWidth / 3, mMaxHeight);
        mLines[2].setLine(0, mMaxHeight / 3, mMaxWidth, mMaxHeight / 3);
        mLines[3].setLine(0, 2 * mMaxHeight / 3, mMaxWidth, 2 * mMaxHeight / 3);

        for (Line l : mLines) {
            canvas.drawLine(l.startX, l.startY, l.stopX, l.stopY, mLinePaint);
        }

        /**
         * drawing the central circle
         */
        float radius = 0.15f * mMaxWidth;
        float centerX = mMaxWidth / 2;
        float centerY = mMaxHeight / 2;
        float accuracyLinesLength = 0.03f * mMaxWidth;

        canvas.drawCircle(centerX, centerY, radius, mLinePaint);

        /**
         * drawing the accuracy lines in the circle
         */
        canvas.drawLine(centerX + radius - accuracyLinesLength, centerY, centerX + radius, centerY, mLinePaint); //right
        canvas.drawLine(centerX, centerY - radius + accuracyLinesLength, centerX, centerY - radius, mLinePaint); //top
        canvas.drawLine(centerX - radius + accuracyLinesLength, centerY, centerX - radius, centerY, mLinePaint); //left
        canvas.drawLine(centerX, centerY + radius - accuracyLinesLength, centerX, centerY + radius, mLinePaint); //bottom
    }
}
