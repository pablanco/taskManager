package com.artech.controls;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.IValuesFormatter;
import com.artech.common.FormatHelper;
import com.artech.controls.common.EditInputDescriptions;
import com.artech.controls.common.TextViewFormatter;
import com.artech.controls.utils.TextViewUtils;
import com.artech.ui.Coordinator;
import com.artech.utils.BackgroundOptions;
import com.artech.utils.ThemeUtils;

public class GxTextView extends android.widget.TextView  implements IGxEdit, IGxThemeable
{
	protected LayoutItemDefinition mDefinition;
	private TextViewFormatter mFormatter;
	private ThemeClassDefinition mClassDefinition;

	private String mValue;

	public GxTextView(Context context, LayoutItemDefinition definition)
	{
		this(context, null, definition);
	}

	public GxTextView(Context context, Coordinator coordinator, LayoutItemDefinition definition)
	{
		this(context, coordinator, definition, null);
	}

	public GxTextView(Context context, Coordinator coordinator, LayoutItemDefinition definition, IValuesFormatter formatter)
	{
		this(context);
		mDefinition = definition;

		if (formatter == null && definition != null)
		{
			if (coordinator != null && EditInputDescriptions.isInputTypeDescriptions(definition))
				formatter = new EditInputDescriptions(coordinator, definition).getValuesFormatter();
			else
				formatter = FormatHelper.getFormatter(definition.getDataItem());
		}

		mFormatter = new TextViewFormatter(this, formatter);
	}

	public GxTextView(Context context)
	{
		super(context);
	}

	public GxTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	public String getGx_Value() {
		return mValue;
	}

	@Override
	public void setGx_Value(String value)
	{
		mValue = value;

		if (mFormatter != null && !mDefinition.isHtml())
			mFormatter.setText(mValue);
		else
			TextViewUtils.setText(this, mValue, mDefinition);
	}


	@Override
	public String getGx_Tag() {
		return (String)this.getTag();
	}

	@Override
	public void setGx_Tag(String data) {
		this.setTag(data);
	}

	@Override
	public void setValueFromIntent(Intent data) { }


	@Override
	public IGxEdit getViewControl() {
		return this;
	}

	@Override
	public IGxEdit getEditControl() {
		return this;
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		mClassDefinition = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass() {
		return mClassDefinition;
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass) {
		//set font properties
		ThemeUtils.setFontProperties(this, themeClass);

		//set background and border properties
		ThemeUtils.setBackgroundBorderProperties(this, themeClass, BackgroundOptions.defaultFor(mDefinition));
	}

	@Override
	public boolean isEditable()
	{
		return false; // Never editable.
	}
}
