package com.artech.controls;

import java.util.List;

import android.app.Activity;
import android.text.Html;

import com.artech.activities.DataViewHelper;
import com.artech.activities.IGxActivity;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.common.ExecutionContext;
import com.artech.fragments.IDataView;

public class FormControl implements IGxControl, IGxControlRuntime
{
	public static final String CONTROL_NAME = "Form";
	private final Activity mActivity;
	private final IDataView mFromDataView;

	public FormControl(Activity activity, IDataView fromDataView)
	{
		mActivity = activity;
		mFromDataView = fromDataView;
	}

	private static final String METHOD_REFRESH = "Refresh";

	@Override
	public void runMethod(String name, List<Object> parameters)
	{
		if (METHOD_REFRESH.equalsIgnoreCase(name))
		{
			// Form.Refresh() means refresh EVERYTHING, ignoring the calling component.
			if (mActivity instanceof IGxActivity)
				((IGxActivity)mActivity).refreshData(false);
		}
	}

	@Override
	public String getName() { return CONTROL_NAME; }

	@Override
	public LayoutItemDefinition getDefinition() { return null; }

	@Override
	public void setEnabled(boolean enabled) { }

	@Override
	public void setFocus(boolean showKeyboard) { }

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass) { }

	@Override
	public void setVisible(boolean visible) { }

	@Override
	public void requestLayout() { }

	@Override
	public void setCaption(String caption)
	{
		// Hack to support (old) DetailActivity, sets child title if only one child is shown.
		DataViewHelper.setTitle(mActivity, mFromDataView, Html.fromHtml(caption.trim()));
	}

	@Override
	public void setExecutionContext(ExecutionContext context) { }

	@Override
	public Object getProperty(String name) { return null; }

	@Override
	public void setProperty(String name, Object value) { }

	@Override
	public boolean isEnabled() { return true; }

	@Override
	public ThemeClassDefinition getThemeClass() { return null; }

	@Override
	public boolean isVisible() { return true; }

	@Override
	public String getCaption() { return mActivity.getTitle().toString(); }
}
