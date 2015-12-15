package com.artech.controls.common;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.utils.BackgroundOptions;
import com.artech.utils.Cast;
import com.artech.utils.ThemeUtils;

public class SpinnerItemsAdapter extends ArrayAdapter<ValueItem>
{
	private ThemeClassDefinition mThemeClass;
	private LayoutItemDefinition mDefinition;

	public SpinnerItemsAdapter(Context context, ValueItems<? extends ValueItem> items, ThemeClassDefinition themeClass, LayoutItemDefinition definition)
	{
		super(context, android.R.layout.simple_spinner_item, items.getItems());
		setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mDefinition = definition;
		mThemeClass = themeClass;
	}

	public void applyClass(ThemeClassDefinition themeClass)
	{
		mThemeClass = themeClass;
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		// Create the view and customize it according to theme class.
		View view = super.getView(position, convertView, parent);
		if (mDefinition.isHtml()) {
			TextView txtView = Cast.as(TextView.class, view);
			if (txtView != null) {
				ValueItem valueItem = this.getItem(position);
				txtView.setText(Html.fromHtml(valueItem.Description));
			}
		}
		applyStyle(view, mThemeClass, false);
		return view;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent)
	{
		// Create the item view and customize it according to theme.
		View view = super.getDropDownView(position, convertView, parent);
		applyStyle(view, mThemeClass, true);
		return view;
	}

	private void applyStyle(View view, ThemeClassDefinition themeClass, boolean setBackground)
	{
		if (themeClass != null)
		{
			TextView text = Cast.as(TextView.class, view.findViewById(android.R.id.text1));
			if (text != null)
			{
				ThemeUtils.setFontProperties(text, themeClass);
				if (setBackground)
					ThemeUtils.setBackgroundBorderProperties(text, themeClass, BackgroundOptions.DEFAULT);
			}
		}
	}
}
