/*
package com.artech.application;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;

import com.artech.R;
import com.artech.activities.ActivityLauncher;
import com.artech.activities.IntentParameters;
import com.artech.base.metadata.GenexusApplication;
import com.artech.base.metadata.IPatternMetadata;
import com.artech.base.metadata.WorkWithDefinition;
import com.artech.base.metadata.loader.LoadResult;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.services.Services;
import com.artech.common.ImageHelper;

public class StartupActivity extends Activity
{
	private boolean mAlreadyLoaded = false;
	private GenexusApplication mApplication;

	private ProgressDialog mWaitDialog;

	private String serverURL = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		mApplication = getApplication(getIntent());
		if (mApplication == null)
		{
			// Failed!
		}

		MyApplication.setApp(mApplication);
		showWelcomeScreen();
		loadApplication();
	}

	private static GenexusApplication getApplication(Intent intent)
	{
        String applicationName = intent.getStringExtra("ApplicationName"); //$NON-NLS-1$
        String applicationUri = intent.getStringExtra("ApplicationUri"); //$NON-NLS-1$
        String applicationEntryPoint = intent.getStringExtra("ApplicationEntryPoint"); //$NON-NLS-1$
        boolean applicationIsSecure = intent.getBooleanExtra("ApplicationIsSecure", false); //$NON-NLS-1$
        String applicationClientId = intent.getStringExtra("ApplicationClientId"); //$NON-NLS-1$
        String applicationSecret = intent.getStringExtra("ApplicationSecret"); //$NON-NLS-1$
        String applicationLoginObject = intent.getStringExtra("ApplicationLoginObject"); //$NON-NLS-1$
        boolean applicationEnableAnonymousUser = intent.getBooleanExtra("ApplicationEnableAnonymousUser", false); //$NON-NLS-1$
        boolean applicationUseDynamicUrl = intent.getBooleanExtra("ApplicationUseDynamicUrl", false); //$NON-NLS-1$
        String applicationDynamicUrl = intent.getStringExtra("ApplicationDynamicUrl"); //$NON-NLS-1$
        boolean applicationEnableAds = intent.getBooleanExtra("ApplicationEnableAds", false); //$NON-NLS-1$
        String applicationAdsPublisherId = intent.getStringExtra("ApplicationAdsPublisherId"); //$NON-NLS-1$

        GenexusApplication application = new GenexusApplication();

        if (applicationUri != null)
        {
	        application = new GenexusApplication();
	        application.setName(applicationName);
	        application.setAPIUri(applicationUri);
	        application.setAppEntry(applicationEntryPoint);
	        application.setUseDynamicUrl(applicationUseDynamicUrl);
	        application.setDynamicUrlAppId(applicationDynamicUrl);
	        application.setUseAds(applicationEnableAds);
	        application.setAdMobPublisherId(applicationAdsPublisherId);
	        if (applicationIsSecure)
	        {
	        	application.setIsSecure(applicationIsSecure);
	        	application.setClientId(applicationClientId);
	        	application.setSecret(applicationSecret);
	        	application.setLoginObject(applicationLoginObject);
	        	application.setEnableAnonymousUser(applicationEnableAnonymousUser);
	        }
        }

        return application;
	}

	private void showWelcomeScreen()
	{
		Drawable background = null;
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
			background = ImageHelper.getStaticImage("appwelcomedefaultlandscape"); //$NON-NLS-1$
		if (background == null)
			background = ImageHelper.getStaticImage("appwelcomedefault"); //$NON-NLS-1$

		if (background != null)
		{
			ImageView loadingImage = new ImageView(this);
			setContentView(loadingImage);
			loadingImage.setImageDrawable(background);
		}
		else
			mWaitDialog = ProgressDialog.show(this, getResources().getText(R.string.GXM_Loading), getResources().getText(R.string.GXM_PleaseWait), true);
	}

	private void loadApplication()
	{
		new ApplicationLoader().execute();
	}

	private static class ApplicationLoader extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params)
		{
			// TODO Auto-generated method stub
			return null;
		}
	}






	private void whereToPutThis()
	{

		if (!Services.Application.isLoaded())
        {
			displayLoadError(this);
			return;
		}

		// al final

		IPatternMetadata pattern = Services.Application.getPattern(MyApplication.getApp().getAppEntry());
		if (pattern != null && pattern instanceof WorkWithDefinition)
		{
			ActivityLauncher.call(this, MyApplication.getApp().getAppEntry());
			finish();
			return;
		}

		if (MyApplication.getApp().Main == null)
		{
			displayLoadError(this);
			return;
		}



		//Type Tabs, call dashboardTab activity.
		if (mDashboardMetadata.getControl().equalsIgnoreCase("Tabs")) //$NON-NLS-1$
		{
			ActivityLauncher.CallDashboardTab(this, mDashboardMetadata);
			finish();
			return;
		}




	}


	private void endLoading()
	{
		if (mWaitDialog != null)
		{
			mWaitDialog.dismiss();
			mWaitDialog = null;
		}
	}

	private boolean tryLoadDashBoard()
	{
		final Object data = getLastNonConfigurationInstance();
        if (data != null)
        {
        	boolean alreadyLoaded = Boolean.parseBoolean(data.toString());
        	if (alreadyLoaded == true && Services.Application.isLoaded())
            {
        		LoadDashBoard();
                return true;
            }
        }
        else
        {
        	if (Services.Application.isLoaded())
            {
        		LoadDashBoard();
                return true;
            }
        }
        return false;
	}

	private void ifnourlpreferences()
	{
        //If use dynamic Url and APIUrl
        if (mApplication.getUseDynamicUrl() && (mApplication.getAPIUri() == null || mApplication.getAPIUri().length() == 0))
        {
			Intent intentURL = getIntent();
			String serverUrl = intentURL.getStringExtra(IntentParameters.ServerURL);

			if (serverUrl == null) {
				startActivityPreference(false, R.string.GXM_ServerUrlIncorrect);
				return;
			} else if(serverUrl.length() == 0)
			{
				startActivityPreference(false, R.string.GXM_ServerUrlEmpty);
				return;
			}
			else {
				mApplication.setAPIUri(updateUri(serverUrl));
			}
        }

	}











	private Runnable doBackgroundProcessing = new Runnable(){
		@Override
		public void run(){
			LoadApplication();
		}
	};

	private void LoadApplication()
	{
		LoadResult loadResult;
		try
		{
			// Load the Application.
			loadResult = Services.Application.initialize();
		}
		catch (Exception ex)
		{
			// Uncaught exception, possibly "out of memory".
			loadResult = LoadResult.error(ex);
		}

		if (loadResult.getCode() == LoadResult.RESULT_OK && mApplication.getUseDynamicUrl())
		{
			if (!ApplicationLoader.MetadataReady ||
				(ApplicationLoader.MetadataReady && !mApplication.getDynamicUrlAppId().equalsIgnoreCase(mApplication.getAppId())))
			{
				startActivityPreference(true, R.string.GXM_ServerUrlIncorrect);
				return;
			}
		}

		runOnUiThread(new AfterLoadRunnable(loadResult));
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
					askInstallNewVersion();
				else
					MyApplication.getInstance().showMessageDialog(StartupActivity.this, Services.Strings.getResource(R.string.GXM_ServerUrlEmpty));
			}
			else
			{
				// Error.
				MyApplication.getInstance().showMessageDialog(StartupActivity.this, mResult.getErrorMessage(), mResult.getErrorDetail());
			}
		}
	};

	private void askInstallNewVersion()
	{
		// Ask the user if they want to install a new version
        new AlertDialog.Builder(this)
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
					if (MetadataLoader.REMOTE_VERSION_URL.endsWith("apk")) //$NON-NLS-1$
			    		promptInstall.setType("application/vnd.android.package-archive"); //$NON-NLS-1$
					startActivity(promptInstall);
					finish();
    			}
	        }

        })
        .setNegativeButton(R.string.GXM_cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		})
        .show();
	}


	@Override
	public Object onRetainNonConfigurationInstance() {
	   	//return metadata.getName();
	   	return mAlreadyLoaded;
	}


	@Override
	protected void onDestroy()
	{
		endLoading();
		super.onDestroy();
	}

	private static void displayLoadError(Activity activity)
	{
		String message = String.format(
			Services.Strings.getResource(R.string.GXM_InvalidApplication), MyApplication.getApp().getAPIUri(), MyApplication.getApp().getAppEntry());

		MyApplication.getInstance().showError(activity, message);
	}
}
*/