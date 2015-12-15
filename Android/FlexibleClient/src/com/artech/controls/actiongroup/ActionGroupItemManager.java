package com.artech.controls.actiongroup;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.layout.ActionGroupDefinition;
import com.artech.base.metadata.layout.ActionGroupItem;
import com.artech.base.metadata.layout.ActionGroupItemDefinition;
import com.artech.base.metadata.layout.ActionGroupSubgroupDefinition;
import com.artech.controls.IGxControl;

abstract class ActionGroupItemManager<TItemControl extends ActionGroupBaseItemControl<TItemControl>>
{
	private final ArrayList<TItemControl> mAllItems;
	private final ArrayList<TItemControl> mMainItems;

	public ActionGroupItemManager()
	{
		// MainItems are those directly in the group. AllItems includes those in subgroups.
		mAllItems = new ArrayList<TItemControl>();
		mMainItems = new ArrayList<TItemControl>();
	}

	public void setDefinition(ActionGroupDefinition definition)
	{
		mAllItems.clear();
		mMainItems.clear();
		initializeControls(definition, mMainItems);
	}

	private void initializeControls(ActionGroupDefinition group, List<TItemControl> parentChildren)
	{
		// For now only actions and subgroups are supported.
		for (ActionGroupItemDefinition groupItem : group.getItems())
		{
			if (groupItem.getType() == ActionGroupItem.TYPE_ACTION)
			{
				TItemControl groupItemControl = newItemControl(groupItem, mAllItems.size());

				mAllItems.add(groupItemControl);
				parentChildren.add(groupItemControl);
			}
			else if (groupItem.getType() == ActionGroupItem.TYPE_GROUP)
			{
				// Add the submenu itself.
				ActionGroupSubgroupDefinition subgroup = (ActionGroupSubgroupDefinition)groupItem;
				TItemControl subgroupControl = newItemControl(subgroup, mAllItems.size());

				mAllItems.add(subgroupControl);
				parentChildren.add(subgroupControl);

				// Read subitems.
				initializeControls(subgroup.getGroup(), subgroupControl.getSubItems());
			}
		}
	}

	protected abstract TItemControl newItemControl(ActionGroupItemDefinition definition, int id);

	protected List<TItemControl> getMainItems()
	{
		return mMainItems;
	}

	protected List<TItemControl> getAllItems()
	{
		return mAllItems;
	}

	public ActionDefinition getItemEvent(int id)
	{
		TItemControl itemControl = getItem(id);
		if (itemControl != null && itemControl.getAction() != null)
			return itemControl.getAction().getEvent();
		else
			return null;
	}

	protected TItemControl getItem(int id)
	{
		if (id < mAllItems.size())
			return mAllItems.get(id);
		else
			return null;
	}

	public IGxControl getControl(String name)
	{
		for (TItemControl control : mAllItems)
		{
			if (control.getName().equalsIgnoreCase(name))
				return control;
		}

		return null;
	}
}
