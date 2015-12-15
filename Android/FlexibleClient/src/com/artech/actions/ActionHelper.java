package com.artech.actions;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.loader.WorkWithMetadataLoader;
import com.artech.base.serialization.INodeObject;
import com.artech.base.utils.Strings;
import com.artech.utils.Cast;

class ActionHelper
{
	static final String ASSIGN_LEFT_VARIABLE = "@assignVariable"; //$NON-NLS-1$
	static final String ASSIGN_LEFT_EXPRESSION = "assignExpression"; //$NON-NLS-1$
	static final String ASSIGN_RIGHT_VALUE = "@assignValue"; //$NON-NLS-1$
	static final String ASSIGN_RIGHT_EXPRESSION = "expression"; //$NON-NLS-1$
	static final String ASSIGN_CONTROL = "@assignControl"; //$NON-NLS-1$
	static final String ASSIGN_CONTROL_PROPERTY = "@assignProperty"; //$NON-NLS-1$

	/**
	 * Returns whether the action definition has values for all the specified properties.
	 * Useful to decide whether the action is of a particular type.
	 */
	static boolean hasProperties(ActionDefinition definition, String... properties)
	{
		for (String property : properties)
		{
			if (definition.getProperty(property) == null)
				return false;
		}

		return true;
	}

	static ActionParameter getAssignmentLeft(ActionDefinition action)
	{
		return newAssignmentParameter(action, ASSIGN_LEFT_VARIABLE, ASSIGN_LEFT_EXPRESSION);
	}

	static ActionParameter getAssignmentRight(ActionDefinition action)
	{
		return newAssignmentParameter(action, ASSIGN_RIGHT_VALUE, ASSIGN_RIGHT_EXPRESSION);
	}

	static ActionParameter newAssignmentParameter(ActionDefinition action, String valueKey, String expressionKey)
	{
		// Read expression (new format) or value (old format).
		String assignValue = action.optStringProperty(valueKey);
		INodeObject assignExpression = Cast.as(INodeObject.class, action.getProperty(expressionKey));

		return WorkWithMetadataLoader.newActionParameter(Strings.EMPTY, assignValue, assignExpression);
	}
}
