package com.artech.actions;

import android.view.Menu;

/**
 * Interface that controls may implement if they want to add custom items to the action bar.
 * @author matiash
 *
 */
public interface ICustomMenuManager
{
	/**
	 * Called during Activity.onCreateOptionsMenu().
	 */
	void onCustomCreateOptionsMenu(Menu menu);
}
