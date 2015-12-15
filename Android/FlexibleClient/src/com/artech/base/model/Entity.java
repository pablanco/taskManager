package com.artech.base.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import android.util.Pair;

import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.DataItemHelper;
import com.artech.base.metadata.LevelDefinition;
import com.artech.base.metadata.StructureDataType;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.IEntity;
import com.artech.base.services.Services;
import com.artech.base.utils.NameMap;
import com.artech.base.utils.Strings;

public class Entity extends PropertiesObject implements IEntity
{
	private static final long serialVersionUID = 1L;

	public static final int OPERATION_NONE = 0;
	public static final int OPERATION_INSERT = 1;
	public static final int OPERATION_UPDATE = 2;
	public static final int OPERATION_DELETE = 3;

	private boolean m_loaded = false;
	private boolean mLoadingHeader = false;
	private String mKeyString = Strings.EMPTY;

	private EntityParentInfo mParentInfo;
	private final StructureDefinition mDefinition;
	private final LevelDefinition mLevel;

	// Data items that can be read or written, but are not part of the structure.
	private final Map<String, DataItem> mExtraMembers = new TreeMap<String, DataItem>(String.CASE_INSENSITIVE_ORDER);

	private OnPropertyValueChangeListener mOnPropertyValueChangeListener;

	private String mSelectionExpression;
	private boolean mIsSelected = false; // Flag used when "selection expression" is not set.

	private final transient EntitySerializer mSerializer;

	private final Hashtable<String, EntityList> mLevels = new Hashtable<String, EntityList>();

	private final NameMap<Object> mTags = new NameMap<Object>();

	public Entity(StructureDefinition definition)
	{
		this(definition, EntityParentInfo.NONE);
	}

	Entity(StructureDefinition definition, EntityParentInfo parent)
	{
		this(definition, (definition != null ? definition.Root : null), parent);
	}

	public Entity(StructureDefinition definition, LevelDefinition level, EntityParentInfo parent)
	{
		if (level == null && definition != null)
			level = definition.Root;

		if (parent == null)
			parent = EntityParentInfo.NONE;

		mDefinition = definition;
		mLevel = level;

		mParentInfo = parent;
		mSerializer = new EntitySerializer(this);
	}

	public EntityParentInfo getParentInfo()
	{
		return mParentInfo;
	}

	public void setParentInfo(EntityParentInfo parentInfo)
	{
		if (mParentInfo.getParent() != null && mParentInfo.getParent() != parentInfo.getParent())
			Services.Log.warning("Changing Entity parent. This should never happen.");

		mParentInfo = parentInfo;
	}

	public boolean isEmpty()
	{
		return (getInternalProperties().size() == 0);
	}

	public void setExtraMembers(List<? extends DataItem> members)
	{
		mExtraMembers.clear();
		for (DataItem item : members)
			mExtraMembers.put(item.getName(), item);

		initializeMembers(members);
	}

	@Override
	public String toString()
	{
		return serialize().toString();
	}

	public String toDebugString()
	{
		return String.format("[Id=%s, Data=%s]", System.identityHashCode(this), toString());
	}

	/**
	 * Initializes all members of the entity with their "empty" values (e.g. 0 for numerics,
	 * empty string for characters, "default" Entity for inner structures).
	 */
	public void initialize()
	{
		initializeMembers(mLevel.Items);
	}

	private void initializeMembers(Iterable<? extends DataItem> members)
	{
		for (DataItem di : members)
		{
			if (baseGetProperty(di.getName()) == null)
				baseSetProperty(di.getName(), di.getEmptyValue());
		}
	}

	@Override
	public boolean setProperty(String name, Object value)
	{
		if (isSpecialProperty(name))
			return baseSetProperty(name, value);

		Pair<Entity, String> propertyContainer = resolvePropertyContainer(name, true);
		if (propertyContainer != null)
		{
			Entity innerEntity = propertyContainer.first;
			String innerName = propertyContainer.second;

			// Perform conversion if necessary.
			Object propertyValue = innerEntity.mSerializer.deserializeValue(innerName, value);
			if (propertyValue != null)
				value = propertyValue;

			Object oldValue = innerEntity.baseGetProperty(innerName);
			boolean set = innerEntity.baseSetProperty(innerName, value);

			// Fire the "property value change" event if applicable.
			if (set && mOnPropertyValueChangeListener != null && (oldValue == null || !oldValue.equals(value)))
				mOnPropertyValueChangeListener.onPropertyValueChange(name, oldValue, value);

			return set;
		}

		// TODO: Remove this line ASAP. It's only for the chart control.
		baseSetProperty(name, value);

		Services.Log.warning(String.format("Entity.setProperty(%s, %s) failed.", name, value));
		return false;
	}

	//TODO: This function should be removed , all levels should be accessible through getProperty
	public EntityList getLevel(String name)
	{
		return mLevels.get(name);
	}

	@Override
	public Object getProperty(String name)
	{
		if (isSpecialProperty(name))
			return baseGetProperty(name);

		if (name != null)
		{
			Pair<Entity, String> propertyContainer = resolvePropertyContainer(name, true);
			if (propertyContainer != null)
			{
				Object value = propertyContainer.first.baseGetProperty(propertyContainer.second);
				if (value != null)
					return value;
			}

			// TODO: Remove these lines ASAP. It's only for the chart control.
			Object valueHere = baseGetProperty(name);
			if (valueHere != null)
				return valueHere;
		}

		Services.Log.warning(String.format("Entity.getProperty(%s) failed.", name));
		return null;
	}

	/**
	 * Similar to getPropertyDefinition(), but parses expressions like "A.B.C" to get the
	 * "C" property in the "A.B" object, instead of returning only "local" properties.
	 */
	public DataItem resolvePropertyDefinition(String name)
	{
		Pair<Entity, String> propertyContainer = resolvePropertyContainer(name, true);
		if (propertyContainer != null)
			return propertyContainer.first.getPropertyDefinition(propertyContainer.second);
		else
			return null;
	}

	/**
	 * Given an entity and a (possibly complex) property name, return the entity
	 * that "really" contains the property. May return the same entity and property name,
	 * or a subordinated entity (e.g. an SDT member) and the property name inside it.
	 */
	private Pair<Entity, String> resolvePropertyContainer(String propertyName, boolean allowGoUp)
	{
		// Variables and attributes are not distinct here, so ignore the '&'.
		propertyName = DataItemHelper.getNormalizedName(propertyName);

		// Consider properties of the type "&var.field".
		String[] propertyPath = Services.Strings.split(propertyName, '.');

		// First find if the property (actually its most immediate member) actually belongs to this entity.
		String rootProperty = propertyPath[0];
		DataItem propDefinition = getPropertyDefinition(rootProperty);
		if (propDefinition == null)
		{
			// NOT here. So we cannot assign in this entity, or go further below if it's a composite property.
			// It MAY, however, be a parent property (e.g. assigning a form variable from a grid row context).
			// This is DISABLED if we already went down a level, because it makes no sense to look for
			// a partial expression in the parent context.
			if (allowGoUp && mParentInfo.getParent() != null)
				return mParentInfo.getParent().resolvePropertyContainer(propertyName, true);

			// Failure. Property unknown here, and we don't have a parent to check.
			return null;
		}

		if (propertyPath.length == 1)
		{
			// Simple property, the current entity is the correct one.
			return new Pair<Entity, String>(this, propertyName);
		}
		else
		{
			// Compound property. Find the entity of the first level and go recursively down.
			Object component = baseGetProperty(propertyPath[0]);
			if (component != null)
			{
				// Build the *remaining* path into an ArrayList, to ease manipulation.
				ArrayList<String> rest = new ArrayList<String>();
				rest.addAll(Arrays.asList(propertyPath).subList(1, propertyPath.length));

				if (component instanceof Entity)
				{
					// A single item. Go recursively.
					return ((Entity)component).resolvePropertyContainer(Services.Strings.join(rest, Strings.DOT), false);
				}
				else if (component instanceof EntityList)
				{
					// A collection item. Go recursively.
					EntityList list = (EntityList)component;

					// The following term in the expression MUST be a collection item selector, either
					// an explicit indexer (item(<n>)) or the enumeration position (CurrentItem).
					String itemSelector = rest.get(0);
					rest.remove(0);

					Entity item = null;
					if (itemSelector.equalsIgnoreCase(StructureDataType.COLLECTION_PROPERTY_CURRENT_ITEM))
					{
						item = list.getCurrentEntity();
					}
					else if (Strings.toLowerCase(itemSelector).contains(Strings.toLowerCase(StructureDataType.COLLECTION_PROPERTY_NTH_ITEM)))
					{
						String itemIndexStr = Strings.toLowerCase(itemSelector).replace(Strings.toLowerCase(StructureDataType.COLLECTION_PROPERTY_NTH_ITEM), Strings.EMPTY)
							.replace("(", Strings.EMPTY).replace(")", Strings.EMPTY); //$NON-NLS-1$ //$NON-NLS-2$

						int itemIndex = Services.Strings.tryParseInt(itemIndexStr, 0) - 1; // GX is 1-based.
						if (itemIndex >= 0 && itemIndex < list.size())
							item = list.get(itemIndex);
					}

					if (item == null)
					{
						Services.Log.warning(String.format("Could not get collection item with selector '%s'.", itemSelector)); //$NON-NLS-1$
						return null;
					}

					return item.resolvePropertyContainer(Services.Strings.join(rest, Strings.DOT), false);
				}
				else
					return null; // Unknown component found.
			}
			else
				return null; // Component not found.
		}
	}

	public DataItem getPropertyDefinition(String name)
	{
		// Variables and attributes are not distinct here, so ignore the '&'.
		name = DataItemHelper.getNormalizedName(name);

		// First look up in structure.
		DataItem member = mLevel.getAttribute(name);
		if (member != null)
			return member;

		// Then look up in "extra members" (i.e. variables).
		member = mExtraMembers.get(name);
		if (member != null)
			return member;

		return null;
	}

	private static boolean isSpecialProperty(String name)
	{
		return (name != null &&
				(name.equalsIgnoreCase("gx_md5_hash") ||
				 name.equalsIgnoreCase("gxdynprop") ||
				 name.equalsIgnoreCase("gxdyncall") ||
				 Strings.starsWithIgnoreCase(name, "Gxprops_")));
	}

	/**
	 * Calls super.setProperty(), without trying to parse property name to find components.
	 */
	private boolean baseSetProperty(String name, Object value)
	{
		return super.setProperty(name, value);
	}

	/**
	 * Calls super.getProperty(), without trying to parse property name to find components.
	 */
	private Object baseGetProperty(String name)
	{
		return super.getProperty(name);
	}

	/**
	 * Copies property values from another entity.
	 * @param other Entity to take values from.
	 * @param dataItems True to copy values from structure.
	 * @param extraMembers True to copy values from "extra members" (i.e. variables).
	 * @param allWhereDestinationEmpty True to copy all values from the source where the destination is empty.
	 */
	public void setPropertiesFrom(Entity other, boolean dataItems, boolean extraMembers, boolean allWhereDestinationEmpty)
	{
		if (other == null)
			return;

		 if (mDefinition != null && other.mDefinition != null && !mDefinition.getName().equalsIgnoreCase(other.mDefinition.getName()))
			 return; // Probably copying from the wrong source.

		// Get the list of items to copy.
		HashSet<DataItem> itemsToCopy = new HashSet<DataItem>();
		if (dataItems)
			itemsToCopy.addAll(mDefinition.getItems());
		if (extraMembers)
			itemsToCopy.addAll(mExtraMembers.values());

		if (allWhereDestinationEmpty)
		{
			ArrayList<DataItem> allItems = new ArrayList<DataItem>();
			allItems.addAll(mDefinition.getItems());
			allItems.addAll(mExtraMembers.values());

			for (DataItem item : allItems)
			{
				Object destValue = getProperty(item.getName());
				if (item.isEmptyValue(destValue))
					itemsToCopy.add(item);
			}
		}

		// Copy the selected ones.
		for (DataItem item : itemsToCopy)
		{
			Object itemValue = other.baseGetProperty(item.getName());
			if (itemValue != null)
				baseSetProperty(item.getName(), itemValue);
		}
	}

	public void load(INodeObject obj)
	{
		if (obj != null)
		{
			deserialize(obj);
			m_loaded = true;
		}
	}

	public boolean IsLoaded()
	{
		return m_loaded;
	}

	public void loadHeader(INodeObject obj)
	{
		mLoadingHeader = true;
		deserialize(obj);
		mLoadingHeader = false;
	}

	@Override
	public void deserialize(INodeObject obj)
	{
		if (obj == null)
			return;

		// Deserialize first level, including complex variables, but not levels.
		for (String attName : obj.names())
		{
			try
			{
				DataItem di = (mDefinition != null ? mDefinition.getAttribute(attName) : null);
				if (di != null)
					deserializeValue(attName, obj.get(attName));
				else
					setProperty(attName, obj.getString(attName));
			}
			catch (Exception ex)
			{
				Services.Log.Error(String.format("Exception during deserialization of '%s'.", attName), ex); //$NON-NLS-1$
			}
		}

		if (mLoadingHeader)
			return;

		for(int j = 0; j < mLevel.Levels.size(); j++)
		{
			LevelDefinition level = mLevel.Levels.get(j);
			INodeCollection entities = obj.optCollection(level.getName());

			EntityList items = new EntityList();
			if (entities != null)
			{
				int lenEntities = entities.length();
				for (int i = 0; i < lenEntities ; i++)
				{
					INodeObject entityJson = entities.getNode(i);
					if (entityJson != null)
					{
						Entity entity = new Entity(mDefinition, level, EntityParentInfo.collectionMemberOf(this, level.getName(), items));
						entity.deserialize(entityJson);
						items.AddEntity(entity);
					}
				}
			}
			mLevels.put(level.getName(), items);
		}
	}

	public boolean deserializeValue(String name, Object value)
	{
		Object propertyValue = mSerializer.deserializeValue(name, value);
		if (propertyValue != null)
		{
			// We already know that this is the proper entity and value has been converted, so just assign it.
			baseSetProperty(name, propertyValue);
			return true;
		}
		else
			return false;
	}

	/**
	 * Gets the entity keys
	 * @return String with entity keys concatenated
	 * */
	public String GetKeyString()
	{
		if (mKeyString == null || mKeyString.length() == 0)
		{
			Vector<String> keys = new Vector<String>();
			if (mLevel != null)
			{
				for(DataItem keyAtt : mLevel.GetKeys())
					keys.add((String)getProperty(keyAtt.getName()));

				mKeyString = Services.Strings.join(keys, Strings.COMMA);
			}
		}
		return mKeyString;
	}

	public List<String> getKey()
	{
		Vector<String> keys = new Vector<String>();
		for (DataItem att : mDefinition.Root.GetKeys())
			keys.addElement((String)getProperty(att.getName()));

		return keys;
	}

	public void setKey(List<String> keys)
	{
		List<DataItem> keysList = mDefinition.Root.GetKeys();

		for(int i = 0; i < keysList.size(); i++)
		{
			DataItem att = keysList.get(i);
			if (keys.size() > i)
				setProperty(att.getName(), keys.get(i));
		}
	}

	public StructureDefinition getDefinition()
	{
		return mDefinition;
	}

	public LevelDefinition getLevel()
	{
		return mLevel;
	}

	public INodeObject serialize()
	{
		INodeObject obj = Services.Serializer.createNode();

		// serialize only first level
		for (String key : getPropertyNames())
		{
			// ignore att that doesn't exist in the structure definition.
			if (!key.equalsIgnoreCase("gx_md5_hash") && mLevel.getAttribute(key) == null) //$NON-NLS-1$
			{
				if (getPropertyDefinition(key) == null && !mLevels.containsKey(key)) // Only warn if it's not a variable or level.
					Services.Log.warning("entitySerialize", key + " is not present in the structure"); //$NON-NLS-1$ //$NON-NLS-2$

				continue;
			}
			if (mLevels.containsKey(key))
				continue;
			// This should not be necesary any more
			if (key.equals("uri")) //$NON-NLS-1$
				continue;
			// Ignore EventFiles of the GxSynchroEvent sdt
			// Temporary fix, this should not be here.
			if (key.equals("EventFiles") && mLevel.getName()!=null && mLevel.getName().equalsIgnoreCase("GxSynchroEventSDT")) //$NON-NLS-1$ //$NON-NLS-2$
				continue;

			obj.put(key, getProperty(key));
		}

		// Serialize sub levels
		for (Map.Entry<String, EntityList> level : mLevels.entrySet())
		{
			INodeCollection arrayLines = Services.Serializer.createCollection();
			for (Entity entity : level.getValue())
			{
				INodeObject objLine = entity.serialize();
				arrayLines.put(objLine);
			}

			obj.put(level.getKey(), arrayLines);
		}

		return obj;
	}

	public boolean isSelected()
	{
		if (Services.Strings.hasValue(mSelectionExpression))
			return optBooleanProperty(mSelectionExpression);
		else
			return mIsSelected;
	}

	public void setIsSelected(boolean value)
	{
		if (Services.Strings.hasValue(mSelectionExpression))
			setProperty(mSelectionExpression, value);
		else
			mIsSelected = value;
	}

	public void setSelectionExpression(String expression)
	{
		mSelectionExpression = expression;
	}

	public interface OnPropertyValueChangeListener
	{
		void onPropertyValueChange(String propertyName, Object oldValue, Object newValue);
	}

	public void setOnPropertyValueChangeListener(OnPropertyValueChangeListener listener)
	{
		mOnPropertyValueChangeListener = listener;
	}

	public Object getTag(String key)
	{
		return mTags.get(key);
	}

	public void setTag(String key, Object value)
	{
		mTags.put(key, value);
	}
}
