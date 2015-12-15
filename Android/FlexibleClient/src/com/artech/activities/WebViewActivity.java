package com.artech.activities;

import java.util.List;

import org.apache.http.cookie.Cookie;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.artech.R;
import com.artech.base.services.Services;
import com.artech.base.utils.ResultDetail;
import com.artech.base.utils.Strings;
import com.artech.common.SecurityHelper;
import com.artech.common.ServiceHelper;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.compatibility.SherlockHelper;

public class WebViewActivity extends GxBaseActivity
{
	private String mUrl = null;
	private String mHtml = null;
	private boolean mShareSession = false;
	private boolean mLoginExternalCall = false;

	private WebView mWebview = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (!Services.Application.isLoaded())
		{
			finish();
			return;
		}

		SherlockHelper.requestWindowFeature(this, Window.FEATURE_PROGRESS);
		ActivityHelper.setActionBarVisibility(this, false);

		Intent intent = getIntent();
		mUrl = intent.getStringExtra("Link"); //$NON-NLS-1$
		mHtml = intent.getStringExtra("Html"); //$NON-NLS-1$
		mShareSession = intent.getBooleanExtra("ShareSession", false); //$NON-NLS-1$
		mLoginExternalCall = intent.getBooleanExtra(IntentParameters.ExternalLoginCall, false);

		mWebview = new WebView(this);
		setContentView(mWebview);

		webviewInit();

		mWebview.getSettings().setBuiltInZoomControls(true);
		mWebview.setWebViewClient(new MyWebViewClient());
		mWebview.setWebChromeClient(new MyWebChromeClient());

		if ((mUrl!=null) && (mUrl.endsWith(".pdf") || mUrl.endsWith(".apk") || mUrl.contains("www.youtube.com"))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		{
			viewInBrowser();
			finish();
			return;
		}

		if (mUrl!=null)
		{
			if (!mUrl.startsWith("http://") && !mUrl.startsWith("https://")) //$NON-NLS-1$ //$NON-NLS-2$
				mUrl = "http://" + mUrl;  //$NON-NLS-1$
			if (mShareSession)
				new WebViewTask().execute();
			else
				mWebview.loadUrl(mUrl);

		}
		else if (mHtml!=null)
		{
			//change for the load data that works with utf-8
			//webview.loadData(html,"text/html", "utf-8");
			mWebview.loadDataWithBaseURL(null, mHtml, "text/html", "utf-8", "about:blank"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		this.setTitle("");
	}


	@SuppressLint("SetJavaScriptEnabled")
	private void webviewInit() {
		mWebview.getSettings().setJavaScriptEnabled(true);
		mWebview.getSettings().setDomStorageEnabled(true);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_web, menu);

		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.viewinBrowser) {
			if (mUrl != null)
			{
				viewInBrowser();
			}
			//finish();
			return true;
		} else {
			return false;
		}
	}


	private void viewInBrowser()
	{
		viewInBrowser(mUrl);
	}

	private void viewInBrowser(String url)
	{
		Intent next = new Intent("android.intent.action.VIEW", Uri.parse( url));   //$NON-NLS-1$
		ActivityLauncher.setIntentFlagsNewDocument(next);
		startActivity(next);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (mWebview!=null && (keyCode == KeyEvent.KEYCODE_BACK) && mWebview.canGoBack())
		{
			mWebview.goBack();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	//Task to allow share session between webview and httpclient.
	public class WebViewTask extends AsyncTask<Void, Void, Boolean>
	{
		private CookieManager cookieManager;
		private List<Cookie> mCookies;

		@Override
		protected void onPreExecute()
		{
			CookieSyncManager.createInstance(WebViewActivity.this);
			cookieManager = CookieManager.getInstance();

			// Copied from
			// http://www.walletapp.net/android-passing-cookie-to-webview

			// get cookies
			mCookies  = ServiceHelper.getThreadSafeClient().getCookieStore().getCookies();

			//delete old cookies , seems not necessary in our case
			//cookieManager.removeSessionCookie();
			// do it to remove any previous session with facebook twitter etc.
			cookieManager.removeAllCookie();

			super.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(Void... param)
		{
			// this is very important - THIS IS THE HACK
			SystemClock.sleep(1000);
			return false;
		}

		@SuppressLint("SetJavaScriptEnabled")
		@Override
		protected void onPostExecute(Boolean result)
		{
			if (mCookies != null)
			{
				for (Cookie cookie : mCookies)
				{
					String cookieString = cookie.getName() + Strings.EQUAL + cookie.getValue() + "; domain=" + cookie.getDomain(); //$NON-NLS-1$
					cookieManager.setCookie(cookie.getDomain(), cookieString);
				}

				CookieSyncManager.getInstance().sync();
			}

			WebSettings webSettings = mWebview.getSettings();
			webSettings.setJavaScriptEnabled(true);
			webSettings.setBuiltInZoomControls(true);
			mWebview.setWebViewClient(new MyWebViewClient());
			mWebview.loadUrl(mUrl);
		}
	}

	private class MyWebViewClient extends WebViewClient
	{
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
			if (url != null)
			{
				//Services.Log.debug("Url " + url);
				if (url.endsWith(".pdf") || url.endsWith(".apk") /*|| url.contains("youtube.com")*/) //$NON-NLS-1$ //$NON-NLS-2$
				{
					viewInBrowser(url);
					return true;
				}
				if (url.startsWith("gxgam://")) //$NON-NLS-1$
				{
					finishExternalLogin(url);
					return true;
				}
				view.loadUrl(url);
			}
			return true;
		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl)
		{
			super.onReceivedError(view, errorCode, description, failingUrl);
			Services.Log.Error("errorCode " + errorCode, " description " + description + " failingUrl " + failingUrl);
			if (mLoginExternalCall)
			{
				Services.Log.debug("extenal login failed, return "); //$NON-NLS-1$
				String errorMessage = Services.Strings.getResource(R.string.GXM_NetworkError, " ");
				ResultDetail<?> result = ResultDetail.error(errorMessage);

				// Return the login result to the caller.
				Intent resultIntent = new Intent();
				resultIntent.putExtra(IntentParameters.EXTERNAL_LOGIN_RESULT, result);
				setResult(RESULT_OK, resultIntent);
				finish();
			}
		}


	}

	private void finishExternalLogin(final String url)
	{
		AsyncTask<Void, Void, ResultDetail<?>> afterLoginTask = new AsyncTask<Void, Void, ResultDetail<?>>()
		{
			@Override
			protected ResultDetail<?> doInBackground(Void... params)
			{
				// Do this in background, since it involves an additional network call.
				return SecurityHelper.afterExternalLogin(url);
			}

			@Override
			protected void onPostExecute(ResultDetail<?> result)
			{
				// Return the login result to the caller.
				Intent resultIntent = new Intent();
				resultIntent.putExtra(IntentParameters.EXTERNAL_LOGIN_RESULT, result);
				setResult(RESULT_OK, resultIntent);
				finish();
			}
		};

		CompatibilityHelper.executeAsyncTask(afterLoginTask);
	}

	private class MyWebChromeClient extends WebChromeClient
	{
		@Override
		public void onProgressChanged(WebView view, int progress)
		{
			// Activities and WebViews measure progress with different scales.
			// The progress meter will automatically disappear when we reach 100%
			SherlockHelper.setProgress(WebViewActivity.this, progress * 100);
		}
	}
}
