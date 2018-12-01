package com.carbonplayer.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * A extension of ImageView that is always 1:1 aspect ratio.
 *
 * @author Nick Butcher https://github.com/nickbutcher/plaid/
 */
public class SquareImageView extends AppCompatImageView {

    public SquareImageView(Context context) {
        super (context);
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int squareHeight = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(widthSpec),
                View.MeasureSpec.EXACTLY);
        super.onMeasure(widthSpec, squareHeight);
    }
}
