package com.artech.base.metadata.rules;

import java.io.Serializable;

import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.serialization.INodeObject;

/**
 * Standard prompt rule; applies to an attribute/variable inside a form.
 * @author matiash
 */
public class PromptRuleDefinition extends AbstractPromptRuleDefinition implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	static final String RULE_NAME = "prompt"; //$NON-NLS-1$

	public PromptRuleDefinition(IDataViewDefinition parent, INodeObject jsonRule)
	{
		super(parent, jsonRule);
	}
}
