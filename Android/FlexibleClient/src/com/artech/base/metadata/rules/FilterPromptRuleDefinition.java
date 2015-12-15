package com.artech.base.metadata.rules;

import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.serialization.INodeObject;

/**
 * Prompt rule that applies to a filter attribute (not a control on a "real" form).
 * @author matiash
 */
public class FilterPromptRuleDefinition extends AbstractPromptRuleDefinition
{
	private static final long serialVersionUID = 1L;
	
	static final String RULE_NAME = "filterPrompt"; //$NON-NLS-1$

	public FilterPromptRuleDefinition(IDataViewDefinition parent, INodeObject jsonRule)
	{
		super(parent, jsonRule);
	}

	@Override
	public String toString()
	{
		return "[Filter Prompt] " + super.toString();
	}
}
