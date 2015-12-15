package com.artech.base.metadata;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.Vector;

import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.model.PropertiesObject;
import com.artech.base.services.Services;
import com.artech.base.utils.NameMap;
import com.artech.base.utils.Strings;
import com.artech.common.StringUtil;

public class BasicDataType implements ITypeDefinition, Serializable
{
	private static final long serialVersionUID = 1L;

	private final PropertiesObject mProps;

	public BasicDataType(NameMap<Object> props)
	{
		mProps = new PropertiesObject(props);
	}

	@Override
	public String getType()
	{
		return (String) mProps.getProperty("Type"); //$NON-NLS-1$
	}

	@Override
	public int getLength()
	{
		return mProps.optIntProperty("Length"); //$NON-NLS-1$
	}

	@Override
	public int getDecimals()
	{
		return mProps.optIntProperty("Decimals"); //$NON-NLS-1$
	}

	@Override
	public boolean getSigned()
	{
		return false;
	}

	@Override
	public boolean getIsEnumeration()
	{
		return false;
	}

	@Override
	public Object getProperty(String propName)
	{
		return mProps.getProperty(propName);
	}

	@Override
	public String getName()
	{
		return getType();
	}

	@Override
	public ITypeDefinition getBaseType()
	{
		return null;
	}

	@Override
	public Vector<EnumValuesDefinition> getEnumValues()
	{
		return null;
	}

	@Override
	public Object getEmptyValue(boolean isCollection)
	{
		// TODO: Support collection of basic type.
		String dataType = getType();
		if (dataType != null && !isCollection)
		{
			if (dataType.equalsIgnoreCase(DataTypes.datetime) || dataType.equalsIgnoreCase(DataTypes.dtime) || dataType.equalsIgnoreCase(DataTypes.time))
				return StringUtil.nullDateTime;
			else if (dataType.equalsIgnoreCase(DataTypes.date))
				return StringUtil.nullDate;
			else if (dataType.equalsIgnoreCase("boolean"))
				return "false";
			else if (dataType.equalsIgnoreCase(DataTypes.guid))
				return new UUID(0L, 0L).toString();
			if (dataType.equals(DataTypes.numeric))
				return Strings.ZERO;
		}

		return Strings.EMPTY;
	}

	@Override
	public boolean isEmptyValue(Object value)
	{
		if (value != null)
		{
			// Special cases: numeric can be "0" or "0.0" or similar.
			if (DataTypes.numeric.equalsIgnoreCase(getType()) && getDecimals() != 0)
			{
				BigDecimal number = Services.Strings.tryParseDecimal(value.toString());
				if (number != null && number.floatValue() == 0f)
					return true;
			}

			String strValue = value.toString();
			String strDefault = getEmptyValue(false).toString();
			return strValue.equals(strDefault);
		}
		else
			return true;
	}
}
