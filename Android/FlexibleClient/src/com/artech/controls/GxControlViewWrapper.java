package com.artech.controls;

import java.util.List;

import android.view.View;

import com.artech.android.layout.GxTheme;
import com.artech.android.layout.LayoutTag;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.controls.IGxControlRuntimeContext;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.utils.Strings;
import com.artech.common.ExecutionContext;
import com.artech.utils.KeyboardUtils;

/**
 * Wrapper class to treat generic Views as an IGxControl. This is a stopgap measure.
 * Actually, DataBoundControl, GxLayout, GxImageView, user controls, &c should implement IGxControl.
 */
public class GxControlViewWrapper implements IGxControl
{
	private final View mView;

	public GxControlViewWrapper(View view)
	{
		mView = view;
	}

	@Override
	public String getName()
	{
		Object tag = mView.getTag(LayoutTag.CONTROL_NAME);
		return (tag != null ? tag.toString() : "<Unknown>"); //$NON-NLS-1$
	}

	@Override
	public LayoutItemDefinition getDefinition()
	{
		return (LayoutItemDefinition)mView.getTag(LayoutTag.CONTROL_DEFINITION);
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		mView.setEnabled(enabled);
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		if (mView instanceof IGxThemeable)
			GxTheme.applyStyle((IGxThemeable)mView, themeClass);
	}

	@Override
	public void setVisible(boolean visible)
	{
		mView.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	@Override
	public void requestLayout()
	{
		mView.requestLayout();
	}

	@Override
	public void setFocus(boolean showKeyboard)
	{
		// Focus & show keyboard.
		mView.requestFocus();

		if (showKeyboard)
		{
			View viewForInput = mView;
			if (mView instanceof DataBoundControl && ((DataBoundControl)mView).getEdit() instanceof View)
				viewForInput = (View)((DataBoundControl)mView).getEdit();

			KeyboardUtils.showKeyboard(viewForInput);
		}
	}

	@Override
	public void setCaption(String caption)
	{
		// For now only set caption to GxButton and TextBlock controls, should work in other controls?
		if (mView instanceof GxButton)
			((GxButton)mView).setCaption(caption);

		if (mView instanceof GxTextBlockTextView)
			((GxTextBlockTextView)mView).setCaption(caption);

		if (mView instanceof DataBoundControl)
		{
			((DataBoundControl) mView).setCaption(caption);
		}
	}

	@Override
	public boolean isEnabled()
	{
		if (mView instanceof IGxEdit)
			return ((IGxEdit)mView).isEditable();
		else
			return mView.isEnabled();
	}

	@Override
	public ThemeClassDefinition getThemeClass()
	{
		if (mView instanceof IGxThemeable)
			return ((IGxThemeable)mView).getThemeClass();
		else
			return (ThemeClassDefinition)mView.getTag(LayoutTag.CONTROL_THEME_CLASS);
	}

	@Override
	public boolean isVisible()
	{
		return (mView.getVisibility() == View.VISIBLE);
	}

	@Override
	public String getCaption()
	{
		// For now only set caption to GxButton and TextBlock controls, should work in other controls?
		if (mView instanceof GxButton)
			((GxButton)mView).getCaption();

		if (mView instanceof GxTextBlockTextView)
			return ((GxTextBlockTextView)mView).getText().toString();

		if (mView instanceof DataBoundControl)
		{
			GxTextView dataLabel = ((DataBoundControl)mView).getLabel();
			if (dataLabel != null)
				return dataLabel.getText().toString();
		}

		return Strings.EMPTY;
	}

	@Override
	public void setExecutionContext(ExecutionContext context)
	{
		if (mView instanceof IGxControlRuntimeContext)
			((IGxControlRuntimeContext)mView).setExecutionContext(context);
	}

	@Override
	public Object getProperty(String name)
	{
		if (mView instanceof IGxControlRuntime)
			return ((IGxControlRuntime)mView).getProperty(name);
		else
			return null;
	}

	@Override
	public void setProperty(String name, Object value)
	{
		if (mView instanceof IGxControlRuntime)
			((IGxControlRuntime)mView).setProperty(name, value);
	}

	@Override
	public void runMethod(String name, List<Object> parameters)
	{
		if (mView instanceof IGxControlRuntime)
			((IGxControlRuntime)mView).runMethod(name, parameters);
	}
}
