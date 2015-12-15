package com.artech.ui.navigation;

import java.util.List;

import com.artech.actions.UIContext;
import com.artech.app.ComponentParameters;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.ILayoutDefinition;
import com.artech.base.metadata.IViewDefinition;

public class UIObjectCall
{
	private final UIContext mContext;
	private final ComponentParameters mParams;
	private ILayoutDefinition mObjectLayout;

	public UIObjectCall(UIContext context, ComponentParameters params)
	{
		mContext = context;
		mParams = params;
	}

	public UIContext getContext() { return mContext; }
	public IViewDefinition getObject() { return mParams.Object; }
	public short getMode() { return mParams.Mode; }
	public List<String> getParameters() { return mParams.Parameters; }

	public ILayoutDefinition getObjectLayout()
	{
		if (mObjectLayout == null)
			mObjectLayout = getLayoutFromViewDefinition(mParams.Object, mParams.Mode);

		return mObjectLayout;
	}

	public ComponentParameters toComponentParams()
	{
		return mParams;
	}

	public static ILayoutDefinition getLayoutFromViewDefinition(IViewDefinition view, short mode)
	{
		if (view instanceof ILayoutDefinition)
			return (ILayoutDefinition)view; // Dashboards are both view and layout.

		if (view instanceof IDataViewDefinition)
			return ((IDataViewDefinition)view).getLayoutForMode(mode); // Get default layout.

		return null;
	}
}
