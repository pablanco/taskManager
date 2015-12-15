package com.artech.base.metadata.layout;

import java.io.Serializable;
import java.util.HashMap;

import android.util.SparseArray;

import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.metadata.enums.LayoutItemsTypes;
import com.artech.base.metadata.rules.PromptRuleDefinition;
import com.artech.base.utils.ListUtils;
import com.artech.base.utils.Strings;

class LayoutPromptsDefinition implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final LayoutDefinition mLayout;

	// Since prompts are different based on layout and BC mode, calculate different values for each.
	private final  SparseArray<HashMap<LayoutItemDefinition, PromptRuleDefinition>> mPrompts;

	LayoutPromptsDefinition(LayoutDefinition layout)
	{
		mLayout = layout;
		mPrompts = new SparseArray<HashMap<LayoutItemDefinition, PromptRuleDefinition>>();
	}

	public PromptRuleDefinition getPromptOn(LayoutItemDefinition layoutItem, short layoutMode, short trnMode)
	{
		if (trnMode == DisplayModes.DELETE)
			return null; // Don't use ANY prompts in delete screen.

		HashMap<LayoutItemDefinition, PromptRuleDefinition> prompts = processPromptDefinitions(layoutMode, trnMode);
		return prompts.get(layoutItem);
	}

	public boolean hasPrompt(LayoutItemDefinition layoutItem, short layoutMode, short trnMode)
	{
		return (getPromptOn(layoutItem, layoutMode, trnMode) != null);
	}

	private HashMap<LayoutItemDefinition, PromptRuleDefinition> processPromptDefinitions(short layoutMode, short trnMode)
	{
		Integer code = getCode(layoutMode, trnMode);
		HashMap<LayoutItemDefinition, PromptRuleDefinition> layoutPrompts = mPrompts.get(code);

		if (layoutPrompts == null)
		{
			layoutPrompts = new HashMap<LayoutItemDefinition, PromptRuleDefinition>();

			// First process prompt rules.
			for (PromptRuleDefinition promptRule : mLayout.getParent().getRules().getRules(PromptRuleDefinition.class))
			{
				LayoutItemDefinition promptItem = getPromptControl(promptRule, layoutMode, trnMode);
				if (promptItem != null && !layoutPrompts.containsKey(promptItem))
					layoutPrompts.put(promptItem, promptRule);
			}

			mPrompts.put(code, layoutPrompts);
		}

		return layoutPrompts;
	}

	private LayoutItemDefinition getPromptControl(PromptRuleDefinition rule, short layoutMode, short trnMode)
	{
		// Cases:
		// 1) Control not specified -> show in prompt attribute present in form (if any).
		if (!Strings.hasValue(rule.getControlName()))
		{
			for (ActionParameter parameter : ListUtils.inReverse(rule.getParameters()))
			{
				if (parameter.isAssignable() && rule.isOutputParameter(parameter))
				{
					LayoutItemDefinition dataItem = mLayout.getDataControl(parameter.getValue());
					if (dataItem != null && !dataItem.getReadOnly(layoutMode, trnMode))
						return dataItem;
				}
			}

			return null;
		}

		// 2) Control specified (normally a button or attribute) -> displayed there, unless it's a read-only control.
		// In Edit if readonly show anyway, that allow prompt in Description Atts.
		LayoutItemDefinition onControl = mLayout.getControl(rule.getControlName());
		if (onControl != null && LayoutItemsTypes.Data.equalsIgnoreCase(onControl.getType()) && onControl.getReadOnly(layoutMode, trnMode)
				&& trnMode==DisplayModes.VIEW)
			onControl = null;

		return onControl;
	}

	private static Integer getCode(short layoutMode, short trnMode)
	{
		return Integer.valueOf(layoutMode << 16 + trnMode);
	}
}
