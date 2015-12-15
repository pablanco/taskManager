package com.artech.base.metadata.rules;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.enums.GxObjectTypes;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.loader.WorkWithMetadataLoader;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

abstract class AbstractPromptRuleDefinition extends RuleDefinition implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	// Prompt to call.
	private final String mCallObject;
	private final String mCallObjectComponent;
	private final ArrayList<ActionParameter> mParameters;

	// Control on which the prompt should appear.
	private final String mControlName;

	// Service to call to calculate inferred attributes.
	private final String mAfterService;
	private final List<String> mAfterServiceInput;
	private final List<String> mAfterServiceOutput;

	AbstractPromptRuleDefinition(IDataViewDefinition parent, INodeObject jsonRule)
	{
		super(parent);
		mCallObject = MetadataLoader.getObjectName(jsonRule.optString("@call")); //$NON-NLS-1$
		mCallObjectComponent = jsonRule.optString("@instanceComponent"); //$NON-NLS-1$
		mControlName = jsonRule.optString("@control"); //$NON-NLS-1$

		mParameters = new ArrayList<ActionParameter>();
		WorkWithMetadataLoader.readActionParameters(parent, mParameters, jsonRule);

		String afterService = null;
		List<ActionParameter> afterServiceInput = new ArrayList<ActionParameter>();
		List<ActionParameter> afterServiceOutput = new ArrayList<ActionParameter>();

		INodeObject jsonAfterService = jsonRule.optNode("afterService"); //$NON-NLS-1$
		if (jsonAfterService != null)
		{
			afterService = jsonAfterService.optString("@service"); //$NON-NLS-1$

			INodeObject jsonServiceInput = jsonAfterService.optNode("inputs"); //$NON-NLS-1$
			if (jsonServiceInput != null)
				WorkWithMetadataLoader.readActionParameterList(parent, afterServiceInput, jsonServiceInput);

			INodeObject jsonServiceOutput = jsonAfterService.optNode("outputs"); //$NON-NLS-1$
			if (jsonServiceOutput != null)
				WorkWithMetadataLoader.readActionParameterList(parent, afterServiceOutput, jsonServiceOutput);
		}

		mAfterService = afterService;
		mAfterServiceInput = ActionParameter.getValues(afterServiceInput);
		mAfterServiceOutput = ActionParameter.getValues(afterServiceOutput);
	}

	@Override
	public String toString()
	{
		return String.format("prompt(%s, %s) on %s", mCallObject, mParameters, mControlName); //$NON-NLS-1$
	}

	public List<ActionParameter> getParameters()
	{
		return mParameters;
	}

	public boolean isOutputParameter(ActionParameter parameter)
	{
		int index = mParameters.indexOf(parameter);
		if (index != -1)
		{
			String objectName = mCallObject;
			if (Strings.hasValue(mCallObjectComponent))
				objectName = String.format("%s.%s", mCallObject, mCallObjectComponent);

			IViewDefinition promptObject = Services.Application.getView(objectName);
			if (promptObject != null)
			{
				if (index < promptObject.getParameters().size())
					return (promptObject.getParameters().get(index).isOutput());
			}
		}

		return false;
	}

	public String getControlName()
	{
		return mControlName;
	}

	public ActionDefinition getPromptAction()
	{
		ActionDefinition promptAction = new ActionDefinition(getParent());

		promptAction.setGxObjectType(GxObjectTypes.SDPANEL);
		promptAction.setGxObject(mCallObject);
		promptAction.setProperty("@instanceComponent", mCallObjectComponent); //$NON-NLS-1$
		promptAction.getParameters().addAll(mParameters);

		if (Strings.hasValue(mAfterService))
		{
			ActionDefinition dependencyAction = new ActionDefinition(getParent());
			dependencyAction.setProperty(ActionDefinition.DependencyInfo.SERVICE, mAfterService);
			dependencyAction.setProperty(ActionDefinition.DependencyInfo.SERVICE_INPUT, mAfterServiceInput);
			dependencyAction.setProperty(ActionDefinition.DependencyInfo.SERVICE_OUTPUT, mAfterServiceOutput);

			promptAction.getActions().add(dependencyAction);
		}

		return promptAction;
	}

	public List<String> getPromptExtraOutput()
	{
		return mAfterServiceOutput;
	}
}
