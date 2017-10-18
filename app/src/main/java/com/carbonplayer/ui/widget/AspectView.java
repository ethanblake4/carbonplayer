package com.carbonplayer.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.carbonplayer.R;
import com.carbonplayer.model.entity.proto.innerjam.visuals.ImageReferenceV1Proto.ImageReference;

import static com.carbonplayer.utils.general.MathUtils.aspectHeightMultiple;


public class AspectView extends View {

        public ImageReference.AspectRatio aspect =
                ImageReference.AspectRatio.TWO_BY_ONE;

        public AspectView(Context context, AttributeSet attrs) {

            super(context, attrs);

            final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AspectView);

            if(a.hasValue(R.styleable.AspectView_aspect)) {
                aspect = ImageReference.AspectRatio.forNumber(
                        a.getInt(R.styleable.AspectView_aspect, 0));
            }

            a.recycle();

        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {

            super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(
                    Math.round(Math.min((float) MeasureSpec.getSize(widthSpec) * aspectHeightMultiple(aspect),
                            MeasureSpec.getSize(heightSpec))),
                    MeasureSpec.EXACTLY));
        }

        public void setAspect(ImageReference.AspectRatio aspect) {
            this.aspect = aspect;
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }
}
