package com.carbonplayer.utils.general;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.carbonplayer.model.entity.proto.innerjam.visuals.ImageReferenceV1Proto.ImageReference;

public class MathUtils {

    private MathUtils() { }

    public static float constrain(float min, float max, float v) {
        return Math.max(min, Math.min(max, v));
    }

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public static int dpToPx2(Resources res, int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                res.getDisplayMetrics()));
    }

    public static int pxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    public static float aspectHeightMultiple(ImageReference.AspectRatio aspect) {
        switch (aspect) {
            case ONE_BY_ONE: return 1.0f;
            case TWO_BY_ONE: return 0.5f;
            case THREE_BY_FOUR: return 4.0f/3.0f;
            case FIVE_BY_ONE: return 1.0f/5.0f;
            case THREE_BY_TWO: return 2.0f/3.0f;
            case FOUR_BY_THREE: return 0.75f;
            default: return 1.0f;
        }
    }

    public static float aspectWidthMultiple(ImageReference.AspectRatio aspect) {
        switch (aspect) {
            case ONE_BY_ONE: return 1.0f;
            case TWO_BY_ONE: return 2.0f;
            case THREE_BY_FOUR: return 0.75f;
            case FIVE_BY_ONE: return 5.0f;
            case THREE_BY_TWO: return 1.5f;
            case FOUR_BY_THREE: return 4.0f/3.0f;
            default: return 1.0f;
        }
    }
}
