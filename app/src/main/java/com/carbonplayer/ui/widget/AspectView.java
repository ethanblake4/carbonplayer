package com.carbonplayer.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.carbonplayer.model.entity.proto.innerjam.visuals.ImageReferenceV1Proto.ImageReference;

import static com.carbonplayer.utils.general.MathUtils.aspectHeightMultiple;


public class AspectView extends View {

        public ImageReference.AspectRatio aspect =
                ImageReference.AspectRatio.THREE_BY_TWO;

        public AspectView(Context context, AttributeSet attrs) {

            super(context, attrs);
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {

            int aspectHeight = MeasureSpec.makeMeasureSpec(
                    Math.round((float) MeasureSpec.getSize(widthSpec)
                            * aspectHeightMultiple(aspect)), MeasureSpec.EXACTLY);

            super.onMeasure(widthSpec, aspectHeight);
        }

        public void setAspect(ImageReference.AspectRatio aspect) {
            this.aspect = aspect;
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }
}
