package com.artech.android.layout;

import com.artech.R;

public class LayoutTag
{
	/**
	 * Tag id for the control name of a view.
	 * Use with view.getTag(key) or ViewHierarchyVisitor.findViewByTag(view, key, value).
	 */
	public static final int CONTROL_NAME = R.id.tag_control_name;

	/**
	 * Tag id for the LayoutItemDefinition of a view.
	 * Use with view.getTag(key) or ViewHierarchyVisitor.findViewByTag(view, key, value).
	 */
	public static final int CONTROL_DEFINITION = R.id.tag_control_definition;

	/**
	 * Tag id for the current ThemeClassDefinition of a view.
	 * Use with view.getTag(key) or ViewHierarchyVisitor.findViewByTag(view, key, value).
	 */
	public static final int CONTROL_THEME_CLASS = R.id.tag_control_theme_class;

	/**
	 * Tag id for the prompt information associated to a control with a prompt rule.
	 */
	public static final int CONTROL_PROMPT_INFO = R.id.tag_control_prompt_info;
}
