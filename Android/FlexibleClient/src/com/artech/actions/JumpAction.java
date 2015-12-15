package com.artech.actions;

import com.artech.android.api.InteropAPI;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.enums.GxObjectTypes;
import com.artech.base.services.Services;

public class JumpAction extends Action
{
	private final Integer mJumpLength;
	private final ActionParameter mIgnoreCondition;

	private static String METHOD_JUMP_IF_NOT = "JumpIfNot";
	private static String METHOD_JUMP = "Jump";

	protected JumpAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
		String methodName = definition.optStringProperty("@exoMethod");

		if (methodName.equalsIgnoreCase(METHOD_JUMP_IF_NOT) && definition.getParameters().size() >= 2)
		{
			mIgnoreCondition = definition.getParameter(0);
			mJumpLength = Services.Strings.tryParseInt(definition.getParameter(1).getValue());
		}
		else if (methodName.equalsIgnoreCase(METHOD_JUMP) && definition.getParameters().size() >= 1)
		{
			mIgnoreCondition = null;
			mJumpLength = Services.Strings.tryParseInt(definition.getParameter(0).getValue());
		}
		else
		{
			Services.Log.warning("Invalid JumpAction definition");
			mIgnoreCondition = null;
			mJumpLength = null;
		}
	}

	public static boolean isAction(ActionDefinition definition)
	{
		String objectName = definition.getGxObject();
		String methodName = definition.optStringProperty("@exoMethod");

		return (objectName != null && methodName != null &&
				definition.getGxObjectType() == GxObjectTypes.API &&
				objectName.equalsIgnoreCase(InteropAPI.OBJECT_NAME) &&
				(methodName.equalsIgnoreCase(METHOD_JUMP_IF_NOT) || methodName.equalsIgnoreCase(METHOD_JUMP)));
	}

	@Override
	public boolean Do()
	{
		if (mJumpLength != null)
		{
			// Check for condition
			boolean doNotJump = false;
			if (mIgnoreCondition != null)
			{
				Object eval = getParameterValue(mIgnoreCondition);
				Boolean evalIgnore = Services.Strings.tryParseBoolean(String.valueOf(eval));
				if (evalIgnore != null)
					doNotJump = evalIgnore;
			}

			if (!doNotJump)
				ActionExecution.movePendingActions(mJumpLength);
		}

		// Always succeeds.
		return true;
	}
}
