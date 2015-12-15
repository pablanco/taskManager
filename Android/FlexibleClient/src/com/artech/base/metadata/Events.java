package com.artech.base.metadata;

import com.artech.base.utils.Strings;

public class Events
{
	public static final String CLIENT_START = "ClientStart";
	public static final String BACK = "Back";

	static ActionDefinition find(Iterable<ActionDefinition> list, String name)
	{
		if (list != null && Strings.hasValue(name))
		{
			for (ActionDefinition action : list)
			{
				if (action.getName().equalsIgnoreCase(name))
					return action;
			}
		}

		return null;
	}
}
