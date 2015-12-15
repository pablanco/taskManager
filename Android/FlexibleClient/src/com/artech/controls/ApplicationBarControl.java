package com.artech.controls;

import java.util.List;

import android.app.Activity;
import android.view.View;

import com.artech.R;
import com.artech.activities.ActivityHelper;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeApplicationBarClassDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.common.ExecutionContext;
import com.artech.utils.Cast;

public class ApplicationBarControl implements IGxControl
{
	public static final String CONTROL_NAME = "ApplicationBar";
	private final Activity mActivity;

	public ApplicationBarControl(Activity activity)
	{
		mActivity = activity;
	}

	@Override
	public String getName()
	{
		return CONTROL_NAME;
	}

	@Override
	public LayoutItemDefinition getDefinition()
	{
		return null;
	}

	@Override
	public boolean isVisible()
	{
		return ActivityHelper.hasActionBar(mActivity);
	}

	@Override
	public void setVisible(boolean visible)
	{
		ActivityHelper.setActionBarVisibility(mActivity, visible);
	}

	@Override
	public ThemeClassDefinition getThemeClass()
	{
		return ActivityHelper.getActionBarThemeClass(mActivity);
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		ActivityHelper.setActionBarThemeClass(mActivity, Cast.as(ThemeApplicationBarClassDefinition.class, themeClass));
	}

	@Override
	public void setExecutionContext(ExecutionContext context) { }

	@Override
	public Object getProperty(String name) { return null; }

	@Override
	public void setProperty(String name, Object value) { }

	@Override
	public void runMethod(String name, List<Object> parameters) { }

	@Override
	public boolean isEnabled() { return true; }

	@Override
	public String getCaption() { return null; }

	@Override
	public void setEnabled(boolean enabled) { }

	@Override
	public void setFocus(boolean showKeyboard) { }

	@Override
	public void setCaption(String caption) { }

	@Override
	public void requestLayout() { }

	public View getView() {
		return mActivity.findViewById(R.id.action_bar_container);
	}
}
