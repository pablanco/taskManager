package com.artech.base.model;

import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;

import com.artech.base.serialization.INodeCollection;
import com.artech.base.services.IEntityList;
import com.artech.base.services.Services;

public class EntityList extends Vector<Entity> implements IEntityList
{
	private static final long serialVersionUID = 1L;
	private Hashtable<String, Entity> mEntities = new Hashtable<String, Entity>();
	private Entity mCurrentEntity;
	private UUID mVersion;

	public EntityList()
	{
		super();
		updateVersion();
	}

	public EntityList(EntityList other)
	{
		this();
		for (int i = 0; i < other.size(); i++)
			AddEntity(other.get(i));

		mVersion = other.mVersion;
	}

	public EntityList(Iterable<Entity> other)
	{
		this();
		for (Entity entity : other)
			AddEntity(entity);
	}

	public UUID getVersion() { return mVersion; }

	@Override
	public String toString()
	{
		return serialize().toString();
	}

	public void AddEntity(Entity entity)
	{
		String key = entity.GetKeyString();

		if (key != null && key.length() > 0)
		{
			if (!mEntities.containsKey(key))
			{
				mEntities.put(key, entity);
				addElement(entity);
			}
		}
		else
		{
			key = String.valueOf(mEntities.size() + 1);
			mEntities.put(key, entity);
			addElement(entity);
		}

		updateVersion();
	}

	public INodeCollection serialize()
	{
		INodeCollection collection = Services.Serializer.createCollection();
		for (Entity entity : this)
			collection.put(entity.serialize());

		return collection;
	}

	public void setCurrentEntity(Entity entity)
	{
		// Used to resolve the <collection>.CurrentItem expression.
		mCurrentEntity = entity;
	}

	public Entity getCurrentEntity()
	{
		// Used to resolve the <collection>.CurrentItem expression.
		return mCurrentEntity;
	}

	private void updateVersion()
	{
		mVersion = UUID.randomUUID();
	}
}
