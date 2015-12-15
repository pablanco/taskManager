package com.artech.android;

import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.content.res.TypedArray;

import com.artech.R;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.enums.ActionTypes;

public class ResourceManager
{
	private static final Map<String, Integer> THEME_DARK;
	private static final Map<String, Integer> THEME_LIGHT;

	private static final int PLACE_ACTION_BAR = 1;
	private static final int PLACE_CONTENT = 2;

	public static final int THEME_DARK_VALUE = 0;
	public static final int THEME_LIGHT_VALUE = 1;
	public static final int THEME_LIGHTDAB_VALUE = 2;

	static
	{
		// Initialize the maps with resources for both themes.
		THEME_DARK = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
		THEME_LIGHT = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);

		// Standard actions.
		THEME_DARK.put(ActionDefinition.STANDARD_ACTION.INSERT, R.drawable.gx_action_insert_dark);
		THEME_LIGHT.put(ActionDefinition.STANDARD_ACTION.INSERT, R.drawable.gx_action_insert_light);
		THEME_DARK.put(ActionDefinition.STANDARD_ACTION.UPDATE, R.drawable.gx_action_update_dark);
		THEME_LIGHT.put(ActionDefinition.STANDARD_ACTION.UPDATE, R.drawable.gx_action_update_light);
		THEME_DARK.put(ActionDefinition.STANDARD_ACTION.EDIT, R.drawable.gx_action_update_dark);
		THEME_LIGHT.put(ActionDefinition.STANDARD_ACTION.EDIT, R.drawable.gx_action_update_light);
		THEME_DARK.put(ActionDefinition.STANDARD_ACTION.DELETE, R.drawable.gx_action_delete_dark);
		THEME_LIGHT.put(ActionDefinition.STANDARD_ACTION.DELETE, R.drawable.gx_action_delete_light);

		THEME_DARK.put(ActionDefinition.STANDARD_ACTION.SAVE, R.drawable.gx_action_save_dark);
		THEME_LIGHT.put(ActionDefinition.STANDARD_ACTION.SAVE, R.drawable.gx_action_save_light);
		THEME_DARK.put(ActionDefinition.STANDARD_ACTION.CANCEL, R.drawable.gx_action_cancel_dark);
		THEME_LIGHT.put(ActionDefinition.STANDARD_ACTION.CANCEL, R.drawable.gx_action_cancel_light);

		THEME_DARK.put(ActionDefinition.STANDARD_ACTION.REFRESH, R.drawable.gx_action_refresh_dark);
		THEME_LIGHT.put(ActionDefinition.STANDARD_ACTION.REFRESH, R.drawable.gx_action_refresh_light);
		THEME_DARK.put(ActionDefinition.STANDARD_ACTION.SEARCH, R.drawable.gx_action_search_dark);
		THEME_LIGHT.put(ActionDefinition.STANDARD_ACTION.SEARCH, R.drawable.gx_action_search_light);
		THEME_DARK.put(ActionDefinition.STANDARD_ACTION.FILTER, R.drawable.gx_action_filter_dark);
		THEME_LIGHT.put(ActionDefinition.STANDARD_ACTION.FILTER, R.drawable.gx_action_filter_light);
		THEME_DARK.put(ActionDefinition.STANDARD_ACTION.SHARE, R.drawable.gx_action_share_dark);
		THEME_LIGHT.put(ActionDefinition.STANDARD_ACTION.SHARE, R.drawable.gx_action_share_light);

		// Contextual actions for semantic domains.
		THEME_DARK.put(ActionTypes.SendEmail, R.drawable.gx_domain_action_email_dark);
		THEME_LIGHT.put(ActionTypes.SendEmail, R.drawable.gx_domain_action_email_light);
		THEME_DARK.put(ActionTypes.LocateAddress, R.drawable.gx_domain_action_locate_dark);
		THEME_LIGHT.put(ActionTypes.LocateAddress, R.drawable.gx_domain_action_locate_light);
		THEME_DARK.put(ActionTypes.LocateGeoLocation, R.drawable.gx_domain_action_locate_dark);
		THEME_LIGHT.put(ActionTypes.LocateGeoLocation, R.drawable.gx_domain_action_locate_light);
		THEME_DARK.put(ActionTypes.CallNumber, R.drawable.gx_domain_action_call_dark);
		THEME_LIGHT.put(ActionTypes.CallNumber, R.drawable.gx_domain_action_call_light);
		THEME_DARK.put(ActionTypes.ViewAudio, R.drawable.gx_domain_action_play_dark);
		THEME_LIGHT.put(ActionTypes.ViewAudio, R.drawable.gx_domain_action_play_light);
		THEME_DARK.put(ActionTypes.ViewVideo, R.drawable.gx_domain_action_play_dark);
		THEME_LIGHT.put(ActionTypes.ViewVideo, R.drawable.gx_domain_action_play_light);
		THEME_DARK.put(ActionTypes.ViewUrl, R.drawable.gx_domain_action_link_dark);
		THEME_LIGHT.put(ActionTypes.ViewUrl, R.drawable.gx_domain_action_link_light);

		// Autolinks & prompt.
		THEME_DARK.put(ActionTypes.Link, R.drawable.gx_field_link_dark);
		THEME_LIGHT.put(ActionTypes.Link, R.drawable.gx_field_link_light);
		THEME_DARK.put(ActionTypes.Prompt, R.drawable.gx_field_prompt_dark);
		THEME_LIGHT.put(ActionTypes.Prompt, R.drawable.gx_field_prompt_light);
	}

	/**
	 * Gets the drawable associated to a standard action (e.g. Insert, Refresh, Share)
	 * when the action is shown in the action bar.
	 * @param action Action name.
	 */
	public static int getActionBarDrawableFor(Context context, String action)
	{
		return getDrawableFor(context, action, PLACE_ACTION_BAR);
	}

	/**
	 * Gets the drawable associated to a standard action (e.g. Insert, Refresh, Share)
	 * when the action is shown in the content area.
	 * @param action Action name.
	 */
	public static int getContentDrawableFor(Context context, String action)
	{
		return getDrawableFor(context, action, PLACE_CONTENT);
	}

	/**
	 * Gets the drawable associated to a standard action (e.g. Insert, Refresh, Share).
	 * @param action Action name.
	 * @param place Where the drawable will be used (action bar or content).
	 */
	private static int getDrawableFor(Context context, String action, int place)
	{
		// In action bar, LightWithDarkActionBar = Dark. In content, LightWithDarkActionBar = Light.
		Map<String, Integer> theme;
		if (place == PLACE_ACTION_BAR)
			theme = getResource(context, THEME_DARK, THEME_LIGHT, THEME_DARK);
		else
			theme = getResource(context, THEME_DARK, THEME_LIGHT, THEME_LIGHT);

		return getThemeDrawable(theme, action);
	}

	private static int getThemeDrawable(Map<String, Integer> theme, String name)
	{
		Integer resourceId = theme.get(name);
		if (resourceId != null)
			return resourceId;
		else
			return 0;
	}

	public static <T> T getResource(Context context, T darkValue, T lightValue)
	{
		return getResource(context, darkValue, lightValue, lightValue);
	}

	private static Integer THEME_KIND;

	private static <T> T getResource(Context context, T darkValue, T lightValue, T lightDABValue)
	{
		getThemeType(context);

		switch (THEME_KIND)
		{
			case THEME_DARK_VALUE : return darkValue;
			case THEME_LIGHT_VALUE : return lightValue;
			case THEME_LIGHTDAB_VALUE : return lightDABValue;
			default : return darkValue;
		}
	}

	public static int getThemeType(Context context)
	{
		if (THEME_KIND == null)
		{
			TypedArray a = context.obtainStyledAttributes(new int[] { R.attr.gx_base_theme });
			if (!a.hasValue(0))
				throw new IllegalStateException("You must use Theme.Genexus.Dark, Theme.Genexus.Light, Theme.Genexus.Light.DarkActionBar, or a derivative.");

			THEME_KIND = a.getInt(0, 0);
			a.recycle();
		}
		return THEME_KIND;
	}

}
