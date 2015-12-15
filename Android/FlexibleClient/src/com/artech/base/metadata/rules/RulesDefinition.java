package com.artech.base.metadata.rules;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.ListUtils;

public class RulesDefinition
{
	private static HashMap<String, Class<? extends RuleDefinition>> RULE_TYPES;

	static
	{
		RULE_TYPES = new HashMap<String, Class<? extends RuleDefinition>>();
		RULE_TYPES.put(PromptRuleDefinition.RULE_NAME, PromptRuleDefinition.class);
		RULE_TYPES.put(FilterPromptRuleDefinition.RULE_NAME, FilterPromptRuleDefinition.class);
	}

	private final IDataViewDefinition mParent;
	private final ArrayList<Object> mRules;

	public RulesDefinition(IDataViewDefinition parent)
	{
		mParent = parent;
		mRules = new ArrayList<Object>();
	}

	public void deserialize(INodeObject jsonRules)
	{
		if (jsonRules == null)
			return;

		for (String ruleName : jsonRules.names())
		{
			Class<? extends RuleDefinition> ruleType = RULE_TYPES.get(ruleName);
			if (ruleType != null)
			{
				for (INodeObject jsonRule : jsonRules.optCollection(ruleName))
				{
					try
					{
						Constructor<? extends RuleDefinition> constructor = ruleType.getConstructor(IDataViewDefinition.class, INodeObject.class);
						RuleDefinition rule = constructor.newInstance(mParent, jsonRule);
						mRules.add(rule);
					}
					catch (Exception ex)
					{
						Services.Log.Error(String.format("Error deserializing rule '%s'.", jsonRule), ex);
					}
				}
			}
			else
				Services.Log.warning(String.format("Unknown rule type: '%s'.", ruleName));
		}
	}

	public <TRuleType extends RuleDefinition> List<TRuleType> getRules(Class<TRuleType> type)
	{
		return ListUtils.itemsOfType(mRules, type);
	}
}
