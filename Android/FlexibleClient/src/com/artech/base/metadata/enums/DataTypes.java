package com.artech.base.metadata.enums;

import com.artech.base.metadata.BasicDataType;
import com.artech.base.metadata.ITypeDefinition;
import com.artech.base.metadata.StructureDataType;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.types.BusinessComponentDataType;
import com.artech.base.metadata.types.SdtCollectionItemDataType;
import com.artech.base.services.Services;
import com.artech.base.utils.NameMap;
import com.artech.base.utils.Strings;

public final class DataTypes
{
	public static final String text = "text"; //$NON-NLS-1$
//	public static final String string = "string"; //$NON-NLS-1$
	public static final String email = "email"; //$NON-NLS-1$
	public static final String address = "address"; //$NON-NLS-1$
	public static final String numeric = "numeric"; //$NON-NLS-1$
	public static final String guid = "guid"; //$NON-NLS-1$
	public static final String phone = "phone"; //$NON-NLS-1$
	public static final String date = "date"; //$NON-NLS-1$
	public static final String dtime = "dtime"; //$NON-NLS-1$
	public static final String time  = "time"; //$NON-NLS-1$
	public static final String datetime = "datetime"; //$NON-NLS-1$
	public static final String bool = "bool";	 //$NON-NLS-1$
	public static final String photo = "photo"; //$NON-NLS-1$
	public static final String photourl = "photourl"; //$NON-NLS-1$
	public static final String image = "bits"; //$NON-NLS-1$
	public static final String component = "component"; //$NON-NLS-1$
	public static final String uri = "uri"; //$NON-NLS-1$
	public static final String video = "video"; //$NON-NLS-1$
	public static final String url = "url"; //$NON-NLS-1$
	public static final String geolocation = "geolocation"; //$NON-NLS-1$
	public static final String audio = "audio"; //$NON-NLS-1$
	public static final String sdt = "gx_sdt"; //$NON-NLS-1$
	public static final String businesscomponent = "gx_buscomp"; //$NON-NLS-1$
	public static final String html = "html"; //$NON-NLS-1$
	public static final String userdefined = "userdefined"; //$NON-NLS-1$

	public static ITypeDefinition getDataTypeOf(NameMap<Object> item)
	{
		// It's a Domain (or Attribute)?
		String domainName = (String)item.get("Domain"); //$NON-NLS-1$
		if (Services.Strings.hasValue(domainName))
		{
			domainName = MetadataLoader.getObjectName(domainName);
			ITypeDefinition domain = Services.Application.getDomain(domainName);

			if (domain == null)
				domain = Services.Application.getAttribute(domainName);

			if (domain == null)
			{
				// Don't stop here if domain could not be loaded, maybe we can make do with the basic type.
				Services.Log.warning(String.format("Domain or attribute '%s' in data type definition could not be resolved.", domainName)); //$NON-NLS-1$
			}
			else
				return domain;
		}

		// It's an SDT or BC?
		String type = (String)item.get("Type"); //$NON-NLS-1$
		if (type != null)
		{
			if (type.equalsIgnoreCase(DataTypes.sdt))
			{
				String sdtName = (String)item.get("TypeName"); //$NON-NLS-1$
				StructureDataType dt = Services.Application.getSDT(sdtName);

				if (dt != null)
					return dt;

				// Support item of SDT collection as a special case.
				// TODO: Support any level, this is only for the first one.
				int separatorPos = sdtName.lastIndexOf(Strings.DOT);
				if (separatorPos != -1)
				{
					StructureDataType sdt = Services.Application.getSDT(sdtName.substring(0, separatorPos));
					if (sdt != null && sdt.isCollection())
					{
						String collectionItemName = sdt.getRoot().getCollectionItemName();
						if (Strings.hasValue(collectionItemName) && collectionItemName.equalsIgnoreCase(sdtName.substring(separatorPos + 1)))
							return new SdtCollectionItemDataType(sdt);
					}
				}

				Services.Log.warning(String.format("SDT type '%s' in data type definition could not be resolved.", sdtName)); //$NON-NLS-1$
				return null;
			}
			else if (type.equalsIgnoreCase(DataTypes.businesscomponent))
			{
				String bcName = (String)item.get("TypeName"); //$NON-NLS-1$
				StructureDefinition bc = Services.Application.getBusinessComponent(bcName);

				if (bc != null)
					return new BusinessComponentDataType(bc);

				Services.Log.warning(String.format("BC type '%s' in data type definition could not be resolved.", bcName)); //$NON-NLS-1$
				return null;
			}
		}

		// Should be a basic type.
		return new BasicDataType(item);
	}

	public static boolean isImage(String dataType)
	{
		return (dataType != null &&
				(dataType.equalsIgnoreCase(DataTypes.image) ||
				 dataType.equalsIgnoreCase(DataTypes.photo) ||
				 dataType.equalsIgnoreCase("bitmap"))); //$NON-NLS-1$
	}

	/**
	 * Returns true if the data type is any of the character-based GX types (char, varchar, longvarchar).
	 */
	public static boolean isCharacter(String dataType)
	{
		return (dataType != null &&
				(dataType.equalsIgnoreCase("char") || //$NON-NLS-1$
				 dataType.equalsIgnoreCase("character") || //$NON-NLS-1$
				 dataType.equalsIgnoreCase("vchar") || //$NON-NLS-1$
				 dataType.equalsIgnoreCase("varchar") || //$NON-NLS-1$
				 dataType.equalsIgnoreCase("longvarchar"))); //$NON-NLS-1$
	}

	public static boolean isLongCharacter(String dataType)
	{
		return (dataType != null && dataType.equalsIgnoreCase("longvarchar")); //$NON-NLS-1$
	}
}
