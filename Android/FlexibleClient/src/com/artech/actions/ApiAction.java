package com.artech.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;

import com.artech.R;
import com.artech.android.api.ScannerAPI;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiFactory;
import com.artech.externalapi.ExternalApiResult;

public class ApiAction extends Action implements IActionWithOutput
{
	private final String mApiName;
	private final String mMethod;
	private final ActionParameter mReturnValue;

	private String mErrorMessage = Strings.EMPTY;
	private ExternalApi mApiInstance = null;

	public boolean finishReturn = false;
	public Intent finishReturnResult = null;
	public int finishReturnRequestCode = RequestCodes.ACTION;
	public int finishReturnResultCode = Activity.RESULT_OK;
	public Activity finishReturnCurrentActivity = null;

	private Boolean mCatchOnActivityResult;

	public ApiAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
		mApiName = definition.getGxObject();
		mMethod = definition.optStringProperty("@exoMethod");
		mReturnValue = ActionHelper.newAssignmentParameter(definition, "@returnValue", ActionHelper.ASSIGN_LEFT_EXPRESSION);
	}

	@Override
	public boolean Do()
	{
		loadInstance();
		mCatchOnActivityResult = false;

		if (mApiInstance != null)
		{
			mApiInstance.setAction(this);

			ExternalApiResult result = mApiInstance.execute(mMethod, getParameterValues());

			//noinspection ConstantConditions
			if (result == null) // This is a sanity check -- supposedly never happens because execute() is annotated as @NonNull.
				throw new IllegalStateException("External API '" + mApiInstance.toString() + " returned a null ExternalAPIResult. This should never happen!");

			if (result.getReturnValue() != null)
				setOutputValue(mReturnValue, result.getReturnValue());

			mErrorMessage = result.getMessage();
			mCatchOnActivityResult = result.getActionResult() == ActionResult.SUCCESS_WAIT;

			return result.getActionResult().isSuccess();
		}
		else
			mErrorMessage = String.format(Services.Strings.getResource(R.string.GXM_InvalidDefinition), mApiName);

		return false;
	}

	@Override
	protected List<Object> getParameterValues()
	{
		List<Object> values = new ArrayList<>();
		for (ActionParameter parameter : getDefinition().getParameters())
		{
			Object value = getParameterValue(parameter);
			values.add(value);
		}

		return values;
	}

	private void loadInstance()
	{
		if (mApiInstance == null)
			mApiInstance = ExternalApiFactory.getInstance(mApiName);
	}

	@Override
	public boolean catchOnActivityResult()
	{
		if (mCatchOnActivityResult == null)
			throw new IllegalStateException("catchOnActivityResult() has been called BEFORE Do(). This is not allowed.");

		return mCatchOnActivityResult;
	}

	@Override
	public ActionResult afterActivityResult(int requestCode, int resultCode, Intent result)
	{
		if (mApiInstance != null)
		{
			ExternalApiResult out = mApiInstance.afterActivityResult(requestCode, resultCode, result, mMethod);
			if (out != null)
			{
				if (out.getReturnValue() != null)
					setOutputValue(out.getReturnValue());

				if (out.getActionResult() != null)
					return out.getActionResult();
			}
		}

		return ActionResult.SUCCESS_CONTINUE;
	}

	public boolean hasOutput()
	{
		return mReturnValue.getExpression() != null || Strings.hasValue(mReturnValue.getValue());
	}

	public void setOutputValue(Object outValue)
	{
		setOutputValue(mReturnValue, outValue);
	}

	public String getErrorMessage()
	{
		return mErrorMessage;
	}

	public boolean isScanInLoopAction() {
		if (mApiName.equalsIgnoreCase(ScannerAPI.OBJECT_NAME)) {
			if (mMethod.equalsIgnoreCase(ScannerAPI.METHOD_ScanInLoop)) {
				return true;
			}
		}
		return false;
	}

	public boolean isReturnAction()
	{
		return (mApiName.equalsIgnoreCase("sdactions") && mMethod.equalsIgnoreCase("return")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean isCancelAction()
	{
		return (mApiName.equalsIgnoreCase("sdactions") && mMethod.toLowerCase(Locale.US).startsWith("cancel")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean isLoginAction()
	{
		return (mApiName.equalsIgnoreCase("sdactions") && mMethod.equalsIgnoreCase("login")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean isLoginExternalAction()
	{
		return (mApiName.equalsIgnoreCase("sdactions") && mMethod.equalsIgnoreCase("loginexternal")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public Activity getActivity()
	{
		return super.getActivity();
	}

	@Override
	public OutputResult getOutput()
	{
		// Return output message error if exists.
		if (Services.Strings.hasValue(mErrorMessage))
			return OutputResult.error(mErrorMessage);
		else
			return OutputResult.ok();
	}
}
