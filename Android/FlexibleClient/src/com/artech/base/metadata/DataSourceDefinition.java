package com.artech.base.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.filter.FilterDefinition;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.loader.WorkWithMetadataLoader;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

/**
 * Information about a particular Data Provider.
 * Panels may have more than one data source.
 */
public class DataSourceDefinition implements IDataSourceDefinition, Serializable
{
	private static final long serialVersionUID = 1L;

	private final IDataViewDefinition mComponent;
	private boolean mHasDataProvider;
	private int mVersion;

	private final List<DataItem> mData;
	private StructureDefinition mStructure;
	private boolean mIsCollection;
	private List<ObjectParameterDefinition> mParameters;

	private boolean mCacheEnabled;
	private int mAutoRefreshTime;
	private int mCacheCheckDataLapse;

	private FilterDefinition mFilter;
	private final OrdersAndBreakDefinition mOrders;

	private String mAssociatedBCName;
	@SuppressWarnings("unused")
	private String mAssociatedBCLevelName;

	public DataSourceDefinition(IDataViewDefinition component, INodeObject jsonData)
	{
		mComponent = component;
		mHasDataProvider = true;
		mData = new ArrayList<DataItem>();
		mOrders = new OrdersAndBreakDefinition(this);

		deserialize(jsonData);
	}

	@Override
	public WorkWithDefinition getPattern() { return mComponent.getPattern(); }

	@Override
	public IDataViewDefinition getParent() { return mComponent; }

	@Override
	public String getName() { return mStructure.getName(); }

	@Override
	public boolean hasDataProvider() { return mHasDataProvider; }

	@Override
	public int getVersion() { return mVersion; }

	@Override
	public List<DataItem> getDataItems() { return mData; }

	@Override
	public StructureDefinition getStructure() { return mStructure; }

	@Override
	public boolean isCollection() { return mIsCollection; }

	@Override
	public boolean isCacheEnabled() { return mCacheEnabled; }

	@Override
	public int getAutoRefreshTime() { return mAutoRefreshTime; }

	@Override
	public int getCacheCheckDataLapse() { return mCacheCheckDataLapse; }

	@Override
	public FilterDefinition getFilter() { return mFilter; }

	@Override
	public OrdersAndBreakDefinition getOrders() { return mOrders; }

	@Override
	public List<ObjectParameterDefinition> getParameters()
	{
		if (mParameters != null)
			return mParameters;
		else
			return mComponent.getParameters();
	}

	@Override
	public String getAssociatedBCName() { return mAssociatedBCName; }

	@Override
	public DataItem getDataItem(String name)
	{
		if (!Services.Strings.hasValue(name))
			return null;

		// TODO: Remove this, variables should have the '&' at the start.
		name = DataItemHelper.getNormalizedName(name);

		return mStructure.getAttribute(name);
	}

	/* ----- Deserialization */

	private void deserialize(INodeObject jsonData)
	{
		deserializeDataStructure(jsonData);
		deserializeFilter(jsonData);
		deserializeOrdersAndBreak(jsonData);
	}

	private void deserializeDataStructure(INodeObject jsonData)
	{
		String dpName = jsonData.optString("@DataProvider"); //$NON-NLS-1$
		mIsCollection = jsonData.optBoolean("@isCollection"); //$NON-NLS-1$
		mVersion = jsonData.optInt("@hash"); //$NON-NLS-1$

		if (jsonData.optBoolean("@onlyDefinition"))
			mHasDataProvider = false;

		mCacheEnabled = jsonData.optBoolean("@CacheEnabled", true); //$NON-NLS-1$
		mCacheCheckDataLapse = jsonData.optInt("@CacheCheckForNewDataLapse") * 60; // minutes to seconds //$NON-NLS-1$
		mAutoRefreshTime = jsonData.optInt("@autoRefreshTime"); //$NON-NLS-1$

		StructureDefinition structure = new StructureDefinition(dpName);
		structure.Root.setName(dpName);

		// Read associated BC and Level.
		mAssociatedBCName = jsonData.optString("@bc"); //$NON-NLS-1$
		mAssociatedBCLevelName = jsonData.optString("@level"); //$NON-NLS-1$

		// Read parameters (optional, may be different from DataView's).
		INodeObject jsonParameters = jsonData.optNode("parameters"); //$NON-NLS-1$
		if (jsonParameters != null)
		{
			mParameters = new ArrayList<ObjectParameterDefinition>();
			WorkWithMetadataLoader.readObjectParameterList(mParameters, jsonParameters);
		}

		// Read Attributes
		for (INodeObject jsonAttribute : jsonData.optCollection("attribute")) //$NON-NLS-1$
		{
			DataItem dataItem = deserializeDataAttribute(jsonAttribute);
			if (dataItem != null)
				mData.add(dataItem);
		}

		// Read Variables
		for (INodeObject jsonVariable : jsonData.optCollection("variable")) //$NON-NLS-1$
		{
			DataItem dataItem = deserializeDataVariable(jsonVariable);
			if (dataItem != null)
				mData.add(dataItem);
		}

		// Look for the Base BusinessComponent for this Data Source and merge properties.
		structure.Root.Items.addAll(mData);
		if (Services.Strings.hasValue(mAssociatedBCName))
		{
			// TODO: Should use mAssociatedBCLevelName too!
			StructureDefinition bc = Services.Application.getBusinessComponent(mAssociatedBCName);
			if (bc != null)
				structure.merge(bc);
			else
				Services.Log.Error("readMetadataError", "BC not found: " + mAssociatedBCName); //$NON-NLS-1$ //$NON-NLS-2$
		}

		mStructure = structure;
	}

	private static DataItem deserializeDataAttribute(INodeObject attributeJson)
	{
		return new DataSourceAttributeDefinition(attributeJson);
	}

	private static DataSourceVariableDefinition deserializeDataVariable(INodeObject varNode)
	{
		return new DataSourceVariableDefinition(varNode);
	}

	private void deserializeFilter(INodeObject jsonData)
	{
		FilterDefinition filter = FilterDefinition.empty(this);
		INodeObject filterJson = jsonData.optNode("filter"); //$NON-NLS-1$
		if (filterJson != null)
			filter = new FilterDefinition(this, filterJson);

		mFilter = filter;
	}

	private void deserializeOrdersAndBreak(INodeObject jsonData)
	{
		INodeObject jsonOrders = jsonData.optNode("orders"); //$NON-NLS-1$
		if (jsonOrders != null)
		{
			for (INodeObject jsonOrder : jsonOrders.optCollection("order")) //$NON-NLS-1$
			{
				OrderDefinition order = new OrderDefinition(this, jsonOrder);
				mOrders.add(order);
			}
		}

		INodeObject jsonBreakBy = jsonData.optNode("breakBy");
		if (jsonBreakBy != null)
		{
			for (INodeObject jsonBreakByAtt : jsonBreakBy.optCollection("attribute"))
			{
				String attributeName = MetadataLoader.getAttributeName(jsonBreakByAtt.optString("@attribute"));
				DataItem dataItem = getDataItem(attributeName);
				if (dataItem != null)
					mOrders.addBreakBy(dataItem);
			}

			mOrders.setBreakByDescriptionAttribute(MetadataLoader.getAttributeName(jsonBreakBy.optString("@descriptionAttribute")));
		}
	}
}
