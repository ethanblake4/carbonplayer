package com.carbonplayer.ui.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;

/**
 * A extension of ImageView that is always 1:1 aspect ratio based on height.
 */
public class SquareHeightImageView extends AppCompatImageView {

    public SquareHeightImageView(Context context) {
        super (context);
    }

    public SquareHeightImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int squareWidth = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(heightSpec),
                View.MeasureSpec.EXACTLY);
        super.onMeasure(squareWidth, heightSpec);
    }
}