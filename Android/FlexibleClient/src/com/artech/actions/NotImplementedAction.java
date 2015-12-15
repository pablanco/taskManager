package com.artech.actions;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.services.Services;

class NotImplementedAction extends Action
{
	public NotImplementedAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
	}
		
	@Override
	public boolean Do()
	{
		Services.Log.Error("NotImplementedAction Do"); //$NON-NLS-1$
		return false;
	}

	@Override
	public boolean catchOnActivityResult() { return false; }
}
