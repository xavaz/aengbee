package com.aengbee.android.leanback.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.aengbee.android.leanback.model.Video;

/**
 * Created by i5 on 2017-03-16.
 */

public class PlaybackOverlayWebViewFragment extends Fragment implements BrowseFragment.MainFragmentAdapterProvider {
    private BrowseFragment.MainFragmentAdapter mMainFragmentAdapter = new BrowseFragment.MainFragmentAdapter(this);
    private WebView mWebview;

    @Override
    public BrowseFragment.MainFragmentAdapter getMainFragmentAdapter() {
        return mMainFragmentAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getMainFragmentAdapter().getFragmentHost().showTitleView(false);

    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(getActivity());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        lp.setMarginStart(32);
        mWebview = new WebView(getActivity());
        mWebview.setWebViewClient(new WebViewClient());
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.getSettings().setMediaPlaybackRequiresUserGesture(false);
        Video video = getActivity().getIntent().getParcelableExtra(VideoDetailsActivity.VIDEO);
        mWebview.loadUrl(video.videoUrl);
        root.addView(mWebview, lp);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        mWebview.resumeTimers();
        mWebview.onResume();
        //getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());

    }

    @Override
    public void onPause() {
        super.onPause();
        mWebview.onPause();
        mWebview.pauseTimers();
    }


    @Override
    public void onDestroy() {
        mWebview.destroy();
        mWebview = null;
        super.onDestroy();
    }
}
