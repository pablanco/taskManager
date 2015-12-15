package com.artech.application;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.artech.R;
import com.artech.activities.ActivityLauncher;
import com.artech.activities.IntentParameters;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.ApplicationHelper;

public class Preferences extends AppCompatActivity
{
	private Dialog mServerUrlDialog;
	private EditText mEditText;

	private String mServerURL;
	private boolean mViewDialog = false;

	private static final String PREFERENCES_KEY = "DynamicUrlPreference"; //$NON-NLS-1$
	private static final int SCAN = 0;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preferences);

		// set support toolbar
		Toolbar toolbar = (Toolbar)this.findViewById(R.id.toolbar);
		this.setSupportActionBar(toolbar);

		Intent intent = getIntent();
		boolean showToast = intent.getBooleanExtra("showToast", false);	 //$NON-NLS-1$
		int messageToast = intent.getIntExtra("messageToast", R.string.GXM_ServerUrlIncorrect);		 //$NON-NLS-1$
		mServerURL = intent.getStringExtra(IntentParameters.ServerURL);

		if (showToast)
			Toast.makeText(this, messageToast, Toast.LENGTH_SHORT).show();

		findViewById(R.id.layoutServerUrl).setOnClickListener(serverUrlClickListener);

    }

	private OnClickListener serverUrlClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			CreateDialog();
		}
	};

	private void CreateDialog()
	{
		// Create the dialog
		mServerUrlDialog = new Dialog(Preferences.this);
		mServerUrlDialog.setContentView(R.layout.dynamic_url_dialog);
		mServerUrlDialog.setTitle(R.string.GXM_ServerUrl);

		mEditText = (EditText) mServerUrlDialog.findViewById(R.id.EditTextServerUrl);
		if ((mServerURL != null) && (mServerURL.length() > 0))
			mEditText.setText(mServerURL);

		mViewDialog = true;

		//Ok button
        Button okButton = (Button) mServerUrlDialog.findViewById(R.id.OkDialog);
        okButton.setOnClickListener(new OnClickListener() {
        	
	        @Override
            public void onClick(View v) {
        		mServerURL = mEditText.getText().toString();
        		if (!mServerURL.contains("://")) { //$NON-NLS-1$
        			mServerURL = "http://" + mServerURL; //$NON-NLS-1$
        		}
        		mViewDialog = false;
        		new ValidateAppServerUri().execute(mServerURL);
            }
        });

        //Cancel button
        Button cancelButton = (Button) mServerUrlDialog.findViewById(R.id.CancelDialog);
        cancelButton.setOnClickListener(new OnClickListener() {
        	
        	@Override
            public void onClick(View v) {
        		mServerUrlDialog.dismiss();
        		mViewDialog = false;
            }
        });

        //Scan button
        Button scanButton = (Button) mServerUrlDialog.findViewById(R.id.ScanDialog);
        scanButton.setOnClickListener(new OnClickListener()
        {
        	@Override
            public void onClick(View v)
        	{
            	try
            	{
	            	Intent intent = new Intent("com.google.zxing.client.android.SCAN"); //$NON-NLS-1$
	            	ActivityLauncher.setIntentFlagsNewDocument(intent);
	    			intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); //$NON-NLS-1$ //$NON-NLS-2$
	                startActivityForResult(intent, SCAN);
            	}
            	catch (Exception ex)
            	{
	     	    }
            }
        });

		mServerUrlDialog.show();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    if (requestCode == SCAN) {
	        if (resultCode == RESULT_OK) {
	            String contents = intent.getStringExtra("SCAN_RESULT");	             //$NON-NLS-1$
	            String newContents = contents;

	            try {
	            	newContents = Strings.EMPTY;
	            	String[] strURL = contents.split("/"); //$NON-NLS-1$
	            	for (int i = 0; i < strURL.length - 1; i++)
	            	{
	            		newContents = newContents.concat(strURL[i]);
	            		newContents = newContents.concat("/"); //$NON-NLS-1$
	            	}
	            } catch (Exception e) {
	            }

	            mEditText.setText(newContents);
	            mServerURL = mEditText.getText().toString();

	            // Handle successful scan
	        } else if (resultCode == RESULT_CANCELED) {
	            // Handle cancel
	        }
	    }
	}

	private void finishPreferences(int result) {
		if (result == RESULT_OK) {
			// Persist new API Uri in the application's preferences.
			SharedPreferences preferences = MyApplication.getAppSharedPreferences(PREFERENCES_KEY);
			Editor editor = preferences.edit();
			editor.putString("dynamicUrl", mServerURL); //$NON-NLS-1$
			editor.commit();
		}
		
		Intent resultIntent = new Intent();
		resultIntent.putExtra(IntentParameters.ServerURL, mServerURL);
		setResult(result, resultIntent);
		finish();
	}

	@Override
	public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
	  super.onRestoreInstanceState(savedInstanceState);
	  if (savedInstanceState.getBoolean("ShowDialog")) //$NON-NLS-1$
	  {
		  mServerURL = savedInstanceState.getString("ServerURL"); //$NON-NLS-1$
		  CreateDialog();
	  }
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
		savedInstanceState.putBoolean("ShowDialog", mViewDialog); //$NON-NLS-1$
		savedInstanceState.putString("ServerURL", mEditText.getText().toString());		 //$NON-NLS-1$
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onDestroy()
	{
		if (mServerUrlDialog != null)
			mServerUrlDialog.dismiss();

		mEditText = null;
		super.onDestroy();
	}
	
	private class ValidateAppServerUri extends AsyncTask<String, Void, Integer> {
		private static final int VALID_URL = 0;
		private static final int INVALID_URL = 1;
		private static final int NO_CONNECTION = 2;
		
		@Override
		protected Integer doInBackground(String... params) {
			if (!Services.HttpService.isOnline()) {
				return NO_CONNECTION;
			}
			
			return ApplicationHelper.checkApplicationUri(params[0]) ? VALID_URL : INVALID_URL;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			switch (result) {
				case VALID_URL:
					finishPreferences(RESULT_OK);
					break;
				case INVALID_URL:
					Toast.makeText(MyApplication.getAppContext(), R.string.GXM_ServerUrlIncorrect, Toast.LENGTH_SHORT).show();
					break;
				case NO_CONNECTION:
					Toast.makeText(MyApplication.getAppContext(), R.string.GXM_NoInternetConnection, Toast.LENGTH_SHORT).show();
					break;
			}
		}
	}
}