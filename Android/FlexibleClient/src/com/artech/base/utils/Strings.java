package com.artech.base.utils;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class Strings
{
	private Strings() { }

	public static final String EMPTY = ""; // $NON-NLS-1$
	public static final String SPACE = " "; // $NON-NLS-1$
	public static final String DOT = "."; // $NON-NLS-1$
	public static final String COMMA = ","; // $NON-NLS-1$
	public static final String ZERO = "0"; // $NON-NLS-1$
	public static final String ONE = "1"; // $NON-NLS-1$
	public static final String QUESTION = "?"; // $NON-NLS-1$
	public static final String AND = "&"; // $NON-NLS-1$
	public static final String EQUAL = "="; // $NON-NLS-1$
	public static final String SLASH = "/"; // $NON-NLS-1$
	public static final String BACKSLASH = "\\"; // $NON-NLS-1$

	public static final String SINGLE_QUOTE = "'"; // $NON-NLS-1$
	public static final String DOUBLE_QUOTE = "\""; // $NON-NLS-1$

	public static final String TRUE = "true"; // $NON-NLS-1$
	public static final String FALSE = "false"; // $NON-NLS-1$

	// Should be "\n" and "/"
	public static final String NEWLINE = System.getProperty("line.separator"); // $NON-NLS-1$
	public static final String PATH_SEPARATOR = System.getProperty("file.separator"); // $NON-NLS-1$

	public static boolean areEqual(String one, String two)
	{
		return areEqual(one, two, false);
	}

	public static boolean areEqual(String one, String two, boolean ignoreCase)
	{
		if (one != null)
			return (ignoreCase ? one.equalsIgnoreCase(two) : one.equals(two));
		else
			return (two == null);
	}

	public static boolean hasValue(CharSequence str)
	{
		return (str != null && str.length() != 0);
	}

	public static String toLowerCase(String str)
	{
		if (str == null)
			return null;

		return str.toLowerCase(Locale.US);
	}

	public static boolean starsWithIgnoreCase(String str, String prefix)
	{
		if (str == null || prefix == null)
			return false;

		if (str.length() < prefix.length())
			return false;

		String strStart = str.substring(0, prefix.length());
		return strStart.equalsIgnoreCase(prefix);
	}

	public static boolean endsWithIgnoreCase(String str, String prefix)
	{
		if (str == null || prefix == null)
			return false;

		if (str.length() < prefix.length())
			return false;

		String strEnd = str.substring(str.length() - prefix.length(), str.length());
		return strEnd.equalsIgnoreCase(prefix);
	}

	public static boolean arrayContains(String[] array, String value, boolean ignoreCase)
	{
		for (String arr : array)
		{
			if (areEqual(arr, value, ignoreCase))
				return true;
		}

		return false;
	}

	public static Set<String> newSet(String... values)
	{
		TreeSet<String> set = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		if (values != null)
			Collections.addAll(set, values);

		return set;
	}
}
