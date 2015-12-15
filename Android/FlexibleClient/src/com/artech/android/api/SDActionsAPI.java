package com.artech.android.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.View;

import com.artech.actions.ActionExecution;
import com.artech.actions.ActionFactory;
import com.artech.actions.ActionParameters;
import com.artech.actions.CompositeAction;
import com.artech.activities.ActivityFlowControl;
import com.artech.activities.ActivityLauncher;
import com.artech.activities.IGxActivity;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.ResultRunnable;
import com.artech.base.utils.Strings;
import com.artech.base.utils.Version;
import com.artech.common.SecurityHelper;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;
import com.artech.fragments.IDataView;
import com.artech.fragments.LayoutFragment;
import com.artech.fragments.LayoutFragmentEditBC;
import com.artech.utils.Cast;

public class SDActionsAPI extends ExternalApi
{
	private static final String METHOD_GO_HOME = "GoHome";
	public static final String METHOD_RETURN_TO = "ReturnTo";
	private static final String METHOD_TAKE_APP_SCREENSHOT = "TakeApplicationScreenshot";
	private static final String METHOD_REFRESH = "Refresh";
	private static final String PARAMETER_REFRESH_KEEP = "keep";
	private static final String METHOD_DO_SUB = "Do";

	private static final Version VERSION_WHERE_SAVE_DOES_NOT_RETURN = new Version(10, 3, 4);

	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		Activity myActivity = getActivity();
		if (method.equalsIgnoreCase("return")) //$NON-NLS-1$
		{
			LayoutFragment fragment = Cast.as(LayoutFragment.class, getContext().getDataView());
			// The return action should consider if it is a fragment inside a dialog or not (dialog = popup | callout).
			if (fragment == null || fragment.getDialog() == null)
				SDActions.returnAction(myActivity);
			else
				fragment.returnOK();

			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (method.equalsIgnoreCase(METHOD_REFRESH))
		{
			final IGxActivity currentActivity = Cast.as(IGxActivity.class, getActivity());
			final IDataView currentComponent = getContext().getDataView();
			final boolean keepPosition = (parameters.size() >= 1 && parameters.get(0) != null && PARAMETER_REFRESH_KEEP.equalsIgnoreCase(parameters.get(0).toString()));

			Services.Device.invokeOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					if (currentComponent != null)
						currentComponent.refreshData(keepPosition);
					else
						currentActivity.refreshData(keepPosition);
				}
			});

			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (method.equalsIgnoreCase("save")) //$NON-NLS-1$
		{
			if (getContext().getDataView() instanceof LayoutFragmentEditBC)
			{
				final LayoutFragmentEditBC editFragment = (LayoutFragmentEditBC)getContext().getDataView();

				// Versions prior to 10.3.4 should also RETURN when they SAVE,
				// Versions after that should not.
				Version metadataVersion = getDefinition().getDataView().getInstanceProperties().getDefinitionVersion();
				final boolean isReturnImplicit = metadataVersion.isLessThan(VERSION_WHERE_SAVE_DOES_NOT_RETURN);

				Services.Device.runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						editFragment.runSaveAction(isReturnImplicit);
					}
				});
				return ExternalApiResult.SUCCESS_WAIT;
			}
			else
				return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (method.equalsIgnoreCase("cancel")) //$NON-NLS-1$
		{
			// The Cancel() action should consider if it is a fragment inside a dialog or not (dialog = popup | callout).
			LayoutFragment fragment = Cast.as(LayoutFragment.class, getContext().getDataView());
			if (fragment == null || fragment.getDialog() == null)
				ActivityFlowControl.finishWithCancel(getActivity());
			else
				fragment.returnCancel();

			return ExternalApiResult.SUCCESS_WAIT;
		}
		else if (method.equalsIgnoreCase("login")) //$NON-NLS-1$
		{
			//Solve this as CallLogin action.
			throw new IllegalStateException("SDActions.Login should've been handled by CallLoginAction.");
		}
		else if (method.equalsIgnoreCase("logout")) //$NON-NLS-1$
		{
			// Clear token and cache, and call server.
			SecurityHelper.logout();
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else if (METHOD_GO_HOME.equalsIgnoreCase(method))
		{
			// Go Home and clear stack.
			ActivityLauncher.callApplicationMain(getActivity(), true, true);
			return ExternalApiResult.SUCCESS_WAIT;
		}
		else if (METHOD_RETURN_TO.equalsIgnoreCase(method) && toString(parameters).size() >= 1)
		{
			String objectToReturn = toString(parameters).get(0);
			if (Services.Strings.hasValue(objectToReturn))
				ActivityFlowControl.returnTo(getActivity(), objectToReturn);

			return ExternalApiResult.SUCCESS_WAIT;
		}
		else if (METHOD_TAKE_APP_SCREENSHOT.equalsIgnoreCase(method))
		{
			Uri uri = getApplicationScreenshot();
			return ExternalApiResult.success(uri != null ? uri.getPath() : Strings.EMPTY);
		}
		else if (METHOD_DO_SUB.equalsIgnoreCase(method))
		{
			//Execute a sub rutine here. Continue this event after sub rutine event finish.
			String subToCall = toString(parameters).get(0);

			// get action from definition.
			IDataViewDefinition dataViewDef = this.getAction().getDefinition().getDataView();
			ActionDefinition action = dataViewDef.getEvent(subToCall);
			
			// Copy parameter entity
			ActionParameters actionParametes =  new ActionParameters(this.getAction().getParameterEntity());
						
			// Call the new "Event" 
			CompositeAction newRunningAction = ActionFactory.getAction(getContext(), action, actionParametes);
			ActionExecution exec = new ActionExecution(newRunningAction);
			exec.executeAction();

			return ExternalApiResult.SUCCESS_WAIT;
		}
		else
			return ExternalApiResult.failureUnknownMethod(this, method);
	}

	private Uri getViewImage(View view)
	{
		try
		{
			File outputDir = getActivity().getCacheDir(); // context being the Activity pointer
			File outputFile = File.createTempFile("screen", ".png", outputDir);

			view.setDrawingCacheEnabled(true);
			Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
			try
			{
				view.setDrawingCacheEnabled(false);
				FileOutputStream out = new FileOutputStream(outputFile);
				try
				{
					bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
					out.flush();
					return Uri.fromFile(outputFile);
				}
				finally
				{
					IOUtils.closeQuietly(out);
				}
			}
			finally
			{
				bitmap.recycle();
			}
		}
		catch (IOException e)
		{
			Services.Log.error(e);
			return null;
		}
	}

	private Uri getApplicationScreenshot()
	{
		return Services.Device.invokeOnUiThread(new ResultRunnable<Uri>()
		{
			@Override
			public Uri run()
			{
				View contentView = getActivity().getWindow().getDecorView().findViewById(android.R.id.content);
				return getViewImage(contentView);
			}
		});
	}
}
