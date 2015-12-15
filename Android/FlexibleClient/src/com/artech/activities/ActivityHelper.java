package com.artech.activities;

import java.util.HashSet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.artech.R;
import com.artech.activities.dashboard.DashboardActivity;
import com.artech.android.ActivityResources;
import com.artech.android.DebugService;
import com.artech.base.metadata.ILayoutDefinition;
import com.artech.base.metadata.enums.Orientation;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.metadata.theme.ThemeApplicationBarClassDefinition;
import com.artech.base.metadata.theme.ThemeApplicationClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.common.ActivityIndicator;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.compatibility.SherlockHelper;
import com.artech.fragments.LayoutFragmentActivity;
import com.artech.utils.Cast;
import com.artech.utils.ThemeUtils;

/**
 * Class used to centralize calls to tasks that must be performed at particular points of
 * an activity's lifecycle (create, pause, resume, destroy) and other helper methods.
 * @author matiash
 */
public class ActivityHelper
{
	/**
	 * Must be called as the first line of the Activity's onCreate() method, before super.onCreate().
	 */
	public static void onBeforeCreate(Activity activity)
	{
		// Nothing yet.
	}

	/**
	 * Performs initialization of an Activity (such as enabling hardware acceleration
	 * in Honeycomb). Must be the first method called after super.onCreate().
	 * @param activity
	 */
	public static void initialize(Activity activity, Bundle savedInstanceState)
	{
		SherlockHelper.requestWindowFeature(activity, Window.FEATURE_INDETERMINATE_PROGRESS);
		SherlockHelper.requestWindowFeature(activity, Window.FEATURE_PROGRESS);
		ActivityResources.setActivitySavedInstanceState(activity, savedInstanceState);
		DebugService.onCreate(activity);
	}

	public static void setActionBarOverlay(Activity activity)
	{
		SherlockHelper.requestWindowFeature(activity, Window.FEATURE_ACTION_BAR_OVERLAY);
	}

	@SuppressLint({ "NewApi", "InlinedApi" })
	public static void setStatusBarOverlay(Activity activity)
	{
		// check http://stackoverflow.com/questions/27856603/lollipop-draw-behind-statusbar-with-its-color-set-to-transparent
		if (CompatibilityHelper.isStatusBarOverlayingAvailable())
		{
			// api level 21. min 16 for above, 21 for status bar color
			activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		}
	}

	/**
	 * Applies "global" theme properties (application bar color, background color
	 * and image) to an activity. Should be called in Activity.onCreate()
	 * <em>after</em> setContentView() has executed.
	 */
	public static void applyStyle(Activity activity, ILayoutDefinition definition)
	{
		// set action bar home option enable
		ActionBar bar = SherlockHelper.getActionBar(activity);
		if (bar != null && hasActionBar(activity))
			customizeAppBar(activity, definition, bar);

		if (activity.getParent() == null)
		{
			// Set app background color and image.
			// Don't do it for "inner" activities; otherwise we end up with a "double background".
			ThemeApplicationClassDefinition appClass = PlatformHelper.getApplicationClass();
			if (appClass != null)
				ThemeUtils.setBackground(activity, appClass);
		}

		// Set progress to end to hide progress bar at activity startup
		// Progress bar will only appear if progress is set to other value in other place. ie. webview
		SherlockHelper.setProgress(activity, Window.PROGRESS_END);
	}

	private static void customizeAppBar(Activity activity, ILayoutDefinition layout, ActionBar bar)
	{
		bar.setHomeButtonEnabled(true);
		bar.setDisplayShowHomeEnabled(true);

		SherlockHelper.setProgressBarIndeterminateVisibility(activity, false);

		if (layout != null && layout.getEnableHeaderRowPattern())
		{
			ThemeApplicationBarClassDefinition appBarClass = layout.getHeaderRowApplicationBarClass();
			if (appBarClass != null)
			{
				setActionBarThemeClass(activity, appBarClass);
				return;
			}
		}

		// if not use default action bar theme
		setActionBarTheme(activity, layout, false);
	}

	public static void setActionBarTheme(Activity activity, ILayoutDefinition layout, boolean animateBackgroundChange)
	{
		// Get specific application bar class from definition, or use generic one.
		ThemeApplicationBarClassDefinition appBarClass = null;
		if (layout != null)
			appBarClass = layout.getApplicationBarClass();
		if (appBarClass == null)
			appBarClass = PlatformHelper.getThemeClass(ThemeApplicationBarClassDefinition.class, ThemeApplicationBarClassDefinition.CLASS_NAME);

		setActionBarThemeClass(activity, appBarClass, animateBackgroundChange);
	}


	private static final String KEY_BAR_THEME_CLASS = "ApplicationBarThemeClass";

	public static void setActionBarThemeClass(Activity activity, ThemeApplicationBarClassDefinition themeClass)
	{
		setActionBarThemeClass(activity, themeClass, false);
	}

	public static void setActionBarThemeClass(Activity activity, ThemeApplicationBarClassDefinition themeClass, boolean animateBackgroundChange)
	{
		ActionBar bar = SherlockHelper.getActionBar(activity);
		if (bar != null && themeClass != null)
		{
			ThemeApplicationBarClassDefinition previousClass = getActionBarThemeClass(activity);
			if (previousClass == themeClass)
				return; // Avoid re-applying the same exact class.

			ActivityTags.put(activity, KEY_BAR_THEME_CLASS, themeClass);

			// Set title bar properties
			ThemeUtils.setActionBarBackground(activity, bar, themeClass, animateBackgroundChange, previousClass);
			ThemeUtils.setTitleFontProperties(activity, themeClass);
			ThemeUtils.setStatusBarColor(activity, themeClass, animateBackgroundChange, previousClass);

			// app icon and title image supported also.
			ThemeUtils.setAppBarIconImage(bar, themeClass);
			ThemeUtils.setAppBarTitleImage(activity, bar, themeClass);
		}
	}

	public static ThemeApplicationBarClassDefinition getActionBarThemeClass(Activity activity)
	{
		return Cast.as(ThemeApplicationBarClassDefinition.class, ActivityTags.get(activity, KEY_BAR_THEME_CLASS));
	}

	public static void setActionBarVisibility(Activity activity, boolean visible)
	{
		ActionBar bar = SherlockHelper.getActionBar(activity);
		if (bar != null)
		{
			if (visible)
				bar.show();
			else
				bar.hide();
		}
	}

	public static boolean hasActionBar(Activity activity)
	{
		ActionBar bar = SherlockHelper.getActionBar(activity);
		return (bar != null && bar.isShowing());
	}

	private static Activity sCurrentActivity;

	public static Activity getCurrentActivity()
	{
		return sCurrentActivity;
	}

	public static boolean hasCurrentActivity()
	{
		return sCurrentActivity != null;
	}

	public static void onNewIntent(Activity activity, Intent intent)
	{
		ActivityResources.onNewIntent(activity, intent);
	}

	/**
	 * Registers the currently running activity.
	 * @param activity
	 */
	public static boolean onResume(Activity activity)
	{
		DebugService.onResume(activity);

		if (!ActivityFlowControl.onResume(activity))
			return false;

		if (activity != sCurrentActivity)
		{
			// If missed previous onPause, signal it now.
			if (sCurrentActivity != null)
				onPause(sCurrentActivity);

			sCurrentActivity = activity;
			ActivityIndicator.onResume(activity);
			ActivityResources.onResume(activity);
		}

		return true;
	}

	public static void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
		ActivityResources.onActivityResult(activity, requestCode, resultCode, data);
	}

	public static void onSaveInstanceState(Activity activity, Bundle outState) {
		ActivityResources.onSaveInstanceState(activity, outState);
	}

	public static void onPause(Activity activity)
	{
		ActivityFlowControl.onPause(activity);
		ActivityIndicator.onPause(activity);
		ActivityResources.onPause(activity);

		if (sCurrentActivity == activity)
			sCurrentActivity = null;
	}

	public static void onDestroy(Activity activity)
	{
		ActivityResources.onDestroy(activity);
		unbindReferences(activity);
	}

	/**
	 * Removes the reference to the activity from every view in a view hierarchy (listeners, images etc.).
	 * This method should be called in the onDestroy() method of each activity
	 *
	 * see http://code.google.com/p/android/issues/detail?id=8488
	 */
	private static void unbindReferences(Activity activity)
	{
		try
		{
			View view = activity.findViewById(android.R.id.content);
			if (view != null)
			{
				unbindViewReferences(view);
			    if (view instanceof ViewGroup)
			    	unbindViewGroupReferences((ViewGroup)view);
			}

			System.gc();
		}
		catch (Throwable e)
		{
			// Whatever exception is thrown just ignore it, because a crash
			// is always worse than this method not doing what it's supposed to do.
		}
	}

	private static void unbindViewGroupReferences(ViewGroup viewGroup)
	{
    	int nChildren = viewGroup.getChildCount();
    	for (int i = 0; i < nChildren; i++)
    	{
    		View view = viewGroup.getChildAt(i);

    		if (view instanceof WebView)
    		{
    			// WebView must be removed from the view hierarchy before calling destroy() on it.
    			// Otherwise we will get a "WebView.destroy() called while still attached" exception.
    			// Since we remove a view, adjust the iteration indexes accordingly.
    			viewGroup.removeViewAt(i);
    			nChildren--;
    			i--;
    		}

        	unbindViewReferences(view);

        	if (view instanceof ViewGroup)
        		unbindViewGroupReferences((ViewGroup)view);
    	}

    	try
    	{
    		viewGroup.removeAllViews();
    	}
    	catch (Throwable mayHappen)
    	{
        	// AdapterViews, ListViews and potentially other ViewGroups don't support the removeAllViews operation
        }
	}

	private static void unbindViewReferences(View view)
	{
		// set all listeners to null (not every view and not every API level supports the methods).
		try { view.setOnClickListener(null); } catch (Throwable mayHappen) { }
		try { view.setOnCreateContextMenuListener(null); } catch (Throwable mayHappen) { }
		try { view.setOnFocusChangeListener(null); } catch (Throwable mayHappen) { }
		try { view.setOnKeyListener(null); } catch (Throwable mayHappen) { }
		try { view.setOnLongClickListener(null); } catch (Throwable mayHappen) { }
		try { view.setOnClickListener(null); } catch (Throwable mayHappen) { }
		try { view.setOnTouchListener(null); } catch (Throwable mayHappen) { }
		try { view.setTag(R.id.tag_grid_item_info, null); } catch (Throwable mayHappen) { }

		// set background to null and remove callback.
		Drawable d = view.getBackground();
		if (d != null)
			d.setCallback(null);

		if (view instanceof ImageView)
		{
			ImageView imageView = (ImageView) view;
			d = imageView.getDrawable();
			if (d != null)
				d.setCallback(null);

			imageView.setImageDrawable(null);
			CompatibilityHelper.setBackground(imageView, null);
		}

		// destroy webview
		if (view instanceof WebView)
		{
			view.destroyDrawingCache();
			((WebView)view).destroy();
		}
	}

	/**
	 * Sets the orientation of the activity to the default orientation specified in the app
	 * (either switching it, or locking to the current one).
	 * @return True if the orientation was switched, false otherwise.
	 */
	public static boolean setDefaultOrientation(Activity activity)
	{
		Orientation defaultOrientation = PlatformHelper.getDefaultOrientation();
		if (defaultOrientation != Orientation.UNDEFINED)
		{
			boolean isSwitch = (defaultOrientation != Services.Device.getScreenOrientation());
			setOrientation(activity, defaultOrientation);
			return isSwitch;
		}
		else
			return false;
	}

	public static void setOrientation(Activity activity, Orientation orientation)
	{
		if (orientation != Orientation.UNDEFINED)
		{
			int requestedOrientation = (orientation == Orientation.PORTRAIT ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
			activity.setRequestedOrientation(requestedOrientation);
		}
	}

	public static View getInvalidMetadataMessage(IGxActivity activity)
	{
		return getInvalidMetadataMessage((Context) activity, activity.getModel().getName());
	}

	public static View getInvalidMetadataMessage(Context context, String objectName)
	{
		if (objectName == null)
			objectName = Services.Strings.getResource(R.string.GXM_Unknown);

		String message = String.format(Services.Strings.getResource(R.string.GXM_InvalidDefinition), objectName);
		return getErrorMessage(context, message);
	}

	public static View getErrorMessage(Context context, String message)
	{
		TextView text = new TextView(context);
		text.setText(message);
		return text;
	}


	private static HashSet<Integer> sActionRequestCodes = new HashSet<Integer>();

	public static void registerActionRequestCode(int requestCode) {
		sActionRequestCodes.add(requestCode);
	}

	public static boolean isActionRequest(int requestCode)
	{
		return (requestCode == RequestCodes.ACTION ||
				requestCode == RequestCodes.ACTIONNOREFRESH ||
				requestCode == RequestCodes.ACTION_ALWAYS_SUCCESSFUL ||
				sActionRequestCodes.contains(requestCode));
	}

	public static boolean isGenexusActivity(Activity activity) {
		return activity instanceof LayoutFragmentActivity || activity instanceof DashboardActivity;
	}

	public static Rect getWindowDimensions(Activity activity) {
		Rect r = new Rect();
		Window w = activity.getWindow();
		View content = w.findViewById(Window.ID_ANDROID_CONTENT).getRootView();
		content.getHitRect(r);
		r.bottom = r.bottom - getStatusBarHeight(activity);
		return r;
	}

	public static int getStatusBarHeight(Activity activity) {
		int result = 0;
		int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = activity.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}
}
