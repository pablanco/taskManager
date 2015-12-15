package com.artech.ui.navigation.tabbed;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.artech.R;
import com.artech.actions.ActionExecution;
import com.artech.actions.ActionFactory;
import com.artech.actions.ActionParameters;
import com.artech.actions.CompositeAction;
import com.artech.actions.CompositeAction.IEventListener;
import com.artech.actions.UIContext;
import com.artech.activities.ActivityHelper;
import com.artech.activities.GenexusActivity;
import com.artech.activities.dashboard.DashboardActivity;
import com.artech.app.ComponentId;
import com.artech.app.ComponentParameters;
import com.artech.app.ComponentType;
import com.artech.app.ComponentUISettings;
import com.artech.base.metadata.DashboardItem;
import com.artech.base.metadata.DashboardMetadata;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.layout.Size;
import com.artech.base.metadata.loader.DashboardMetadataLoader;
import com.artech.base.metadata.theme.TabControlThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.compatibility.SherlockHelper;
import com.artech.controls.tabs.SlidingTabLayout;
import com.artech.controls.tabs.TabUtils;
import com.artech.fragments.BaseFragment;
import com.artech.fragments.IDataView;
import com.artech.fragments.IFragmentHandleKeyEvents;
import com.artech.fragments.LayoutFragmentActivityState;
import com.artech.fragments.WebViewFragment;
import com.artech.ui.navigation.NavigationHandled;
import com.artech.ui.navigation.UIObjectCall;
import com.artech.ui.navigation.controllers.AbstractNavigationController;
import com.artech.utils.ThemeUtils;

class TabbedNavigationController extends AbstractNavigationController
{
	private final GenexusActivity mActivity;
	private final DashboardMetadata mDashboard;
	private final Handler mHandler;

	private ArrayList<TabItemInfo> mTabItems;
	private TabControlThemeClassDefinition mThemeClassForTabs;

	private LinearLayout mMainView;
	private SlidingTabLayout mSlidingTabs;
	private ViewPager mViewPager;
	private TabsPagerAdapter mTabsAdapter;

	private Entity mData;
	private int mCurrentTabIndex;
	private Fragment mCurrentTabFragment;
	private boolean mClientStartExecuted;

	private static final String STATE_CURRENT_TAB = "com.artech.ui.navigation.tabbed.TabbedNavigationController::CURRENT_TAB"; //$NON-NLS-1$
	private static final String STATE_DASHBOARD_DATA = "com.artech.ui.navigation.tabbed.TabbedNavigationController::DATA"; //$NON-NLS-1$
	private static final String CONTEXT_TAG_DASHBOARD_ITEM_INDEX = STATE_CURRENT_TAB;

	public TabbedNavigationController(GenexusActivity activity, DashboardMetadata dashboard)
	{
		mActivity = activity;
		mDashboard = dashboard;
		mHandler = new Handler();

		mTabItems = new ArrayList<TabItemInfo>();
		mThemeClassForTabs = mDashboard.getThemeClassForTabs();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		mActivity.setContentView(R.layout.tabbed_navigation);

		mMainView = (LinearLayout)mActivity.findViewById(R.id.content_frame);

		// set support toolbar
		Toolbar toolbar = (Toolbar)mActivity.findViewById(R.id.toolbar);
		mActivity.setSupportActionBar(toolbar);

		ActivityHelper.applyStyle(mActivity, mDashboard);

		// HACK: Discard all fragments on configuration change, recreate them later.
		if (savedInstanceState != null && mActivity.getSupportFragmentManager().getFragments().size() != 0)
		{
			FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
			for (Fragment fragment : mActivity.getSupportFragmentManager().getFragments())
			{
				if (fragment instanceof BaseFragment || fragment instanceof WebViewFragment || fragment instanceof TabPlaceholderFragment)
					ft.remove(fragment);
			}

			ft.commitAllowingStateLoss();
		}
	}

	@Override
	public boolean start(ComponentParameters mainParams, LayoutFragmentActivityState previousState)
	{
		if (mDashboard == null)
			throw new IllegalArgumentException("TabbedNavigationController requires a Dashboard view definition.");

		mData = new Entity(StructureDefinition.EMPTY);
		mData.setExtraMembers(mDashboard.getVariables());

		// Hide title bar if data view instructs it. This must be done here to work.
		if (!mDashboard.getShowApplicationBar())
			ActivityHelper.setActionBarVisibility(mActivity, false);

		initTabItems();

		Integer previousCurrentTab = null;
		if (previousState != null)
		{
			previousCurrentTab = (Integer)previousState.getProperty(STATE_CURRENT_TAB);
			Entity previousData = (Entity)previousState.getProperty(STATE_DASHBOARD_DATA);
			if (previousData != null)
			{
				mClientStartExecuted = true;
				mData = previousData;
			}
		}

		if (!mClientStartExecuted)
		{
			// Execute the ClientStart event, add tabs afterwards.
			UIContext context = UIContext.base(mActivity, mDashboard.getConnectivitySupport());
			DashboardActivity.runClientStart(mDashboard, context, mData, mAfterClientStart);
		}
		else
			initTabView(previousCurrentTab);

		return true;
	}

	private final IEventListener mAfterClientStart = new IEventListener()
	{
		@Override
		public void onEndEvent(CompositeAction event, boolean successful)
		{
			mClientStartExecuted = true;
			initTabView(null); // There can be no previous tab if ClientStart hadn't executed.
		}
	};

	private static class TabItemInfo
	{
		private int index;
		private DashboardItem definition;
		private boolean started;
		private TabPlaceholderFragment holderFragment;
		private Fragment contentFragment;
		private TextView titleView;

		ComponentId getComponentId()
		{
			return new ComponentId(null, "[TabbedNavigation]::TAB." + index);
		}
	}

	private void initTabItems()
	{
		for (DashboardItem item : mDashboard.getItems())
		{
			TabItemInfo tabItem = new TabItemInfo();
			tabItem.index = mTabItems.size();
			tabItem.definition = item;
			tabItem.started = false;
			tabItem.holderFragment = TabPlaceholderFragment.newInstance(tabItem.index);

			mTabItems.add(tabItem);
		}
	}

	private void initTabView(Integer setAsCurrentTab)
	{
		mViewPager = new ViewPager(mActivity);
		mViewPager.setId(R.id.tab_navigation_viewpager); // Necessary for FragmentPagerAdapter
		mViewPager.setOffscreenPageLimit(mTabItems.size()); // HACK: To avoid fragment view recycling (our fragments do not support it).

		mMainView.addView(mViewPager, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

		int tabsPosition = 0; // By default, tabs go up.
		if (mThemeClassForTabs != null && mThemeClassForTabs.getTabStripPosition() == TabControlThemeClassDefinition.TAB_STRIP_POSITION_BOTTOM)
			tabsPosition = 1;

		mSlidingTabs = new CustomSlidingTabLayout(SherlockHelper.getActionBarThemedContext(mActivity));
		mMainView.addView(mSlidingTabs, tabsPosition, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mSlidingTabs.setDistributeEvenly(true);

		applyThemeClass();

		// Connect views and the adapter to construct the actual tabs.
		mTabsAdapter = new TabsPagerAdapter();
		mViewPager.setAdapter(mTabsAdapter);
		mSlidingTabs.setViewPager(mViewPager);
		mSlidingTabs.setOnPageChangeListener(mPageChangeListener);

		// Restore the current tab.
		// BUGFIX: ViewPager does not fire onPageSelected() for the first page. Force it in that case.
		if (setAsCurrentTab != null && setAsCurrentTab != 0)
			mViewPager.setCurrentItem(setAsCurrentTab);
		else
			mPageChangeListener.onPageSelected(0);
	}

	private void applyThemeClass()
	{
		// Before actually applying the class, set the defaults from the Action Bar theme
		// (since tabs should be shown as if they were part of the action bar itself).
		Integer actionBarBackgroundColor = ThemeUtils.getAndroidThemeColorId(mActivity, R.attr.colorPrimary);

		// If the Action Bar class has a background color, use it instead.
		if (mDashboard.getApplicationBarClass() != null && Strings.hasValue(mDashboard.getApplicationBarClass().getBackgroundColor()))
			actionBarBackgroundColor = ThemeUtils.getColorId(mDashboard.getApplicationBarClass().getBackgroundColor());

		if (actionBarBackgroundColor != null)
			mSlidingTabs.setBackgroundColor(actionBarBackgroundColor);

		// Use same elevation as action bar.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mActivity.getSupportActionBar() != null)
		{
			mSlidingTabs.setElevation(mActivity.getSupportActionBar().getElevation());
			mActivity.getSupportActionBar().setElevation(0f);
		}

		// Then apply the custom class.
		TabUtils.applyTabControlClass(mMainView, mSlidingTabs, mThemeClassForTabs);
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
			TabItemInfo itemInfo = mTabItems.get(position);

			TextView tabView = createDefaultTabView(getContext());
			tabView.setText(itemInfo.definition.getTitle());
	        TabUtils.setTabImage(tabView, itemInfo.definition.getImageName(), null);
	        applyTabItemClass(tabView);

	        itemInfo.titleView = tabView;
			return tabView;
		}
	}

	private class TabsPagerAdapter extends FragmentPagerAdapter
	{
		public TabsPagerAdapter()
		{
			super(mActivity.getSupportFragmentManager());
		}

		@Override
		public Fragment getItem(int position)
		{
			TabItemInfo tabItem = mTabItems.get(position);
			return tabItem.holderFragment;
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			TabItemInfo tabItem = mTabItems.get(position);
			return tabItem.definition.getTitle();
		}

		@Override
		public int getCount()
		{
			return mTabItems.size();
		}
	}

	private final ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener()
	{
		@Override
		public void onPageSelected(int position)
		{
			mCurrentTabIndex = position;
			TabItemInfo tabItem = mTabItems.get(position);
			setTabItemAsSelected(tabItem);

			if (!tabItem.started)
			{
				// This is the first time switching to this tab.
				// Launch the tab action, eventually the tab content will be filled.
				// Set a tag on the UIContext so that we can intercept the first object call and treat it as the tab content.
				UIContext context = UIContext.base(mActivity, mDashboard.getConnectivitySupport());
				context.setTag(CONTEXT_TAG_DASHBOARD_ITEM_INDEX, position);
				tabItem.started = true;

				startTab(context, tabItem);
			}
			else
			{
				// Refresh data when switching back to a tab (content may not be available yet, that case will be ignored).
				updateCurrentContent(true);
			}
		}

		private void setTabItemAsSelected(TabItemInfo tabItem)
		{
			for (TabItemInfo otherTabItem : mTabItems)
			{
				if (otherTabItem != tabItem)
				{
					if (otherTabItem.titleView.isSelected())
					{
						otherTabItem.titleView.setSelected(false);
						applyTabItemClass(otherTabItem.titleView);
					}
				}
			}

			tabItem.titleView.setSelected(true);
			applyTabItemClass(tabItem.titleView);
		}

		@Override
		public void onPageScrollStateChanged(int state) { }

		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
	};

	private void startTab(final UIContext context, final TabItemInfo tabItem)
	{
		if (tabItem.definition.getKind() == DashboardMetadataLoader.COMPONENT_KIND)
		{
			// Special case, show WebView.
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					ComponentParameters webParams = new ComponentParameters(tabItem.definition.getObjectName());
					handle(new UIObjectCall(context, webParams), null);
				}
			});
		}
		else
		{
			// Standard case, normal action. Will eventually display a Fragment.
			CompositeAction action = ActionFactory.getAction(context, tabItem.definition.getActionDefinition(), new ActionParameters(mData));
			ActionExecution exec = new ActionExecution(action);
			exec.executeAction();
		}
	}

	private void updateCurrentContent(boolean refresh)
	{
		Fragment fragment = mTabItems.get(mCurrentTabIndex).contentFragment;

		if (fragment != mCurrentTabFragment)
		{
			// Mark the previous tab as inactive.
			if (mCurrentTabFragment != null && mCurrentTabFragment instanceof IDataView)
				((IDataView)mCurrentTabFragment).setActive(false);

			mCurrentTabFragment = fragment;

			if (fragment instanceof IDataView)
			{
				IDataView dataView = (IDataView)fragment;
				dataView.setActive(true);

				if (refresh)
					dataView.refreshData(true);
			}
		}
	}

	@Override
	public NavigationHandled handle(final UIObjectCall call, Intent callIntent)
	{
		// If instantiating a Tab, intercept the call. Otherwise let it fall through.
		Integer tabIndex = (Integer)call.getContext().getTag(CONTEXT_TAG_DASHBOARD_ITEM_INDEX);
		if (tabIndex != null)
		{
			final TabItemInfo tabItem = mTabItems.get(tabIndex);
			if (!tabItem.started || tabItem.contentFragment != null)
				throw new IllegalStateException("Invalid state when about to set tab item content!");

			Services.Device.invokeOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					// The ViewPager is already measured so we can check getWidth()/getHeight() to figure content size.
					int contentWidth = mViewPager.getWidth() - mViewPager.getPaddingLeft() - mViewPager.getPaddingRight();
					int contentHeight = mViewPager.getHeight() - mViewPager.getPaddingTop() - mViewPager.getPaddingBottom();
					Size fragmentSize = new Size(contentWidth, contentHeight);

					Fragment calledFragment;
					ComponentParameters params = call.toComponentParams();
					ComponentUISettings uiSettings = new ComponentUISettings(false, null, fragmentSize);

					if (params.Type == ComponentType.Form)
						calledFragment = mActivity.createComponent(tabItem.getComponentId(), params, uiSettings);
					else
						calledFragment = WebViewFragment.newInstance(params.Url);

					// Add the content fragment inside the placeholder.
					tabItem.contentFragment = calledFragment;
					tabItem.holderFragment.setContentFragment(calledFragment);

					if (tabItem.index == mCurrentTabIndex)
						updateCurrentContent(false);
				}
			});

			return NavigationHandled.HANDLED_CONTINUE;
		}

		return NavigationHandled.NOT_HANDLED;
	}

	private void applyTabItemClass(TextView tabTitleView)
	{
		if (mThemeClassForTabs != null)
			TabUtils.applyTabItemClass(tabTitleView, mThemeClassForTabs.getUnselectedPageClass(), mThemeClassForTabs.getSelectedPageClass());
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (mCurrentTabFragment != null && mCurrentTabFragment instanceof IFragmentHandleKeyEvents)
		{
			IFragmentHandleKeyEvents f = (IFragmentHandleKeyEvents)mCurrentTabFragment;
			if (f.onKeyUp(keyCode, event))
				return true;
		}

		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void saveActivityState(LayoutFragmentActivityState outState)
	{
		super.saveActivityState(outState);
		outState.setProperty(STATE_CURRENT_TAB, mCurrentTabIndex);

		if (mClientStartExecuted)
			outState.setProperty(STATE_DASHBOARD_DATA, mData);
	}
}
