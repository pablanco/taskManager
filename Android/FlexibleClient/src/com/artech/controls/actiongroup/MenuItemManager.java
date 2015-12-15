package com.artech.controls.actiongroup;

import java.util.List;

import android.content.Context;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import com.artech.base.metadata.layout.ActionGroupItem;
import com.artech.base.metadata.layout.ActionGroupItemDefinition;
import com.artech.base.services.Services;
import com.artech.common.UIActionHelper;

class MenuItemManager extends ActionGroupItemManager<MenuItemControl>
{
	private final Context mContext;

	public MenuItemManager(Context context)
	{
		mContext = context;
	}

	@Override
	protected MenuItemControl newItemControl(ActionGroupItemDefinition definition, int id)
	{
		return new MenuItemControl(definition, id);
	}

	public void initializeMenu(Menu menu)
	{
		initializeMenu(menu, getMainItems(), true);
	}

	private void initializeMenu(Menu menu, List<MenuItemControl> controls, boolean isFirstLevel)
	{
		for (MenuItemControl control : controls)
		{
			if (control.getType() == ActionGroupItem.TYPE_ACTION)
			{
				MenuItem item = menu.add(Menu.NONE, control.getMenuId(), Menu.NONE, control.getCaption());
				configureMenuItem(item, control);
				control.setMenuItem(item);
			}
			else if (control.getType() == ActionGroupItem.TYPE_GROUP)
			{
				SubMenu subMenu = menu.addSubMenu(Menu.NONE, control.getMenuId(), Menu.NONE, control.getCaption());
				configureMenuItem(subMenu.getItem(), control);
				control.setMenuItem(subMenu.getItem());

				// Android only supports only ONE LEVEL of SubMenus.
				// Trying to create a SubMenu inside of another SubMenu throws an exception.
				if (isFirstLevel)
					initializeMenu(subMenu, control.getSubItems(), false);
				else
					Services.Log.warning(String.format("Android does not support nested SubMenus. Ignoring children of '%s'.", control.getName()));
			}
		}
	}

	private void configureMenuItem(MenuItem menuItem, ActionGroupBaseItemControl<?> control)
	{
		int showOption;
		if (control.getPriority() == ActionGroupItemDefinition.PRIORITY_HIGH)
			showOption = MenuItemCompat.SHOW_AS_ACTION_IF_ROOM | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT;
		else if (control.getPriority() == ActionGroupItemDefinition.PRIORITY_LOW)
			showOption = MenuItemCompat.SHOW_AS_ACTION_NEVER;
		else
			showOption = MenuItemCompat.SHOW_AS_ACTION_IF_ROOM;

		MenuItemCompat.setShowAsAction(menuItem, showOption);
		menuItem.setEnabled(control.isEnabled());
		menuItem.setVisible(control.isVisible());

		if (control.getAction() != null)
			UIActionHelper.setMenuItemImage(mContext, menuItem, control.getAction());
	}

	public void finalizeMenu()
	{
		for (MenuItemControl control : getAllItems())
			control.setMenuItem(null);
	}
}
