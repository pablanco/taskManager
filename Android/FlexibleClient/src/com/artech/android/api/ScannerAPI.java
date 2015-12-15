package com.artech.android.api;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.artech.R;
import com.artech.actions.ActionResult;
import com.artech.activities.ActivityHelper;
import com.artech.activities.ActivityLauncher;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.services.Services;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

public class ScannerAPI extends ExternalApi
{
	public static final String OBJECT_NAME = "ScannerAPI";
	public final static String METHOD_ScanInLoop = "ScanInLoop"; //$NON-NLS-1$
	public final static String METHOD_ScanBarcode = "ScanBarcode"; //$NON-NLS-1$

	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		List<String> parameterValues = toString(parameters);

		if (method.equalsIgnoreCase(METHOD_ScanBarcode))
		{
			return callScanBarcode(getActivity());
		}
		else if (method.equalsIgnoreCase(METHOD_ScanInLoop))
		{
			mLoopResult = new JSONArray();
			mIncludeBeep = readBoolean(parameterValues, 2);

			// Put empty in result
			if (getAction() != null)
				getAction().setOutputValue(mLoopResult);

			return callScanBarcode(getActivity());
		}
		else
			return ExternalApiResult.failureUnknownMethod(this, method);
	}

	static ExternalApiResult callScanBarcode(Activity activity)
	{
		Intent intent = new Intent("com.google.zxing.client.android.SCAN"); //$NON-NLS-1$
		ActivityLauncher.setIntentFlagsNewDocument(intent);
		try
		{
			activity.startActivityForResult(intent, RequestCodes.ACTIONNOREFRESH);
			return ExternalApiResult.SUCCESS_WAIT;
		}
		catch (ActivityNotFoundException ex)
		{
			callDownloadScanner(activity);
			return ExternalApiResult.FAILURE;
		}
	}

	@Override
	public ExternalApiResult afterActivityResult(int requestCode, int resultCode, Intent result, String method)
	{
		if (resultCode == Activity.RESULT_OK)
		{
			if (method.equalsIgnoreCase(METHOD_ScanInLoop))
			{
				if (result != null)
				{
					String scanResult = result.getStringExtra("SCAN_RESULT"); //$NON-NLS-1$
					addValueToResult(scanResult);
					callScanBarcode(getActivity());
					return new ExternalApiResult(ActionResult.SUCCESS_WAIT, mLoopResult);
				}
			}
			else if (method.equalsIgnoreCase(METHOD_ScanBarcode))
			{
				return afterScanActivityResult(resultCode, result);
			}
		}
		return null;
	}

	static ExternalApiResult afterScanActivityResult(int resultCode, Intent result)
	{
		if (resultCode == Activity.RESULT_OK && result != null)
		{
			String scanResult = result.getStringExtra("SCAN_RESULT"); //$NON-NLS-1$
			return new ExternalApiResult(ActionResult.SUCCESS_CONTINUE, scanResult);
		}
		else
			return null;
	}

	// Helper function to convert result from array of string to JSONArray
	private static JSONArray mLoopResult = new JSONArray();
	private static boolean mIncludeBeep = false;

	private static void addValueToResult(String scannerValue)
	{
		JSONObject jsonProperty = new JSONObject();
		try
		{
			jsonProperty.put("Barcode", scannerValue);
			mLoopResult.put(jsonProperty);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private static boolean readBoolean(List<String> values, int arrayIndex)
	{
		boolean includeBeep = false;
		if (values.size() > arrayIndex)
			includeBeep = Boolean.parseBoolean(values.get(arrayIndex));

		return includeBeep;
	}

	private static void callDownloadScanner(Activity activity)
	{
		final Activity dialogActivity = (activity == null ? ActivityHelper.getCurrentActivity() : activity);
		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				new AlertDialog.Builder(dialogActivity)
					.setTitle(R.string.GXM_BarcodeScanner)
					.setMessage(R.string.GXM_BarcodeScannerInstallQuestion)
					.setPositiveButton(R.string.GXM_Yes, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.zxing.client.android")); //$NON-NLS-1$
							dialogActivity.startActivity(intent);
						}
					})
					.setNegativeButton(R.string.GXM_No, null)
					.show();

			}
		});
	}
}
