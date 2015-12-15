package com.artech.actions;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;

class AssignmentAction extends Action
{
	private final ActionParameter mAssignTarget;
	private final ActionParameter mAssignExpression;

	AssignmentAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
		mAssignTarget = ActionHelper.getAssignmentLeft(definition);
		mAssignExpression = ActionHelper.getAssignmentRight(definition);
	}

	public static boolean isAction(ActionDefinition definition)
	{
		return ActionHelper.hasProperties(definition, ActionHelper.ASSIGN_LEFT_VARIABLE, ActionHelper.ASSIGN_RIGHT_VALUE);
	}

	@Override
	public boolean Do()
	{
		// Evaluate expression and perform assignment.
		Object value = getParameterValue(mAssignExpression);
		setOutputValue(mAssignTarget, value);

		return true;
	}
}
