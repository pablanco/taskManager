package com.artech.actions;

import com.artech.base.services.Services;
import com.artech.controls.IGxGridControl;

class MultipleSelectionPreAction extends Action
{
	private final MultipleSelectionAction mAction;

	protected MultipleSelectionPreAction(MultipleSelectionAction action)
	{
		super(action.getContext(), action.getDefinition(), action.getParameters());
		mAction = action;
	}

	@Override
	public boolean Do()
	{
		// This action corresponds to the first step of a "selection on demand" action.
		// Therefore, turn on selection in grid.
		final IGxGridControl grid = mAction.getGrid();

		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				grid.setSelectionMode(true, mAction.getDefinition());
			}
		});

		return true;
	}

	@Override
	public boolean catchOnActivityResult()
	{
		return true; // On continuation, go to corresponding MultipleSelectionAction.
	}
}
