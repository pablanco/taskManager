package com.artech.common;

import java.math.BigDecimal;
import java.util.Date;

import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.metadata.expressions.ExpressionFormatHelper;
import com.artech.base.services.IValuesFormatter;
import com.artech.base.services.Services;
import com.artech.base.utils.ParametersStringUtil;
import com.artech.base.utils.Strings;

public class FormatHelper
{
	public static IValuesFormatter getFormatter(DataItem dataItem)
	{
		return new Formatter(dataItem);
	}

	private static class Formatter implements IValuesFormatter
	{
		private final DataItem mDefinition;

		private Formatter(DataItem definition)
		{
			mDefinition = definition;
		}

		@Override
		public CharSequence format(String value)
		{
			return formatValue(value, mDefinition);
		}

		@Override
		public boolean needsAsync() { return false; }
	}

	public static CharSequence formatValue(String value, DataItem dataItem)
	{
		if (value == null)
			return Strings.EMPTY;

		if (dataItem == null)
			return value;

		if (dataItem.getIsEnumeration())
			return ParametersStringUtil.getDescriptionOfEnum(dataItem.getBaseType().getEnumValues(), value);

		String type = dataItem.getDataTypeName().GetDataType();
		String picture = dataItem.getInputPicture();
		return formatValue(value, type, picture);
	}

	public static CharSequence formatValue(String value, String type, String picture)
	{
		if (value != null)
		{
			if (type.equals(DataTypes.html))
			{
				return Services.Strings.processHtml(value);
			}
			if (type.equals(DataTypes.date))
			{
				Date date = Services.Strings.getDate(value);
				return formatDate(date, type, picture);
			}
			else if (type.equals(DataTypes.dtime) || type.equals(DataTypes.datetime))
			{
				Date dateTime = Services.Strings.getDateTime(value);
				return formatDate(dateTime, type, picture);
			}
			else if (type.equals(DataTypes.time))
			{
				Date time = Services.Strings.getDateTime(value, true);
				return formatDate(time, type, picture);
			}
			else if (type.equals(DataTypes.numeric))
			{
				return formatNumber(value, picture);
			}
		}

		return value;
	}

	private static String formatNumber(String value, String picture)
	{
		if (!Services.Strings.hasValue(picture))
			return value;

		if (!Services.Strings.hasValue(value))
			value = Strings.ZERO;

		try
		{
			BigDecimal numericValue = new BigDecimal(value);
			return formatNumber(numericValue, picture);
		}
		catch (NumberFormatException ex)
		{
			Services.Log.warning(String.format("Unexpected numeric value: '%s'.", value), ex);
			return value;
		}
	}

	public static String formatNumber(BigDecimal value, String picture)
	{
		if (value == null)
			value = BigDecimal.ZERO;

		String str = ExpressionFormatHelper.formatNumber(value, picture);

		// Trim left spaces, looks better in Android that way.
		return str.trim();
	}

	public static String formatDate(Date date, String type, String picture)
	{
		if (type.equals(DataTypes.date))
		{
			return Services.Strings.getDateString(date, picture);
		}
		else if (type.equals(DataTypes.dtime) || type.equals(DataTypes.datetime))
		{
			return Services.Strings.getDateTimeString(date, picture);
		}
		else if (type.equals(DataTypes.time))
		{
			return Services.Strings.getTimeString(date, picture);
		}
		else
		{
			// Default = date.
			return Services.Strings.getDateString(date, picture);
		}
	}
}
