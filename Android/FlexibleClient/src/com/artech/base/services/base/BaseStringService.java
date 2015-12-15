package com.artech.base.services.base;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.artech.base.metadata.enums.MeasureUnit;
import com.artech.base.services.IStringUtil;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

/**
 * Base service implementation for IStringUtil, valid for both Android and Blackberry.
 */
public abstract class BaseStringService implements IStringUtil
{
	private static final int PAD_LIMIT = 8192;

	@Override
	public boolean tryParseBoolean(String str, boolean defaultValue)
	{
		Boolean value = tryParseBoolean(str);
		return (value != null ? value : defaultValue);
	}

	@Override
	public Boolean tryParseBoolean(String str)
	{
		if (str != null)
		{
			str = str.trim();

			if (str.equalsIgnoreCase("true") || str.equals("1")) //$NON-NLS-1$ //$NON-NLS-2$
				return true;

			if (str.equalsIgnoreCase("false") || str.equals("0")) //$NON-NLS-1$ //$NON-NLS-2$
				return false;
		}
		return null;
	}

	@Override
	public boolean parseBoolean(String str)
	{
		return tryParseBoolean(str, false);
	}

	@Override
	public boolean hasValue(CharSequence str)
	{
		return Strings.hasValue(str);
	}

	@Override
	public Integer tryParseInt(String str)
	{
		Integer res = null;

		if (str != null)
		{
			str = str.trim();

			try
			{
				res = Integer.parseInt(str);
			}
			catch (NumberFormatException e)
			{
				res = null;
			}
		}

		return res;
	}

	@Override
	public long tryParseLong(String str, long defaultValue)
	{
		Long value = tryParseLong(str);
		return (value != null ? value : defaultValue);
	}

	public Long tryParseLong(String str)
	{
		Long res = null;

		if (str != null)
		{
			str = str.trim();

			try
			{
				res = Long.parseLong(str);
			}
			catch (NumberFormatException e)
			{
				res = null;
			}
		}

		return res;
	}


	@Override
	public double tryParseDouble(String str, double defaultValue)
	{
		Double value = tryParseDouble(str);
		return (value != null ? value : defaultValue);
	}

	@Override
	public Double tryParseDouble(String str)
	{
		Double res = null;

		if (str != null)
		{
			str = str.trim();

			try
			{
				res = Double.parseDouble(str);
			}
			catch (NumberFormatException e)
			{
				res = null;
			}
		}

		return res;
	}

	@Override
	public float tryParseFloat(String str, float defaultValue)
	{
		Float value = tryParseFloat(str);
		return (value != null ? value : defaultValue);
	}

	@Override
	public Float tryParseFloat(String str)
	{
		Float res = null;

		if (str != null)
		{
			str = str.trim();

			try
			{
				res = Float.parseFloat(str);
			}
			catch (NumberFormatException e)
			{
				res = null;
			}
		}

		return res;
	}

	@Override
	public int tryParseInt(String str, int defaultValue)
	{
		Integer value = tryParseInt(str);
		return (value != null ? value : defaultValue);
	}

	@Override
	public BigDecimal tryParseDecimal(String str)
	{
		BigDecimal res = null;

		if (str != null)
		{
			str = str.trim();
			try
			{
				res = new BigDecimal(str);
			}
			catch (NumberFormatException ex)
			{
				res = null;
			}
		}

		return res;
	}

	@Override
	public UUID tryParseGuid(String str)
	{
		UUID res = null;
		if (Strings.hasValue(str))
		{
			try
			{
				res = UUID.fromString(str.trim());
			}
			catch (Exception e)
			{
				res = null;
			}
		}

		return res;
	}

	@Override
	public int parseEnum(String value, String... options)
	{
		if (value == null || value.equals(Strings.EMPTY))
			return -1;

		for (int i = 0; i < options.length; i++)
		{
			if (value.equalsIgnoreCase(options[i]))
				return i;
		}

		return -1;
	}

	@Override
	public Integer parseMeasureValue(String str, String unit)
	{
		if (!hasValue(str))
			return null;

		if (unit == null)
			unit = Strings.EMPTY;

		str = str.replace(unit, Strings.EMPTY);
		if (unit.equals(MeasureUnit.DIP))
		{
			str = str.replace("dp", Strings.EMPTY); //$NON-NLS-1$
			str = str.replace("DIP", Strings.EMPTY); //$NON-NLS-1$
		}

		if (str.length() != 0)
		{
			try
			{
				return Integer.parseInt(str);
			}
			catch (NumberFormatException ex)
			{
				Services.Log.Error("parseValueWithUnit(" + unit + ")", "Error in parseInt", ex); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}

		return null;
	}

	protected boolean useUtcConversion()
	{
		if (Services.Application != null && Services.Application.getPatternSettings() != null)
			return Services.Application.getPatternSettings().useUtcConversion();
		else
			return true;
	}

	@Override
	public String encodeStringList(List<String> list, char separator)
	{
		StringBuilder sb = new StringBuilder(list.size() * 50);

		for (String item : list)
		{
			if (sb.length() != 0)
				sb.append(separator);

			// Escape the separator character.
			sb.append(item.replace(Character.toString(separator), "\\" + separator));
		}

		return sb.toString();
	}

	@Override
	public List<String> decodeStringList(String encoded, char separator)
	{
		ArrayList<String> items = new ArrayList<String>();

		if (hasValue(encoded))
		{
			// Use negative look-behind with backslash, because it's used for escaping the separator.
			// Expression is "(?<!\)s" with doubling because of escaping in regex, and again because of escaping in Java).
			String splitter = "(?<!\\\\)" + separator; //$NON-NLS-1$
			String[] parts = encoded.split(splitter);

			// While converting to list, take out the escape characters used to escape the now-removed separator.
			for (String part : parts)
				items.add(part.replace("\\" + separator, Character.toString(separator)));
		}

		return items;
	}

    @Override
	public String repeat(final String str, final int repeat)
    {
        // Performance tuned for 2.0 (JDK1.4)

        if (str == null) {
            return null;
        }
        if (repeat <= 0) {
            return Strings.EMPTY;
        }
        final int inputLength = str.length();
        if (repeat == 1 || inputLength == 0) {
            return str;
        }
        if (inputLength == 1 && repeat <= PAD_LIMIT) {
            return repeat(str.charAt(0), repeat);
        }

        final int outputLength = inputLength * repeat;
        switch (inputLength) {
            case 1 :
                return repeat(str.charAt(0), repeat);
            case 2 :
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                final char[] output2 = new char[outputLength];
                for (int i = repeat * 2 - 2; i >= 0; i--, i--) {
                    output2[i] = ch0;
                    output2[i + 1] = ch1;
                }
                return new String(output2);
            default :
                final StringBuilder buf = new StringBuilder(outputLength);
                for (int i = 0; i < repeat; i++) {
                    buf.append(str);
                }
                return buf.toString();
        }
    }

    private static String repeat(final char ch, final int repeat)
    {
        final char[] buf = new char[repeat];
        for (int i = repeat - 1; i >= 0; i--) {
            buf[i] = ch;
        }
        return new String(buf);
    }
}
