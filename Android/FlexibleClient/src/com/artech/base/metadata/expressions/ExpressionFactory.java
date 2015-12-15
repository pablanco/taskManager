package com.artech.base.metadata.expressions;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.expressions.Expression.Type;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class ExpressionFactory
{
	public static Expression parse(INodeObject node)
	{
		String exprType = node.getString("@exprType");

		if (ArithmeticExpression.TYPE.equalsIgnoreCase(exprType))
			return new ArithmeticExpression(node);

		if (AttributeExpression.TYPE.equalsIgnoreCase(exprType))
			return new AttributeExpression(node);

		if (BooleanExpression.TYPE.equalsIgnoreCase(exprType))
			return new BooleanExpression(node);

		if (ConstantBooleanExpression.TYPE.equalsIgnoreCase(exprType))
			return new ConstantBooleanExpression(node);

		if (ConstantDateExpression.TYPE.equalsIgnoreCase(exprType))
			return new ConstantDateExpression(node);

		if (ConstantNumberExpression.TYPE.equalsIgnoreCase(exprType))
			return new ConstantNumberExpression(node);

		if (ConstantStringExpression.TYPE.equalsIgnoreCase(exprType))
			return new ConstantStringExpression(node);

		if (ControlExpression.TYPE.equalsIgnoreCase(exprType))
			return new ControlExpression(node);

		if (FunctionExpression.TYPE.equalsIgnoreCase(exprType))
			return new FunctionExpression(node);

		if (KeywordExpression.TYPE.equalsIgnoreCase(exprType))
			return new KeywordExpression(node);

		if (MethodExpression.TYPE.equalsIgnoreCase(exprType))
			return new MethodExpression(node);

		if (PropertyExpression.TYPE.equalsIgnoreCase(exprType))
			return new PropertyExpression(node);

		if (VariableExpression.TYPE.equalsIgnoreCase(exprType))
			return new VariableExpression(node);

		throw new IllegalArgumentException(String.format("Unknown expression type: '%s'.", exprType));
	}

	static List<Expression> parseParameters(INodeObject node)
	{
		ArrayList<Expression> parameters = new ArrayList<Expression>();

		INodeObject parametersNode = node.optNode("parameters");
		if (parametersNode == null)
			parametersNode = node.optNode("Parameters"); // Difference in case between panel and dashboard.

		if (parametersNode != null)
		{
			for (INodeObject parameterNode : parametersNode.optCollection("parameter"))
				parameters.add(ExpressionFactory.parse(parameterNode.getNode("expression")));
		}

		return parameters;
	}

	static Type parseGxDataType(String dataType)
	{
		if (Strings.hasValue(dataType))
		{
			if (dataType.equalsIgnoreCase("Character") || dataType.equalsIgnoreCase("VarChar") || dataType.equalsIgnoreCase("LongVarChar") || dataType.equalsIgnoreCase("Blob"))
				return Type.STRING;
			else if (Strings.starsWithIgnoreCase(dataType, "Numeric"))
				return (dataType.endsWith(",0)") || dataType.endsWith(",0-)") ? Type.INTEGER : Type.DECIMAL);
			else if (dataType.equalsIgnoreCase("Boolean"))
				return Type.BOOLEAN;
			else if (dataType.equalsIgnoreCase("Date"))
				return Type.DATE;
			else if (dataType.equalsIgnoreCase("DateTime")) // TODO: Differentiate from Time. But how?
				return Type.DATETIME;
			else if (dataType.equalsIgnoreCase("GUID"))
				return Type.GUID;
			else if (dataType.equalsIgnoreCase("Image") || dataType.equalsIgnoreCase("Audio") || dataType.equalsIgnoreCase("Video"))
				return Type.STRING;
			else if (Strings.starsWithIgnoreCase(dataType, "BC/") || Strings.starsWithIgnoreCase(dataType, "SDT/"))
				return Type.ENTITY;
		}

		Services.Log.Error(String.format("Unknown expression data type: '%s'.", dataType));
		return Type.UNKNOWN;
	}
}
