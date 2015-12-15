package com.artech.base.metadata.filter;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.DataSourceMemberDefinition;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

public class SearchDefinition extends DataSourceMemberDefinition
{
	private static final long serialVersionUID = 1L;

	public static final int OPERATOR_CONTAINS = 1;
	public static final int OPERATOR_BEGINS_WITH = 2;

	private final String mCaption;
	private final boolean mAlwaysVisible;
	private final boolean mHasOptionForIndividualFields;
	private final List<SearchAttributeDefinition> mAttributes;
	private final int mOperator;

	SearchDefinition(IDataSourceDefinition parent, INodeObject jsonData)
	{
		super(parent);

		mCaption = jsonData.optString("@caption"); //$NON-NLS-1$
		mHasOptionForIndividualFields = jsonData.optBoolean("@optionForIndividualFields"); //$NON-NLS-1$
		mAlwaysVisible = jsonData.optBoolean("@alwaysVisible"); //$NON-NLS-1$

		mAttributes = new ArrayList<SearchAttributeDefinition>();
		INodeCollection jsonAttributes = jsonData.optCollection("attribute"); //$NON-NLS-1$
		for (int i = 0; i < jsonAttributes.length(); i++)
		{
			SearchAttributeDefinition searchAtt = new SearchAttributeDefinition(parent, jsonAttributes.getNode(i));
			mAttributes.add(searchAtt);
		}

		String operator = jsonData.optString("@filterOperator"); //$NON-NLS-1$
		mOperator = (operator.equalsIgnoreCase("Begins with") ? OPERATOR_BEGINS_WITH : OPERATOR_CONTAINS); //$NON-NLS-1$
	}

	@Override
	public String getName()
	{
		return "Search"; //$NON-NLS-1$
	}

	public String getCaption() { return Services.Resources.getTranslation(mCaption); }
	public boolean isAlwaysVisible() { return mAlwaysVisible; }
	public boolean hasOptionForIndividualFields() { return mHasOptionForIndividualFields; }
	public List<SearchAttributeDefinition> getAttributes() { return mAttributes; }
	public int getOperator() { return mOperator; }

	public List<String> getAttributeNames()
	{
		ArrayList<String> names = new ArrayList<String>();
		for (SearchAttributeDefinition attribute : mAttributes)
			names.add(attribute.getName());

		return names;
	}
}
