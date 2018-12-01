package com.carbonplayer.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.carbonplayer.model.entity.proto.innerjam.visuals.ImageReferenceV1Proto;

import androidx.appcompat.widget.AppCompatImageView;

import static com.carbonplayer.utils.general.MathUtils.aspectHeightMultiple;

public class AspectImageView extends AppCompatImageView {
    public ImageReferenceV1Proto.ImageReference.AspectRatio aspect =
            ImageReferenceV1Proto.ImageReference.AspectRatio.FIVE_BY_ONE;

    public AspectImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {

        int aspectHeight = View.MeasureSpec.makeMeasureSpec(
                Math.round((float) View.MeasureSpec.getSize(widthSpec)
                        * aspectHeightMultiple(aspect)), View.MeasureSpec.EXACTLY);

        super.onMeasure(widthSpec, aspectHeight);
    }
}
