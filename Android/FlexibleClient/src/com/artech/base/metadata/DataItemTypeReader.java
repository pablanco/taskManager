package com.artech.base.metadata;

import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

class DataItemTypeReader
{
	public static void readDataType(DataItem item, INodeObject json)
	{
		item.setIsCollection(readBoolean(json, "isCollection"));

		String domain = MetadataLoader.getObjectName(readString(json, "Domain")); //$NON-NLS-1$
		String type = readString(json, "Type"); //$NON-NLS-1$
		String typeName = readString(json, "TypeName"); //$NON-NLS-1$

		if (type.equalsIgnoreCase(DataTypes.sdt) || type.equalsIgnoreCase(DataTypes.businesscomponent))
		{
			item.setProperty("Type", type); //$NON-NLS-1$
			item.setProperty("TypeName", typeName); //$NON-NLS-1$
		}
		else
		{
			item.setProperty("Type", type); //$NON-NLS-1$
			item.setProperty("Length", readString(json, "Length", Strings.ZERO)); //$NON-NLS-1$ //$NON-NLS-2$
			item.setProperty("Decimals", readString(json, "Decimals", Strings.ZERO)); //$NON-NLS-1$ //$NON-NLS-2$
			item.setProperty("InputPicture", readString(json, "picture", Strings.EMPTY)); //$NON-NLS-1$ //$NON-NLS-2$
			item.setProperty("IsPassword", readString(json, "isPassword")); //$NON-NLS-1$ //$NON-NLS-2$

			if (Services.Strings.hasValue(domain))
				item.setProperty("Domain", domain); //$NON-NLS-1$
		}

		item.setDataType(DataTypes.getDataTypeOf(item.getInternalProperties()));
	}

	private static String readString(INodeObject json, String key)
	{
		// In some cases the '@' is present and in some others it's not. Check for both.
		String value = json.optString(key);
		if (!Strings.hasValue(value))
			value = json.optString("@" + key);

		return value;
	}

	private static String readString(INodeObject json, String key, String defaultValue)
	{
		String value = readString(json, key);
		if (!Strings.hasValue(value))
			value = defaultValue;

		return value;
	}

	private static boolean readBoolean(INodeObject json, String key)
	{
		String strValue = readString(json, key);
		return Services.Strings.tryParseBoolean(strValue, false);

	}
}
