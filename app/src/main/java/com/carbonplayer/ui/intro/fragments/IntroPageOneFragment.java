package com.carbonplayer.ui.intro.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.carbonplayer.R;

import androidx.fragment.app.Fragment;

/**
 * Welcome screen
 */
public final class IntroPageOneFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.intro_fragment_1, container, false);
    }
}