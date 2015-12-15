package com.artech.base.metadata.expressions;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.artech.base.metadata.expressions.Expression.Type;
import com.artech.base.metadata.expressions.Expression.Value;
import com.artech.base.utils.MultiMap;
import com.artech.base.utils.Strings;
import com.genexus.GXutil;
import com.genexus.GxRegex;
import com.genexus.LocalUtil;
import com.genexus.util.Encryption;

class FunctionHelper
{
	private static MultiMap<String, Function> sFunctions;
	private static HashMap<Class<?>, Object> sImplementationClasses;

	public static Value call(String functionName, List<Value> parameters)
	{
		initFunctionHelper();

		Function function = getFunction(functionName, parameters.size());
		if (function == null)
			throw new IllegalArgumentException(String.format("Unknown function %s/%s.", functionName, parameters.size()));

		return function.run(parameters);
	}

	private static Function getFunction(String name, int parameterCount)
	{
		List<Function> overloads = sFunctions.get(Strings.toLowerCase(name));
		if (overloads.size() >= 1)
		{
			// Filter by number of arguments.
			for (Function func : overloads)
			{
				if (func.mParameterTypes.size() == parameterCount)
					return func;
			}
		}

		// Function not found.
		return null;
	}

	private static void initFunctionHelper()
	{
		if (sFunctions == null)
		{
			// Register information about known functions.
			sFunctions = new MultiMap<String, FunctionHelper.Function>();
			registerFunctions();

			// Create implementors for non-static methods.
			sImplementationClasses = new HashMap<Class<?>, Object>();
			sImplementationClasses.put(LocalUtil.class, GXutilPlus.getLocalUtil());
		}
	}

	private static void registerFunctions()
	{
		// Functions.
		registerFunction(new Function("AddMth", Type.DATE, new Type[] { Type.DATE, Type.INTEGER }, GXutil.class, "addmth"));
		registerFunction(new Function("AddYr", Type.DATE, new Type[] { Type.DATE, Type.INTEGER }, GXutil.class, "addyr"));
		registerFunction(new Function("Age", Type.INTEGER, new Type[] { Type.DATE }, GXutil.class, "age"));
		registerFunction(new Function("Age", Type.INTEGER, new Type[] { Type.DATE, Type.DATE }, GXutil.class, "age"));
		registerFunction(new Function("Asc", Type.INTEGER, new Type[] { Type.STRING }, GXutil.class, "asc"));
		registerFunction(new Function("CDoW", Type.STRING, new Type[] { Type.DATE, Type.STRING }, LocalUtil.class, "cdow"));
		registerFunction(new Function("CDoW", Type.STRING, new Type[] { Type.DATE }, LocalUtil.class, "cdow"));
		registerFunction(new Function("Chr", Type.STRING, new Type[] { Type.INTEGER }, GXutil.class, "chr"));
		registerFunction(new Function("CMonth", Type.STRING, new Type[] { Type.DATE, Type.STRING }, LocalUtil.class, "cmonth"));
		registerFunction(new Function("CMonth", Type.STRING, new Type[] { Type.DATE }, LocalUtil.class, "cmonth"));
		registerFunction(new Function("Concat", Type.STRING, new Type[] { Type.STRING, Type.STRING }, GXutil.class, "concat"));
		registerFunction(new Function("Concat", Type.STRING, new Type[] { Type.STRING, Type.STRING, Type.STRING }, GXutil.class, "concat"));
		registerFunction(new Function("CtoD", Type.DATE, new Type[] { Type.STRING }, LocalUtil.class, "ctod"));
		registerFunction(new Function("CtoT", Type.DATETIME, new Type[] { Type.STRING }, LocalUtil.class, "ctot"));
		registerFunction(new Function("Day", Type.INTEGER, new Type[] { Type.DATE }, GXutil.class, "day"));
		registerFunction(new Function("Decrypt64", Type.STRING, new Type[] { Type.STRING, Type.STRING }, Encryption.class, "decrypt64"));
		registerFunction(new Function("DoW", Type.INTEGER, new Type[] { Type.DATE }, GXutil.class, "dow"));
		registerFunction(new Function("DtoC", Type.STRING, new Type[] { Type.DATE }, GXutilPlus.class, "dtoc_1"));
		registerFunction(new Function("Encrypt64", Type.STRING, new Type[] { Type.STRING, Type.STRING }, Encryption.class, "encrypt64"));
		registerFunction(new Function("EoM", Type.DATETIME, new Type[] { Type.DATETIME }, GXutil.class, "eom"));
		registerFunction(new Function("EoM", Type.DATE, new Type[] { Type.DATE }, GXutil.class, "eomdate"));
		registerFunction(new Function("GetEncryptionKey", Type.STRING, new Type[] { }, Encryption.class, "getNewKey"));
		registerFunction(new Function("Hour", Type.INTEGER, new Type[] { Type.DATETIME }, GXutil.class, "hour"));
		registerFunction(new Function("Int", Type.INTEGER, new Type[] { Type.DECIMAL }, GXutilPlus.class, "decimalToInteger"));
		registerFunction(new Function("Len", Type.INTEGER, new Type[] { Type.STRING }, GXutil.class, "len"));
		registerFunction(new Function("Lower", Type.STRING, new Type[] { Type.STRING }, GXutil.class, "lower"));
		registerFunction(new Function("LTrim", Type.STRING, new Type[] { Type.STRING }, GXutil.class, "ltrim"));
		registerFunction(new Function("Minute", Type.INTEGER, new Type[] { Type.DATETIME }, GXutil.class, "minute"));
		registerFunction(new Function("Month", Type.INTEGER, new Type[] { Type.DATE }, GXutil.class, "month"));
		registerFunction(new Function("NewLine", Type.STRING, new Type[] { }, GXutil.class, "newLine"));
		registerFunction(new Function("Now", Type.DATETIME, new Type[] { }, GXutil.class, "now"));
		registerFunction(new Function("PadL", Type.STRING, new Type[] { Type.STRING, Type.INTEGER, Type.STRING }, GXutil.class, "padl"));
		registerFunction(new Function("PadL", Type.STRING, new Type[] { Type.STRING, Type.INTEGER }, GXutilPlus.class, "padl_2"));
		registerFunction(new Function("PadR", Type.STRING, new Type[] { Type.STRING, Type.INTEGER, Type.STRING }, GXutil.class, "padr"));
		registerFunction(new Function("PadR", Type.STRING, new Type[] { Type.STRING, Type.INTEGER }, GXutilPlus.class, "padr_2"));
		registerFunction(new Function("Random", Type.DECIMAL, new Type[] { }, GXutil.class, "random"));
		registerFunction(new Function("Round", Type.DECIMAL, new Type[] { Type.DECIMAL, Type.INTEGER }, GXutil.class, "roundDecimal"));
		registerFunction(new Function("RoundToEven", Type.DECIMAL, new Type[] { Type.DECIMAL, Type.INTEGER }, GXutil.class, "roundToEven"));
		registerFunction(new Function("RTrim", Type.STRING, new Type[] { Type.STRING }, GXutil.class, "rtrim"));
		registerFunction(new Function("Second", Type.INTEGER, new Type[] { Type.DATETIME }, GXutil.class, "second"));
		registerFunction(new Function("ServerDate", Type.DATE, new Type[] { }, GXutil.class, "today"));
		registerFunction(new Function("ServerNow", Type.DATETIME, new Type[] { }, GXutil.class, "now"));
		registerFunction(new Function("ServerTime", Type.STRING, new Type[] { }, GXutil.class, "time"));
		registerFunction(new Function("Str", Type.STRING, new Type[] { Type.DECIMAL, Type.INTEGER, Type.INTEGER }, GXutil.class, "str"));
		registerFunction(new Function("Str", Type.STRING, new Type[] { Type.DECIMAL, Type.INTEGER }, GXutilPlus.class, "str_2"));
		registerFunction(new Function("Str", Type.STRING, new Type[] { Type.DECIMAL }, GXutilPlus.class, "str_1"));
		registerFunction(new Function("StrReplace", Type.STRING, new Type[] { Type.STRING, Type.STRING, Type.STRING }, GXutil.class, "strReplace"));
		registerFunction(new Function("StrSearch", Type.INTEGER, new Type[] { Type.STRING, Type.STRING, Type.INTEGER }, GXutil.class, "strSearch"));
		registerFunction(new Function("StrSearch", Type.INTEGER, new Type[] { Type.STRING, Type.STRING }, GXutilPlus.class, "strSearch_2"));
		registerFunction(new Function("StrSearchRev", Type.INTEGER, new Type[] { Type.STRING, Type.STRING, Type.INTEGER }, GXutil.class, "strSearchRev"));
		registerFunction(new Function("StrSearchRev", Type.INTEGER, new Type[] { Type.STRING, Type.STRING }, GXutilPlus.class, "strSearchRev_2"));
		registerFunction(new Function("SubStr", Type.STRING, new Type[] { Type.STRING, Type.INTEGER, Type.INTEGER }, GXutil.class, "substring"));
		registerFunction(new Function("SubStr", Type.STRING, new Type[] { Type.STRING, Type.INTEGER }, GXutilPlus.class, "substring_2"));
		registerFunction(new Function("SysDate", Type.DATE, new Type[] { }, GXutil.class, "today"));
		registerFunction(new Function("SysTime", Type.STRING, new Type[] { }, GXutil.class, "time"));
		registerFunction(new Function("TAdd", Type.DATETIME, new Type[] { Type.DATETIME, Type.INTEGER }, GXutil.class, "dtadd"));
		registerFunction(new Function("TDiff", Type.INTEGER, new Type[] { Type.DATETIME, Type.DATETIME }, GXutil.class, "dtdiff"));
		registerFunction(new Function("Time", Type.STRING, new Type[] { }, GXutil.class, "time"));
		registerFunction(new Function("Today", Type.DATE, new Type[] { }, GXutil.class, "today"));
		registerFunction(new Function("Trim", Type.STRING, new Type[] { Type.STRING }, GXutil.class, "trim"));
		registerFunction(new Function("Trunc", Type.DECIMAL, new Type[] { Type.DECIMAL, Type.INTEGER }, GXutil.class, "truncDecimal"));
		registerFunction(new Function("TtoC", Type.STRING, new Type[] { Type.DATETIME }, GXutilPlus.class, "ttoc_1"));
		registerFunction(new Function("TtoC", Type.STRING, new Type[] { Type.DATETIME, Type.INTEGER }, GXutilPlus.class, "ttoc_2"));
		registerFunction(new Function("TtoC", Type.STRING, new Type[] { Type.DATETIME, Type.INTEGER, Type.INTEGER }, GXutilPlus.class, "ttoc_3"));
		registerFunction(new Function("Upper", Type.STRING, new Type[] { Type.STRING }, GXutil.class, "upper"));
		registerFunction(new Function("Val", Type.DECIMAL, new Type[] { Type.STRING, Type.STRING }, GXutil.class, "val"));
		registerFunction(new Function("Val", Type.DECIMAL, new Type[] { Type.STRING }, GXutil.class, "val"));
		registerFunction(new Function("Year", Type.INTEGER, new Type[] { Type.DATE }, GXutil.class, "year"));
		registerFunction(new Function("YMDHMStoT", Type.DATETIME, new Type[] { Type.INTEGER, Type.INTEGER, Type.INTEGER, Type.INTEGER, Type.INTEGER, Type.INTEGER }, LocalUtil.class, "ymdhmsToT"));
		registerFunction(new Function("YMDHMStoT", Type.DATETIME, new Type[] { Type.INTEGER, Type.INTEGER, Type.INTEGER, Type.INTEGER, Type.INTEGER }, LocalUtil.class, "ymdhmsToT"));
		registerFunction(new Function("YMDHMStoT", Type.DATETIME, new Type[] { Type.INTEGER, Type.INTEGER, Type.INTEGER, Type.INTEGER }, LocalUtil.class, "ymdhmsToT"));
		registerFunction(new Function("YMDHMStoT", Type.DATETIME, new Type[] { Type.INTEGER, Type.INTEGER, Type.INTEGER }, LocalUtil.class, "ymdhmsToT"));
		registerFunction(new Function("YMDtoD", Type.DATE, new Type[] { Type.INTEGER, Type.INTEGER, Type.INTEGER }, LocalUtil.class, "ymdtod"));

		// Methods mapped to functions, but not functions by themselves.
		registerFunction(new Function("BOOLEAN::IsEmpty", Type.BOOLEAN, new Type[] { Type.BOOLEAN }, GXutilPlus.class, "isBooleanEmpty"));
		registerFunction(new Function("BOOLEAN::ToString", Type.STRING, new Type[] { Type.BOOLEAN }, GXutil.class, "booltostr"));
		registerFunction(new Function("DATETIME::AddDays", Type.DATETIME, new Type[] { Type.DATETIME, Type.INTEGER }, GXutilPlus.class, "addDays"));
		registerFunction(new Function("DATETIME::AddHours", Type.DATETIME, new Type[] { Type.DATETIME, Type.INTEGER }, GXutilPlus.class, "addHours"));
		registerFunction(new Function("DATETIME::AddMinutes", Type.DATETIME, new Type[] { Type.DATETIME, Type.INTEGER }, GXutilPlus.class, "addMinutes"));
		registerFunction(new Function("DATETIME::IsEmpty", Type.BOOLEAN, new Type[] { Type.DATETIME }, GXutilPlus.class, "isDateEmpty"));
		registerFunction(new Function("DATETIME::ToDate", Type.DATETIME, new Type[] { Type.DATETIME }, GXutil.class, "resetTime"));
		registerFunction(new Function("DATETIME::ToUniversalTime", Type.DATETIME, new Type[] { Type.DATETIME }, GXutil.class, "DateTimeToUTC"));
		registerFunction(new Function("DECIMAL::IsEmpty", Type.BOOLEAN, new Type[] { Type.DECIMAL }, GXutilPlus.class, "isNumberEmpty"));
		registerFunction(new Function("GUID::IsEmpty", Type.BOOLEAN, new Type[] { Type.GUID }, GXutilPlus.class, "isGuidEmpty"));
		registerFunction(new Function("GUID::ToString", Type.STRING, new Type[] { Type.GUID }, GXutilPlus.class, "guidToString"));
		registerFunction(new Function("INTEGER::IsEmpty", Type.BOOLEAN, new Type[] { Type.INTEGER }, GXutilPlus.class, "isNumberEmpty"));
		registerFunction(new Function("STRING::CharAt", Type.STRING, new Type[] { Type.STRING, Type.INTEGER }, GXutil.class, "charAt"));
		registerFunction(new Function("STRING::Contains", Type.BOOLEAN, new Type[] { Type.STRING, Type.STRING }, GXutil.class, "contains"));
		registerFunction(new Function("STRING::EndsWith", Type.BOOLEAN, new Type[] { Type.STRING, Type.STRING }, GXutil.class, "endsWith"));
		registerFunction(new Function("STRING::IsEmpty", Type.BOOLEAN, new Type[] { Type.STRING }, GXutilPlus.class, "isStringEmpty"));
		registerFunction(new Function("STRING::IsMatch", Type.BOOLEAN, new Type[] { Type.STRING, Type.STRING }, GxRegex.class, "IsMatch"));
		registerFunction(new Function("STRING::ReplaceRegEx", Type.STRING, new Type[] { Type.STRING, Type.STRING, Type.STRING }, GxRegex.class, "Replace"));
		registerFunction(new Function("STRING::StartsWith", Type.BOOLEAN, new Type[] { Type.STRING, Type.STRING }, GXutil.class, "startsWith"));
		registerFunction(new Function("STRING::ToString", Type.STRING, new Type[] { Type.STRING }, GXutilPlus.class, "strIdentity"));
	}

	private static void registerFunction(Function function)
	{
		sFunctions.put(Strings.toLowerCase(function.mName), function);
	}

	private static class Function
	{
		// Function specification.
		private final String mName;
		private final Type mResultType;
		private final List<Type> mParameterTypes;
		private final Class<?> mImplementationClass;
		private final String mImplementationMethodName;
		// private final List<Object> mImplementationExtraParameters;

		// Cached information.
		private Object mImplementationObject;
		private Method mImplementationMethod;

		public Function(String name, Type resultType, Type[] parameterTypes, Class<?> implementationClass, String implementationMethod)
		{
			mName = name;
			mResultType = resultType;
			mParameterTypes = Arrays.asList(parameterTypes);

			mImplementationClass = implementationClass;
			mImplementationMethodName = implementationMethod;
			// mImplementationExtraParameters = (implementationExtraParameters != null ? Arrays.asList(implementationExtraParameters) : Collections.emptyList());
		}

		public Value run(List<Value> parameters)
		{
			// Get the class and method to execute.
			prepareReflection();

			// Prepare parameters.
			if (parameters.size() /* + mImplementationExtraParameters.size() */ != mParameterTypes.size())
				throw new IllegalArgumentException(String.format("Unexpected number of parameters for function %s (expected %s, received %s).", mImplementationMethodName, mParameterTypes.size(), parameters.size()));

			Object[] methodParameters = new Object[mParameterTypes.size() /* + mImplementationExtraParameters.size() */];
			for (int i = 0; i < parameters.size(); i++)
				methodParameters[i] = ExpressionHelper.valueToJavaObject(parameters.get(i), mParameterTypes.get(i));

			/*
			// Put extra parameters.
			for (int i = 0; i < mImplementationExtraParameters.size(); i++)
				methodParameters[i + parameters.size()] = mImplementationExtraParameters.get(i);
			*/

			try
			{
				// Execute function via reflection.
				Object result = mImplementationMethod.invoke(mImplementationObject, methodParameters);
				return ExpressionHelper.javaObjectToValue(mResultType, result);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException(String.format("An exception occurred calling function '%s/%s' via reflection.", mName, mParameterTypes.size()), e);
			}
		}

		private void prepareReflection()
		{
			if (mImplementationMethod == null)
			{
				try
				{
					// Obtain the method via reflection and cache it.
					Class<?>[] parameterTypes = new Class[mParameterTypes.size() /* + mImplementationExtraParameters.size() */];
					for (int i = 0; i < mParameterTypes.size(); i++)
						parameterTypes[i] = ExpressionHelper.typeToJavaClass(mParameterTypes.get(i));

					/*
					for (int i = 0; i < mImplementationExtraParameters.size(); i++)
						parameterTypes[i + mParameterTypes.size()] = mImplementationExtraParameters.get(i).getClass();
					*/

					Method implementationMethod = mImplementationClass.getDeclaredMethod(mImplementationMethodName, parameterTypes);

					// Obtain an instance to run the method (unless it's static, in which case we don't need it).
					Object implementationObject = null;
					if (!Modifier.isStatic(implementationMethod.getModifiers()))
					{
						implementationObject = sImplementationClasses.get(mImplementationClass);
						if (implementationObject == null)
							throw new IllegalArgumentException(String.format("No object provided for non-static method '%s' in class '%s'.", mImplementationMethodName, mImplementationClass.getName()));
					}

					// All set.
					mImplementationObject = implementationObject;
					mImplementationMethod = implementationMethod;
				}
				catch (NoSuchMethodException e)
				{
					throw new IllegalArgumentException(String.format("Method '%s.%s()' (for function '%s/%s') could not be obtained via reflection.", mImplementationClass.getName(), mImplementationMethodName, mName, mParameterTypes.size()));
				}
			}
		}
	}
}
