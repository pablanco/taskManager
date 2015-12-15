package com.artech.android.api;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.artech.R;
import com.artech.actions.ActionResult;
import com.artech.activities.ActivityHelper;
import com.artech.activities.ActivityLauncher;
import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.PhoneHelper;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;
import com.artech.providers.EntityDataProvider;

public class InteropAPI extends ExternalApi
{
	public static final String OBJECT_NAME = "Interop";

	private final static String METHOD_OPEN = "Open"; //$NON-NLS-1$
	private final static String METHOD_OPEN_IN_BROWSER = "OpenInBrowser"; //$NON-NLS-1$
	private final static String METHOD_CAN_OPEN = "CanOpen"; //$NON-NLS-1$

	private final static String METHOD_CLEAR_CACHE = "ClearCache"; //$NON-NLS-1$
	private final static String METHOD_IS_ONLINE = "IsOnline"; //$NON-NLS-1$
	private final static String METHOD_MESSAGE = "Msg"; //$NON-NLS-1$
	private final static String PARAMETER_MESSAGE_TOAST = "nowait"; //$NON-NLS-1$
	private final static String PARAMETER_MESSAGE_LOG = "status"; //$NON-NLS-1$

	private final static String METHOD_SLEEP = "Sleep"; //$NON-NLS-1$

	private final static String METHOD_TO_STRING = "ToString"; //$NON-NLS-1$
	private final static String METHOD_FORMAT = "Format"; //$NON-NLS-1$

	private final static String METHOD_PLACE_CALL = "PlaceCall"; //$NON-NLS-1$
	private final static String METHOD_SEND_EMAIL = "SendEmail"; //$NON-NLS-1$
	private final static String METHOD_SEND_EMAIL_ADVANCED = "SendEmailAdvanced"; //$NON-NLS-1$
	private final static String METHOD_SEND_SMS = "SendSms"; //$NON-NLS-1$

	private final static String METHOD_GET_APPLICATION_STATE = "ApplicationState"; //$NON-NLS-1$
	private final static int APPLICATION_STATE_ACTIVE = 0;
	private final static int APPLICATION_STATE_BACKGROUND = 2;

	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		List<String> parameterValues = toString(parameters);

		Activity myActivity = getActivity();
		if (method.equalsIgnoreCase("sendmessage")) //$NON-NLS-1$
		{
			SDActions.sendMessageFromParameters(getActivity(), parameterValues);
			return ExternalApiResult.SUCCESS_WAIT; // Picker opened.
		}
		else if (method.equalsIgnoreCase("scanbarcode")) //$NON-NLS-1$
		{
			return ScannerAPI.callScanBarcode(getActivity());
		}
		else if (method.equalsIgnoreCase("playvideo") || method.equalsIgnoreCase("playaudio")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			//play video
			if (parameterValues.size() > 0)
			{
				String data = parameterValues.get(0);
				if (method.equalsIgnoreCase("playvideo")) //$NON-NLS-1$
					ActivityLauncher.CallViewVideo(getContext(), data);
				else
					ActivityLauncher.CallViewAudio(getContext(), data);

				return ExternalApiResult.SUCCESS_WAIT;
			}
		}
		else if (method.equalsIgnoreCase(METHOD_PLACE_CALL))
		{
			//call number
			if (parameterValues.size() > 0)
			{
				String data = parameterValues.get(0);

				if (PhoneHelper.callNumber(getActivity(), data))
					return ExternalApiResult.SUCCESS_WAIT;
				else
					return getInteropActionFailureResult();
			}
		}
		else if (method.equalsIgnoreCase(METHOD_SEND_EMAIL_ADVANCED))
		{
			//send mail
			if (parameterValues.size() > 4)
			{
				String[] email = convertJsonArrayToList(parameterValues.get(0));
				String[] ccEmail = convertJsonArrayToList(parameterValues.get(1));
				String[] bccEmail = convertJsonArrayToList(parameterValues.get(2));
				String subject = parameterValues.get(3);
				String message = parameterValues.get(4);

				if (PhoneHelper.sendEmail(getActivity(), email, ccEmail, bccEmail, subject, message))
					return ExternalApiResult.SUCCESS_WAIT;
				else
					return getInteropActionFailureResult();
			}
		}
		else if (method.equalsIgnoreCase(METHOD_SEND_EMAIL))
		{
			//send mail
			if (parameterValues.size() > 2)
			{
				String email = parameterValues.get(0);
				String subject = parameterValues.get(1);
				String message = parameterValues.get(2);

				if (PhoneHelper.sendEmail(getActivity(), email, subject, message))
					return ExternalApiResult.SUCCESS_WAIT;
				else
					return getInteropActionFailureResult();
			}
		}
		else if (method.equalsIgnoreCase(METHOD_SEND_SMS))
		{
			//send sms
			if (parameterValues.size() > 1)
			{
				String phone = parameterValues.get(0);
				String message = parameterValues.get(1);

				if (PhoneHelper.sendSms(getActivity(), phone, message))
					return ExternalApiResult.SUCCESS_WAIT;
				else
					return getInteropActionFailureResult();
			}
		}
		else if (method.equalsIgnoreCase(METHOD_MESSAGE))
		{
			if (parameterValues.size() > 0)
			{
				String message = parameterValues.get(0);
				boolean isToast = parameterValues.size() >= 2 && PARAMETER_MESSAGE_TOAST.equalsIgnoreCase(parameterValues.get(1));
				boolean isLog = parameterValues.size() >= 2 && PARAMETER_MESSAGE_LOG.equalsIgnoreCase(parameterValues.get(1));

				if (isLog)
				{
					Services.Log.debug(message);
					return ExternalApiResult.SUCCESS_CONTINUE;
				}
				else
				{
					SDActions.showMessage(myActivity, message, isToast);
					return (isToast ? ExternalApiResult.SUCCESS_CONTINUE : ExternalApiResult.SUCCESS_WAIT);
				}
			}
		}
		else if (method.equalsIgnoreCase("confirm")) //$NON-NLS-1$
		{
			if (parameterValues.size() > 0 )
			{
				String message = parameterValues.get(0);
				SDActions.showConfirmDialog(getAction(), myActivity, message);
				return ExternalApiResult.SUCCESS_WAIT;
			}
		}
		else if (method.equalsIgnoreCase(METHOD_OPEN) || method.equalsIgnoreCase(METHOD_OPEN_IN_BROWSER))
		{
			if (parameterValues.size() != 0)
			{
				if (open(parameterValues.get(0)))
					return ExternalApiResult.SUCCESS_WAIT;
				else
					return getInteropActionFailureResult();
			}
		}
		else if (method.equalsIgnoreCase(METHOD_CAN_OPEN))
		{
			if (parameterValues.size() != 0)
			{
				boolean canOpen = canOpen(parameterValues.get(0));
				return ExternalApiResult.success(canOpen ? Strings.TRUE : Strings.FALSE);
			}
		}
		else if (method.equalsIgnoreCase(METHOD_CLEAR_CACHE))
		{
			EntityDataProvider.clearAllCaches();
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (method.equalsIgnoreCase(METHOD_IS_ONLINE))
		{
			String result = Boolean.toString(Services.HttpService.isOnline());
			return ExternalApiResult.success(result);

		}
		else if (method.equalsIgnoreCase(METHOD_SLEEP))
		{
			if (parameterValues.size() != 0)
			{
				Double seconds = Services.Strings.tryParseDouble(parameterValues.get(0));
				//noinspection EmptyCatchBlock
				try
				{
					if (seconds != null)
						Thread.sleep((long)(seconds * 1000));
				}
				catch (InterruptedException e) { }

				return ExternalApiResult.SUCCESS_CONTINUE;
			}
		}
		else if (method.equalsIgnoreCase(METHOD_TO_STRING))
		{
			if (parameterValues.size() != 0)
			{
				// Second and third parameters can be length and decimals.
				Integer length = null;
				if (parameterValues.size() >= 2)
					length = Services.Strings.tryParseInt(parameterValues.get(1));

				Integer decimals = null;
				if (parameterValues.size() >= 3)
					decimals = Services.Strings.tryParseInt(parameterValues.get(2));

				String str = GenexusFunctions.gxToString(getAction(), parameterValues.get(0), length, decimals);
				return ExternalApiResult.success(str);
			}
		}
		else if (method.equalsIgnoreCase(METHOD_FORMAT))
		{
			String str = GenexusFunctions.gxFormat(getAction(), parameterValues);
			return ExternalApiResult.success(str);
		}
		else if (method.equalsIgnoreCase(METHOD_GET_APPLICATION_STATE))
		{
			int state = (ActivityHelper.hasCurrentActivity() ? APPLICATION_STATE_ACTIVE : APPLICATION_STATE_BACKGROUND);
			return ExternalApiResult.success(String.valueOf(state));
		}
		else if (method.equalsIgnoreCase("IOSSetBadgeNumber"))
		{
			// Ignored method
			return ExternalApiResult.SUCCESS_CONTINUE;
		}

		return ExternalApiResult.failureUnknownMethod(this, method, parameters.size());
	}

	@Override
	public ExternalApiResult afterActivityResult(int requestCode, int resultCode, Intent result, String method)
	{
		if (method.equalsIgnoreCase("scanbarcode"))
			return ScannerAPI.afterScanActivityResult(resultCode, result);

		return null;
	}

	static ExternalApiResult getInteropActionFailureResult()
	{
		showNoApplicationAvailableMessage();
		return new ExternalApiResult(ActionResult.FAILURE, null);
	}

	private static void showNoApplicationAvailableMessage()
	{
		String str = Services.Strings.getResource(R.string.GXM_NoApplicationAvailable);
		MyApplication.getInstance().showMessage(str);
	}

	private boolean open(String url)
	{
		Intent intent = getIntent(url);
		if (intent == null)
			return false;

		try
		{
			ActivityLauncher.setIntentFlagsNewDocument(intent);
			return PhoneHelper.startAction(getActivity(), intent);
		}
		catch (ActivityNotFoundException e)
		{
			return false;
		}
	}

	private boolean canOpen(String url)
	{
		Intent intent = getIntent(url);
		if (intent != null)
		{
			List<ResolveInfo> intentActivities = getContext().getPackageManager().queryIntentActivities(intent, 0);
			return (intentActivities.size() != 0);
		}
		else
			return false;
	}

	private static Intent getIntent(String url)
	{
		if (!Strings.hasValue(url))
			return null;

		Intent intent = new Intent(Intent.ACTION_VIEW);

		final String FILE_SCHEME = "file://"; //$NON-NLS-1$
		if (Strings.starsWithIgnoreCase(url, FILE_SCHEME))
		{
			File file = new File(url.substring(FILE_SCHEME.length()));
			String mimeType = URLConnection.guessContentTypeFromName(file.getName());
			intent.setDataAndType(Uri.fromFile(file), mimeType);
		}
		else
		{
			// Add http as scheme, if missing.
			if (!url.contains(":")) //$NON-NLS-1$
				url = "http://" + url;  //$NON-NLS-1$

			intent.setData(Uri.parse(url));
		}

		return intent;
	}

	/**
	 * Helper function to convert parameters from JSONArray to array of strings
	 */
	private static String[] convertJsonArrayToList(String arrayValues)
	{
		List<String> result = new ArrayList<>();
		try
		{
			JSONArray array = new JSONArray(arrayValues);
			for (int i = 0; i < array.length(); i++)
			{
				String value = array.getString(i);
				result.add(value);
			}
		}
		catch (JSONException e)
		{
			Services.Log.Error("Cannot convert " + arrayValues + " to json array"); //$NON-NLS-1$
		}

		return result.toArray(new String[result.size()]);
	}
}
