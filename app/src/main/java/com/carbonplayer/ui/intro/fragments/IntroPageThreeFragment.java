package com.carbonplayer.ui.intro.fragments;

import android.os.Bundle;
import android.os.Process;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.carbonplayer.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Signing in / fetching library screen
 */
public final class IntroPageThreeFragment extends Fragment {

    @BindView(R.id.nautilusFailedOKButton) Button nautilusFailedOKButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.intro_fragment_3, container, false);
        ButterKnife.bind(this, rootView);

        nautilusFailedOKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.isEnabled()) {
                    Process.killProcess(Process.myPid());
                }
            }
        });

        return rootView;
    }
}