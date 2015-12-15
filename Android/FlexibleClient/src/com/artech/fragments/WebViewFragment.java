package com.artech.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.artech.controls.GxWebView;

public class WebViewFragment extends Fragment implements IFragmentHandleKeyEvents
{
	private static final String KEY_URL = "url";
	
	private GxWebView mWebView;

	public static WebViewFragment newInstance(String url)
	{
		WebViewFragment f = new WebViewFragment();
		Bundle args = new Bundle();
		args.putString(KEY_URL, url);
		
		f.setArguments(args);
		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		String url = getArguments().getString(KEY_URL);

		mWebView = new GxWebView(inflater.getContext());
		mWebView.setOpenLinksInNewWindow(false);
		mWebView.navigate(url);

		return mWebView;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (mWebView != null && keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack())
		{
			mWebView.goBack();
			return true;
		}
		else
			return false;
	}
}
