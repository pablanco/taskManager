package com.artech.base.metadata.filter;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.DataItemHelper;
import com.artech.base.metadata.DataSourceMemberDefinition;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class FilterAttributeDefinition extends DataSourceMemberDefinition
{
	private static final long serialVersionUID = 1L;

	private final String mName;
	private final String mDescription;
	private final String mType;

	private final String mDefaultValue;
	private final String mDefaultBeginValue;
	private final String mDefaultEndValue;
	private final boolean mAllValue;

	private final String mCustomCondition;

	public final static String TYPE_STANDARD = "Standard"; //$NON-NLS-1$
	public final static String TYPE_RANGE = "Range"; //$NON-NLS-1$

	public FilterAttributeDefinition(IDataSourceDefinition parent, INodeObject jsonData)
	{
		super(parent);
		String name = MetadataLoader.getAttributeName(jsonData.getString("@attribute")); //$NON-NLS-1$

		mName = DataItemHelper.getNormalizedName(name);
		mDescription = jsonData.optString("@description"); //$NON-NLS-1$
		mType = jsonData.optString("@type"); //$NON-NLS-1$

		mDefaultValue = parseExpression(jsonData.optString("@default")); //$NON-NLS-1$
		mDefaultBeginValue = parseExpression(jsonData.optString("@defaultBeginRange")); //$NON-NLS-1$
		mDefaultEndValue = parseExpression(jsonData.optString("@defaultEndRange")); //$NON-NLS-1$
		mAllValue = jsonData.optBoolean("@allValue"); //$NON-NLS-1$

		mCustomCondition = jsonData.optString("@customCondition"); //$NON-NLS-1$
	}

	@Override
	public String getName() { return mName; }
	public String getDescription() { return Services.Resources.getTranslation(mDescription); }
	public String getType() { return mType; }
	public String getDefaultValue() { return mDefaultValue; }
	public String getDefaultBeginValue() { return mDefaultBeginValue; }
	public String getDefaultEndValue() { return mDefaultEndValue; }
	public boolean getAllValue() { return mAllValue; }
	public String getCustomCondition() { return mCustomCondition; }

	private static String parseExpression(String expression)
	{
		// For now, we do not support "real" expressions here, just constants.
		// So the only processing we do is to remove quotes from string constants.
		if (Strings.hasValue(expression))
		{
			expression = expression.trim();
			if (expression.length() >= 2)
			{
				if ((expression.startsWith(Strings.SINGLE_QUOTE) && expression.endsWith(Strings.SINGLE_QUOTE)) ||
					(expression.startsWith(Strings.DOUBLE_QUOTE) && expression.endsWith(Strings.DOUBLE_QUOTE)))
				{
					// GX accepts both types of quotes.
					expression = expression.substring(1, expression.length() - 1);
				}
			}
		}

		return expression;
	}

	public List<String> getParameterNames()
	{
		List<String> names = new ArrayList<String>();

		if (mType.equals(TYPE_RANGE))
		{
			names.add("C" + Strings.toLowerCase(mName) + "from"); //$NON-NLS-1$ //$NON-NLS-2$
			names.add("C" + Strings.toLowerCase(mName) + "to"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else //  if (mType.equals(TYPE_STANDARD))
		{
			names.add("C" + Strings.toLowerCase(mName)); //$NON-NLS-1$
		}

		return names;
	}
}
