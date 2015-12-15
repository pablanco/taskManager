package com.artech.actions;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.model.Entity;

public class ActionParameters
{
	private final List<Entity> mEntities;
	
	public final static ActionParameters EMPTY = new ActionParameters(); 
	
	private ActionParameters()
	{
		mEntities = new ArrayList<Entity>();
	}
	
	public ActionParameters(Entity entity)
	{
		this();
		mEntities.add(entity);
	}
	
	public ActionParameters(List<Entity> entities)
	{
		this();
		mEntities.addAll(entities);
	}

	public List<Entity> getEntities()
	{
		return mEntities;
	}
	
	public Entity getEntity()
	{
		if (!mEntities.isEmpty())
			return mEntities.get(0);
		else
			return null;
	}
}
