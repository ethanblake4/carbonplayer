package com.carbonplayer.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.carbonplayer.model.entity.proto.innerjam.visuals.ImageReferenceV1Proto;

import static com.carbonplayer.utils.MathUtils.aspectHeightMultiple;


public class AspectView extends View {

        public ImageReferenceV1Proto.ImageReference.AspectRatio aspect =
                ImageReferenceV1Proto.ImageReference.AspectRatio.TWO_BY_ONE;
        public AspectView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {

            super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(
                    Math.round(Math.min((float) MeasureSpec.getSize(widthSpec) * aspectHeightMultiple(aspect),
                            MeasureSpec.getSize(heightSpec))),
                    MeasureSpec.EXACTLY));
        }

        public void setAspect(ImageReferenceV1Proto.ImageReference.AspectRatio aspect) {
            this.aspect = aspect;
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }
}
