package com.artech.ui;

import java.util.LinkedList;
import java.util.Queue;

import android.util.Pair;

import com.artech.actions.UIContext;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.fragments.IDataView;

public class FormCoordinator extends CoordinatorBase
{
	private final IDataView mDataView;
	private final Queue<Pair<ActionDefinition, Anchor>> mPendingActions;

	public FormCoordinator(UIContext context, IDataView dataView)
	{
		super(context);
		mDataView = dataView;
		mPendingActions = new LinkedList<Pair<ActionDefinition, Anchor>>();
	}

	@Override
	protected IViewDefinition getContainerDefinition()
	{
		return mDataView.getDefinition();
	}

	@Override
	public void setData(Entity data)
	{
		boolean isFirst = (getData() == null);
		super.setData(data);

		if (data != null)
		{
			if (isFirst)
				runPendingActions();
		}
		else
			mPendingActions.clear();
	}

	@Override
	public boolean runAction(ActionDefinition action, Anchor anchor)
	{
		if (getData() == null)
		{
			// Delay actions (e.g. control events) until the coordinator has data.
			mPendingActions.add(new Pair<ActionDefinition, Anchor>(action, anchor));
		}
		else
			mDataView.runAction(action, anchor);

		return true;
	}

	private void runPendingActions()
	{
		while (!mPendingActions.isEmpty())
		{
			final Pair<ActionDefinition, Anchor> item = mPendingActions.remove();
			Services.Device.postOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					runAction(item.first, item.second);
				}
			});
		}
	}
}