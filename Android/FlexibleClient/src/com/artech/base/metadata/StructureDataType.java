package com.artech.base.metadata;

import java.io.Serializable;
import java.util.List;

import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.metadata.types.IStructuredDataType;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.model.PropertiesObject;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;

public class StructureDataType extends DataTypeDefinition implements Serializable, IStructuredDataType
{
	private static final long serialVersionUID = 1L;
	private StructureDefinition mStructure;

	public static final String COLLECTION_PROPERTY_CURRENT_ITEM = "CurrentItem"; //$NON-NLS-1$
	public static final String COLLECTION_PROPERTY_NTH_ITEM = "Item"; //$NON-NLS-1$

	public StructureDataType(INodeObject jsonData)
	{
		super(jsonData);
	}

	private LevelDefinition mRoot;

	public LevelDefinition getRoot()
	{
		if (mRoot == null)
			mRoot = new LevelDefinition(this);

		return mRoot;
	}

	public LevelDefinition getLevel(String code)
	{
		if (getRoot().getName().equalsIgnoreCase(code))
			return getRoot();

		return getRoot().getLevel(code);
	}

	@Override
	public void deserialize(INodeObject obj)
	{
		LevelDefinition rootLevel = getRoot();
		rootLevel.setName(obj.getString("Name")); //$NON-NLS-1$
		rootLevel.setIsCollection(obj.optBoolean("IsCollection")); //$NON-NLS-1$
		rootLevel.setCollectionItemName(obj.optString("CollectionItemName")); //$NON-NLS-1$

		INodeCollection items = obj.optCollection("Items"); //$NON-NLS-1$
		if (items != null)
			deserializeLevel(rootLevel, items);
	}

	private void deserializeLevel(LevelDefinition levelDef, INodeCollection items)
	{
		for (int i = 0; i < items.length(); i++)
		{
			INodeObject item = items.getNode(i);
			INodeCollection subItems = item.getCollection("Items"); //$NON-NLS-1$
			boolean isCollection = item.optBoolean("IsCollection"); //$NON-NLS-1$

			if (subItems != null)
			{
				LevelDefinition subLevel = new LevelDefinition(this);
				subLevel.setParent(levelDef);

				subLevel.setName(item.getString("Name")); //$NON-NLS-1$
				subLevel.setIsCollection(isCollection);
				subLevel.setCollectionItemName(item.optString("CollectionItemName")); //$NON-NLS-1$

				levelDef.Levels.add(subLevel);
				deserializeLevel(subLevel, subItems);
			}
			else
			{
				PropertiesObject props = new PropertiesObject();
				props.deserialize(item);
				ITypeDefinition tdef = DataTypes.getDataTypeOf(props.getInternalProperties());
				DataItem subItem = new DataItem(tdef);
				subItem.setIsCollection(isCollection);

				levelDef.Items.add(subItem);
				subItem.deserialize(item);
			}
		}
	}

	@Override
	public StructureDefinition getStructure()
	{
		if (mStructure == null)
		{
			StructureDefinition sdtStructure = new StructureDefinition(getName());
			sdtStructure.Root = getRoot();
			mStructure = sdtStructure;
		}

		return mStructure;
	}

	@Override
	public boolean isCollection() { return getRoot().isCollection(); }

	@Override
	public List<DataItem> getItems()
	{
		return getRoot().getAttributes();
	}

	/**
	 * Searches for the specified subitem in the SDT structure
	 * @param name Item name (generally an individual item but may be a level).
	 */
	@Override
	public DataItem getItem(String name)
	{
		return getRoot().getAttribute(name);
	}

	@Override
	public String getType() { return DataTypes.sdt; }

	@Override
	public int getLength() { return 0; }

	@Override
	public int getDecimals() { return 0; }

	@Override
	public boolean getSigned() { return false; }

	@Override
	public boolean getIsEnumeration() { return false; }

	@Override
	public Object getEmptyValue(boolean isCollection)
	{
		if (isCollection || isCollection())
		{
			return new EntityList();
		}
		else
		{
			Entity entity = new Entity(getStructure());
			entity.initialize();
			return entity;
		}
	}

	@Override
	public boolean isEmptyValue(Object value)
	{
		if (value instanceof Entity)
		{
			Entity entity = (Entity)value;
			for (DataItem rootItem : mRoot.Items)
			{
				Object itemValue = entity.getProperty(rootItem.getName());
				if (!rootItem.isEmptyValue(itemValue))
					return false;
			}

			return true; // All items are empty, so the structure itself is empty.
		}
		else if (value instanceof List)
		{
			return ((List)value).size() == 0;
		}
		else
			return (value == null);
	}
}
