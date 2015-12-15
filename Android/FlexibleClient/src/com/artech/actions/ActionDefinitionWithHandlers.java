package com.artech.actions;

import com.artech.base.metadata.ActionDefinition;

public class ActionDefinitionWithHandlers extends ActionDefinition
{
	private static final long serialVersionUID = 1L;

	private final ActionDefinition mDefinition;
	private final Runnable mPreHandler;
	private final Runnable mPostHandler;

	public ActionDefinitionWithHandlers(ActionDefinition def, Runnable preHandler, Runnable postHandler)
	{
		super(null);
		mDefinition = def;
		mPreHandler = preHandler;
		mPostHandler = postHandler;
	}

	public ActionDefinition getDefinition()
	{
		return mDefinition;
	}

	public Runnable getPreHandler()
	{
		return mPreHandler;
	}

	public Runnable getPostHandler()
	{
		return mPostHandler;
	}
}
