package com.artech.base.metadata.expressions;

import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

class ConstantStringExpression extends ConstantExpression
{
	static final String TYPE = "string";

	public ConstantStringExpression(INodeObject node)
	{
		super(node);
	}

	@Override
	public Value eval(IExpressionContext context)
	{
		String str = Services.Resources.getExpressionTranslation(getConstant());

		// Remove the quotes to get the value.
		String[] quoteSet = new String[] { "\"", "'" }; //$NON-NLS-1$ //$NON-NLS-2$
		for (String quote : quoteSet)
		{
			if (str.startsWith(quote) && str.endsWith(quote))
			{
				str = str.substring(1, str.length() - 1);
				break;
			}
		}

		return Value.newString(str);
	}
}
