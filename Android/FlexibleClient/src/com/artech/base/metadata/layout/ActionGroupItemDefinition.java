package com.artech.base.metadata.layout;

import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.PropertiesObject;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

public abstract class ActionGroupItemDefinition extends PropertiesObject
{
	private static final long serialVersionUID = 1L;

	private final ActionGroupDefinition mParent;
	private final String mControlName;

	public static final int PRIORITY_LOW = 1;
	public static final int PRIORITY_NORMAL = 2;
	public static final int PRIORITY_HIGH = 3;

	private static final String STR_PRIORITY_LOW = "Low"; //$NON-NLS-1$
	private static final String STR_PRIORITY_NORMAL = "Normal"; //$NON-NLS-1$
	private static final String STR_PRIORITY_HIGH = "High"; //$NON-NLS-1$

	public ActionGroupItemDefinition(ActionGroupDefinition parent, INodeObject json)
	{
		mParent = parent;
		mControlName = json.optString("@controlName");

		deserialize(json);
	}

	public abstract int getType();

	protected ActionGroupDefinition getParent()
	{
		return mParent;
	}

	public String getControlName()
	{
		return mControlName;
	}

	public boolean isEnabled()
	{
		return getBooleanProperty("@enabled", true);
	}

	public boolean isVisible()
	{
		return getBooleanProperty("@visible", true);
	}

	public abstract String getCaption();

	public abstract ThemeClassDefinition getThemeClass();

	public int getPriority()
	{
		String strPriority = getPriorityValue();

		if (STR_PRIORITY_LOW.equalsIgnoreCase(strPriority))
			return PRIORITY_LOW;
		else if (STR_PRIORITY_NORMAL.equalsIgnoreCase(strPriority))
			return PRIORITY_NORMAL;
		else if (STR_PRIORITY_HIGH.equalsIgnoreCase(strPriority))
			return PRIORITY_HIGH;

		Services.Log.warning(String.format("Unknown priority value (%s) in action bar item '%s'.", strPriority, getControlName())); //$NON-NLS-1$
		return PRIORITY_NORMAL;
	}

	protected String getPriorityValue()
	{
		 return optStringProperty("@priority"); //$NON-NLS-1$
	}
}
