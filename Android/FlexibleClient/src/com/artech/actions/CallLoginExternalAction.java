package com.artech.actions;

import java.net.HttpURLConnection;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AlertDialog;

import com.artech.R;
import com.artech.activities.ActivityLauncher;
import com.artech.activities.IntentParameters;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.services.ServiceResponse;
import com.artech.base.utils.ResultDetail;
import com.artech.base.utils.Strings;
import com.artech.common.ActionsHelper;
import com.artech.common.SecurityHelper;

class CallLoginExternalAction extends Action
{
	private String mErrorMessage = Strings.EMPTY;
    private boolean mLoginRedirectToOtherActivity = false;

	public CallLoginExternalAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
	}

	@Override
	public boolean Do()
	{
		ServiceResponse serviceResponse = ActionsHelper.runLoginExternalAction(this);
		if (serviceResponse != null && serviceResponse.HttpCode == HttpURLConnection.HTTP_SEE_OTHER)
		{
			// Must show browser for login.
			String newUrl = serviceResponse.Message;
			Intent intent = ActivityLauncher.GetComponent(getActivity(), newUrl, true);
			intent.putExtra(IntentParameters.ExternalLoginCall, true);
		   	
			getActivity().startActivityForResult(intent, RequestCodes.ACTION); // RequestCodes.LOGINEXTERNAL
			mLoginRedirectToOtherActivity = true;
			return true;
		}
		else
		{
			// Already handled.
			mLoginRedirectToOtherActivity = false;
			ResultDetail<?> result = SecurityHelper.afterLogin(serviceResponse);

			// Keep error message if unsuccessful.
			if (!result.getResult())
				mErrorMessage = result.getMessage();

			return result.getResult();
		}
	}

	public String getErrorMessage()
	{
		return mErrorMessage;
	}

	@Override
	public boolean catchOnActivityResult()
	{
		//if redirect return true else false
		return mLoginRedirectToOtherActivity;

	}

	@Override
	public ActionResult afterActivityResult(int requestCode, int resultCode, Intent result)
	{
		if (result != null && resultCode == Activity.RESULT_OK)
		{
			ResultDetail<?> resultDetail = (ResultDetail<?>)result.getSerializableExtra(IntentParameters.EXTERNAL_LOGIN_RESULT);
			if (!resultDetail.getResult())
			{
				// Show error message
				AlertDialog.Builder builder = new AlertDialog.Builder(getMyActivity());
				builder.setTitle(R.string.GXM_errtitle);
				builder.setMessage(resultDetail.getMessage());
				builder.setPositiveButton(R.string.GXM_button_ok, null);
				builder.show();
			}
		}

		return super.afterActivityResult(requestCode, resultCode, result);
	}
}
