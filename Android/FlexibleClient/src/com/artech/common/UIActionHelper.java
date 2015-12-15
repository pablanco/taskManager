package com.artech.common;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.widget.TextView;

import com.artech.android.ResourceManager;
import com.artech.application.MyApplication;
import com.artech.base.metadata.enums.Alignment;
import com.artech.base.metadata.layout.ILayoutActionDefinition;
import com.artech.base.utils.Strings;
import com.artech.compatibility.CompatibilityHelper;

public class UIActionHelper
{
	public static void setActionButtonImage(Context context, ILayoutActionDefinition action, int position, TextView control)
	{
		if (position == Alignment.NONE)
			return;

		Drawable drawable = getActionImage(context, action);
		if (drawable != null)
		{
			if (position == Alignment.CENTER)
				CompatibilityHelper.setBackground(control, drawable);
			else
			{
				// Position should be read from theme class afterwards.
				control.setCompoundDrawables(
					position == Alignment.LEFT ? drawable : null,
					position == Alignment.TOP ? drawable : null,
					position == Alignment.RIGHT ? drawable : null,
					position == Alignment.BOTTOM ? drawable : null);
			}
		}
	}

	public static void setMenuItemImage(Context context, MenuItem menuItem, ILayoutActionDefinition action)
	{
		// Either use specified image or default one.
		Drawable customDrawable = readActionImage(MyApplication.getAppContext(), action);
		if (customDrawable != null)
			setMenuItemIcon(context, menuItem, customDrawable);
		else
			setStandardMenuItemImage(context, menuItem, action.getEventName());
	}

	public static void setStandardMenuItemImage(Context context, MenuItem menuItem, String action)
	{
		if (Strings.hasValue(action))
		{
			int standardResourceId = ResourceManager.getActionBarDrawableFor(context, action);
			if (standardResourceId != 0)
				setMenuItemIcon(context, menuItem, standardResourceId);
		}
	}

	public static void setMenuItemIcon(Context context, MenuItem menuItem, Drawable icon)
	{
		if (icon != null)
		{
			// Apply the action tint color from theme.
			// Disabled for now, as I couldn't make it work for the overflow icon.
//			Integer tintColor = ThemeUtils.getColorId(PlatformHelper.getApplicationClass().getActionTintColor());
//			if (tintColor != null)
//				icon = DrawableUtils.getTintedDrawable(icon, tintColor);

			menuItem.setIcon(icon);
		}
	}

	public static void setMenuItemIcon(Context context, MenuItem menuItem, int iconRes)
	{
		setMenuItemIcon(context, menuItem, ContextCompat.getDrawable(context, iconRes));
	}

	public static Drawable getActionImage(Context context, ILayoutActionDefinition action)
	{
		Drawable drawable = readActionImage(context, action);
		if (drawable != null)
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

		return drawable;
	}

	private static Drawable readActionImage(Context context, ILayoutActionDefinition action)
	{
		Drawable normalImage = ImageHelper.getStaticImage(action.getImage());

		if (normalImage == null)
			return null; // We don't support highlighted/disabled unless normal is set.

		StateListDrawable stateImage = new StateListDrawable();
		Drawable disabledImage = ImageHelper.getStaticImage(action.getDisabledImage());
		Drawable highlightedImage = ImageHelper.getStaticImage(action.getHighlightedImage());

		if (highlightedImage != null)
			stateImage.addState(new int[] { android.R.attr.state_pressed }, highlightedImage);

		if (disabledImage != null)
			stateImage.addState(new int[] { -android.R.attr.state_enabled }, disabledImage);

		stateImage.addState(new int[] { }, normalImage);

		return stateImage;
	}
}
