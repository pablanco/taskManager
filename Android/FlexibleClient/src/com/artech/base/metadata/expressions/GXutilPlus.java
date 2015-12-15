package com.artech.base.metadata.expressions;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.genexus.DecimalUtil;
import com.genexus.GXutil;
import com.genexus.LocalUtil;

/**
 * Additional functions for which the GX Java Generator generates code instead.
 * Having them as functions with no additional parameters simplifies FunctionHelper.
 * @author matiash
 */
@SuppressWarnings("unused")
class GXutilPlus
{
	private static LocalUtil sLocalUtil;
	private static String sLocalUtilLanguage;

	static LocalUtil getLocalUtil()
	{
		// Check that language hasn't changed since the LocalUtil object was created.
		if (sLocalUtil != null && sLocalUtilLanguage != null && !sLocalUtilLanguage.equals(Services.Resources.getCurrentLanguage()))
			sLocalUtil = null;

		if (sLocalUtil == null)
		{
			String decimalPoint = getLanguageProperty("LangDecimalPoint", ".");
			String dateFormat =  mapLanguageDateFormatToLocalUtilDateFormat(getLanguageProperty("LangDateFormat", "ENG"));
			String timeFormat = getLanguageProperty("LangTimeFormat", "24");
			int firstYear2K = 40; // TODO: This should be read from metadata too.

			String languageCode = Services.Resources.getCurrentLanguage();
			if (Strings.hasValue(languageCode))
				languageCode = Strings.toLowerCase(languageCode.substring(0, 3));
			else
				languageCode = "eng";

			sLocalUtil = LocalUtil.getLocalUtil(decimalPoint.charAt(0), dateFormat, timeFormat, firstYear2K, languageCode);
			sLocalUtilLanguage = Services.Resources.getCurrentLanguage();
		}

		return sLocalUtil;
	}

	private static String getLanguageProperty(String property, String defaultValue)
	{
		String value = Services.Resources.getCurrentLanguageProperty(property);
		if (!Strings.hasValue(value))
			value = defaultValue;

		return value;
	}

	public static long decimalToInteger(BigDecimal value)
	{
		return value.longValue();
	}

	public static String guidToString(UUID value)
	{
		return (value != null ? value.toString() : Strings.EMPTY);
	}

	public static boolean isBooleanEmpty(boolean value)
	{
		//noinspection PointlessBooleanExpression
		return (value == false);
	}

	public static boolean isDateEmpty(Date value)
	{
		return GXutil.nullDate().equals(value);
	}

	public static boolean isGuidEmpty(UUID value)
	{
		return (new UUID(0L, 0L).equals(value));
	}

	public static boolean isNumberEmpty(BigDecimal value)
	{
		return (DecimalUtil.ZERO.compareTo(value) == 0);
	}

	public static boolean isNumberEmpty(int value)
	{
		return (value == 0);
	}

	public static boolean isStringEmpty(String value)
	{
		return (GXutil.strcmp("", value) == 0);
	}

	public static String strIdentity(String value)
	{
		return value;
	}

	public static Date addDays(Date date, int days)
	{
		return GXutil.dtadd(date, days * 86400);
	}

	public static Date addHours(Date date, int hours)
	{
		return GXutil.dtadd(date, hours * 3600);
	}

	public static Date addMinutes(Date date, int minutes)
	{
		return GXutil.dtadd(date, minutes * 60);
	}

	public static String padl_2(String text, int size)
	{
		return GXutil.padl(text, size, " ");
	}

	public static String padr_2(String text, int size)
	{
		return GXutil.padr(text, size, " ");
	}

	public static String str_1(BigDecimal value)
	{
		return str_2(value, 10);
	}

	public static String str_2(BigDecimal value, int length)
	{
		return GXutil.str(value, length, 0);
	}

	public static String substring_2(String text, int start)
	{
		return GXutil.substring(text, start, -1);
	}

	public static int strSearch_2(String a, String b)
	{
		return GXutil.strSearch(a, b, 1);
	}

	public static int strSearchRev_2(String a, String b)
	{
		return GXutil.strSearchRev(a, b, -1);
	}

	public static String dtoc_1(Date date)
	{
		// From evalfng.ari, "/" is fixed. Others depend on language.
		return getLocalUtil().dtoc(date, getDateFormat(), "/");
	}

	public static String ttoc_1(Date d)
	{
		return ttoc_2(d, 10);
	}

	public static String ttoc_2(Date d, int dateLength)
	{
		return ttoc_3(d, dateLength, 8);
	}

	public static String ttoc_3(Date d, int dateLength, int timeLength)
	{
		// From evalfng.ari, "/" ":" " " are fixed. Others depend on language.
		return getLocalUtil().ttoc(d, dateLength, timeLength, getTimeFormat(), getDateFormat(), "/", ":", " ");
	}

	private static String mapLanguageDateFormatToLocalUtilDateFormat(String df)
	{
		// Copied from date_format_from_language/2 in dtfmt.ari
		if (Strings.hasValue(df))
		{
			if (df.equalsIgnoreCase("ENG"))
				return "MDY";
			else if (df.equalsIgnoreCase("SPA") || df.equalsIgnoreCase("ITA") || df.equalsIgnoreCase("POR"))
				return "DMY";
			else if (df.equalsIgnoreCase("CHS") || df.equalsIgnoreCase("CHT") || df.equalsIgnoreCase("JAP"))
				return "YMD";
			else if (df.equalsIgnoreCase("ANSI"))
				return "YMD";
		}

		return "DMY";
	}

	private static int getDateFormat()
	{
		String dateFormat = Services.Resources.getCurrentLanguageProperty("LangDateFormat");
		dateFormat = mapLanguageDateFormatToLocalUtilDateFormat(dateFormat);
		return getLocalUtil().mapDateFormat(dateFormat);
	}

	private static int getTimeFormat()
	{
		String timeFormat = Services.Resources.getCurrentLanguageProperty("LangTimeFormat");
		return (timeFormat != null && timeFormat.equals("12") ? 1 : 0);
	}
}
