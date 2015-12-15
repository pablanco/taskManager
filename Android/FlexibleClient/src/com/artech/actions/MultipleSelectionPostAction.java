package com.artech.actions;

import com.artech.base.services.Services;
import com.artech.controls.IGxGridControl;

public class MultipleSelectionPostAction extends Action
{
	private final MultipleSelectionAction mAction;

	protected MultipleSelectionPostAction(MultipleSelectionAction action)
	{
		super(action.getContext(), action.getDefinition(), action.getParameters());
		mAction = action;
	}

	@Override
	public boolean Do()
	{
		// This action corresponds to the final step of a selection action.
		// Either clear selection (if selection mode = "always") or end selection (if "on demand").
		final IGxGridControl grid = mAction.getGrid();

		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				grid.setSelectionMode(false, null);
			}
		});

		return true;
	}
}
