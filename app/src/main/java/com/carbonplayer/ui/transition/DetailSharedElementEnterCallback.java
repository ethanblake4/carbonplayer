package com.carbonplayer.ui.transition;

import android.app.SharedElementCallback;
import android.content.res.Resources;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.carbonplayer.ui.main.AlbumActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetailSharedElementEnterCallback extends SharedElementCallback {

    private final TransitionSet mTransitionSet;
    private final AlbumActivity mActivity;

    public Map<TextView, Pair<Integer, Integer>> textViewList = new HashMap<>();


    public DetailSharedElementEnterCallback(AlbumActivity activity) {

        mActivity = activity;

        Transition transitionWindow = activity.getWindow().getSharedElementEnterTransition();

        if (!(transitionWindow instanceof TransitionSet)) {
            mTransitionSet = new TransitionSet();
            mTransitionSet.addTransition(transitionWindow);
        } else {
            mTransitionSet = (TransitionSet) transitionWindow;
        }

        activity.setEnterSharedElementCallback(this);

    }


    public void addTextViewSizeResource(TextView tv, int sizeBegin, int sizeEnd) {

        Resources res = mActivity.getResources();
        addTextView(tv,
                res.getDimensionPixelSize(sizeBegin),
                res.getDimensionPixelSize(sizeEnd));
    }

    public void addTextView(TextView tv, int sizeBegin, int sizeEnd) {

        Transition textSize = new TextResize();
        textSize.addTarget(tv.getId());
        textSize.addTarget(tv.getText().toString());
        mTransitionSet.addTransition(textSize);

        textViewList.put(tv, new Pair<>(sizeBegin, sizeEnd));
    }

    @Override
    public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {

        for (View v : sharedElements) {

            if (!textViewList.containsKey(v)) {
                continue;
            }

            ((TextView) v).setTextSize(TypedValue.COMPLEX_UNIT_PX, textViewList.get(v).first);
        }
    }

    @Override
    public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
        boolean h = false;
        for (View v : sharedElements) {

            if (!textViewList.containsKey(v)) {
                continue;
            }

            TextView textView = (TextView) v;

            // Record the TextView's old width/height.
            int oldWidth = textView.getMeasuredWidth();
            int oldHeight = textView.getMeasuredHeight();

            // Setup the TextView's end values.
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textViewList.get(v).second);

            // Re-measure the TextView (since the text size has changed).
            int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            textView.measure(widthSpec, heightSpec);

            // Record the TextView's new width/height.
            int newWidth = textView.getMeasuredWidth();
            int newHeight = textView.getMeasuredHeight();

            // Layout the TextView in the center of its container, accounting for its new width/height.
            int widthDiff = newWidth - oldWidth;
            int heightDiff = newHeight - oldHeight;
            textView.layout(textView.getLeft() /*- widthDiff / 2*/, textView.getTop() /*- heightDiff / 2*/,
                    textView.getRight() + widthDiff, textView.getBottom() + heightDiff);

            if(!h) {
                mActivity.setTransformedTextPosition(heightDiff);
                h = true;
            }
        }
    }

}