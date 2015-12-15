package com.artech.base.model;

import java.io.Serializable;

public class EntityParentInfo implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private final Entity mParentEntity;
	private final EntityList mParentCollection;
	private final boolean mIsMember;
	private final String mMemberName;

	private EntityParentInfo(Entity parentEntity, EntityList parentCollection, String memberName)
	{
		mParentEntity = parentEntity;
		mParentCollection = parentCollection;
		mIsMember = (memberName != null);
		mMemberName = memberName;
	}

	static final EntityParentInfo NONE = new EntityParentInfo(null, null, null);

	static EntityParentInfo memberOf(Entity parent, String name)
	{
		return new EntityParentInfo(parent, null, name);
	}

	static EntityParentInfo collectionMemberOf(Entity parent, String collectionName, EntityList collection)
	{
		return new EntityParentInfo(parent, collection, collectionName);
	}

	public static EntityParentInfo subordinatedProviderOf(Entity rootData)
	{
		return new EntityParentInfo(rootData, null, null);
	}

	public Entity getParent() { return mParentEntity; }
	public EntityList getParentCollection() { return mParentCollection; }
	public boolean isMember() { return mIsMember; }
	public String getMemberName() { return mMemberName; }
}
