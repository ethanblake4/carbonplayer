package com.carbonplayer.ui.intro.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.carbonplayer.R;
import com.carbonplayer.ui.intro.IntroActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Sign In screen
 */
public final class IntroPageTwoFragment extends Fragment {

    @BindView(R.id.introOAuthStart) Button startOAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.intro_fragment_2, container, false);
        ButterKnife.bind(this, rootView);

        startOAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((IntroActivity) getActivity()).beginOAuth2Authentication();
            }
        });

        return rootView;
    }
}