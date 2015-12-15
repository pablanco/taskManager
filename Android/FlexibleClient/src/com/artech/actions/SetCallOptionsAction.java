package com.artech.actions;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.services.Services;
import com.artech.ui.navigation.CallOptionsHelper;

public class SetCallOptionsAction extends Action
{
	private final String mTargetObject;
	private final String mOption;
	private final String mValue;

	public SetCallOptionsAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);

		mTargetObject = MetadataLoader.getObjectName(definition.optStringProperty("@optionTarget")); //$NON-NLS-1$
		mOption = definition.optStringProperty("@optionName"); //$NON-NLS-1$
		String value = definition.optStringProperty("@optionValue"); //$NON-NLS-1$
		mValue = Services.Resources.getExpressionTranslation(value);
	}

	public static boolean isAction(ActionDefinition definition)
	{
		return (definition.getProperty("@optionTarget") != null); //$NON-NLS-1$
	}

	@Override
	public boolean Do()
	{
		Object optionValue = getParameterValue(new ActionParameter(mValue));
		if (optionValue == null)
			return false;

		CallOptionsHelper.setCallOption(mTargetObject, mOption, optionValue.toString());
		return true; // Never fail, ignore wrong options.
	}
}
