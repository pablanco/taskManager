package com.artech.ui.navigation.controllers;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import com.artech.app.ComponentParameters;
import com.artech.fragments.IDataView;
import com.artech.fragments.LayoutFragmentActivityState;
import com.artech.ui.navigation.NavigationController;
import com.artech.ui.navigation.NavigationHandled;
import com.artech.ui.navigation.UIObjectCall;

/**
 * Class for supporting multiple navigation controllers at once (e.g. tabs and slider menu).
 * @author matiash
 */
public class CompositeNavigationController implements NavigationController
{
	private NavigationController[] mControllers;

	public CompositeNavigationController(NavigationController... controllers)
	{
		mControllers = controllers;
	}

	@Override
	public boolean start(ComponentParameters mainParams, LayoutFragmentActivityState previousState)
	{
		for (NavigationController controller : mControllers)
			if (controller.start(mainParams, previousState))
				return true;

		return false;
	}

	@Override
	public NavigationHandled handle(UIObjectCall call, Intent callIntent)
	{
		for (NavigationController controller : mControllers)
		{
			NavigationHandled handled = controller.handle(call, callIntent);
			if (handled != NavigationHandled.NOT_HANDLED)
				return handled;
		}

		return NavigationHandled.NOT_HANDLED;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		for (NavigationController controller : mControllers)
			controller.onCreate(savedInstanceState);
	}

	@Override
	public void onPostCreate(Bundle savedInstanceState)
	{
		for (NavigationController controller : mControllers)
			controller.onPostCreate(savedInstanceState);
	}

	@Override
	public void onResume()
	{
		for (NavigationController controller : mControllers)
			controller.onResume();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		for (NavigationController controller : mControllers)
			controller.onConfigurationChanged(newConfig);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		for (NavigationController controller : mControllers)
			controller.onSaveInstanceState(outState);
	}

	@Override
	public void saveActivityState(LayoutFragmentActivityState outState)
	{
		for (NavigationController controller : mControllers)
			controller.saveActivityState(outState);
	}

	@Override
	public void onPause()
	{
		for (NavigationController controller : mControllers)
			controller.onPause();
	}

	@Override
	public View findViewById(int id)
	{
		for (NavigationController controller : mControllers)
		{
			View view = controller.findViewById(id);
			if (view != null)
				return view;
		}

		return null;
	}

	@Override
	public boolean setTitle(IDataView fromDataView, CharSequence title)
	{
		for (NavigationController controller : mControllers)
		{
			if (controller.setTitle(fromDataView, title))
				return true;
		}

		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		for (NavigationController controller : mControllers)
			if (controller.onOptionsItemSelected(item))
				return true;

		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		for (NavigationController controller : mControllers)
			if (controller.onKeyUp(keyCode, event))
				return true;

		return false;
	}
	
	@Override
	public boolean onBackPressed()
	{
		for (NavigationController controller : mControllers)
			if (controller.onBackPressed())
				return true;

		return false;
	}
}
