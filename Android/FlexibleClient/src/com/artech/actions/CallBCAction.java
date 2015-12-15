package com.artech.actions;

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.net.Uri;

import com.artech.application.MyApplication;
import com.artech.base.application.IBusinessComponent;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.enums.ControlTypes;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.model.Entity;
import com.artech.base.providers.IApplicationServer;
import com.artech.base.services.Services;
import com.artech.base.utils.ParametersStringUtil;
import com.artech.common.ServiceHelper;

class CallBCAction extends Action implements IActionWithOutput
{
	private final StructureDefinition mStructureDefinition;
	private final String mBCVariableName;

	private OutputResult mOutput;

	public CallBCAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
		mStructureDefinition = Services.Application.getBusinessComponent(definition.getGxObject());
		mBCVariableName = definition.optStringProperty("@bcVariable"); //$NON-NLS-1$
	}

	private short getMode()
	{
		// Only batch modes (exclude view).
		if (getDefinition().optStringProperty("@bcMode").equalsIgnoreCase("Delete")) //$NON-NLS-1$ //$NON-NLS-2$
			return DisplayModes.DELETE;
		else if (getDefinition().optStringProperty("@bcMode").equalsIgnoreCase("Update")) //$NON-NLS-1$ //$NON-NLS-2$
			return DisplayModes.EDIT;
		else // Default Mode
			return DisplayModes.INSERT;
	}

	@Override
	public boolean catchOnActivityResult()
	{
		return false; // It's a batch call.
	}

	@Override
	public boolean Do()
	{
		mOutput = callBcBatch(mStructureDefinition, getBCParameters());
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

	private Map<String, String> getBCParameters()
	{
		return ActionParametersHelper.getParametersForBC(this);
	}

	private OutputResult callBcBatch(StructureDefinition structureDef, Map<String, String> parameters)
	{
		if (structureDef == null)
			return OutputResult.error(String.format("Structure definition for '%s' not found.", getDefinition().getGxObject()));

		IBusinessComponent businessComponent = getApplicationServer().getBusinessComponent(structureDef.getName());

		Entity entity = null;
		short mode = getMode();
		OutputResult result = null;

		if (mode == DisplayModes.INSERT)
		{
			entity = new Entity(structureDef);
			setBCFieldValues(getApplicationServer(), entity, parameters);

			result = businessComponent.save(entity);
		}
		else if (mode == DisplayModes.EDIT || mode == DisplayModes.DELETE)
		{
			// Read entity to update or delete.
			entity = new Entity(structureDef);
			List<String> key = ParametersStringUtil.getKeyValuesFromFieldValues(parameters, structureDef);

			// TODO: What, if any, message should be shown when the entity to update/delete is not in the server?
			OutputResult loadResult = businessComponent.load(entity, key);

			if (loadResult.isOk())
			{
				if (mode == DisplayModes.EDIT)
				{
					setBCFieldValues(getApplicationServer(), entity, parameters);
					result = businessComponent.save(entity);
				}
				else
					result = businessComponent.delete();
			}
			else
				result = loadResult; // Return the load error as the error.
		}

		if (result != null && result.isOk())
		{
			// Assign variable with BC and return.
			setOutputValue(mBCVariableName, entity);
		}

		return result;
	}

	private static void setBCFieldValues(IApplicationServer mServer, Entity entity, Map<String, String> parameters)
	{
		if (parameters == null)
			return;

		for (DataItem def : entity.getLevel().Items)
		{
			String result = parameters.get(def.getName());
    		if (Services.Strings.hasValue(result))
    		{
    			entity.setProperty(def.getName(), result);
    			if (def.getControlType().equalsIgnoreCase(ControlTypes.PhotoEditor))
    			{
    				//Upload images in save BC batch
    				Uri imageUri = Uri.parse(result);
    				ServiceHelper.resizeAndUploadImage(MyApplication.getAppContext(), mServer, imageUri, def.getName(), def.getMaximumUploadSizeMode(), entity, null);
    			}
    			else if (def.getControlType().equalsIgnoreCase(ControlTypes.VideoView)
    					|| def.getControlType().equalsIgnoreCase(ControlTypes.AudioView) // audio and videos
						|| def.getControlType().equalsIgnoreCase(ControlTypes.BinaryBlob)) // include base blobs
    			{
    				ServiceHelper.uploadFileFromPath(mServer, entity, def, result);
    	    				
    			}
    		}
		}
	}

	
}
