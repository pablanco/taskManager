package com.artech.ui;

import com.artech.actions.UIContext;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.model.Entity;
import com.artech.controls.grids.GridHelper;

public class GridItemCoordinator extends CoordinatorBase
{
	private final GridHelper mHelper;

	public GridItemCoordinator(UIContext context, GridHelper helper, Entity itemData)
	{
		super(context);
		mHelper = helper;
		setData(itemData);
	}

	@Override
	protected IViewDefinition getContainerDefinition()
	{
		return mHelper.getDefinition().getLayout().getParent();
	}

	@Override
	public boolean runAction(ActionDefinition action, Anchor anchor)
	{
		return mHelper.runAction(action, getData(), anchor);
	}
}
