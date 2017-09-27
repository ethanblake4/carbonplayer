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
import butterknife.Unbinder;

/**
 * Sign In screen
 */
public final class IntroPageTwoFragment extends Fragment {

    Unbinder unbinder;

    @BindView(R.id.introOAuthStart)
    Button startOAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.intro_fragment_2, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        startOAuth = (Button) rootView.findViewById(R.id.introOAuthStart);

        startOAuth.setOnClickListener(v ->
                ((IntroActivity) getActivity()).beginAuthentication());

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}