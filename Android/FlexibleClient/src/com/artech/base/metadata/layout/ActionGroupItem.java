package com.artech.base.metadata.layout;

import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class ActionGroupItem
{
	public static final int TYPE_ACTION = 1;
	public static final int TYPE_DATA = 2;
	public static final int TYPE_GROUP = 3;
	public static final int TYPE_SEARCH = 4;

	static ActionGroupItemDefinition create(ActionGroupDefinition parent, INodeObject json)
	{
		if (json == null)
			return null;

		if (Strings.hasValue(json.optString("@actionElement")))
			return new ActionGroupActionDefinition(parent, json);

		INodeObject jsonSearch = json.optNode("search");
		if (jsonSearch != null)
			return new ActionGroupSearchDefinition(parent, jsonSearch);

		INodeObject jsonData = json.optNode("data");
		if (jsonData != null)
			return new ActionGroupDataItemDefinition(parent, jsonData);

		INodeObject jsonGroup = json.optNode("actionGroup");
		if (jsonGroup != null)
			return new ActionGroupSubgroupDefinition(parent, jsonGroup);

		Services.Log.warning("Unknown member in action group: " + json.toString());
		return null;
	}
}
