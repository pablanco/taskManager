package com.artech.actions;

import com.artech.android.layout.ControlHelper;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.utils.Strings;
import com.artech.common.ExecutionContext;
import com.artech.controls.IGxControl;

class GetControlPropertyAction extends Action
{
	private final ActionParameter mAssignTarget;
	private final String mControl;
	private final String mProperty;

	public GetControlPropertyAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
		mAssignTarget = ActionHelper.getAssignmentLeft(definition);
		mControl = definition.optStringProperty(ActionHelper.ASSIGN_CONTROL);
		mProperty = definition.optStringProperty(ActionHelper.ASSIGN_CONTROL_PROPERTY);
	}

	public static boolean isAction(ActionDefinition definition)
	{
		return ActionHelper.hasProperties(definition, ActionHelper.ASSIGN_LEFT_VARIABLE, ActionHelper.ASSIGN_CONTROL, ActionHelper.ASSIGN_CONTROL_PROPERTY);
	}

	@Override
	public boolean Do()
	{
		if (Strings.hasValue(mControl) && Strings.hasValue(mProperty) && mAssignTarget != null)
		{
			// Find the control to update properties.
			IGxControl control = findControl(mControl);
			if (control != null)
			{
				Object value = ControlHelper.getProperty(ExecutionContext.inAction(this), control, mProperty);
				setOutputValue(mAssignTarget, value);
			}
		}

		// Never fail. Ignore wrong control, property, or variable.
		return true;
	}
}
