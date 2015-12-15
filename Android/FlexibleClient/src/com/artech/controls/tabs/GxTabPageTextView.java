package com.artech.controls.tabs;

import java.util.List;

import android.content.Context;
import android.text.Html;
import android.widget.TextView;

import com.artech.android.layout.LayoutControlFactory;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.TabControlThemeClassDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.Strings;
import com.artech.common.ExecutionContext;
import com.artech.controls.IGxControl;
import com.artech.controls.IGxThemeable;
import com.artech.controls.tabs.GxTabControl.TabItemInfo;

class GxTabPageTextView extends TextView implements IGxControl, IGxControlRuntime, IGxThemeable
{
	private final GxTabControl mParent;
	private final TabItemInfo mTabItemInfo;
	
	private ThemeClassDefinition mUnselectedClass;
	private ThemeClassDefinition mSelectedClass;
	private ThemeClassDefinition mCurrentlyAppliedClass;

	private static final String PROPERTY_SELECTED_CLASS = "SelectedClass";

	public GxTabPageTextView(Context context, GxTabControl parent, TabItemInfo itemInfo)
	{
		super(context);
		mParent = parent;
		mTabItemInfo = itemInfo;
		LayoutControlFactory.setDefinition(this, itemInfo.definition);
		
		mSelectedClass = itemInfo.definition.getSelectedClass();
		mUnselectedClass = itemInfo.definition.getUnselectedClass();
	}

	@Override
	public String getName()
	{
		return mTabItemInfo.definition.getName();
	}

	@Override
	public LayoutItemDefinition getDefinition()
	{
		return mTabItemInfo.definition;
	}

	@Override
	public boolean isVisible()
	{
		return mTabItemInfo.visible;
	}

	@Override
	public void setVisible(boolean visible)
	{
		if (mTabItemInfo.visible != visible)
		{
			mTabItemInfo.visible = visible;
			mParent.updateVisibleTabItemsList();
			mParent.notifyTabsChanged();
		}
	}
	
	@Override
	public String getCaption()
	{
		return getText().toString();
	}

	@Override
	public void setCaption(String caption)
	{
		setText(Html.fromHtml(caption));
	}

    @Override
	public void setSelected(boolean selected)
    {
    	super.setSelected(selected);
    	applyThemeClasses();
    }
	
	@Override
	public ThemeClassDefinition getThemeClass()
	{
		return mUnselectedClass;
	}
	
	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		mUnselectedClass = themeClass;
		applyThemeClasses();
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
	}

	public void applyParentThemeClass(TabControlThemeClassDefinition tabControlClass)
	{
		if (tabControlClass == null)
			return;

		// The parent class is applied ONLY if the child does not have selected/unselected classes itself.
		if (mTabItemInfo.definition.getUnselectedClass() == null)
			mUnselectedClass = tabControlClass.getUnselectedPageClass();
		
		if (mTabItemInfo.definition.getSelectedClass() == null)
			mSelectedClass = tabControlClass.getSelectedPageClass();

		applyThemeClasses();
	}
	
	public void applyThemeClasses()
	{
		ThemeClassDefinition appliedClass = TabUtils.applyTabItemClass(this, mUnselectedClass, mSelectedClass, mCurrentlyAppliedClass);
		mCurrentlyAppliedClass = appliedClass;
	}
	
	@Override
	public Object getProperty(String name)
	{
		if (PROPERTY_SELECTED_CLASS.equalsIgnoreCase(name))
			return (mSelectedClass != null ? mSelectedClass.getName() : Strings.EMPTY);
		
		return null;
	}

	@Override
	public void setProperty(String name, Object value)
	{
		if (PROPERTY_SELECTED_CLASS.equalsIgnoreCase(name))
		{
			mSelectedClass = PlatformHelper.getThemeClass(String.valueOf(value));
			applyThemeClasses();
		}
	}

	@Override
	public void runMethod(String name, List<Object> parameters)
	{
		// No custom methods now.
	}
	
	@Override
	public void setFocus(boolean showKeyboard) { }

	@Override
	public void setExecutionContext(ExecutionContext context) { }
}
