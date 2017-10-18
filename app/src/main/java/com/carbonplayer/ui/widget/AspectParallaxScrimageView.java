package com.carbonplayer.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.carbonplayer.R;
import com.carbonplayer.model.entity.proto.innerjam.visuals.ImageReferenceV1Proto;

import static com.carbonplayer.utils.general.MathUtils.aspectHeightMultiple;

public class AspectParallaxScrimageView extends ParallaxScrimageViewSz {

    public ImageReferenceV1Proto.ImageReference.AspectRatio aspect =
            ImageReferenceV1Proto.ImageReference.AspectRatio.FOUR_BY_THREE;

    public AspectParallaxScrimageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = getContext()
                .obtainStyledAttributes(attrs, R.styleable.AspectParallaxScrimageView);

        if(a.hasValue(R.styleable.AspectParallaxScrimageView_imageAspect)) {
            aspect = ImageReferenceV1Proto.ImageReference.AspectRatio.values()[
                    a.getInt(R.styleable.AspectParallaxScrimageView_imageAspect, 0)];
        }

        a.recycle();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {

        int aspectHeight = MeasureSpec.makeMeasureSpec(
                Math.round((float) MeasureSpec.getSize(widthSpec)
                        * aspectHeightMultiple(aspect)), MeasureSpec.EXACTLY);

        super.onMeasure(widthSpec, aspectHeight);
    }

}
