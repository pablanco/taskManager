package com.artech.base.metadata.layout;

import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;

public class ActionGroupSearchDefinition extends ActionGroupItemDefinition
{
	private static final long serialVersionUID = 1L;

	private final String mInviteMessage;

	public ActionGroupSearchDefinition(ActionGroupDefinition parent, INodeObject json)
	{
		super(parent, json);
		mInviteMessage = json.optString("@inviteMessage");
	}

	@Override
	public int getType()
	{
		return ActionGroupItem.TYPE_SEARCH;
	}

	@Override
	public String getCaption()
	{
		return Services.Resources.getTranslation(mInviteMessage);
	}

	@Override
	public ThemeClassDefinition getThemeClass()
	{
		return null;
	}
}
