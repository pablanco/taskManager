package com.artech.controls.tabs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.artech.android.ViewHierarchyVisitor;
import com.artech.android.layout.CustomScrollView;
import com.artech.android.layout.GridsLayoutVisitor;
import com.artech.android.layout.GxLayoutInTab;
import com.artech.base.controls.IGxControlPreserveState;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.layout.TabControlDefinition;
import com.artech.base.metadata.layout.TabItemDefinition;
import com.artech.base.metadata.theme.TabControlThemeClassDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.compatibility.SherlockHelper;
import com.artech.controls.IGxThemeable;
import com.artech.fragments.ComponentContainer;
import com.artech.fragments.LayoutFragment;
import com.artech.ui.Coordinator;
import com.artech.utils.Cast;

public class GxTabControl extends LinearLayout implements IGxThemeable, IGxControlRuntime, IGxControlPreserveState, ViewHierarchyVisitor.ICustomViewChildrenProvider
{
	private final Coordinator mCoordinator;
	private final TabControlDefinition mDefinition;
	private List<TabItemInfo> mTabItems;
	private ThemeClassDefinition mThemeClass;
	private ThemeClassDefinition mAppliedThemeClass;

	private CustomSlidingTabLayout mSlidingTabs;
	private ViewPager mViewPager;
	private TabsAdapter mTabsAdapter;

	private boolean mInitializing;
	private int mInitialTabIndex;

	private List<TabItemInfo> mVisibleTabItems;
	private int mCurrentVisibleIndex;
	private int mCurrentAbsoluteIndex;

	private static final String PROPERTY_ACTIVE_PAGE = "ActivePage";

	public GxTabControl(Context context, Coordinator coordinator, TabControlDefinition definition)
	{
		super(context);
		mCoordinator = coordinator;
		mDefinition = definition;
		initView();
	}

	private void initView()
	{
		mInitializing = true;
		mSlidingTabs = new CustomSlidingTabLayout(getContext());
		mViewPager = new ViewPager(getContext());

		setOrientation(VERTICAL);
		addView(mSlidingTabs, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		addView(mViewPager, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

		mSlidingTabs.setDistributeEvenly(mDefinition.getTabStripKind() == TabControlDefinition.TabStripKind.Fixed);

		// Initialize the containers for each tab view.
		mTabItems = new ArrayList<TabItemInfo>();
		for (TabItemDefinition tabItem : mDefinition.getTabItems())
		{
			TabItemInfo tabInfo = initTabItem(tabItem);
			tabInfo.index = mTabItems.size();
			mTabItems.add(tabInfo);
		}

		mCurrentVisibleIndex = -1;
		mVisibleTabItems = new ArrayList<TabItemInfo>();
		updateVisibleTabItemsList();

		// Connect views and the adapter to construct the actual tabs.
		mTabsAdapter = new TabsAdapter();
		mViewPager.setAdapter(mTabsAdapter);
		mSlidingTabs.setViewPager(mViewPager);
		mSlidingTabs.setOnPageChangeListener(mPageChangeListener);

		mInitialTabIndex = 0;
		mInitializing = false;
	}

	private TabItemInfo initTabItem(TabItemDefinition tabItem)
	{
		TabItemInfo tabItemInfo = new TabItemInfo(tabItem);

		// Create tab title view (necessary even for invisible tabs to access their runtime properties).
		tabItemInfo.titleView = mSlidingTabs.createTabTitleView(tabItemInfo);
		tabItemInfo.titleView.applyThemeClasses();

		// Create tab content view (necessary even for invisible tabs to access runtime properties on its controls).
		GxLayoutInTab contentView = new GxLayoutInTab(getContext(),tabItem.getTable(), mCoordinator);
		contentView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		tabItemInfo.contentView = contentView;

		// See if we can/should add a ScrollView for the tab.
		if (!GridsLayoutVisitor.hasScrollableViews(tabItem.getTable()))
		{
			CustomScrollView scrollView = new CustomScrollView(getContext());
			scrollView.addView(contentView);
			tabItemInfo.rootView = scrollView;
			contentView.setHasScroll(true);
		}
		else
			tabItemInfo.rootView = contentView;

		return tabItemInfo;
	}

	public TabControlDefinition getDefinition()
	{
		return mDefinition;
	}

	public List<TabItemInfo> getTabItems()
	{
		return mTabItems;
	}

	private TabItemInfo getCurrentTabItem()
	{
		if (mCurrentVisibleIndex >= 0 && mCurrentVisibleIndex < mVisibleTabItems.size())
			return mVisibleTabItems.get(mCurrentVisibleIndex);
		else
			return null;
	}

	private void setCurrentTabItem(TabItemInfo tab)
	{
		if (tab.visibleIndex != -1)
			mInitialTabIndex = tab.visibleIndex;

		// SHOULD BE: mViewPager.setCurrentItem(tab.visibleIndex);
		// but this does not work when initializing because we have no place for the inner fragments.
	}

	@Override
	public Collection<View> getCustomViewChildren()
	{
		LinkedList<View> children = new LinkedList<View>();

		// We want to return all controls relevant for us: the tab widget, and the layout of all the tabs.
		// Invisible tabs are not in the hierarchy, so we return the individual views themselves.
		for (TabItemInfo tabItem : mTabItems)
		{
			children.add(tabItem.titleView);
			children.add(tabItem.contentView);
		}

		return Collections.unmodifiableCollection(children);
	}

	public static class TabItemInfo
	{
		public final TabItemDefinition definition;
		public final String id;

		public View rootView;
		public GxLayoutInTab contentView;
		public GxTabPageTextView titleView;

		public int index;
		public CharSequence title;
		public boolean visible;
		public int visibleIndex;

		public TabItemInfo(TabItemDefinition tabItem)
		{
			definition = tabItem;
			id = tabItem.getName();
			title = tabItem.getCaption();
			visible = tabItem.isVisible();
		}

		public void setActive(boolean value)
		{
			if (titleView != null)
				titleView.setSelected(value);

			if (contentView != null)
				contentView.setIsActiveTab(value);
		}
	}

	void updateVisibleTabItemsList()
	{
		mVisibleTabItems.clear();
		for (TabItemInfo itemInfo : mTabItems)
		{
			if (itemInfo.visible)
			{
				itemInfo.visibleIndex = mVisibleTabItems.size();
				mVisibleTabItems.add(itemInfo);
			}
			else
				itemInfo.visibleIndex = -1;
		}
	}

	void notifyTabsChanged()
	{
		if (mTabsAdapter != null)
		{
			mTabsAdapter.notifyDataSetChanged();
			mSlidingTabs.setViewPager(mViewPager);
		}
	}

	private class CustomSlidingTabLayout extends SlidingTabLayout
	{
		public CustomSlidingTabLayout(Context context)
		{
			super(context);
		}

		@Override
		protected View createTabView(PagerAdapter adapter, int position)
		{
			TabItemInfo itemInfo = mVisibleTabItems.get(position);
			return itemInfo.titleView;
		}

		private GxTabPageTextView createTabTitleView(TabItemInfo itemInfo)
		{
			TabItemDefinition itemDefinition = itemInfo.definition;

			// Create a textview with a compound drawable for the image and the specified background color.
			GxTabPageTextView tabView = new GxTabPageTextView(getContext(), GxTabControl.this, itemInfo);
			applyDefaultTabViewStyle(tabView);
			tabView.setText(itemInfo.title);

	        TabUtils.setTabImage(tabView, itemDefinition.getImageUnselected(), itemDefinition.getImage(), itemDefinition.getImageAlignment());

			// HACK: We need a FIXED size for tab title views, because we have measured all controls before!
			// Remove this when we measure in onMeasure() instead of before adding the controls.
			tabView.getLayoutParams().height = TabControlDefinition.getTabWidgetHeight();
			tabView.setPadding(tabView.getPaddingLeft(), 0, tabView.getPaddingRight(), 0);
			// End HACK

			return tabView;
		}
	}

	private class TabsAdapter extends PagerAdapter
	{
		@Override
		public int getCount()
		{
			return mVisibleTabItems.size();
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			return mVisibleTabItems.get(position).title;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position)
		{
			// Return TabItemInfo as object (instead of the view itself) so that we can match them correctly
			// in getItemPosition() and isViewFromObject() (to support dynamic changes to tab visibility).
			TabItemInfo tabItem = mVisibleTabItems.get(position);
			container.addView(tabItem.rootView, 0);
			return tabItem;
		}

		@Override
		public void setPrimaryItem(ViewGroup container, int position, Object object)
		{
			// BUGFIX: onPageSelected() is not executed for the first page.
			if (mCurrentVisibleIndex == -1 && position == 0)
			{
				if (mInitialTabIndex != 0)
					mViewPager.setCurrentItem(mInitialTabIndex);
				else
					mPageChangeListener.onPageSelected(mInitialTabIndex); // Force event.
			}
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object)
		{
			TabItemInfo tabItem = (TabItemInfo)object;
			container.removeView(tabItem.rootView);
		}

		@Override
		public boolean isViewFromObject(View view, Object object)
		{
			TabItemInfo tabItem = (TabItemInfo)object;
			return tabItem.rootView == view;
		}

	    @Override
		public int getItemPosition(Object object)
	    {
	    	TabItemInfo tabItem = (TabItemInfo)object;
	    	int position = mVisibleTabItems.indexOf(tabItem);
	    	if (position != -1)
	    		return position;
	    	else
	    		return POSITION_NONE;
	    }
	}

	private final ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener()
	{
		private List<ComponentContainer> mLastTabContainer = null;

		@Override
		public void onPageSelected(int position)
		{
			mCurrentVisibleIndex = position;
			TabItemInfo selectedTab = getCurrentTabItem();

			// Why mark all tabs unselected instead of just the previous visible one?
			// Because the visible tabs list may have changed, so the previous value of mCurrentVisibleIndex would be off.
			for (TabItemInfo otherTab : mTabItems)
			{
				if (otherTab != selectedTab)
					otherTab.setActive(false);
			}

			if (selectedTab != null)
				selectedTab.setActive(true);

			// If the new or old tabs contain Fragments, set their activated/inactivated state.
			adjustFragmentsAfterTabChanged(selectedTab);

			// Fire the ActivePageChanged event if the page effectively changed (when making tabs visible/invisible
			// the visible index will change although the user stays in the "same" tab).
			if (selectedTab != null && selectedTab.index != mCurrentAbsoluteIndex)
			{
				mCurrentAbsoluteIndex = selectedTab.index;
				mCoordinator.runControlEvent(GxTabControl.this, "ActivePageChanged");
			}
		}

		@Override
		public void onPageScrollStateChanged(int state) { }

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

		private void adjustFragmentsAfterTabChanged(TabItemInfo currentTab)
		{
			LayoutFragment parentFragment = null;

			// Inactivate old container
			if (mLastTabContainer != null)
			{
				for (ComponentContainer container : mLastTabContainer)
				{
					if (container.getStatus() == ComponentContainer.ACTIVE)
					{
						container.setStatus(ComponentContainer.TOINACTIVATED);
						if (container.getParentFragment() != null)
							parentFragment = container.getParentFragment();
					}

					// Tab not activate yet, never get show to the user. set to inactive
					if (container.getStatus() == ComponentContainer.TOACTIVATED && container.getParentFragment() == null)
						container.setStatus(ComponentContainer.INACTIVE);
				}
			}

			// Activated new container
			if (currentTab != null)
			{
				mLastTabContainer = ViewHierarchyVisitor.getViews(ComponentContainer.class, currentTab.contentView);
				for (ComponentContainer container : mLastTabContainer)
				{
					//Try to activate tabs that cannot be activated before.
					if (container.getStatus() == ComponentContainer.TOACTIVATED)
					{
						if (container.getParentFragment() != null)
							parentFragment = container.getParentFragment();
					}

					//Activate content of current selected tab
					if (container.getStatus() == ComponentContainer.INACTIVE)
					{
						container.setStatus(ComponentContainer.TOACTIVATED);
						if (container.getParentFragment() != null)
							parentFragment = container.getParentFragment();
					}
				}
			}

			if (parentFragment != null)
				parentFragment.attachContentContainers();

			// Menu could be updated after tabs change (because grids become visible/invisible).
			if (!mInitializing && getContext() instanceof Activity)
				SherlockHelper.invalidateOptionsMenu((Activity)getContext());
		}
	};

	@Override
	public ThemeClassDefinition getThemeClass()
	{
		return mThemeClass;
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		mThemeClass = themeClass;
		applyClass(themeClass);
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		TabControlThemeClassDefinition tabThemeClass = Cast.as(TabControlThemeClassDefinition.class, themeClass);
		if (tabThemeClass == null || tabThemeClass.equals(mAppliedThemeClass))
			return;

		// Apply classes to the whole view and to the sliding tabs container.
		TabUtils.applyTabControlClass(this, mSlidingTabs, tabThemeClass);

		// Apply related classes to tab item title views.
		for (TabItemInfo tabItem : mTabItems)
			tabItem.titleView.applyParentThemeClass(tabThemeClass);

		mAppliedThemeClass = themeClass;
	}

	@Override
	public void setProperty(String name, Object value)
	{
		if (PROPERTY_ACTIVE_PAGE.equalsIgnoreCase(name) && value != null)
		{
			Integer pageNumber = Services.Strings.tryParseInt(value.toString());
			if (pageNumber != null && pageNumber >= 1 && pageNumber <= mTabItems.size())
			{
				// Check that we don't switch to an invisible tab.
				int index = pageNumber - 1;
				TabItemInfo tabItem = mTabItems.get(index);
				if (tabItem.visibleIndex != -1)
					mViewPager.setCurrentItem(tabItem.visibleIndex);
			}
		}
	}

	@Override
	public Object getProperty(String name)
	{
		if (PROPERTY_ACTIVE_PAGE.equalsIgnoreCase(name))
		{
			if (mCurrentVisibleIndex == -1)
				return 0;

			int index = mVisibleTabItems.get(mCurrentVisibleIndex).index;
			return index + 1;
		}
		else
			return null;
	}

	@Override
	public void runMethod(String name, List<Object> parameters) { }

	@Override
	public String getControlId()
	{
		return mDefinition.getName();
	}

	@Override
	public void saveState(Map<String, Object> state)
	{
		TabItemInfo currentTab = getCurrentTabItem();
		if (currentTab != null)
			state.put(PROPERTY_ACTIVE_PAGE, currentTab.id);
	}

	@Override
	public void restoreState(Map<String, Object> state)
	{
		String activePageId = Cast.as(String.class, state.get(PROPERTY_ACTIVE_PAGE));
		if (Strings.hasValue(activePageId))
		{
			for (TabItemInfo tab : mVisibleTabItems)
			{
				if (tab.id.equals(activePageId))
					setCurrentTabItem(tab);
			}
		}
	}
}
