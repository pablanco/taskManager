package com.artech.fragments;

import java.util.List;

import android.content.Context;
import android.view.ViewParent;
import android.widget.LinearLayout;

import com.artech.android.layout.GxLayoutInTab;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.layout.ComponentDefinition;
import com.artech.base.metadata.layout.Size;

public class ComponentContainer extends LinearLayout implements IGxControlRuntime
{
	//status
	public static final int INACTIVE = 0;
	public static final int TOACTIVATED = 1;
	public static final int ACTIVE = 2;
	public static final int TOINACTIVATED = 3;

	private static final String METHOD_REFRESH = "Refresh";
	private static final String METHOD_CLEAR = "Clear";

	private static int sLastId = 95;

	private final ComponentDefinition mDefinition;

	private int mId;
	private LayoutFragment mParentFragment;
	private BaseFragment mFragment;
	private Size mSize;
	private int mStatus = INACTIVE;

	public ComponentContainer(Context context, ComponentDefinition definition)
	{
		super(context);
		setWillNotDraw(true);
		mDefinition = definition;
	}

	public ComponentDefinition getDefinition()
	{
		return mDefinition;
	}

	public void setComponentSize(Size size)
	{
		mSize = size;
	}

	public Size getComponentSize()
	{
		return mSize;
	}

	public int getContentControlId()
	{
		if (mId==0)
		{
			mId = sLastId;
			sLastId++;
		}
		return mId;
	}

	public int getStatus()
	{
		return mStatus;
	}

	public void setStatus(int status)
	{
		mStatus = status;
	}

	public void setActive(boolean active)
	{
		if (mFragment != null)
			mFragment.setActive(active);

		setStatus(active ? ACTIVE : INACTIVE);
	}

	public BaseFragment getFragment()
	{
		return mFragment;
	}

	public void setFragment(BaseFragment fragment)
	{
		mFragment = fragment;
	}

	public LayoutFragment getParentFragment()
	{
		return mParentFragment;
	}

	public void setParentFragment(LayoutFragment fragment)
	{
		mParentFragment = fragment;
	}

	public boolean hasTabParent()
	{
		return (getTabParent(getParent()) != null);
	}

	public boolean hasTabParentWithScroll()
	{
		GxLayoutInTab gxLayout = getTabParent(getParent());
		return gxLayout != null && gxLayout.getHasScroll();
	}

	private GxLayoutInTab getTabParent(ViewParent parent)
	{
		if (parent != null)
		{
			if (parent instanceof GxLayoutInTab)
				return (GxLayoutInTab) parent;

			return getTabParent(parent.getParent());
		}
		else
			return null;
	}

	public boolean hasTabParentDisconected()
	{
		if (getParent()!=null)
		{
			GxLayoutInTab tabParent = getTabParent(getParent().getParent());
			if (tabParent!=null && tabParent.getParent()==null)
				return true;
		}
		return false;
	}

	@Override
	public void setProperty(String name, Object value) { }

	@Override
	public Object getProperty(String name)
	{
		return null;
	}

	@Override
	public void runMethod(String name, List<Object> parameters)
	{
		if (METHOD_REFRESH.equalsIgnoreCase(name))
		{
			if (mFragment instanceof IDataView)
				((IDataView)mFragment).refreshData(false);
		}
		else if (METHOD_CLEAR.equalsIgnoreCase(name))
		{
			if (mParentFragment != null && mFragment != null)
				mParentFragment.removeInnerComponent(this);
		}
	}
}
