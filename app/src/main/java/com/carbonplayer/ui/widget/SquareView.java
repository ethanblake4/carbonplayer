package com.carbonplayer.ui.widget;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * A View that always has a 1:1 aspect ratio.
 */
public class SquareView extends View {

    public SquareView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int squareHeight = View.MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthSpec),
                MeasureSpec.EXACTLY);
        super.onMeasure(widthSpec, squareHeight);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}