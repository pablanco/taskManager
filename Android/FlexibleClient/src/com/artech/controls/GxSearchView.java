package com.artech.controls;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.MenuItem;

import com.artech.compatibility.SherlockHelper;

public class GxSearchView extends SearchView
{
	private GxSearchView(Context context)
	{
		super(context);
	}

	public static GxSearchView create(Activity activity, MenuItem searchItem)
	{
		GxSearchView searchView = new GxSearchView(SherlockHelper.getActionBarThemedContext(activity));
    	searchView.setIconifiedByDefault(true);

		SearchManager searchManager = (SearchManager)activity.getSystemService(Context.SEARCH_SERVICE);
    	searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));

    	searchView.setOnQueryTextListener(searchView.mSearchOnQueryTextListener);
    	searchView.setOnSuggestionListener(searchView.mSearchOnSuggestionListener);

	    MenuItemCompat.setActionView(searchItem, searchView);
	    return searchView;
	}

	private final SearchView.OnQueryTextListener mSearchOnQueryTextListener = new SearchView.OnQueryTextListener()
	{
		@Override
		public boolean onQueryTextChange(String newText) { return false; }

		@Override
		public boolean onQueryTextSubmit(String query)
		{
			hide();
			return false;
		}
	};

	private final SearchView.OnSuggestionListener mSearchOnSuggestionListener = new SearchView.OnSuggestionListener()
	{
		@Override
		public boolean onSuggestionSelect(int position) { return false; }

		@Override
		public boolean onSuggestionClick(int position)
		{
			hide();
			return false;
		}
	};

	public void show()
	{
		setIconified(false);
	}

	public void hide()
	{
		setQuery("", false);
		setIconified(true);
	}
}
