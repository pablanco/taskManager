package com.artech.android.api;

import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.annotation.NonNull;

import com.artech.R;
import com.artech.actions.Action;
import com.artech.android.ActivityResourceBase;
import com.artech.android.ActivityResources;
import com.artech.base.services.Services;
import com.artech.base.utils.Function;
import com.artech.base.utils.Strings;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

public class ProgressIndicatorApi extends ExternalApi
{
	private static final String METHOD_SHOW = "Show"; //$NON-NLS-1$
	private static final String METHOD_HIDE = "Hide"; //$NON-NLS-1$
	private static final String METHOD_SHOW_SPECIAL = "ShowWithTitle"; // Also: ShowWithTitleAndDescription //$NON-NLS-1$

	private static final String PREFIX_SET_PROPERTY = "set"; //$NON-NLS-1$
	private static final String SET_PROPERTY_TYPE = "setType"; //$NON-NLS-1$
	private static final String SET_PROPERTY_TITLE = "setTitle"; //$NON-NLS-1$
	private static final String SET_PROPERTY_DESCRIPTION = "setDescription"; //$NON-NLS-1$
	private static final String SET_PROPERTY_MAX_VALUE = "setMaxValue"; //$NON-NLS-1$
	private static final String SET_PROPERTY_VALUE = "setValue"; //$NON-NLS-1$

	private static final int TYPE_INDETERMINATE = 0;
	private static final int TYPE_DETERMINATE = 1;

	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		ProgressIndicator data = getCurrentIndicator();
		List<String> parameterValues = toString(parameters);

		// Methods
		if (METHOD_SHOW.equalsIgnoreCase(method) || Strings.starsWithIgnoreCase(method, METHOD_SHOW_SPECIAL))
		{
			showIndicator(data, parameterValues);
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (METHOD_HIDE.equalsIgnoreCase(method))
		{
			hideIndicator(data);
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (Strings.starsWithIgnoreCase(method, PREFIX_SET_PROPERTY) && parameterValues.size() == 1)
		{
			// Property setter.
			String value = parameterValues.get(0);

			if (SET_PROPERTY_TYPE.equalsIgnoreCase(method))
				data.Type = Services.Strings.tryParseInt(value, TYPE_INDETERMINATE);
			else if (SET_PROPERTY_TITLE.equalsIgnoreCase(method))
				data.Title = value;
			else if (SET_PROPERTY_DESCRIPTION.equalsIgnoreCase(method))
				data.Description = value;
			else if (SET_PROPERTY_MAX_VALUE.equalsIgnoreCase(method))
				data.MaxValue = Services.Strings.tryParseInt(value, 0);
			else if (SET_PROPERTY_VALUE.equalsIgnoreCase(method))
				data.Value = Services.Strings.tryParseInt(value, 0);

			// Refresh changed settings into current dialog, if any.
			updateIndicator(data);
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else
			return ExternalApiResult.failureUnknownMethod(this, method);
	}

	private ProgressIndicator getCurrentIndicator()
	{
		return getCurrentIndicator(getActivity());
	}

	private static ProgressIndicator getCurrentIndicator(Activity activity)
	{
		// Get the current indicator or create a new one.
		return ActivityResources.getResource(activity, ProgressIndicator.class,
			new Function<Activity, ProgressIndicator>()
			{
				@Override
				public ProgressIndicator run(Activity activity) { return new ProgressIndicator(); }
			});
	}

	private void showIndicator(final ProgressIndicator data, final List<String> parameters)
	{
		if (parameters.size() >= 1)
			data.Title = parameters.get(0);

		if (parameters.size() >= 2)
			data.Description = parameters.get(1);

		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				// Hide previous dialog, if any.
				hideIndicator(data);

				// Create dialog from configured data (and store it for later operations).
				data.Dialog = new ProgressDialog(getContext());
				data.Dialog.setCancelable(false);
				updateIndicator(data);

				// Show the new dialog.
				data.Dialog.show();
			}
		});
	}

	private void updateIndicator(final ProgressIndicator data)
	{
		final ProgressDialog dialog = data.Dialog;
		if (dialog == null)
			return;

		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				dialog.setTitle(data.Title);
				dialog.setMessage(data.Description);
				dialog.setProgressStyle(data.Type == TYPE_DETERMINATE ? ProgressDialog.STYLE_HORIZONTAL : ProgressDialog.STYLE_SPINNER);
				dialog.setIndeterminate(data.Type == TYPE_INDETERMINATE);
				dialog.setMax(data.MaxValue);
				dialog.setProgress(data.Value);
			}
		});
	}

	private static void hideIndicator(final ProgressIndicator data)
	{
		final ProgressDialog dialog = data.Dialog;
		if (dialog == null)
			return;

		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				dialog.dismiss();
				data.Dialog = null;
			}
		});
	}

	public static boolean isShowing(Activity activity)
	{
		return (getCurrentIndicator(activity).Dialog != null);
	}

	public static void onEndEvent(Action action, boolean success)
	{
		Activity activity = action.getContext().getActivity();
		hideIndicator(getCurrentIndicator(activity));
	}

	private static class ProgressIndicator extends ActivityResourceBase
	{
		int Type = TYPE_INDETERMINATE;
		String Title = null;
		String Description = Services.Strings.getResource(R.string.GXM_PleaseWait);
		int MaxValue = 100;
		int Value = 0;

		private ProgressDialog Dialog;

		@Override
		public void onDestroy(Activity activity)
		{
			// A dialog attached to a destroyed activity causes a memory leak, and will cause a crash if accessed later on.
			if (Dialog != null)
				hideIndicator(this);
		}
	}
}
