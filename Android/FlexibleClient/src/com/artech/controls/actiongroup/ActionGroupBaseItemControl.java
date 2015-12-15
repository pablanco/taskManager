package com.artech.controls.actiongroup;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.layout.ActionGroupItemDefinition;
import com.artech.base.metadata.layout.ILayoutActionDefinition;
import com.artech.controls.GxControlBase;
import com.artech.utils.Cast;

abstract class ActionGroupBaseItemControl<TItemControl extends ActionGroupBaseItemControl<TItemControl>> extends GxControlBase
{
	private final ActionGroupItemDefinition mDefinition;
	private final ArrayList<TItemControl> mSubItems;
	private int mPriority;

	public ActionGroupBaseItemControl(ActionGroupItemDefinition definition)
	{
		mDefinition = definition;
		setEnabled(definition.isEnabled());
		setCaption(definition.getCaption());
		setVisible(definition.isVisible());
		setThemeClass(definition.getThemeClass());
		setPriority(definition.getPriority());

		mSubItems = new ArrayList<TItemControl>();
	}

	public int getType()
	{
		return mDefinition.getType();
	}

	public ILayoutActionDefinition getAction()
	{
		return Cast.as(ILayoutActionDefinition.class, mDefinition);
	}

	public List<TItemControl> getSubItems()
	{
		return mSubItems;
	}

	@Override
	public String getName()
	{
		return mDefinition.getControlName();
	}

	public int getPriority()
	{
		return mPriority;
	}

	public void setPriority(int priority)
	{
		mPriority = priority;
	}
}
