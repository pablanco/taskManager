package com.artech.actions;

import java.util.List;

import android.app.Activity;
import android.net.Uri;

import com.artech.application.MyApplication;
import com.artech.base.application.IGxObject;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.GxObjectDefinition;
import com.artech.base.metadata.ObjectParameterDefinition;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.metadata.enums.ControlTypes;
import com.artech.base.model.Entity;
import com.artech.base.model.PropertiesObject;
import com.artech.base.providers.IApplicationServer;
import com.artech.base.services.Services;
import com.artech.common.ServiceHelper;
import com.artech.layers.LocalDataProvider;
import com.artech.layers.LocalProcedure;

public class CallGxObjectAction extends Action implements IActionWithOutput
{
    private OutputResult mOutput;

	public CallGxObjectAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
	}

	@Override
	public boolean Do()
	{
		mOutput = runGxObject(this, getDefinition(), getParameters());
		return mOutput.isOk();
	}

	@Override
	public Activity getActivity()
	{
		return super.getActivity();
	}

	@Override
	public OutputResult getOutput()
	{
		return mOutput;
	}

	/**
	 * Calls a GeneXus object on the server and returns its result.
	 * Receives definition and parameters separately because they may differ from those in the action
	 * (e.g. if a custom action is mapped to a procedure call).
	 * However the output is assigned to the "real" action.
	 */
	public static OutputResult runGxObject(Action action, ActionDefinition definition, ActionParameters parameters)
	{
		beginWorking();
		try
		{
			// Read definition from action.
			GxObjectDefinition actionObject = Services.Application.getGxObject(definition.getGxObject());
			List<ActionParameter> actionParameters = definition.getParameters();

			// Obtain implementation.
			IGxObject gxObject = action.getApplicationServer().getGxObject(definition.getGxObject());

			// Prepare input parameters.
			PropertiesObject callParameters = prepareCallParameters(gxObject, action, definition, actionObject, parameters.getEntity());

			// Call object.
			OutputResult result = gxObject.execute(callParameters);

			if (result.isOk())
			{
				// Read output parameters.
				for (int i = 0; i < actionObject.getParameters().size(); i++)
				{
					ObjectParameterDefinition procParameter = actionObject.getParameter(i);
					if (procParameter.isOutput())
					{
						// Read result parameter from object.
						Object outValue = callParameters.getProperty(procParameter.getName());

						// See if we have a local variable to assign it to.
						if (i < actionParameters.size())
						{
							ActionParameter actionParameter = actionParameters.get(i);
							if (actionParameter != null)
							{
								if (actionParameter.isAssignable())
									action.setOutputValue(actionParameter, outValue);
							}
						}
					}
				}
			}

			return result;
		}
		finally
		{
			endWorking();
		}
	}

	static PropertiesObject prepareCallParameters(IGxObject gxObject, Action actionContext, ActionDefinition actionDefinition, GxObjectDefinition actionObject, Entity from)
	{
		PropertiesObject callParameters = new PropertiesObject();
		IApplicationServer server = getServerForProc(gxObject);

		if (actionObject != null)
		{
			List<ActionParameter> actionParameters = actionDefinition.getParameters();
			for (int parameterNumber = 0; parameterNumber < actionParameters.size(); parameterNumber++)
			{
				Object value = actionContext.getParameterValue(actionParameters.get(parameterNumber), from);
				if (parameterNumber < actionObject.getInParameters().size())
				{
					ObjectParameterDefinition parm = actionObject.getInParameters().get(parameterNumber);
					if (parm != null)
					{
						callParameters.setProperty(parm.getName(), value);

						// special case , process Image call to procedure
						// call upload of images.
						if (value!= null)  //defensive , in some case the parameter could be null
						{
							String result = value.toString();

							if (parm.getControlType().equalsIgnoreCase(ControlTypes.PhotoEditor)) // ?
							{
								Uri imageUri = Uri.parse(result);
								if (server!=null)
								{
									// for now not "upload" for LocalProcedure run, just send the path as parameter.
									ServiceHelper.resizeAndUploadImage(MyApplication.getAppContext(), server, imageUri, parm.getName(), parm.getMaximumUploadSizeMode(), callParameters, null);
								}
							}
							else if (parm.getControlType().equalsIgnoreCase(ControlTypes.VideoView)
									|| parm.getControlType().equalsIgnoreCase(ControlTypes.AudioView)  // audio and videos
									|| parm.getControlType().equalsIgnoreCase(ControlTypes.BinaryBlob)) // include base blobs
							{
								if (server!=null)
								{
									// for now not "upload" for LocalProcedure run, just send the path as parameter.
									ServiceHelper.uploadFileFromPath(server, callParameters, parm, result);
								}
							}
						}
					}
				}
			}
		}

		return callParameters;
	}

	private static IApplicationServer getServerForProc(IGxObject gxObject)
	{
		// temp, for now not "upload" for LocalDataProvider run
		if (gxObject instanceof LocalDataProvider)
			return null;
		// for LocalProcedure "upload" the image locally to allow save in the correct path, like BC in user events.
		if (gxObject instanceof LocalProcedure)
			return MyApplication.getApplicationServer(Connectivity.Offline);
		return MyApplication.getApplicationServer(Connectivity.Online);
	}

	@Override
	public boolean catchOnActivityResult() { return false; }

	private static int sWorkingCount = 0;
	private static final Object sWorkingLock = new Object();

	public static boolean isWorking()
	{
		synchronized (sWorkingLock) { return (sWorkingCount > 0); }
	}

	private static void beginWorking()
	{
		synchronized (sWorkingLock) { sWorkingCount++; }
	}

	private static void endWorking()
	{
		synchronized (sWorkingLock) { sWorkingCount--; }
	}
}
