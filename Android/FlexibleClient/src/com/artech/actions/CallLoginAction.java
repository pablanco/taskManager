package com.artech.actions;

import com.artech.activities.ActivityLauncher;
import com.artech.application.MyApplication;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.services.ServiceResponse;
import com.artech.base.services.Services;
import com.artech.base.utils.ResultDetail;
import com.artech.base.utils.Strings;
import com.artech.common.ActionsHelper;
import com.artech.common.SecurityHelper;
import com.artech.common.SecurityHelper.LoginStatus;

class CallLoginAction extends Action
{
	private String mErrorMessage = Strings.EMPTY;
	private boolean mWaitForPasswordChange;

	public CallLoginAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
	}

	@Override
	public boolean Do()
	{
		mWaitForPasswordChange = false;
		ServiceResponse response = ActionsHelper.runLoginAction(this);
		ResultDetail<SecurityHelper.LoginStatus> result = SecurityHelper.afterLogin(response);

		if (!result.getResult())
		{
			if (result.getData() == LoginStatus.CHANGE_PASSWORD)
			{
				// Go to the "change password" screen if available. The login screen waits.
				String changePasswordPanel = MyApplication.getApp().getChangePasswordObject();
				if (Services.Strings.hasValue(changePasswordPanel) && ActivityLauncher.callForResult(getContext(), changePasswordPanel, RequestCodes.ACTION))
				{
					MyApplication.getInstance().showMessage(result.getMessage());
					mErrorMessage = Strings.EMPTY;

					mWaitForPasswordChange = true;
					return true; // Current action must wait.
				}
			}

			// Generic error, or we do not have a change password panel. Keep error message.
			mErrorMessage = result.getMessage();
			return false;
		}

		return result.getResult();
	}

	public String getErrorMessage()
	{
		return mErrorMessage;
	}

	@Override
	public boolean catchOnActivityResult() { return mWaitForPasswordChange; }
}
