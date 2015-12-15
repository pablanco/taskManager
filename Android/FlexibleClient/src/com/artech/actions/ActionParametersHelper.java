package com.artech.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.util.Pair;

import com.artech.base.metadata.ActionParameter;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public abstract class ActionParametersHelper
{
	private static Object getParameterValue(Entity entity, ActionParameter parameter)
	{
		return getParameterValue(entity, parameter.getValue());
	}

	static Object getParameterValue(Entity entity, String parameter)
	{
		if (!Services.Strings.hasValue(parameter))
			return null;

		parameter = parameter.trim(); // Workaround for bug in metadata generator, sometimes inserts line breaks at the end.

		// Try to evaluate as a constant (i.e. a string between quotes, number, &c).
		Pair<Boolean, String> asConstant = evaluateAsConstantExpression(parameter);
		if (asConstant.first)
			return asConstant.second;

		// Try to evaluate as an attribute/variable name.
		return getValueFromEntity(parameter, entity);
	}

	public static boolean isConstantExpression(String parameter)
	{
		return evaluateAsConstantExpression(parameter).first;
	}

	private static Pair<Boolean, String> evaluateAsConstantExpression(String parameter)
	{
		// String constant?
		// TODO: Error, this matches something like "hello" + "how are you?" which is not a constant
		// (but can't be evaluated by getValueFromEntities() anyway).
		String[] quoteSet = new String[] { "\"", "'" }; //$NON-NLS-1$ //$NON-NLS-2$
		for (String quote : quoteSet)
			if (parameter.startsWith(quote) && parameter.endsWith(quote))
				return new Pair<Boolean, String>(true, parameter.substring(1, parameter.length() - 1));

		// Numeric constant?
		if (parameter.matches("^(-)?[0-9.]+$")) //$NON-NLS-1$
			return new Pair<Boolean, String>(true, parameter);

		// Boolean constant?
		if (parameter.equalsIgnoreCase("true") || parameter.equalsIgnoreCase("false")) //$NON-NLS-1$ //$NON-NLS-2$
			return new Pair<Boolean, String>(true, parameter);

		// Special constants?
		if (parameter.equalsIgnoreCase("nowait") || parameter.equalsIgnoreCase("status") || parameter.equalsIgnoreCase("keep")) //$NON-NLS-1$ //$NON-NLS-2$
			return new Pair<Boolean, String>(true, parameter);

		// Not a constant value, apparently.
		return new Pair<Boolean, String>(false, parameter);
	}

	private static Object getValueFromEntity(String name, Entity entity)
	{
		if (entity != null)
		{
			Object value = entity.getProperty(name);
			if (value != null)
				return value;
		}

		return null;
	}

	public static Map<String, String> getParametersForBC(Action action)
	{
		TreeMap<String, String> values = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		for (ActionParameter parameter : action.getDefinition().getParameters())
		{
			if (Strings.hasValue(parameter.getName()))
			{
				Object oneValue = action.getParameterValue(parameter);
				values.put(parameter.getName(), (oneValue != null ? oneValue.toString() : Strings.EMPTY));
			}
		}

		return values;
	}

	public static List<String> getParametersForDataView(Action action)
	{
		ArrayList<String> values = new ArrayList<String>();
		for (ActionParameter parameter : action.getDefinition().getParameters())
		{
			Object oneValue = action.getParameterValue(parameter);
			values.add(oneValue != null ? oneValue.toString() : Strings.EMPTY);
		}

		return values;
	}

	public static List<String> getParametersForDataView(List<ActionParameter> parameters, Entity data)
	{
		ArrayList<String> values = new ArrayList<String>();
		for (ActionParameter parameter : parameters)
		{
			Object oneValue = getParameterValue(data, parameter);
			values.add(oneValue != null ? oneValue.toString() : Strings.EMPTY);
		}

		return values;
	}
}
