package com.artech.actions;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.model.Entity;

/**
 * Custom action used to include a specific method call (via a Runnable) inside a composite.
 * @author matiash
 */
public class RunnableAction extends Action
{
	private final Runnable mRunnable;

	public RunnableAction(UIContext context, Runnable runnable)
	{
		super(context, new ActionDefinition(null), new ActionParameters(new Entity(StructureDefinition.EMPTY)));
		mRunnable = runnable;
	}

	public RunnableAction(UIContext context, Runnable runnable, ActionDefinition actionDef, ActionParameters parms)
	{
		super(context, actionDef, parms);
		mRunnable = runnable;
	}

	@Override
	public boolean Do()
	{
		mRunnable.run();
		return true;
	}
}
