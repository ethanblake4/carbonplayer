package com.carbonplayer.ui.intro.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.carbonplayer.R;

/**
 * Welcome screen
 */
public final class IntroPageOneFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.intro_fragment_1, container, false);

        return rootView;
    }
}