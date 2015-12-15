package com.artech.base.metadata.layout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.serialization.INodeObject;
import com.artech.base.utils.PlatformHelper;

public class ActionGroupDefinition implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private final LayoutDefinition mLayout;
	private final List<ActionGroupItemDefinition> mItems;
	private String mControlName;
	private String mControlType;
	protected String mThemeClass;
	private String mCaption;

	ActionGroupDefinition(LayoutDefinition layout, INodeObject json)
	{
		mLayout = layout;
		mItems = new ArrayList<ActionGroupItemDefinition>();
		mThemeClass = null;

		deserialize(json);
	}

	private void deserialize(INodeObject json)
	{
		if (json != null)
		{
			mControlName = json.optString("@controlName"); //$NON-NLS-1$
			mControlType = json.optString("@controlType"); //$NON-NLS-1$
			mThemeClass = json.optString("@class"); //$NON-NLS-1$
			mCaption = json.optString("@caption"); //$NON-NLS-1$

			for (INodeObject jsonActionBarItem : json.optCollection("action")) //$NON-NLS-1$
			{
				ActionGroupItemDefinition item = ActionGroupItem.create(this, jsonActionBarItem);
				if (item != null)
					mItems.add(item);
			}
		}
	}

	public LayoutDefinition getLayout()
	{
		return mLayout;
	}

	public String getName()
	{
		return mControlName;
	}

	public String getControlType()
	{
		return mControlType;
	}

	public List<ActionGroupItemDefinition> getItems()
	{
		return mItems;
	}

	public ThemeClassDefinition getThemeClass()
	{
		return PlatformHelper.getThemeClass(mThemeClass);
	}

	public String getCaption()
	{
		return mCaption;
	}

	public Iterable<ActionGroupActionDefinition> getActions()
	{
		ArrayList<ActionGroupActionDefinition> actions = new ArrayList<ActionGroupActionDefinition>();
		for (ActionGroupItemDefinition item : getItems())
		{
			if (item.getType() == ActionGroupItem.TYPE_ACTION)
				actions.add((ActionGroupActionDefinition)item);
		}

		return actions;
	}
}
