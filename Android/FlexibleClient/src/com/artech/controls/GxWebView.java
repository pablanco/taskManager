package com.artech.controls;

import java.lang.reflect.Method;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ZoomButtonsController;

import com.artech.R;
import com.artech.activities.ActivityLauncher;
import com.artech.application.MyApplication;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.PhoneHelper;
import com.artech.compatibility.SherlockHelper;
import com.artech.ui.Coordinator;
import com.artech.utils.ThemeUtils;

public class GxWebView extends WebView implements IGxEdit, IGxThemeable
{
	private String mUrl;
	private ThemeClassDefinition mThemeClass;
	private Coordinator mCoordinator;
	private boolean mLoadAsHtmlDone;
	private boolean mOpenLinksInNewWindow;
	private LayoutItemDefinition mLayoutItem;

	public GxWebView(Context context, Coordinator coordinator, LayoutItemDefinition item)
	{
		super(context);
		mCoordinator = coordinator;
		mLayoutItem = item;
		initialize();
	}

	public GxWebView(Context context)
	{
		super(context);
		initialize();
	}

	public GxWebView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initialize();
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void initialize()
	{
		mOpenLinksInNewWindow = true;

		getSettings().setJavaScriptEnabled(true);
		getSettings().setDomStorageEnabled(true);

		//		getSettings().setUseWideViewPort(true);
		//		getSettings().setLoadWithOverviewMode(true);

		//getSettings().setBuiltInZoomControls(true);
		//getSettings().setSupportZoom(true);
		//No zoom control , but zoom working.
		disableControls();
	}

	private ZoomButtonsController zoomButtons;

	@SuppressLint("NewApi")
	private void disableControls() {
        getSettings().setBuiltInZoomControls(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getSettings().setDisplayZoomControls(false);
        } else {
            try {
                Method method = getClass()
                        .getMethod("getZoomButtonsController");
                zoomButtons = (ZoomButtonsController) method.invoke(this);
            } catch (Exception e) {
                // pass
            }
        }
    }

	public void setOpenLinksInNewWindow(boolean value)
	{
		mOpenLinksInNewWindow = value;
	}

	public class MyWebViewClient extends WebViewClient
	{
		private boolean loadingFinished = false;
		private boolean redirect = false;

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
			if (url != null)
			{
				if (PhoneHelper.launchFromWebView(getContext(), url))
					return true;

				if (url.endsWith(".pdf") || url.endsWith(".apk")) //$NON-NLS-1$ //$NON-NLS-2$
				{
					viewInBrowser(url);
					return true;
				}

				//check state
				if (loadingFinished)
				{
					// only open in new window a diferent url
					if (mOpenLinksInNewWindow && Strings.hasValue(url) && !url.equalsIgnoreCase(mUrl))
					{
						// now link works as double tab, open in new windows
						ActivityLauncher.CallComponent(getContext(), url);
						return true;
					}
				}
				else
				{
					redirect = true;
				}
				loadingFinished = false;
				view.loadUrl(url);
				return true;
			}
			return false;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			loadingFinished = false;
		}

		@Override
		public void onPageFinished(final WebView view, String url)
		{
			super.onPageFinished(view, url);
			if (!redirect)
		       loadingFinished = true;

			if (loadingFinished && !redirect)
		    {
		    	// try to re measure parent.
		    	boolean needsMeasure = mLayoutItem != null && mLayoutItem.hasAutoGrow();
				if (needsMeasure && (!url.equalsIgnoreCase("about:blank") || mLoadAsHtml))
				{
					// Wait until html is rendered, then request layout to move other controls down.
					postDelayed(new Runnable()
					{
						@Override
						public void run() {
							view.requestLayout();
						}
					}, 300);
				}
	       	}
		    else
	       		redirect = false;
		}
	}

	private void viewInBrowserYoutube(String url) {

		String vid= Uri.parse(url).getQueryParameter("v");
		if (!Services.Strings.hasValue(vid))
		{
			vid= Uri.parse(url).getLastPathSegment();
		}
		//Call directly to youtube app
		Uri uri = Uri.parse("vnd.youtube:" + vid);
		//Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + url));
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		//Intent next = new Intent("android.intent.action.VIEW", Uri.parse( url));   //$NON-NLS-1$
		List<ResolveInfo> list = getContext().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		if (list.size() == 0) {
			viewInBrowser( url);
		}
		else
		{
			//start youtube
			getContext().startActivity(intent);
		}
	}

	private void viewInBrowser(String url)
	{
		Intent next = new Intent("android.intent.action.VIEW", Uri.parse(url));   //$NON-NLS-1$
		ActivityLauncher.setIntentFlagsNewDocument(next);
		getContext().startActivity(next);
	}


	@Override
	public boolean onTouchEvent(MotionEvent e)
	{
		try
		{
			if (mCoordinator != null && mCoordinator.hasAnyEventHandler(this, GxTouchEvents.ALL_EVENTS))
				return false; // Let custom events execute. This will disable scrolling, zooming, &c though.

			// Handle special gestures (e.g. tap to open YouTube app).
			if (mGestureDetector.onTouchEvent(e))
				return true;

			// Hide the zoom buttons.
			boolean result = super.onTouchEvent(e);
		    if (zoomButtons != null)
		    {
		        zoomButtons.setVisible(false);
		        zoomButtons.getZoomControls().setVisibility(View.GONE);
		    }

		    return result;
		}
		catch (Exception ex) { }

		return true;
	}

	private final GestureDetector mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener()
	{
		@Override
		public boolean onDoubleTap(MotionEvent e)
		{
			// double tab only show video youtube, not open component in new window anymore.
			if (mUrl!=null && mUrl.length()>0)
			{
				Services.Log.info("onDoubleTap", "Url: " + mUrl); //$NON-NLS-1$ //$NON-NLS-2$
				if (mUrl.contains("www.youtube.com")) //$NON-NLS-1$
					viewInBrowserYoutube(mUrl);
				else
					return super.onDoubleTap(e);
				//else
				//	ActivityLauncher.CallComponent(getContext(), mUrl);
			}
			else
				return super.onDoubleTap(e);
			//else if (mHtml!=null && mHtml.length()>0)
			//	ActivityLauncher.CallComponentHtml(getContext(), mHtml);
			return true;
		}
	});

	public void navigate(String url)
	{
		if (!Strings.starsWithIgnoreCase(url, "http") && !Strings.starsWithIgnoreCase(url, "https")) //$NON-NLS-1$
			url = Services.Application.linkObjectUrl(url);

		// Don't call loadUrl() if the URL is the same as the current one. This is a common occurrence when
		// loading with cache data, then server data afterwards. Note that we check against getUrl() instead of mUrl
		// because the user might have navigated away if the WebView allows it.
		String currentUrl = getUrl();
		if (currentUrl != null && currentUrl.equalsIgnoreCase(url))
			return;

		mUrl = url;

		setWebViewClient(new MyWebViewClient());
		setWebChromeClient(new MyWebChromeClient());

		super.loadUrl(url);

		if (Services.Strings.hasValue(mUrl) && mUrl.contains("www.youtube.com")) //$NON-NLS-1$
		{
			// double tap to play in youtube. in place player should work
			MyApplication.getInstance().showMessage(Services.Strings.getResource(R.string.GXM_TapToPlay));
		}
	}

	public void loadHtml(String html)
	{
		loadHtml(html, mThemeClass);
	}

	public void loadHtml(String html, ThemeClassDefinition themeClass)
	{
		//mHtml = html;
		//int size = LayoutHelper.convertDipToPixels(getContext(), 64);
		//this.setMinimumHeight(size);

		//Services.Log.debug("loadHtml "+ html + (themeClass!=null?themeClass.getName():"null") + " test");
		String backColor = null;
		String foreColor = null;
		String fontSize = null;
		String fontFamily = null;
		if (themeClass != null) {
			backColor = getBackColor(themeClass);
			foreColor = getForeColor(themeClass);
			fontSize = getFontSize(themeClass);
			fontFamily = getFontFamily(themeClass);
		}

		if ((foreColor == null) || (foreColor.length() == 0)) {
			foreColor = ThemeUtils.getAndroidThemeColor(getContext(), android.R.attr.textColorPrimary);
		}

		// Services.Log.info("GxWebView", "B " + backColor); //$NON-NLS-1$ //$NON-NLS-2$
		// Services.Log.info("GxWebView", "F " + foreColor); //$NON-NLS-1$ //$NON-NLS-2$

		String backgroundHtml = "";
		if (Services.Strings.hasValue(backColor)) {
			backgroundHtml = "background-color: " + backColor + "; ";
		}
		String forecolorHtml = "";
		if (Services.Strings.hasValue(foreColor)) {
			forecolorHtml = " color: " + foreColor + ";";
		}
		String fontSizeHtml = "";
		if (Services.Strings.hasValue(fontSize)) {
			fontSizeHtml = " font-size: " + fontSize + "px;";
		}
		String fontFamilyHtml = "";
		String fontFamilyExtraHtml = "";
		if (Services.Strings.hasValue(fontFamily)) {
			fontFamilyHtml = " @font-face { font-family: " + fontFamily + "; src: url('file:///android_asset/fonts/" + fontFamily + ".ttf'); }";
			fontFamilyExtraHtml = " font-family: " + fontFamily + ";";
		}

		String htmlToLoad = html;
		if (htmlToLoad != null && !Strings.toLowerCase(htmlToLoad).startsWith("<html>"))
		{
			// Append header for background / foreground color.
			htmlToLoad = "<html> <head><style type=\"text/css\">" + fontFamilyHtml + " body {" + fontFamilyExtraHtml + backgroundHtml + forecolorHtml + fontSizeHtml + " } a { color: #ddf; }</style></head><body>" + //$NON-NLS-1$ //$NON-NLS-2$
					htmlToLoad +
					"</body></html>"; //$NON-NLS-1$
		}

		setWebViewClient(new MyWebViewClient());
		//setWebChromeClient(new MyWebChromeClient());

		loadDataWithBaseURL(null, htmlToLoad, "text/html", "utf-8", "about:blank"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if ((backColor == null) || (backColor.length() == 0)) {
			// Transparent background AFTER loading HTML, as per
			// http://stackoverflow.com/questions/5003156/android-webview-style-background-colortransparent-ignored-on-android-2-2/5899705#5899705
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				// Set alpha to 1, so it 'looks' transparent. It might have a slight performance penalty.
				// See: http://stackoverflow.com/a/18256729/1922917
				// This solves a problem that on devices >= 3.0, transparent background on a WebView causes flickering when scrolling with hardwareAcceleration on.
				setBackgroundColor(0x01000000);
			} else {
				setBackgroundColor(0x00000000);

			}
		}
		mLoadAsHtmlDone = true;
	}

	private String getBackColor(ThemeClassDefinition themeClass)
	{
		String color = themeClass.getBackgroundColor();
		if (color != null && color.contains("#") && color.length() == 9) //$NON-NLS-1$
			color = color.substring(0, 7);
		return color;
	}

	private String getForeColor(ThemeClassDefinition themeClass) {
		String color = themeClass.getColor();
		if (color != null && color.contains("#") && color.length() == 9) //$NON-NLS-1$
			color = color.substring(0, 7);
		return color;
	}

	private String getFontSize(ThemeClassDefinition themeClass) {
		// Important detail: HTML "pixel" font sizes are equivalent to Android's DPs.
		// Since getFontSize() returns a value in pixels, we need to "convert back" here.
		Integer fontSize = themeClass.getFont().getFontSize();
		if (fontSize != null && fontSize != 0)
			return String.valueOf(Services.Device.pixelsToDips(fontSize));
		else
			return null;
	}

	private String getFontFamily(ThemeClassDefinition themeClass) {
		return themeClass.getFont().getFontFamily();
	}

	private boolean mLoadAsHtml = true;

	public void setMode(boolean loadAsHtml)
	{
		mLoadAsHtml = loadAsHtml;
	}

	private String mValue = Strings.EMPTY;
	@Override
	public String getGx_Value() {
		return mValue;

	}

	@Override
	public void setGx_Value(String value) {
		mValue = value;
		if (mValue!=null && mValue.length()>0)
		{
			if (mLoadAsHtml)
			{
				loadHtml(mValue);
			}
			else
			{
				navigate(mValue);
			}
		}
		else
		{
			if (mLoadAsHtml)
			{
				loadHtml(Strings.EMPTY);
			}

		}
	}

	@Override
	public String getGx_Tag() {
		return (String)this.getTag();

	}

	@Override
	public void setGx_Tag(String data) {
		this.setTag(data);
	}

	@Override
	public void setValueFromIntent(Intent data) {
	}

	@Override
	public IGxEdit getViewControl() {
		return this;
	}

	@Override
	public IGxEdit getEditControl() {
		return this;
	}


	@Override
	public void setThemeClass(ThemeClassDefinition themeClass) {
		mThemeClass = themeClass;
		// if already loaded, reload it.
		if (mLoadAsHtmlDone)
		{
			applyClass(themeClass);
		}
	}

	@Override
	public ThemeClassDefinition getThemeClass() {
		return mThemeClass;
	}

	@Override
	public void destroy() {
		synchronized(sLock)
		{
			sIsWorking = false;
		}

		setWebChromeClient(null);
		super.destroy();
	}

	@Override
	protected void onDetachedFromWindow() {
		// remove old webview content on rotate
		loadHtml(Strings.EMPTY);
		super.onDetachedFromWindow();
	}

	//Added to allow show loading in action bar from webview status.
	//TODO: see if need in webviewfragment. WebviewActivity should show action bar?
	private static final Object sLock = new Object();
	private static boolean sIsWorking = false;

	public class MyWebChromeClient extends WebChromeClient
	{
		@Override
		public void onProgressChanged(WebView view, int progress)
		{
			// Activities and WebViews measure progress with different scales.
			// The progress meter will automatically disappear when we reach 100%
			if (getContext() instanceof Activity)
			{
				Activity myActivity = (Activity)getContext();
				SherlockHelper.setProgress(myActivity, progress * 100);
			}
			synchronized(sLock)
			{
				sIsWorking = progress < 100;
			}
		}
	}

	public static boolean isWorking()
	{
		synchronized(sLock)
		{
			return sIsWorking;
		}
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		// Should Apply the given class to the control but it doesn't change the class for it.
		// reload the Html if the class change an is not null.
		if (themeClass!=null)
		{
			//reload with new theme set.
			if (mLoadAsHtml && mValue!=null && mValue.length()>0)
			{
				loadHtml(mValue, themeClass);
			}
		}
	}

	@Override
	public boolean isEditable()
	{
		return false; // Never editable.
	}
}
