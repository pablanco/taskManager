package com.artech.common;

import android.view.View;

import com.artech.actions.UIContext;
import com.artech.activities.ActivityLauncher;
import com.artech.activities.IntentParameters;
import com.artech.android.layout.LayoutTag;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.AttributeDefinition;
import com.artech.base.metadata.RelationDefinition;
import com.artech.base.metadata.WorkWithDefinition;
import com.artech.base.metadata.rules.PromptRuleDefinition;
import com.artech.base.services.Services;
import com.artech.ui.Anchor;
import com.artech.ui.Coordinator;

/**
 * Used whenever we need to call a List to select a single entity.
 *
 */
public class PromptHelper
{
	private static final int REQUEST_CODE = IntentParameters.REQUEST_CODE_PROMPT;

	public static boolean callPrompt(Coordinator coordinator, View control)
	{
		PromptRuleDefinition prompt = (PromptRuleDefinition)control.getTag(LayoutTag.CONTROL_PROMPT_INFO);
		if (prompt == null)
			return false;

		// Execute the prompt as an action.
		ActionDefinition promptAction = prompt.getPromptAction();
		return coordinator.runAction(promptAction, new Anchor(control));
	}

	public static boolean hasPrompt(View control)
	{
		return (control.getTag(LayoutTag.CONTROL_PROMPT_INFO) != null);
	}

	public static void setAssociatedPrompt(View control, PromptRuleDefinition prompt)
	{
		control.setTag(LayoutTag.CONTROL_PROMPT_INFO, prompt);
	}

	/** From here on it's all compatibility stuff... will be deleted soon. */

	public static boolean callPrompt(UIContext caller, RelationDefinition relation)
	{
		if (relation != null)
		{
			// Find prompt for this BC.
			WorkWithDefinition wwDef = Services.Application.getWorkWithForBC(relation.getBCRelated());
			if (wwDef != null)
			{
				if (wwDef.getLevel(0).getList() != null)
				{
					ActivityLauncher.callForResult(caller, wwDef.getLevel(0).getList(), null, REQUEST_CODE, true);
					return true;
				}
			}
		}

		return false;
	}

	public static String calculateAttName(String att, RelationDefinition relation)
	{
		// Search in Keys
		for (int i = 0; i < relation.getKeys().size() ; i++) {
			String attRef = relation.getKeys().get(i);
			AttributeDefinition attDef = Services.Application.getAttribute(attRef);
			if (attRef.equals(att))
				return att;
			if (attDef.getSupertype() != null && attDef.getSupertype().equals(att))
				return attRef;
		}
		// Search on inferred
		for (int i = 0; i < relation.getInferredAtts().size() ; i++) {
			String attRef = relation.getInferredAtts().get(i);
			if (attRef.equals(att))
				return att;
			AttributeDefinition attDef = Services.Application.getAttribute(attRef);
			if (attDef.getSupertype() != null && attDef.getSupertype().equals(att))
				return attRef;
		}

		//default
		return att;
	}
}
