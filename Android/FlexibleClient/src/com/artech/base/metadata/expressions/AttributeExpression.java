package com.artech.base.metadata.expressions;

import com.artech.base.serialization.INodeObject;

class AttributeExpression extends ValueExpression
{
	static final String TYPE = "attribute";

	public AttributeExpression(INodeObject node)
	{
		super(node);
	}
}
