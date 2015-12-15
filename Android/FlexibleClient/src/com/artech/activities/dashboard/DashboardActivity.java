package com.artech.activities.dashboard;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.artech.R;
import com.artech.actions.ActionExecution;
import com.artech.actions.ActionFactory;
import com.artech.actions.ActionParameters;
import com.artech.actions.CompositeAction;
import com.artech.actions.CompositeAction.IEventListener;
import com.artech.actions.RunnableAction;
import com.artech.actions.UIContext;
import com.artech.activities.ActivityHelper;
import com.artech.activities.ActivityLauncher;
import com.artech.activities.IGxDashboardActivity;
import com.artech.activities.IntentParameters;
import com.artech.adapters.DashBoardAdapter;
import com.artech.android.analytics.Tracker;
import com.artech.android.gcm.GcmIntentService;
import com.artech.android.layout.GxTheme;
import com.artech.android.layout.OrientationLock;
import com.artech.application.MyApplication;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.DashboardItem;
import com.artech.base.metadata.DashboardMetadata;
import com.artech.base.metadata.Events;
import com.artech.base.metadata.GenexusApplication;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.metadata.enums.GxObjectTypes;
import com.artech.base.metadata.enums.Orientation;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.metadata.loader.ApplicationLoader;
import com.artech.base.metadata.loader.LoadResult;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.ApplicationHelper;
import com.artech.common.ImageHelper;
import com.artech.common.LayoutHelper;
import com.artech.common.SecurityHelper;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.controls.GxLinearLayout;
import com.artech.controls.IGxLocalizable;
import com.artech.controls.ImageViewDisplayImageWrapper;
import com.artech.providers.EntityDataProvider;
import com.artech.utils.Cast;
import com.fedorvlasov.lazylist.ImageLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class DashboardActivity extends AppCompatActivity implements IGxDashboardActivity, IGxLocalizable
{
	private GenexusApplication mApplication;
	private AdapterView<ListAdapter> mDashboardView;
	protected ImageLoader mImageLoader;
	private ProgressDialog m_ProgressDialog = null;
	private boolean mHasWelcomeImage;
	private Connectivity mConnectivity;
	private Entity mDashboardEntity;
	private boolean mClientStartExecuted;

	boolean mIsMain = true;
	private DashboardMetadata mDashboardMetadata;

	private String mServerUrl = null;

	// Analytics Tracking.
	private final Tracker.ActivityTracker mTracker = new Tracker.ActivityTracker(this);

	@Override
	protected void onDestroy()
	{
		hideLoadingIndicator();

		//Services.Log.warning("ondestroy", "ondestroy dashboard imageloader");
		mImageLoader.stopThread();
		mImageLoader = null;

		mHandler = null;

		ActivityHelper.onDestroy(this);
		super.onDestroy();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		ActivityHelper.onBeforeCreate(this);
		super.onCreate(savedInstanceState);
		ActivityHelper.initialize(this, savedInstanceState);

		Services.Log.debug("Dashboard Activity onCreate");

		setContentView(R.layout.maindashboard);

		// set support toolbar
		Toolbar toolbar = (Toolbar)this.findViewById(R.id.toolbar);
		this.setSupportActionBar(toolbar);

		// TODO : check for metadata to remove action bar
		// cannot be done here because need to be done after setContentView() and after loadMetadata
		ActivityHelper.setActionBarVisibility(this, false);

		mImageLoader = new ImageLoader(this);
		mDashboardEntity = new Entity(StructureDefinition.EMPTY);

		// Set welcome image, before showing the "loading" screen.
		LinearLayout l = (LinearLayout) findViewById(R.id.DashBoardMainLinearLayout);

		if (MetadataLoader.MUST_RELOAD_APP)
		{
			// Need to update. Update or fail if there is no URL to do so.
			if (Services.Strings.hasValue(MetadataLoader.REMOTE_VERSION_URL))
			{
				DashboardActivity.askInstallNewVersion(this);
			}
			else
			{
				AlertDialog.Builder dialog = MyApplication.getInstance().createMessageDialog(DashboardActivity.this, Services.Strings.getResource(R.string.GXM_ServerUrlEmpty));
				dialog.setPositiveButton(R.string.GXM_button_ok, new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						finish();
					}
				});
				dialog.show();
			}
			return;
		}

		Intent intent = getIntent();

		String subDashboardMetadata = intent.getStringExtra(IntentParameters.DashBoardMetadata);

		if (Strings.hasValue(subDashboardMetadata))
		{
			mIsMain = false;
			mDashboardMetadata = (DashboardMetadata) Services.Application.getPattern(subDashboardMetadata);
			mConnectivity = Connectivity.getConnectivitySupport(intent, mDashboardMetadata);
		}

		mApplication = MyApplication.getApp();

		// Restore dynamic url if persisted in the application's preferences.
		if (mApplication.getUseDynamicUrl())
		{
			SharedPreferences preferences = MyApplication.getAppSharedPreferences("DynamicUrlPreference"); //$NON-NLS-1$
			mServerUrl = preferences.getString("dynamicUrl", mApplication.getAPIUri());
			if (mServerUrl != null) {
				mApplication.setAPIUri(updateUri(mServerUrl));
			}
		}

		// Possible work around for market/play store launches. See http://code.google.com/p/android/issues/detail?id=2373
		// for more details. Essentially, the market launches the main activity on top of other activities.
		// same may happens with apps list shortcut and desktop shortcut
		// we never want this to happen. Instead, we check if we are the root and if not, we finish.
		if (!isTaskRoot())
		{
			String notificationAction = intent.getStringExtra(IntentParameters.NotificationAction);
			final String intentAction = intent.getAction();
		    if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent.ACTION_MAIN)
		    		&& !Strings.hasValue(notificationAction)) {
		    	Services.Log.warning("Main Activity is not the root.  Finishing Main Activity instead of launching.");
		        finish();
		        return;
		    }
		}

		if (!MetadataLoader.MUST_RELOAD_METADATA && tryLoadDashBoard())
			return;

		//If use dynamic Url and APIUrl
		if (mApplication.getUseDynamicUrl() && (mApplication.getAPIUri() == null || mApplication.getAPIUri().length() == 0))
		{
			Intent intentURL = getIntent();
			String serverUrl = intentURL.getStringExtra(IntentParameters.ServerURL);

			if (serverUrl == null)
			{
				startActivityPreference(false, R.string.GXM_ServerUrlIncorrect);
				return;
			}
			else if (serverUrl.length() == 0)
			{
				startActivityPreference(false, R.string.GXM_ServerUrlEmpty);
				return;
			}
			else
			{
				mApplication.setAPIUri(updateUri(serverUrl));
			}
		}

		if (ApplicationLoader.preloadApplication() && ActivityHelper.setDefaultOrientation(this))
			return; // This activity will rotate and loading will continue later.

		Drawable draw = null;

		//get landscape welcome image if necessary
		if (Services.Device.getScreenOrientation() == Orientation.LANDSCAPE)
			draw = ImageHelper.getStaticImage("appwelcomedefaultlandscape"); //$NON-NLS-1$

		if (draw == null)
			draw = ImageHelper.getStaticImage("appwelcomedefault"); //$NON-NLS-1$

		if (draw != null)
		{
			if (l!=null)
			{
				CompatibilityHelper.setBackground(l, draw);
			}
			else
				Services.Log.Error("DashboardActivity", "Cannot get DashBoardMainLinearLayout on Create"); //$NON-NLS-1$ //$NON-NLS-2$

			mHasWelcomeImage = true;
		}

		showLoadingIndicator();

		//Load Metadata in background.
		LoadMetadata();
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance()
	{
		if (mClientStartExecuted)
			return mDashboardEntity;
		else
			return null;
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		ActivityHelper.onNewIntent(this, intent);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		ActivityHelper.onResume(this);
		supportInvalidateOptionsMenu(); // "Logout" option visibility may have changed.
	}

	@Override
	protected void onPause()
	{
		ActivityHelper.onPause(this);
		super.onPause();
	}

	private void showLoadingIndicator()
	{
		if (!mHasWelcomeImage)
			m_ProgressDialog = ProgressDialog.show(this, getResources().getText(R.string.GXM_Loading), getResources().getText(R.string.GXM_PleaseWait), true);
		else if (MetadataLoader.MUST_RELOAD_METADATA)
			m_ProgressDialog = ProgressDialog.show(this, "", getResources().getText(R.string.GXM_UpdatingApplication), true);
		//TODO : change message to updating application...
	}

	private void hideLoadingIndicator()
	{
		if (m_ProgressDialog != null)
			m_ProgressDialog.dismiss();
	}

	private boolean tryLoadDashBoard()
	{
		Services.Log.debug("Dashboard Activity tryLoadDashBoard");
		if (Services.Application.isLoaded())
		{
			LoadDashBoard();
			Services.Log.debug("Dashboard Activity tryLoadDashBoard true");
			return true;
		}
		else
			return false;
	}

	private Handler mHandler = new Handler();

	private void LoadMetadata()
	{
		Thread thread = new Thread(null, doBackgroundProcessing,"Background"); //$NON-NLS-1$
		thread.start();
	}

	private final Runnable doBackgroundProcessing = new Runnable(){
		@Override
		public void run(){
			LoadApplication();
		}
	};

	private void LoadApplication()
	{
		//Lock screen
		OrientationLock.lock(this, OrientationLock.REASON_LOAD_METADATA);

		LoadResult loadResult;
		try
		{
			Services.Log.debug("Dashboard Activity LoadApplication");
			// Load the Application.
			loadResult = Services.Application.initialize();
		}
		catch (Exception ex)
		{
			// Uncaught exception, possibly "out of memory".
			loadResult = LoadResult.error(ex);
		}
		finally
		{
			OrientationLock.unlock(this, OrientationLock.REASON_LOAD_METADATA);
		}

		// if activity is reload, the mHandler is set to null onDestroy method.
		// Avoid crash on activity rotate
		if (mHandler==null)
			return;

		if (loadResult.getCode() == LoadResult.RESULT_OK && mApplication.getUseDynamicUrl())
		{
			if (!ApplicationLoader.MetadataReady ||	(ApplicationLoader.MetadataReady &&
					!ApplicationHelper.checkApplicationUri(mApplication.getAPIUri())))
			{
				startActivityPreference(true, R.string.GXM_ServerUrlIncorrect);
				return;
			}
		}

		mHandler.post(new AfterLoadRunnable(loadResult));
	}

	private class AfterLoadRunnable implements Runnable
	{
		private final LoadResult mResult;

		private AfterLoadRunnable(LoadResult result)
		{
			mResult = result;
		}

		@Override
		public void run()
		{
			hideLoadingIndicator();

			if (mResult.getCode() == LoadResult.RESULT_OK)
			{
				// Loading ok and no update; continue;
				LoadDashBoard();
			}
			else if (mResult.getCode() == LoadResult.RESULT_UPDATE)
			{
				// Need to update. Update or fail if there is no URL to do so.
				if (Services.Strings.hasValue(MetadataLoader.REMOTE_VERSION_URL))
					DashboardActivity.askInstallNewVersion(DashboardActivity.this);
				else
					MyApplication.getInstance().showMessageDialog(DashboardActivity.this, Services.Strings.getResource(R.string.GXM_ServerUrlEmpty));
			}
			else if (mResult.getCode() == LoadResult.RESULT_UPDATE_RELAUNCH)
			{
				// Do nothing app already re launching
			}
			else
			{
				// Error.
				// App should be reinitialized on next start!
				Exception errorDetail = null; // mResult.getErrorDetail(); // For testing only.
				AlertDialog.Builder dialog = MyApplication.getInstance().createMessageDialog(DashboardActivity.this, mResult.getErrorMessage(), errorDetail);
				dialog.setPositiveButton(R.string.GXM_button_ok, new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						Services.Application.resetLoad();
						finish();
					}
				});
				dialog.show();
			}
		}
	}

	public static void askInstallNewVersion(final Activity activity)
	{
		// Ask the user if they want to install a new version
		new AlertDialog.Builder(activity)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle(R.string.GXM_NewVersionAvailable)
		.setMessage(R.string.GXM_NewVersionInstallQuestion)
		.setPositiveButton(R.string.GXM_button_ok, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Intent promptInstall = new Intent(Intent.ACTION_VIEW);
				if (Services.Strings.hasValue(MetadataLoader.REMOTE_VERSION_URL)) {
					promptInstall.setData(Uri.parse(MetadataLoader.REMOTE_VERSION_URL));
					// if apk download it and install.
					//if (MetadataLoader.REMOTE_VERSION_URL.endsWith("apk")) //$NON-NLS-1$
					//	promptInstall.setType("application/vnd.android.package-archive"); //$NON-NLS-1$
					activity.startActivity(promptInstall);
					activity.finish();
				}
			}

		})
		.setNegativeButton(R.string.GXM_cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				activity.finish();
			}
		})
		.show();
		// not necessary?
		//MetadataLoader.MUST_RELOAD_APP = false;
		//MetadataLoader.MUST_RELOAD_METADATA = true; or empty metadata already charged?. Services.Application.isLoaded() = false
	}

	private void LoadDashBoard()
	{
		hideLoadingIndicator();

		if (mIsMain && !Services.Application.isLoaded())
		{
			displayLoadError(this);
			return;
		}

		if (ActivityHelper.setDefaultOrientation(this))
			return; // Rotation switched, activity will be recreated.

		if (mIsMain)
			mDashboardMetadata = Cast.as(DashboardMetadata.class, MyApplication.getApp().getMain());

		if (mDashboardMetadata != null)
			mConnectivity = Connectivity.getConnectivitySupport(getIntent(), mDashboardMetadata);

		LinearLayout l = (LinearLayout) findViewById(R.id.DashBoardMainLinearLayout);

		// dismiss welcome image
		if (mHasWelcomeImage)
		{
			if (l!=null)
			{
				CompatibilityHelper.setBackground(l, null);
			}
			else
				Services.Log.Error("DashboardActivity", "Cannot get DashBoardMainLinearLayout on LoadDashBoard"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (mIsMain && MyApplication.getApp().getMain() == null)
		{
			displayLoadError(this);
			return;
		}

		// Show login if the application is secure, unless we are already logged in.
		if (mDashboardMetadata != null && SecurityHelper.callLoginIfNecessary(getUIContext(), mDashboardMetadata))
			return;

		if (mIsMain && ActivityLauncher.startMainActivity(getUIContext(), getIntent()))
		{
			finish();
			return;
		}

		if (mDashboardMetadata == null)
		{
			setContentView(ActivityHelper.getErrorMessage(this, String.format(Services.Strings.getResource(R.string.GXM_InvalidDefinition) , "<Dashboard>"))); //$NON-NLS-1$
			return;
		}

		Entity previousData = Cast.as(Entity.class, getLastCustomNonConfigurationInstance());
		if (previousData != null)
		{
			mDashboardEntity = previousData;
			mClientStartExecuted = true;
		}
		else
		{
			// Prepare entity for variables.
			mDashboardEntity.setExtraMembers(mDashboardMetadata.getVariables());
		}

		applyStyle();

		// Hide both controls here, show the correct one in showDashboardOptions().
		GridView grid = (GridView)findViewById(R.id.DashBoardGridView);
		ListView list = (ListView)findViewById(R.id.DashBoardListView);
		grid.setVisibility(View.GONE);
		list.setVisibility(View.GONE);

		if (mDashboardMetadata.getControl().equalsIgnoreCase(DashboardMetadata.CONTROL_LIST))
			mDashboardView = list;
		else
			mDashboardView = grid;

		//set Ads.
		//dashboard should have a property for show adds.
		if (MyApplication.getApp().getUseAds() && mDashboardMetadata.getShowAds())
		{
			AdView adView = LayoutHelper.getAdView(this);

			int adSize = Services.Device.dipsToPixels(LayoutHelper.AdsSizeDpi);
			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, adSize);
			l.addView(adView, params);

			LinearLayout.LayoutParams paramsGrid = (LinearLayout.LayoutParams)mDashboardView.getLayoutParams();
			paramsGrid.weight =1;
			// Initiate a generic request to load it with an ad
			AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
			adRequestBuilder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
			AdRequest adRequest = adRequestBuilder.build();
			adView.loadAd(adRequest);
		}

		// Is a SherlockActivity, use support invalidate
		supportInvalidateOptionsMenu(); // "Logout" option visibility may have changed.

		// Analytics tracking.
		mTracker.onStart(mDashboardMetadata);

		if (!mClientStartExecuted)
		{
			// Execute the ClientStart event.
			runClientStart(this, mAfterClientStart);
		}
		else
			showDashboardOptions();

		// Run Startup from Intent
		handleIntent(getIntent(), getUIContext(), mDashboardMetadata, mDashboardEntity);
 	}

	private void handleIntent(Intent intent, UIContext uiContext, DashboardMetadata metadata, Entity entity)
	{
	    if (!handleNotification(intent, uiContext, metadata, entity))
	    	Services.Application.handleIntent(getUIContext(), intent, entity);
	}

	private final IEventListener mAfterClientStart = new IEventListener()
	{
		@Override
		public void onEndEvent(CompositeAction event, boolean successful)
		{
			mClientStartExecuted = true;

			Services.Device.runOnUiThread(new Runnable()
			{
				@Override
				public void run() {
					showDashboardOptions();
				}
			});
		}
	};

	private void showDashboardOptions()
	{
		//Initialize adapter.
		DashBoardAdapter adapter = new DashBoardAdapter(UIContext.base(this, mConnectivity), mDashboardEntity);
		if (mDashboardMetadata != null)
			adapter.setDefinition(mDashboardMetadata);

		mDashboardView.setVisibility(View.VISIBLE);
		mDashboardView.setAdapter(adapter);
		mDashboardView.setOnItemClickListener(adapter);
	}

	@Override
	public UIContext getUIContext()
	{
		return UIContext.base(this, mConnectivity);
	}

	@Override
	public Entity getData()
	{
		return mDashboardEntity;
	}

	private static void displayLoadError(Activity activity)
	{
		String message = String.format(
				Services.Strings.getResource(R.string.GXM_InvalidApplication), MyApplication.getApp().getAPIUri(), MyApplication.getApp().getAppEntry());

		MyApplication.getInstance().showError(activity, message);
	}

	private void applyStyle()
	{
		//set app bar visible or not., could be done after on create.
		if (mDashboardMetadata.getShowApplicationBar())
			ActivityHelper.setActionBarVisibility(this, true);

		// set title, not sure if necessary
		setTitle(mDashboardMetadata.getCaption());

		// Set title color and background.
		ActivityHelper.applyStyle(this, mDashboardMetadata);

		// Set dashboard background and header images.
		GxLinearLayout root = (GxLinearLayout)findViewById(R.id.DashBoardMainLinearLayout);
		ImageHelper.displayBackground(root, mDashboardMetadata.getBackgroundImage());
		ImageView header = (ImageView)findViewById(R.id.DashBoardHeaderImage);
		ImageHelper.displayImage(ImageViewDisplayImageWrapper.to(header), mDashboardMetadata.getHeaderImage());

		// Apply dashboard theme class.
		ThemeClassDefinition gridThemeClass = mDashboardMetadata.getThemeClassForGrid();
		if (gridThemeClass != null)
			GxTheme.applyStyle(root, gridThemeClass);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu , menu);

		//Set visible menu items.
		MenuItem item;

		//Always Hide about
		item = menu.findItem(R.id.about);
		item.setVisible(false);

		//Preferences
		item = menu.findItem(R.id.preferences);
		if (mApplication!=null)
		{
			item.setVisible(mApplication.getUseDynamicUrl());

			item = menu.findItem(R.id.logout);
			boolean showLogout = (mDashboardMetadata != null && MyApplication.getApp().isSecure() && mIsMain && mDashboardMetadata.getShowLogout() &&
					SecurityHelper.isLoggedIn() && !SecurityHelper.isAnonymousUser());
			item.setVisible(showLogout);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.about)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.GXM_aboutcapt);
			builder.setMessage(R.string.app_name);
			builder.setPositiveButton(R.string.GXM_button_ok , null);  //this
			builder.show();
			return true;
		}
		else if (item.getItemId() == R.id.preferences)
		{
			startActivityPreference(false, R.string.GXM_ServerUrlIncorrect);
			return true;
		}
		else if (item.getItemId() == R.id.logout)
		{
			logout();
			return true;
		}
		else
			return false;
	}

	private void logout()
	{
		final UIContext context = getUIContext();
		ActionParameters parameters = new ActionParameters(mDashboardEntity);

		// Prepare logout action
		ActionDefinition logout = new ActionDefinition(null);
		logout.setGxObject("SDActions"); //$NON-NLS-1$
		logout.setGxObjectType(GxObjectTypes.API);
		logout.setProperty("@exoMethod", "Logout"); //$NON-NLS-1$ //$NON-NLS-2$

		// Execution is "logout + show login dialog".
		CompositeAction composite = new CompositeAction(context, null, parameters);
		composite.addAction(ActionFactory.getAction(context, logout, parameters));
		composite.addAction(new RunnableAction(context, new Runnable() { @Override	public void run() { ActivityLauncher.callLogin(context); }}));

		new ActionExecution(composite).executeAction();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		ActivityHelper.onSaveInstanceState(this, outState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,  Intent data)
	{
		ActivityHelper.onActivityResult(this, requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK && ActivityHelper.isActionRequest(requestCode))
		{
			// is an action continuation
			ActionExecution.continueCurrentFromActivityResult(requestCode, resultCode, data, this);
			return;
		}

		if (resultCode != Activity.RESULT_OK && ActivityHelper.isActionRequest(requestCode))
		{
			//clean last pending actions
			ActionExecution.cleanCurrentOrLastPendingActionFromActivityResult(requestCode, resultCode, data, this);
		}

		if (resultCode == Activity.RESULT_OK)
		{
			if (requestCode == RequestCodes.PREFERENCE)
			{
				loadFromUrl(data);
			}
			else
			{
				//TODO: What happens when a Facebook button is on screen and call an activity? Here I think we should
				// take into account the request code, must we check for ActionRequest here ?
				if (tryLoadDashBoard())
					return;

				ApplicationLoader.MetadataReady = false;
				showLoadingIndicator();
				LoadMetadata();
			}
		}
		if (resultCode != Activity.RESULT_OK && (requestCode == RequestCodes.LOGIN || requestCode == RequestCodes.PREFERENCE))
		{
			//finish dashboard?
			finish();
		}
	}

	private void clearCaches()
	{
		EntityDataProvider.clearAllCaches();
	}

	// Reload the dashboard from the dynamic url choosen on the Preferences Dialog
	private void loadFromUrl(Intent data)
	{
		String serverUrl = data.getStringExtra(IntentParameters.ServerURL);
		mServerUrl = serverUrl;
		if (serverUrl == null || serverUrl.length() == 0)
		{
			startActivityPreference(true, R.string.GXM_ServerUrlEmpty);
			return;
		}
		else
		{
			new UpdateAppUrlTask().execute(serverUrl);
		}
	}

	private class UpdateAppUrlTask extends AsyncTask<String, Void, Void> {

		@Override
		protected void onPreExecute() {
			showLoadingIndicator();
		}

		@Override
		protected Void doInBackground(String... params) {
			String serverUrl = params[0];

			if (MyApplication.getApp().isSecure()) {
				SecurityHelper.logout();
			} else {
				clearCaches();
			}

			Services.Log.warning("Change url", serverUrl); //$NON-NLS-1$
			mApplication.setAPIUri(updateUri(serverUrl));

			ApplicationLoader.MetadataReady = false;
			LoadApplication();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			ActivityLauncher.callApplicationMain(DashboardActivity.this, true, true);
		}
	}

	private static String updateUri(String serverUrl)
	{
		if (serverUrl.contains("://")) //$NON-NLS-1$
			return serverUrl;
		else
			return "http://".concat(serverUrl); //$NON-NLS-1$
	}

	private void startActivityPreference(boolean showToast, int message)
	{
		ActivityLauncher.callPreferences(this, showToast, message, mServerUrl);
		hideLoadingIndicator();
	}

	@Override
	public void onStop()
	{
		super.onStop();
		mTracker.onStop();
	}

	private static void runClientStart(IGxDashboardActivity dashboard, IEventListener afterClientStart)
	{
		runClientStart(dashboard.getDashboardDefinition(), dashboard.getUIContext(), dashboard.getData(), afterClientStart);
	}

	public static void runClientStart(IViewDefinition definition, UIContext uiContext, Entity data, IEventListener afterClientStart)
	{
		if (!runEvent(definition, Events.CLIENT_START, uiContext, data, afterClientStart))
		{
			// No ClientStart, execute "After ClientStart" immediately.
			if (afterClientStart != null)
				afterClientStart.onEndEvent(null, true);
		}
	}

	private static boolean runDashboardEvent(IGxDashboardActivity dashboard, String eventName)
	{
		return runEvent(dashboard.getDashboardDefinition(), eventName, dashboard.getUIContext(), dashboard.getData(), null);
	}

	private static boolean runEvent(IViewDefinition definition, String eventName, UIContext uiContext, Entity data, IEventListener eventListener)
	{
		if (definition != null)
		{
			// Run the event if present.
			ActionDefinition event = definition.getEvent(eventName);
			if (event != null)
			{
				CompositeAction action = ActionFactory.getAction(uiContext, event, new ActionParameters(data));
				if (eventListener != null)
					action.setEventListener(eventListener);

				ActionExecution exec = new ActionExecution(action);
				exec.executeAction();
				return true;
			}
		}

		return false;
	}

	static boolean handleNotification(Intent intent, UIContext uiContext, DashboardMetadata dashboardDefinition, Entity dashboardData)
	{
		String notificationAction = intent.getStringExtra(IntentParameters.NotificationAction);
		String notificationParameters = intent.getStringExtra(IntentParameters.NotificationParameters);

		if (Services.Strings.hasValue(notificationParameters))
		{
			GcmIntentService.addNotificationParametersToEntity(dashboardData, notificationParameters);
		}

		if (dashboardDefinition != null && Services.Strings.hasValue(notificationAction))
		{
			DashboardItem dashboardItem = dashboardDefinition.getNotificationActions().get(notificationAction);
			DashBoardAdapter.runDashboardItem(uiContext, dashboardItem, dashboardData);
			return true;
		}

		return false;
	}

	@Override
	public IViewDefinition getDashboardDefinition()
	{
		return mDashboardMetadata;
	}

	@Override
	public void onBackPressed()
	{
		if (runDashboardEvent(this, Events.BACK))
			return;

		super.onBackPressed();
	}

	@Override
	public void onTranslationChanged() {
		setTitle(mDashboardMetadata.getCaption());
		((BaseAdapter) mDashboardView.getAdapter()).notifyDataSetChanged();
	}
}