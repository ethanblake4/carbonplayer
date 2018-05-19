package com.carbonplayer.ui.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.carbonplayer.model.entity.proto.innerjam.visuals.ImageReferenceV1Proto;

import static com.carbonplayer.utils.general.MathUtils.aspectHeightMultiple;

public class AspectImageView extends AppCompatImageView {
    public ImageReferenceV1Proto.ImageReference.AspectRatio aspect =
            ImageReferenceV1Proto.ImageReference.AspectRatio.FIVE_BY_ONE;

    public AspectImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {

        int aspectHeight = MeasureSpec.makeMeasureSpec(
                Math.round((float) MeasureSpec.getSize(widthSpec)
                        * aspectHeightMultiple(aspect)), MeasureSpec.EXACTLY);

        super.onMeasure(widthSpec, aspectHeight);
    }
}
