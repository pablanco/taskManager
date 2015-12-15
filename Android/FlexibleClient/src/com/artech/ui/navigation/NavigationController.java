package com.artech.ui.navigation;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import com.artech.app.ComponentParameters;
import com.artech.fragments.IDataView;
import com.artech.fragments.LayoutFragmentActivityState;

/**
 * A Navigation Controller is the object responsible for handling fragment instantiation
 * (and related operations such as managing call targets, replacing or stacking history, &c)
 * inside a single activity. Examples are: sliding menu, tabbed interface, split.
 *
 * @author matiash
 */
public interface NavigationController
{
	boolean start(ComponentParameters mainParams, LayoutFragmentActivityState previousState);
	NavigationHandled handle(UIObjectCall call, Intent callIntent);

	// Life-cycle events.
	void onCreate(Bundle savedInstanceState);
	void onPostCreate(Bundle savedInstanceState);
	void onResume();
	void onConfigurationChanged(Configuration newConfig);
	void onSaveInstanceState(Bundle outState);
	void onPause();

	void saveActivityState(LayoutFragmentActivityState outState);

	// Misc events.
	View findViewById(int id);
	boolean setTitle(IDataView fromDataView, CharSequence title);
	boolean onOptionsItemSelected(MenuItem item);
	boolean onKeyUp(int keyCode, KeyEvent event);
	boolean onBackPressed();
}
