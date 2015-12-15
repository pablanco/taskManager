package com.artech.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.text.Html;

import com.artech.application.MyApplication;
import com.artech.base.services.IStringUtil;
import com.artech.base.services.base.BaseStringService;
import com.artech.base.utils.Strings;

public class StringUtil extends BaseStringService implements IStringUtil
{
	@Override
	public String getResource(int resourceId)
	{
		return MyApplication.getAppContext().getResources().getString(resourceId);
	}

	@Override
	public String getResource(int resourceId, Object... formatArgs)
	{
		return MyApplication.getAppContext().getResources().getString(resourceId, formatArgs);
	}

	@Override
	public String convertStreamToString(InputStream is)
	{
		final int BUFFER_SIZE = 8192; // Default value, just to avoid warning in emulator.

		/*
		 * To convert the InputStream to String we use the BufferedReader.readLine()
		 * method. We iterate until the BufferedReader return null which means
		 * there's no more data to read. Each line will appended to a StringBuilder
		 * and returned as String.
		 */
		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is), BUFFER_SIZE);
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n"); //$NON-NLS-1$
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return sb.toString();
		} else {
			return Strings.EMPTY;
		}
	}

	private static String joinS(Iterable<?> pColl, String separator)
	{
		Iterator<?> oIter;
		if (pColl == null || (!(oIter = pColl.iterator()).hasNext()))
			return Strings.EMPTY;

		StringBuilder oBuilder = new StringBuilder(String.valueOf(oIter.next()));
		while (oIter.hasNext())
			oBuilder.append(separator).append(oIter.next());

		return oBuilder.toString();
	}

	private SimpleDateFormat getDateFormat()
	{
		return new SimpleDateFormat("yyyy-MM-dd", Locale.US); //$NON-NLS-1$
	}

	private SimpleDateFormat getDateTimeFormat()
	{
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US); //$NON-NLS-1$
	}

	@Override
	public boolean isDateFormatValid(String str, String format) {
		if (str == null) {
			return false;
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.US);
		dateFormat.setLenient(false);

		try {
			dateFormat.parse(str);
		} catch (ParseException e) {
			return false;
		}

		return true;
	}

	//TODO: null date now return null, see what is the correct value to return.
	public static String nullDate = "0000-00-00"; //$NON-NLS-1$
	public static String nullDateTime = "0000-00-00T00:00:00"; //$NON-NLS-1$

	@Override
	public Date getDate(String value)
	{
		Date date = new Date();
		if (value!=null && value.equalsIgnoreCase(nullDate))
		{
			date = null;
		}
		else if (value!=null)
		{
			ParsePosition pos = new ParsePosition(0);
			date = getDateFormat().parse(value, pos);
		}
		return date;
	}

	@Override
	public Date getDateTime(String value)
	{
		return getDateTime(value, false);
	}

	@Override
	public Date getDateTime(String value, boolean isOnlyTime)
	{
		// If we get passed a DATE string (i.e. "2001-11-30"), parse it as Date.
		// Before this change it was returning null because a date string does not match the expected format.
		if (value != null && value.length() == nullDate.length())
			return getDate(value);

		Date date = new Date();

		// A DateTime of 00/00/00 00:00 is considered null. However, 00:00 is a valid TIME (the date is not important).
		if (value != null && !isOnlyTime && value.equalsIgnoreCase(nullDateTime))
		{
			date = null;
		}
		else if (value != null)
		{
			value = value.replace("0001-", "1970-"); //$NON-NLS-1$ //$NON-NLS-2$
			SimpleDateFormat formatter = getDateTimeFormat();

			ParsePosition pos = new ParsePosition(0);
			date = formatter.parse(value, pos);
			if (date == null)
			{
				value = value.replace("T", Strings.SPACE); //$NON-NLS-1$
				pos = new ParsePosition(0);
				date = formatter.parse(value, pos);
			}

			// Convert From UTC To Local Time
			if (date != null && useUtcConversion() && !isOnlyTime)
			{
				long offset = TimeZone.getDefault().getOffset(date.getTime());
				date.setTime(date.getTime() + offset);
			}
		}
		return date;
	}

	@Override
	public String getDateString(Date date, String inputType)
	{
		if (date == null || (Strings.hasValue(inputType) && !inputType.startsWith("99/99/99")))
			return Strings.EMPTY;

		// First we get the application context in order to format the date string according to the device's settings.
		Context context = MyApplication.getAppContext();

		// This gives us a date string in the 4-digit year format according to the user's order preference: DD/MM/YYYY, MM/DD/YYYY or YYYY/MM/DD.
		String dateString = android.text.format.DateFormat.getDateFormat(context).format(date);

		// has at least one '/'
		if (dateString.indexOf('/')>0)
		{
			int yearPos;
			if (dateString.indexOf('/') == 1 || dateString.indexOf('/') == 2) {
				// DD/MM/YYYY or MM/DD/YYYY format
				yearPos = dateString.length() - 4;
			} else {
				// YYYY/MM/DD format
				yearPos = 0;
			}

			// Make it into a 2-digit year format if specified or no inputType was provided (default format).
			if (!Strings.hasValue(inputType) || !inputType.startsWith("99/99/9999"))
				dateString = dateString.substring(0, yearPos) + dateString.substring(yearPos + 2, dateString.length());
		}

		return dateString;
	}

	@Override
	public String getTimeString(Date date, String inputType)
	{
		if (date == null)
			return Strings.EMPTY;

		// First we get the application context in order to format the time string according to the device's settings.
		Context context = MyApplication.getAppContext();

		String timeString;
		String dateFormatString;

		// Depending on the inputType we either show just hh, hh:mm (default) or hh:mm:ss.
		if (Strings.hasValue(inputType) && inputType.endsWith("99:99:99")) {
			dateFormatString = ":mm:ss";
		} else if (Strings.hasValue(inputType) && (inputType.equals("99") || inputType.endsWith(" 99"))) {
			dateFormatString = "";
		} else {
			dateFormatString = ":mm";
		}

		if (android.text.format.DateFormat.is24HourFormat(context)) {
			// 24-hours format.
			timeString = (new SimpleDateFormat("H" + dateFormatString, Locale.US)).format(date);
		} else {
			// AM/PM format.
			timeString = (new SimpleDateFormat("h" + dateFormatString + " a", Locale.US)).format(date);
		}

		return timeString;
	}

	@Override
	public String getDateTimeString(Date date, String inputType)
	{
		if (date==null)
			return Strings.EMPTY;

		String dateString = getDateString(date, inputType); // Returns an empty string in case of TimeDate with no date.
		String timeString = getTimeString(date, inputType);

		return dateString + Strings.SPACE + timeString;
	}

	@Override
	public String getDateStringForServer(Date date)
	{
		if (date == null)
			return nullDate;

		return getDateFormat().format(date);
	}

	@Override
	public String getDateTimeStringForServer(Date date)
	{
		return getDateTimeStringForServer(date, false);
	}

	@Override
	public String getDateTimeStringForServer(Date date, boolean shouldNotConvertOnlyTime)
	{
		if (date == null)
			return nullDateTime;

		// Make a copy of the Date object, since it may be modified below.
		date = new Date(date.getTime());

		// Convert From Local Time to UTC
		if (useUtcConversion() && !shouldNotConvertOnlyTime)
		{
			long offset = TimeZone.getDefault().getOffset(date.getTime());
			date.setTime(date.getTime() - offset );
		}
		if (useUtcConversion() && shouldNotConvertOnlyTime)
		{
			//reset date
			date = com.genexus.GXutil.resetDate(date);
		}
		return getDateTimeFormat().format(date);
	}

	@Override
	public String join(Iterable<?> components, String separator)
	{
		return StringUtil.joinS(components, separator);
	}

	@Override
	public String processHtml(String text)
	{
		return Html.fromHtml(text, null, null).toString();
	}

	@Override
	public String[] split(String str, char separator)
	{
		// Special case optimization.
		if (str.indexOf(separator) == -1)
			return new String[] { str };

		return split(str, Character.toString(separator));
	}

	@Override
	public String[] split(String str, String separator)
	{
		return str.split(Pattern.quote(separator));
	}

	@Override
	public long valueOf(String val)
	{
		return Long.valueOf(val);
	}

	private final static SimpleDateFormat sHttpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH); //$NON-NLS-1$


	private static DateFormat getHttpDateFormat()
	{
		// According to http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1
		synchronized (sHttpDateFormat) {
			sHttpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
		}
		return sHttpDateFormat;
	}

	public static Date dateFromHttpFormat(String dateString)
	{
		try
		{
			return getHttpDateFormat().parse(dateString);
		}
		catch (ParseException ex)
		{
			ex.printStackTrace();
			return new Date(0);
		}
	}

	public static String dateToHttpFormat(Date date)
	{
		// For some reason, the conversion adds "GMT+00:00" to the end. We want just the "GMT" part.
		String dateString = getHttpDateFormat().format(date);
		return dateString.replace("GMT+00:00", "GMT"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String TimeZoneOffsetID()
	{
		return TimeZone.getDefault().getID();
	}

	@Override
	public String getStringHash(String value)
	{
		MessageDigest digest;
		byte[] hashBytes;

		try
		{
			// Do NOT cache MessageDigest in a member variable. Instances of this class are not thread safe.
			digest = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException("SHA-256 algorithm not found? This should never happen.", e);
		}

		try
		{
			hashBytes = digest.digest(value.getBytes(HTTP.UTF_8));
		}
		catch (UnsupportedEncodingException e)
		{
			throw new IllegalStateException("UTF-8 charset not supported? This should never happen.", e);
		}

		// Convert the hash bytes to a readable hexadecimal string.
		return asHex(hashBytes);
	}

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private static String asHex(byte[] buf)
    {
        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i)
        {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }

        return new String(chars);
    }
}
