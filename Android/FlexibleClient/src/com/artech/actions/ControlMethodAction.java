package com.artech.actions;

import com.artech.android.layout.ControlHelper;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.services.Services;
import com.artech.common.ExecutionContext;
import com.artech.controls.IGxControl;

public class ControlMethodAction extends Action
{
	private final String mControl;
	private final String mMethod;

	protected ControlMethodAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);

		mControl = definition.optStringProperty("@executeControl"); //$NON-NLS-1$
		mMethod = definition.optStringProperty("@executeMethod"); //$NON-NLS-1$
	}

	public static boolean isAction(ActionDefinition definition)
	{
		return (definition.getProperty("@executeControl") != null); //$NON-NLS-1$
	}

	@Override
	public boolean Do()
	{
		if (Services.Strings.hasValue(mControl) &&
			Services.Strings.hasValue(mMethod))
		{
			// Find the control to run method.
			IGxControl control = findControl(mControl);
			if (control != null)
				ControlHelper.runMethod(ExecutionContext.inAction(this), control, mMethod, getParameterValues());
		}

		// Never fail. Ignore wrong control or method.
		return true;
	}
}
