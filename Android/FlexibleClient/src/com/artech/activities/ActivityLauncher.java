package com.artech.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.artech.actions.UIContext;
import com.artech.activities.dashboard.DashboardActivity;
import com.artech.application.MyApplication;
import com.artech.application.Preferences;
import com.artech.base.metadata.DashboardMetadata;
import com.artech.base.metadata.DetailDefinition;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.IPatternMetadata;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.RelationDefinition;
import com.artech.base.metadata.WWLevelDefinition;
import com.artech.base.metadata.WorkWithDefinition;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.metadata.enums.Orientation;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.metadata.settings.PlatformDefinition;
import com.artech.base.model.Entity;
import com.artech.base.providers.GxUri;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.Strings;
import com.artech.common.IntentHelper;
import com.artech.common.StorageHelper;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.controllers.IDataSourceController;
import com.artech.controls.maps.Maps;
import com.artech.controls.maps.common.LocationPickerHelper;
import com.artech.ui.navigation.CallOptions;
import com.artech.ui.navigation.CallOptionsHelper;
import com.artech.ui.navigation.CallTarget;
import com.artech.ui.navigation.CallType;
import com.artech.utils.Cast;

public class ActivityLauncher
{
	private static final String INTENT_EXTRA_OBJECT_NAME = "com.artech.activities.ActivityLauncher.ObjectName"; //$NON-NLS-1$

	public static boolean startMainActivity(UIContext startup, Intent launcherIntent)
	{
		// TODO: Get all this out when DashboardActivity is converted to a normal GenexusActivity, and split is a normal NavigationController.
		// 1) If main is dashboard and uses slide navigation, call GenexusActivity.
		// 2) If main is dashboard an uses tabs, call GenexusActivity.
		// 3) If main is WW, call GenexusActivity.
		IViewDefinition mainView = MyApplication.getApp().getMain();
		if (mainView == null)
			return false;

		// When creating mainIntent, copy data from launcherIntent. It may include extras from notification or applinks.
		Intent mainIntent = null;
		if (mainView instanceof DashboardMetadata)
		{
			DashboardMetadata dashboard = (DashboardMetadata)mainView;

			if (dashboard.getControl().equalsIgnoreCase(DashboardMetadata.CONTROL_TABS) ||
				PlatformHelper.getNavigationStyle() == PlatformDefinition.NAVIGATION_SLIDE)
			{
				mainIntent = new Intent();
				mainIntent.putExtras(launcherIntent);
				mainIntent.setClass(startup, GenexusActivity.class);
				mainIntent.putExtra(IntentParameters.DashBoardMetadata, mainView.getName());
			}
		}
		else if (mainView instanceof IDataViewDefinition)
		{
			mainIntent = new Intent();
			mainIntent.putExtras(launcherIntent);
			setupIntent(mainIntent, startup, (IDataViewDefinition)mainView, Collections.<String>emptyList(), DisplayModes.VIEW, null);
		}

		if (mainIntent != null)
		{
			mainIntent.putExtra(IntentParameters.IS_STARTUP_ACTIVITY, true);
			startActivity(startup.getActivity(), mainIntent);
			return true;
		}
		else
			return false;
	}

	public static void call(UIContext context, IDataViewDefinition dataView, List<String> parameters)
	{
		Intent intent = getIntent(context, dataView, parameters);
		startActivity(context.getActivity(), intent);
	}

	public static void callForResult(UIContext context, IDataViewDefinition dataView, List<String> parameters, int requestCode, boolean isSelecting)
	{
		Intent intent = getIntent(context, dataView, parameters);
		ActivityLauncher.setIntentFlagsNewDocument(intent);

		intent.putExtra(IntentParameters.IsSelecting, isSelecting);
		startActivityForResult(context.getActivity(), intent, requestCode);
	}

	public static Intent getIntent(UIContext context, IViewDefinition dataView, List<String> parameters, short mode, Map<String, String> fieldParameters)
	{
		Intent intent = new Intent();
		setupIntent(intent, context, dataView, parameters, mode, fieldParameters);
        return intent;
	}

	private static Intent getIntent(UIContext context, IDataViewDefinition dataView, List<String> parameters)
	{
		return getIntent(context, dataView, parameters, DisplayModes.VIEW, null);
	}

	private static void setupIntent(Intent intent, UIContext context, IViewDefinition dataView, List<String> parameters, short mode, Map<String, String> fieldParameters)
	{
		intent.setClass(context, GenexusActivity.class);
		intent.putExtra(INTENT_EXTRA_OBJECT_NAME, dataView.getObjectName());
		intent.putExtra(IntentParameters.DataView, dataView.getName());
		intent.putExtra(IntentParameters.Mode, mode);
		intent.putExtra(IntentParameters.Connectivity, context.getConnectivitySupport());

		CallOptions callOptions = CallOptionsHelper.getCallOptions(dataView, mode);
		CallOptionsHelper.setCurrentCallOptions(intent, callOptions);

		IntentHelper.putList(intent, IntentParameters.Parameters, parameters);
		IntentHelper.putMap(intent, IntentParameters.BCFieldParameters, fieldParameters);
	}

	public static void callRelated(UIContext context, Entity baseEntity, RelationDefinition relation)
	{
		// TODO: We should have a global dictionary for Entity -> View Panel, just like for edit panels.
		WorkWithDefinition relatedWorkWith = Services.Application.getWorkWithForBC(relation.getBCRelated());
		if (relatedWorkWith != null && relatedWorkWith.getLevels().size() != 0 && relatedWorkWith.getLevel(0).getDetail() != null)
		{
			DetailDefinition detail = relatedWorkWith.getLevel(0).getDetail();

			Intent intent = new Intent();
			intent.setClass(context, GenexusActivity.class);
			intent.putExtra(IntentParameters.Connectivity, context.getConnectivitySupport());
			intent.putExtra(IntentParameters.DataView, detail.getName());
			IntentHelper.putList(intent, IntentParameters.Parameters, getFKKeyString(relation, baseEntity));

			startActivity(context.getActivity(), intent);
		}
	}

	private static List<String> getFKKeyString(RelationDefinition rel, Entity entity)
	{
		ArrayList<String> keys = new ArrayList<String>();
		for (String att : rel.getKeys())
			keys.add((String)entity.getProperty(att));

		return keys;
	}

	public static boolean call(UIContext context, String workWithName)
	{
		IDataViewDefinition definition = getDefaultDataView(workWithName);
		if (definition != null)
		{
			call(context, definition, null);
			return true;
		}
		else
			return false;
	}

	public static boolean callForResult(UIContext from, String workWithName, int requestCode)
	{
		IDataViewDefinition definition = getDefaultDataView(workWithName);
		if (definition != null)
		{
			callForResult(from, definition, null, requestCode, false);
			return true;
		}
		else
			return false;
	}

	private static IDataViewDefinition getDefaultDataView(String objectName)
	{
		WorkWithDefinition pattern = Cast.as(WorkWithDefinition.class, Services.Application.getPattern(objectName));
		if (pattern != null && pattern.getLevels().size() != 0)
		{
			WWLevelDefinition wwLevel = pattern.getLevel(0);
			if (wwLevel != null)
			{
				if (wwLevel.getList() != null)
					return wwLevel.getList();
				else if (wwLevel.getDetail() != null)
					return wwLevel.getDetail();
			}
		}

		return null; // Could not find WW definition, or it was empty.
	}

	public static Intent getDashboard(UIContext context, String dashboardName)
	{
		Intent intent = new Intent();
		intent.setClass(context, GenexusActivity.class);
		intent.putExtra(IntentParameters.DashBoardMetadata, dashboardName);
		intent.putExtra(IntentParameters.Connectivity, context.getConnectivitySupport());

		return intent;
	}

	@SuppressLint("InlinedApi")
	public static void callApplicationMain(Context context, boolean asRoot, boolean allowReturnToRoot)
	{
		Intent intent = new Intent();

		// Call main class of this application.
		intent.setClassName(context, context.getPackageName() + ".Main"); //$NON-NLS-1$

		if (asRoot)
		{
			IPatternMetadata pattern = Services.Application.getPattern(MyApplication.getApp().getAppEntry());

			if (pattern instanceof WorkWithDefinition
					|| isDashboardTabOrNavigationSlide(pattern))
			{
				// if ww is the main, the main dashboard call to it and the main is not in the stack
				if (CompatibilityHelper.isApiLevel(Build.VERSION_CODES.HONEYCOMB))
				{
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
				}
				else
				{
					if (context instanceof Activity && allowReturnToRoot)
					{
						//use return to implementation , because FLAG_ACTIVITY_CLEAR_TASK not exists in API 10 and lower
						Activity fromActivity = (Activity)context;
						IViewDefinition definition = ActivityFlowControl.getMainDefinition(fromActivity);
						// Home button normally calls main. Disable it if already on main.
					    //if already in the main, do nothing
						if (definition == MyApplication.getApp().getMain())
							return;
						ActivityFlowControl.returnTo(fromActivity, MyApplication.getApp().getMain().getName());
						return;
					}
					else
					{
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
					}
				}
			}
			else
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		}

		try
		{
			context.startActivity(intent);
		}
		catch (ActivityNotFoundException ex)
		{
			// Probably in prototyper, call startup.
			intent.setClass(context, DashboardActivity.class);

			context.startActivity(intent);
		}
	}

	private static boolean isDashboardTabOrNavigationSlide(IPatternMetadata pattern)
	{
		if (pattern instanceof DashboardMetadata)
		{
			DashboardMetadata dashboard = (DashboardMetadata)pattern;

			if (dashboard.getControl().equalsIgnoreCase(DashboardMetadata.CONTROL_TABS)
					|| PlatformHelper.getNavigationStyle() == PlatformDefinition.NAVIGATION_SLIDE)
			{
				return true;
			}
		}
		return false;
	}



	public static void CallLocationPicker(Activity context, String mapType, String currentValue)
	{
		Class<? extends Activity> pickerClass = Maps.getLocationPickerActivityClass(context);
		if (pickerClass != null)
		{
			Intent intent = new Intent();
			intent.setClass(context, pickerClass);

			if (Strings.hasValue(mapType))
				intent.putExtra(LocationPickerHelper.EXTRA_MAP_TYPE, mapType);
			if (Strings.hasValue(currentValue))
				intent.putExtra(LocationPickerHelper.EXTRA_LOCATION, currentValue);

			context.startActivityForResult(intent, RequestCodes.PICKER);
		}
	}

	public static void CallComponent(Context context, String link)
	{
		Intent intent = GetComponent(context, link);
		ActivityLauncher.setIntentFlagsNewDocument(intent);
   		context.startActivity(intent);
	}

	private static Intent GetComponent(Context context, String link)
	{
		Intent intent = new Intent();
   		intent.setClass(context, WebViewActivity.class);
   		intent.putExtra("Link", link); //$NON-NLS-1$
   		return intent;
	}

	public static Intent GetComponent(Context context, String link, boolean shareSession)
	{
		Intent intent = new Intent();
   		intent.setClass(context, WebViewActivity.class);
   		intent.putExtra("Link", link); //$NON-NLS-1$
   		intent.putExtra("ShareSession", shareSession); //$NON-NLS-1$
   		return intent;
	}

	public static void CallViewVideo(Context context, String link)
	{
		Intent intent = getMultimediaViewerIntent(context, link, true, null, 0);
   		context.startActivity(intent);
	}

	public static void CallViewVideoFullscreen(Context context, String link, Orientation orientation, int currentPosition)
	{
		Intent intent = getMultimediaViewerIntent(context, link, true, orientation, currentPosition);
		context.startActivity(intent);
	}

	public static void CallViewAudio(Context context, String link)
	{
		Intent intent = getMultimediaViewerIntent(context, link, true, null, 0);
   		intent.putExtra(VideoViewActivity.INTENT_EXTRA_IS_AUDIO, true);
   		intent.putExtra(VideoViewActivity.INTENT_EXTRA_SHOW_BUTTONS, true);
   		context.startActivity(intent);
	}

	private static Intent getMultimediaViewerIntent(Context context, String link, boolean showButtons, Orientation orientation, int currentPosition)
	{
		Intent intent = new Intent();
   		intent.setClass(context, VideoViewActivity.class);
   		ActivityLauncher.setIntentFlagsNewDocument(intent);

   		// If not an absolute URL, add base path
		if (!link.contains("://") && !StorageHelper.isLocalFile(link)) //$NON-NLS-1$
			link = MyApplication.getApp().UriMaker.MakeImagePath(link);

   		intent.putExtra(VideoViewActivity.INTENT_EXTRA_LINK, link);
   		intent.putExtra(VideoViewActivity.INTENT_EXTRA_SHOW_BUTTONS, showButtons);

   		if (orientation != null)
   			intent.putExtra(VideoViewActivity.INTENT_EXTRA_ORIENTATION, orientation.toString());

   		if (currentPosition != 0)
   			intent.putExtra(VideoViewActivity.INTENT_EXTRA_CURRENT_POSITION, currentPosition);

   		return intent;
	}

	public static void callLogin(UIContext from)
	{
		// Call login panel.
		String loginObject = MyApplication.getApp().getLoginObject();
		WorkWithDefinition wwMetadata = (WorkWithDefinition) Services.Application.getPattern(loginObject);

		if (wwMetadata != null && wwMetadata.getLevels().size() != 0 && wwMetadata.getLevel(0).getDetail() != null)
		{
			CallOptionsHelper.setCallOption(loginObject, CallOptions.OPTION_TARGET, CallTarget.BLANK.getName());
			ActivityLauncher.callForResult(from, wwMetadata.getLevel(0).getDetail(), null, RequestCodes.LOGIN, false);
		}
		else
			Services.Log.Error(String.format("Login object (%s) is not defined.", loginObject));
	}

	public static void callFilters(UIContext context, IDataSourceController dataSource)
	{
		Intent intent = new Intent();
	    intent.setClass(context, FiltersActivity.class);
	    ActivityLauncher.setIntentFlagsNewDocument(intent);

	    IntentHelper.putObject(intent, IntentParameters.Filters.DataSource, IDataSourceDefinition.class, dataSource.getDefinition());
	    intent.putExtra(IntentParameters.Filters.DataSourceId, dataSource.getId());
	    IntentHelper.putObject(intent, IntentParameters.Filters.Uri, GxUri.class, dataSource.getModel().getUri());
	    intent.putExtra(IntentParameters.Filters.FiltersFK, dataSource.getModel().getFilterExtraInfo());
		intent.putExtra(IntentParameters.Connectivity, context.getConnectivitySupport());


		context.getActivity().startActivityForResult(intent, RequestCodes.FILTERS);
	}

	public static void CallDetailFilters(UIContext context, IDataSourceDefinition dataSource, String attName, String rangeBegin, String rangeEnd, String filterDefault, String filterRangeFk)
	{
		Intent next = new Intent();
		next.setClass(context, DetailFiltersActivity.class);
		IntentHelper.putObject(next, IntentParameters.Filters.DataSource, IDataSourceDefinition.class, dataSource);
    	next.putExtra(IntentParameters.AttName, attName);
    	next.putExtra(IntentParameters.RangeBegin, rangeBegin);
    	next.putExtra(IntentParameters.RangeEnd, rangeEnd);
    	next.putExtra(IntentParameters.FilterDefault, filterDefault);
    	next.putExtra(IntentParameters.FilterRangeFk, filterRangeFk);
    	next.putExtra(IntentParameters.Connectivity, context.getConnectivitySupport());

    	context.getActivity().startActivityForResult(next, 0);
	}

	public static void callPreferences(Activity activity, boolean showToast, int message, String serverURL) {
		Intent intent = new Intent(activity, Preferences.class);
		intent.putExtra("showToast", showToast); //$NON-NLS-1$
		intent.putExtra("messageToast", message); //$NON-NLS-1$
		intent.putExtra(IntentParameters.ServerURL, serverURL);
		activity.startActivityForResult(intent, RequestCodes.PREFERENCE);
	}

	/**
	 * Start an activity from a given context.
	 * This called is made on the UI thread (so that transitions work).
	 */
	public static void startActivity(final Activity from, final Intent intent)
	{
		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				from.startActivity(intent);
				applyCallOptions(from, intent);
			}
		});
	}

	public static void startActivityForResult(final Activity from, final Intent intent, final int requestCode)
	{
		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				from.startActivityForResult(intent, requestCode);
				applyCallOptions(from, intent);
			}
		});
	}

	private static void applyCallOptions(Activity fromActivity, Intent intent)
	{
		String objectName = intent.getStringExtra(INTENT_EXTRA_OBJECT_NAME);
		CallOptions callOptions = CallOptionsHelper.getCurrentCallOptions(intent);

		if (callOptions != null && fromActivity != null)
		{
			// Use enter/exit effects.
			if (callOptions.getEnterEffect() != null)
				callOptions.getEnterEffect().onCall(fromActivity);

			// Use replace/push.
			// TODO: Use popup, callout, target, target size.
			if (callOptions.getCallType() == CallType.REPLACE)
			{
				intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
				fromActivity.finish();
			}
		}

		// Remove global configured CallOptions after call.
		CallOptionsHelper.resetCallOptions(objectName);
	}

	public static void onReturn(Activity from, Intent intent)
	{
		CallOptions callOptions = CallOptionsHelper.getCurrentCallOptions(intent);

		if (callOptions != null && callOptions.getExitEffect() != null)
			callOptions.getExitEffect().onReturn(from);
	}

	@SuppressLint("InlinedApi")
	public static void setIntentFlagsNewDocument(Intent intent)
	{
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
	}

}
