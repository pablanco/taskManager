package com.artech.actions;

import java.util.ArrayList;

import android.content.Intent;
import android.support.annotation.NonNull;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.IPatternMetadata;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.WorkWithDefinition;
import com.artech.base.metadata.enums.GxObjectTypes;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.ui.navigation.CallOptions;
import com.artech.ui.navigation.CallOptionsHelper;
import com.artech.ui.navigation.CallType;
import com.artech.utils.Cast;

public class DynamicCallAction extends Action
{
	private StructureDefinition mBusinessComponent;
	private Action mComputedAction;
	private boolean mIsRedirect;

	public static @NonNull DynamicCallAction redirect(UIContext context, Entity data, String dynamicCall)
	{
		return new DynamicCallAction(context, data, dynamicCall);
	}

	private DynamicCallAction(UIContext context, Entity entity, String dynamicCall)
	{
		super(context, null, new ActionParameters(entity));
		if (entity != null && Services.Strings.hasValue(dynamicCall))
		{
			ActionDefinition staticActionDefinition = getActionDefinitionFromString(dynamicCall, null);
			mComputedAction = ActionFactory.getAction(context, staticActionDefinition, new ActionParameters(entity));
			mIsRedirect = true; // Assuming that only calls to SD objects are made on start (otherwise it wouldn't have arrived from server).
		}
	}

	public DynamicCallAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
		// Do NOT compute action here, because object or parameters may be set by previous action in a composite.
	}

	private void calculateComputedActionFromEntity()
	{
		if (getParameterEntity() != null && getDefinition().getParameters().size() > 0)
		{
			String dynamicCallString = getParameterEntity().optStringProperty(getDefinition().getParameter(0).getValue());
			if (Services.Strings.hasValue(dynamicCallString))
			{
				ActionDefinition staticActionDefinition = getActionDefinitionFromString(dynamicCallString, getDefinition());
				mComputedAction = ActionFactory.getAction(getContext(), staticActionDefinition, getParameters());
			}
		}
	}

	private ActionDefinition getActionDefinitionFromString(String dynamicCallString, ActionDefinition actionDef)
	{
		ActionDefinition def;
		if (actionDef != null)
			def = new ActionDefinition(actionDef.getDataView());
		else
			def = new ActionDefinition(null);

		try {
			// Now check first if it is a call to a Smart Device object
			populateAction(dynamicCallString, def);

			// The first parameter of the dynamic call is the variable, but after the call can contain additionally parameters
			if (actionDef != null) {
				for (int i = 1; i < actionDef.getParameters().size(); i++) {
					def.getParameters().add(actionDef.getParameter(i));
				}
				// If Insert , Update, Delete parameters in Dynamic call Haven't the name so we take them from the keys names
				if (mBusinessComponent != null) {
					for (int i = 0; i < mBusinessComponent.Root.GetKeys().size(); i++) {
						DataItem di = mBusinessComponent.Root.GetKeys().get(i);
						if (i < def.getParameters().size()) {
							ActionParameter parm = def.getParameter(i);
							parm.setName(di.getName());
						} else {
							// Something was wrong, parameters should be the same that the keys.
							break;
						}
					}
				}
			}
		} catch (Exception ex) {
			Services.Log.Error("Invalid Parsing for DynamicCall: " + dynamicCallString, ex); //$NON-NLS-1$
		}

		return def;
	}

	private void populateAction(String callString, ActionDefinition def)
	{
		String dynamicCallString = callString.trim();

		String objType = "sd"; //$NON-NLS-1$
		if (Strings.starsWithIgnoreCase(callString, "sd:"))  { //$NON-NLS-1$
			dynamicCallString = callString.substring(3);
		}
		else if (callString.startsWith("eo:")) //$NON-NLS-1$
		{
			objType = "eo"; //$NON-NLS-1$
			dynamicCallString = callString.substring(3);
		}
		else { // wbp: | prc:
			if (callString.startsWith("prc:") || callString.startsWith("wbp:")) { //$NON-NLS-1$ //$NON-NLS-2$
				dynamicCallString = callString.substring(4);
				objType = callString.substring(0, 3);
			}
		}

		// Parse call
		String[] callParts = Services.Strings.split(dynamicCallString, Strings.QUESTION);

		// Object Name
		String objName = Strings.EMPTY;
		if (callParts.length > 0)
			objName = callParts[0];
		def.setGxObject(objName);

		// Object Parameters
		String [] objParameters = null;
		String objComponent = Strings.EMPTY;

		if (callParts.length > 1)
			objParameters = Services.Strings.split(callParts[1].trim(), ',');

		if (objParameters != null)
		{
			for (String objParameter : objParameters)
			{
				// The parameters are not *exactly* URI-encoded, GX uses '+' for spaces instead of '%20'.
				String parameterValue = objParameter.replace("+", Strings.SPACE); //$NON-NLS-1$
				parameterValue = Services.HttpService.UriDecode(parameterValue);

				// These parameters are always constants.
				ActionParameter parm = new ActionParameter("\"" + parameterValue + "\""); //$NON-NLS-1$ //$NON-NLS-2$
				def.getParameters().add(parm);
			}
		}

		// Object Type
		if (objType.equalsIgnoreCase("prc")) //$NON-NLS-1$
			def.setGxObjectType(GxObjectTypes.PROCEDURE);
		else if (objType.equalsIgnoreCase("wbp")) //$NON-NLS-1$
			def.setGxObjectType(GxObjectTypes.WEBPANEL);
		else if (objType.equalsIgnoreCase("eo")) //$NON-NLS-1$
		{
			def.setGxObjectType( GxObjectTypes.API);
			String[] objNameParts = Services.Strings.split(objName, Strings.DOT);
			if (objNameParts.length > 0) {
				objName = objNameParts[0];
				def.setGxObject(objName);
			}
			if (objNameParts.length > 1) {
				String method = objNameParts[1];
				def.setProperty("@exoMethod", method); //$NON-NLS-1$
			}
		}
		else if (objType.equalsIgnoreCase("sd")) { //$NON-NLS-1$
			String[] objNameParts = Services.Strings.split(objName, Strings.DOT);
			if (objNameParts.length > 1) {
				objName = objNameParts[0];
				int objSeparIndex = 0;
				while (Services.Application.getPattern(objName) == null && objSeparIndex < objNameParts.length - 1) {
					objSeparIndex++;
					objName += Strings.DOT + objNameParts[objSeparIndex];
				}
				
				def.setGxObject(objName);

				ArrayList<String> values = new ArrayList<String>();
				for (int i = objSeparIndex + 1; (i < objNameParts.length && i <= objSeparIndex + 2) ; i++)
					values.add(objNameParts[i]);
				objComponent = Services.Strings.join(values, Strings.DOT);

				if (objNameParts.length > objSeparIndex + 3) {
					// Remove ( from mode, could be wrong in the metadata
					String bcMode = objNameParts[objSeparIndex + 3].replace("(", ""); //$NON-NLS-1$ //$NON-NLS-2$
					def.setProperty("@bcMode", bcMode); //$NON-NLS-1$
				}
			}
			IPatternMetadata metadata = Services.Application.getPattern(objName);
			WorkWithDefinition workWith = Cast.as(WorkWithDefinition.class, metadata);
			if (workWith != null) {
				def.setGxObjectType(GxObjectTypes.SDPANEL);
				def.setProperty("@instanceComponent", objComponent); //$NON-NLS-1$
				if (Services.Strings.hasValue(def.optStringProperty("@bcMode"))) // Ask for the bc only when Update, Insert, Delete mode. //$NON-NLS-1$
					mBusinessComponent = workWith.getBusinessComponent();
			}
			else if (metadata!=null)
			{
				def.setGxObjectType(GxObjectTypes.DASHBOARD);
			}
			else
			{
				Services.Log.Error("Could not execute dyncall to " + callString); //$NON-NLS-1$
			}
		}
	}

	@Override
	public boolean catchOnActivityResult()
	{
		if (mComputedAction == null)
			calculateComputedActionFromEntity(); // Should not execute before Do() but, defensively, calculate anyway.

		if (mComputedAction == null)
			return false;

		return mComputedAction.catchOnActivityResult();
	}

	@Override
	public boolean Do()
	{
		if (mComputedAction == null)
			calculateComputedActionFromEntity();

		if (mComputedAction == null)
		{
			Services.Log.debug("Could not execute DynamicCallAction "); //$NON-NLS-1$
			// Allow continue
			return true;
		}

		if (mIsRedirect)
			setupForRedirect(mComputedAction);

		return mComputedAction.Do();
	}

	private static void setupForRedirect(Action action)
	{
		WorkWithAction callAction = Cast.as(WorkWithAction.class, action);
		if (callAction == null && action instanceof CompositeAction)
			callAction = Cast.as(WorkWithAction.class, ((CompositeAction)action).getNextActionToExecute());

		if (callAction != null)
		{
			IViewDefinition calledObject = callAction.getObject();
			if (calledObject != null)
				CallOptionsHelper.setCallOption(calledObject.getObjectName(), CallOptions.OPTION_TYPE, CallType.REPLACE.name());
		}
	}

	@Override
	public boolean isActivityEnding()
	{
		if (mComputedAction != null)
			return mComputedAction.isActivityEnding();
		else
			return super.isActivityEnding();
	}

	@Override
	public ActionResult afterActivityResult(int requestCode, int resultCode, Intent result) 
	{
		return super.afterActivityResult(requestCode, resultCode, result);
	}
	
	
}
