/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carbonplayer.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Property;

import com.carbonplayer.R;
import com.carbonplayer.model.entity.proto.innerjam.visuals.ImageReferenceV1Proto.ImageReference;
import com.carbonplayer.utils.ui.AnimUtils;
import com.carbonplayer.utils.ui.ColorUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.view.ViewCompat;


/**
 * An image view which supports parallax scrolling and applying a scrim onto it's content. Get it.
 *
 * It also has a custom pinned state, for use via state lists.
 */
public class ParallaxFadeScrimageView extends AppCompatImageView {

    private ImageReference.AspectRatio aspectRatio;

    private int scrimFadeOffset = 40;
    private static final int[] STATE_PINNED = {R.attr.state_pinned};
    private final Paint scrimPaint;
    private int imageOffset;
    private int minOffset;
    private float scrimAlpha = 0f;
    private float maxScrimAlpha = 1f;
    private int scrimColor = 0x00000000;
    private int scrimColor2 = 0x00000000;
    private float parallaxFactor = -0.5f;
    private boolean isPinned = false;
    private boolean immediatePin = false;

    GradientDrawable grad;

    public static final Property<ParallaxFadeScrimageView, Float> OFFSET = new AnimUtils
            .FloatProperty<ParallaxFadeScrimageView>("offset") {

        @Override
        public void setValue(ParallaxFadeScrimageView parallaxFadeScrimageView, float value) {
            parallaxFadeScrimageView.setOffset(value);
        }

        @Override
        public Float get(ParallaxFadeScrimageView parallaxFadeScrimageView) {
            return parallaxFadeScrimageView.getOffset();
        }
    };

    public ParallaxFadeScrimageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        scrimPaint = new Paint();
        scrimPaint.setColor(ColorUtils.modifyAlpha(scrimColor, scrimAlpha));

        grad = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {
                    ColorUtils.modifyAlpha(scrimColor, scrimAlpha),
                    ColorUtils.modifyAlpha(scrimColor2, 1.0f)}
                );

        grad.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        grad.setShape(GradientDrawable.RECTANGLE);

        grad.setSize(400, 400);
    }

    public float getOffset() {
        return getTranslationY();
    }

    public void setOffset(float offset) {
        offset = Math.max(minOffset, offset);
        if (offset != getTranslationY()) {
            setTranslationY(offset);
            imageOffset = (int) (offset * parallaxFactor);
            setScrimAlpha(Math.min((-offset / getMinimumHeight()) * maxScrimAlpha, maxScrimAlpha));
            ViewCompat.postInvalidateOnAnimation(this);
        }
        setPinned(offset == minOffset);
    }

    public void setScrimColor(@ColorInt int scrimColor) {
        if (this.scrimColor != scrimColor) {
            this.scrimColor = scrimColor;
            ViewCompat.postInvalidateOnAnimation(this);

        }
    }

    public void setScrimColor2(@ColorInt int scrimColor2) {
        if (this.scrimColor2 != scrimColor2) {
            this.scrimColor2 = scrimColor2;
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void setAspectRatio (ImageReference.AspectRatio aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public void setScrimAlpha(@FloatRange(from = 0f, to = 1f) float alpha) {
        if (scrimAlpha != alpha) {
            scrimAlpha = alpha;
            scrimPaint.setColor(ColorUtils.modifyAlpha(scrimColor, scrimAlpha));
            grad = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {
                            ColorUtils.modifyAlpha(scrimColor, scrimAlpha),
                            ColorUtils.modifyAlpha(scrimColor2, 1.0f)}
            );
            grad.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            grad.setShape(GradientDrawable.RECTANGLE);

            grad.setSize(400, 400);
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (h > getMinimumHeight()) {
            minOffset = getMinimumHeight() - h;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (imageOffset != 0) {
            canvas.save();
            canvas.translate(0f, imageOffset);
            canvas.clipRect(0f, 0f, canvas.getWidth(), canvas.getHeight() + imageOffset);
            super.onDraw(canvas);
            grad.draw(canvas);
            /*if(aspectRatio != null) {
                /*canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), scrimPaint);

                canvas.drawRect(0, canvas.getWidth() * MathUtils.aspectHeightMultiple(aspectRatio) - scrimFadeOffset,
                        canvas.getWidth(), canvas.getWidth() * MathUtils.aspectHeightMultiple(aspectRatio), scrimFadePaint);

                if (canvas.getHeight() > MathUtils.aspectHeightMultiple(aspectRatio) * canvas.getWidth()) {
                    canvas.drawRect(0, canvas.getWidth() * MathUtils.aspectHeightMultiple(aspectRatio),
                            canvas.getWidth(), canvas.getHeight(), scrimFadePaint);
                }
                grad.draw(canvas);
            }*/
            canvas.restore();
        } else {
            super.onDraw(canvas);
            grad.draw(canvas);
            if(aspectRatio != null) {
                /*canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), scrimPaint);

                canvas.drawRect(0, canvas.getWidth() * MathUtils.aspectHeightMultiple(aspectRatio) - scrimFadeOffset,
                        canvas.getWidth(), canvas.getWidth() * MathUtils.aspectHeightMultiple(aspectRatio), scrimFadePaint);

                if (canvas.getHeight() > MathUtils.aspectHeightMultiple(aspectRatio) * canvas.getWidth()) {
                    canvas.drawRect(0, canvas.getWidth() * MathUtils.aspectHeightMultiple(aspectRatio),
                            canvas.getWidth(), canvas.getHeight(), scrimFadePaint);
                }*/
                //grad.draw(canvas);
            }
        }
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isPinned) {
            mergeDrawableStates(drawableState, STATE_PINNED);
        }
        return drawableState;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean isPinned) {
        if (this.isPinned != isPinned) {
            this.isPinned = isPinned;
            refreshDrawableState();
            if (isPinned && immediatePin) {
                jumpDrawablesToCurrentState();
            }
        }
    }

    public boolean isImmediatePin() {
        return immediatePin;
    }

    /**
     * As the pinned state is designed to work with a {@see StateListAnimator}, we may want to short
     * circuit this animation in certain situations e.g. when flinging a list.
     */
    public void setImmediatePin(boolean immediatePin) {
        this.immediatePin = immediatePin;
    }
}