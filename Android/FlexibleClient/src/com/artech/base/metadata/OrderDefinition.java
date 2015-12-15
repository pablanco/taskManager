package com.artech.base.metadata;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

public class OrderDefinition extends DataSourceMemberDefinition
{
	private static final long serialVersionUID = 1L;

	private String mName;
	private final List<OrderAttributeDefinition> mAttributes;

	private boolean mBreakBy = false;
	private String mBreakByUpToAttribute = null;
	private String mBreakByDescriptionAttribute = null;
	private boolean mEnableAlphaIndexer = false;

	public OrderDefinition(IDataSourceDefinition parent, INodeObject jsonOrder)
	{
		super(parent);
		mAttributes = new ArrayList<OrderAttributeDefinition>();
		deserialize(jsonOrder);
	}

	public int getId()
	{
		return getParent().getOrders().indexOf(this);
	}

	@Override
	public String getName() { return Services.Resources.getTranslation(mName); }
	public List<OrderAttributeDefinition> getAttributes() { return mAttributes;	}

	boolean hasBreakBy()	{ return mBreakBy; }

	public boolean getEnableAlphaIndexer() { return mEnableAlphaIndexer; }

	List<DataItem> getBreakByAttributes()
	{
		ArrayList<DataItem> attributes = new ArrayList<DataItem>();
		for (OrderAttributeDefinition orderAtt : mAttributes)
		{
			attributes.add(orderAtt.getAttribute());
			if (Services.Strings.hasValue(mBreakByUpToAttribute) && mBreakByUpToAttribute.equalsIgnoreCase(orderAtt.getName()))
				break; // Up to here.
		}

		return attributes;
	}

	List<DataItem> getBreakByDescriptionAttributes()
	{
		DataItem single = getParent().getDataItem(mBreakByDescriptionAttribute);
		if (single != null)
		{
			ArrayList<DataItem> attributes = new ArrayList<DataItem>();
			attributes.add(single);
			return attributes;
		}
		else
			return getBreakByAttributes();
	}

	private void deserialize(INodeObject jsonOrder)
	{
		mName = jsonOrder.optString("@name"); //$NON-NLS-1$
		mBreakBy = jsonOrder.optBoolean("@GroupBy"); //$NON-NLS-1$
		mBreakByUpToAttribute = MetadataLoader.getObjectName(jsonOrder.optString("@groupByUpTo", null)); //$NON-NLS-1$
		mBreakByDescriptionAttribute = MetadataLoader.getObjectName(jsonOrder.optString("@descriptionAttribute", null)); //$NON-NLS-1$
		mEnableAlphaIndexer = jsonOrder.optBoolean("@EnableAlphaIndexer"); //$NON-NLS-1$

		// Read Attributes in Order
		for (INodeObject jsonAttribute : jsonOrder.optCollection("attribute")) //$NON-NLS-1$
		{
			OrderAttributeDefinition attribute = new OrderAttributeDefinition(this, jsonAttribute);
			mAttributes.add(attribute);
		}
	}
}
