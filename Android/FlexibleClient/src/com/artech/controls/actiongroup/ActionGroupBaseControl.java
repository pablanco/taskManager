package com.artech.controls.actiongroup;

import java.util.List;

import android.app.Activity;

import com.artech.actions.UIContext;
import com.artech.activities.ActivityController;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.layout.ActionGroupDefinition;
import com.artech.common.ExecutionContext;
import com.artech.controls.GxControlBase;
import com.artech.controls.IGxControl;
import com.artech.fragments.IDataView;

abstract class ActionGroupBaseControl extends GxControlBase
{
	private final static String METHOD_SHOW = "Show";
	private final static String METHOD_HIDE = "Hide";

	private final IDataView mDataView;
	private final ActionGroupDefinition mDefinition;
	private ExecutionContext mCurrentContext;

	protected ActionGroupBaseControl(IDataView dataView, ActionGroupDefinition definition)
	{
		mDataView = dataView;
		mDefinition = definition;
		setCaption(definition.getCaption());
		setThemeClass(definition.getThemeClass());
	}

	@Override
	public String getName()
	{
		return mDefinition.getName();
	}

	@Override
	public void setExecutionContext(ExecutionContext context)
	{
		mCurrentContext = context;
	}

	@Override
	public void runMethod(String name, List<Object> parameters)
	{
		if (METHOD_SHOW.equalsIgnoreCase(name))
			showActionGroup();
		else if (METHOD_HIDE.equalsIgnoreCase(name))
			hideActionGroup();
		else
			super.runMethod(name, parameters);
	}

	protected UIContext getContext()
	{
		if (mCurrentContext != null)
			return mCurrentContext.getUIContext();
		else
			return mDataView.getUIContext();
	}

	protected Activity getActivity()
	{
		return getContext().getActivity();
	}

	protected abstract void showActionGroup();
	protected abstract void hideActionGroup();

	public abstract IGxControl getControl(String name);

	protected void runAction(ActionDefinition action)
	{
		if (action != null)
		{
			if (mCurrentContext != null && mCurrentContext.getData() != null)
			{
				ActivityController controller = mDataView.getController().getParent();
				controller.runAction(mCurrentContext.getUIContext(), action, mCurrentContext.getData());
			}
			else
				mDataView.runAction(action, null);
		}
	}

	public void onCloseDataView()
	{
		// Finish the action group if the data view is closed (e.g. fragment removed).
		hideActionGroup();
		mCurrentContext = null;
	}
}
