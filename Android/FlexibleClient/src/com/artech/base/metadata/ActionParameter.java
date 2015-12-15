package com.artech.base.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.expressions.Expression;
import com.artech.base.metadata.expressions.IAssignableExpression;
import com.artech.base.services.Services;

public class ActionParameter implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String mName;
	private final String mValue;
	private final Expression mExpression;
	private DataItem mValueDefinition;

	/**
	 * Creates a "named" parameter (unless name is null, in which case it's a "value" one).
	 */
	public ActionParameter(String name, String value, Expression expression)
	{
		mName = name;
		mValue = value;
		mExpression = expression;
	}

	/**
	 * Creates a "value" parameter.
	 */
	public ActionParameter(String value)
	{
		this(null, value, null);
	}

	public String getName() { return mName; }
	public void setName(String name) { mName = name; }

	public String getValue()
	{
		return Services.Resources.getExpressionTranslation(mValue);
	}

	public Expression getExpression()
	{
		return mExpression;
	}

	@Override
	public String toString()
	{
		if (Services.Strings.hasValue(mName))
			return String.format("%s = %s", mName, mValue); //$NON-NLS-1$
		else
			return mValue;
	}

	private static final String ATT_OR_VAR_REGEX = "&?[a-zA-Z](\\w|\\.)*";  //$NON-NLS-1$

	public boolean isAssignable()
	{
		// Attributes, variables, and sdt fields are assignable.
		return (mValue != null && mValue.matches(ATT_OR_VAR_REGEX)) ||
			   (mExpression != null && mExpression instanceof IAssignableExpression);
	}

	public static List<String> getValues(Iterable<ActionParameter> parameters)
	{
		ArrayList<String> values = new ArrayList<String>();

		if (parameters != null)
		{
			for (ActionParameter parameter : parameters)
				values.add(parameter.getValue());
		}

		return values;
	}

	public void setValueDefinition(DataItem definition)
	{
		mValueDefinition = definition;
	}

	public DataItem getValueDefinition()
	{
		return mValueDefinition;
	}
}
