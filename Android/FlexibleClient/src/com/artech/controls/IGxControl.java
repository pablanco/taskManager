package com.artech.controls;

import java.util.List;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.common.ExecutionContext;

/**
 * Interface for controls in a form.
 */
public interface IGxControl
{
	String getName();
	LayoutItemDefinition getDefinition();

	void setExecutionContext(ExecutionContext context);
	Object getProperty(String name);
	void setProperty(String name, Object value);
	void runMethod(String name, List<Object> parameters);

	boolean isEnabled();
	ThemeClassDefinition getThemeClass();
	boolean isVisible();
	String getCaption();

	void setEnabled(boolean enabled);
	void setFocus(boolean showKeyboard);
	void setThemeClass(ThemeClassDefinition themeClass);
	void setVisible(boolean visible);
	void setCaption(String caption);

	void requestLayout();

	// Gets the underlying View associated to this control. Use as sparingly as possible!
	//View getView();
}
