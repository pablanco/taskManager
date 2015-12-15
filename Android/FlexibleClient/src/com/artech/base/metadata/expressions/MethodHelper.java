package com.artech.base.metadata.expressions;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.expressions.Expression.Type;
import com.artech.base.metadata.expressions.Expression.Value;
import com.artech.base.utils.DoubleMap;

class MethodHelper
{
	private static DoubleMap<String, String, Method> sMethods;

	public static Value call(Value target, String methodName, List<Value> parameters)
	{
		initMethodHelper();

		Method method = getMethod(target.getType(), methodName);
		if (method == null)
			throw new IllegalArgumentException(String.format("Unknown method %s.%s()/%s.", target.getType(), methodName, parameters.size()));

		return method.run(target, parameters);
	}

	private static Method getMethod(Type targetType, String name)
	{
		return sMethods.get(targetType.toString(), name);
	}

	private static void initMethodHelper()
	{
		if (sMethods == null)
		{
			sMethods = DoubleMap.newStringMap();
			registerMethods();
		}
	}

	private static void registerMethods()
	{
		// BOOLEAN methods.
		registerMethod(new Method(Type.BOOLEAN, "IsEmpty", "BOOLEAN::IsEmpty"));
		registerMethod(new Method(Type.BOOLEAN, "ToString", "BOOLEAN::ToString"));

		// DATE/DATETIME methods.
		registerMethod(new Method(Type.DATETIME, "AddDays", "DATETIME::AddDays"));
		registerMethod(new Method(Type.DATETIME, "AddHours", "DATETIME::AddHours"));
		registerMethod(new Method(Type.DATETIME, "AddMinutes", "DATETIME::AddMinutes"));
		registerMethod(new Method(Type.DATETIME, "AddMonths", "AddMth"));
		registerMethod(new Method(Type.DATETIME, "AddSeconds", "TAdd"));
		registerMethod(new Method(Type.DATETIME, "AddYears", "AddYr"));
		registerMethod(new Method(Type.DATETIME, "Age", "Age"));
		registerMethod(new Method(Type.DATETIME, "Day", "Day"));
		registerMethod(new Method(Type.DATETIME, "DayOfWeek", "DoW"));
		registerMethod(new Method(Type.DATETIME, "DayOfWeekName", "CDoW"));
		registerMethod(new Method(Type.DATETIME, "Difference", "TDiff"));
		registerMethod(new Method(Type.DATETIME, "EndOfMonth", "EoM"));
		registerMethod(new Method(Type.DATETIME, "Hour", "Hour"));
		registerMethod(new Method(Type.DATETIME, "IsEmpty", "DATETIME::IsEmpty"));
		registerMethod(new Method(Type.DATETIME, "Minute", "Minute"));
		registerMethod(new Method(Type.DATETIME, "Month", "Month"));
		registerMethod(new Method(Type.DATETIME, "MonthName", "CMonth"));
		registerMethod(new Method(Type.DATETIME, "Second", "Second"));
		registerMethod(new Method(Type.DATETIME, "ToDate", "DATETIME::ToDate"));
		registerMethod(new Method(Type.DATETIME, "ToString", "TtoC"));
		registerMethod(new Method(Type.DATETIME, "ToUniversalTime", "DATETIME::ToUniversalTime"));
		registerMethod(new Method(Type.DATETIME, "Year", "Year"));

		// DECIMAL/INTEGER methods.
		registerMethod(new Method(Type.DECIMAL, "Integer", "Int"));
		registerMethod(new Method(Type.DECIMAL, "IsEmpty", "DECIMAL::IsEmpty"));
		registerMethod(new Method(Type.DECIMAL, "Round", "Round"));
		registerMethod(new Method(Type.DECIMAL, "RoundToEven", "RoundToEven"));
		registerMethod(new Method(Type.DECIMAL, "ToString", "Str"));
		registerMethod(new Method(Type.DECIMAL, "Truncate", "Trunc"));

		// GUID methods.
		registerMethod(new Method(Type.GUID, "IsEmpty", "GUID::IsEmpty"));
		registerMethod(new Method(Type.GUID, "ToString", "GUID::ToString"));

		// INTEGER methods.
		registerMethod(new Method(Type.INTEGER, "IsEmpty", "INTEGER::IsEmpty"));

		// STRING methods.
		registerMethod(new Method(Type.STRING, "CharAt", "STRING::CharAt"));
		registerMethod(new Method(Type.STRING, "Contains", "STRING::Contains"));
		registerMethod(new Method(Type.STRING, "EndsWith", "STRING::EndsWith"));
		registerMethod(new Method(Type.STRING, "IndexOf", "StrSearch"));
		registerMethod(new Method(Type.STRING, "IsEmpty", "STRING::IsEmpty"));
		registerMethod(new Method(Type.STRING, "IsMatch", "STRING::IsMatch"));
		registerMethod(new Method(Type.STRING, "LastIndexOf", "StrSearchRev"));
		registerMethod(new Method(Type.STRING, "Length", "Len"));
		registerMethod(new Method(Type.STRING, "PadLeft", "PadL"));
		registerMethod(new Method(Type.STRING, "PadRight", "PadR"));
		registerMethod(new Method(Type.STRING, "Replace", "StrReplace"));
		registerMethod(new Method(Type.STRING, "ReplaceRegEx", "STRING::ReplaceRegEx"));
		registerMethod(new Method(Type.STRING, "StartsWith", "STRING::StartsWith"));
		registerMethod(new Method(Type.STRING, "Substring", "SubStr"));
		registerMethod(new Method(Type.STRING, "ToLower", "Lower"));
		registerMethod(new Method(Type.STRING, "ToNumeric", "Val"));
		registerMethod(new Method(Type.STRING, "ToString", "STRING::ToString"));
		registerMethod(new Method(Type.STRING, "ToUpper", "Upper"));
		registerMethod(new Method(Type.STRING, "Trim", "Trim"));
		registerMethod(new Method(Type.STRING, "TrimEnd", "RTrim"));
		registerMethod(new Method(Type.STRING, "TrimStart", "LTrim"));
	}

	private static void registerMethod(Method method)
	{
		for (Type type : ExpressionHelper.getCompatibleTypesForType(method.mTargetType))
			sMethods.put(type.toString(), method.mMethodName, method);
	}

	private static class Method
	{
		private final Type mTargetType;
		private final String mMethodName;

		private final String mMappedFunction;

		private Method(Type targetType, String methodName, String mappedFunction)
		{
			mTargetType = targetType;
			mMethodName = methodName;
			mMappedFunction = mappedFunction;
		}

		public Value run(Value target, List<Value> parameters)
		{
			List<Value> functionParameters = new ArrayList<Value>();
			functionParameters.add(target);
			functionParameters.addAll(parameters);

			// Delegate to the corresponding function.
			return FunctionHelper.call(mMappedFunction, functionParameters);
		}
	}
}
