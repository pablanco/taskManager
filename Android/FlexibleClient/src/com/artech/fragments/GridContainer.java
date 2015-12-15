package com.artech.fragments;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.artech.R;
import com.artech.actions.UIContext;
import com.artech.android.ViewHierarchyVisitor;
import com.artech.android.layout.GridContext;
import com.artech.android.layout.GxLayoutInTab;
import com.artech.android.layout.GxTheme;
import com.artech.android.layout.LayoutControlFactory;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.controls.IGxControlRuntimeContext;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.filter.FilterAttributeDefinition;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.Size;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.providers.GxUri;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.ExecutionContext;
import com.artech.common.ImageHelper;
import com.artech.common.SecurityHelper;
import com.artech.controllers.IDataSourceBoundView;
import com.artech.controllers.IDataSourceController;
import com.artech.controllers.IDataViewController;
import com.artech.controllers.ViewData;
import com.artech.controls.GxControlViewWrapper;
import com.artech.controls.GxImageViewStatic;
import com.artech.controls.GxListView;
import com.artech.controls.GxTextBlockTextView;
import com.artech.controls.IDataViewHosted;
import com.artech.controls.IGridView;
import com.artech.controls.IGxGridControl;
import com.artech.controls.IGxThemeable;
import com.artech.controls.LoadingIndicatorView;
import com.artech.controls.grids.IGridSite;
import com.artech.controls.grids.ISupportsEditableControls;
import com.artech.controls.grids.ISupportsMultipleSelection;
import com.artech.ui.Coordinator;
import com.artech.usercontrols.UcFactory;
import com.artech.utils.Cast;
import com.artech.utils.ThemeUtils;

/**
 * Container for all controls that implement a Grid interface.
 * Handles paging and necessary plumbing so that IGridViews only need to have an update([data]) method.
 */
public class GridContainer extends LinearLayout implements IGxGridControl, IDataViewHosted, IDataSourceBoundView, IGxThemeable, SwipeRefreshLayout.OnRefreshListener
{
	private IDataView mHost;
	private IDataSourceController mController;
	private final GridDefinition mDefinition;

	private final IGridView mGrid;
	private final View mGridView;
	private final GxImageViewStatic mEmptyDataSetImage;
	private final GxTextBlockTextView mEmptyDataSetText;
	private final TextView mStatusText;
	private final LoadingIndicatorView mLoadingIndicator;
	private final SwipeRefreshLayout mSwipeRefreshLayout;

	private ViewData mCurrentData;
	private boolean mNeedsMoreData;
	private final SecurityHelper.Token mSecurityToken;
	private ThemeClassDefinition mThemeClass;

	private final Coordinator mCoordinator;
	private final GridContext mGridContext;
	private final GxControlViewWrapper mControlWrapper;
	private UIContext mUIContext;

	public GridContainer(GridContext context, Coordinator coordinator, GridDefinition definition)
	{
		super(context);
		setWillNotDraw(true);

		mUIContext = context.getCoordinator().getUIContext();
		mDefinition = definition;
		mGridContext = context;
		mCoordinator = coordinator;
		mControlWrapper = new GxControlViewWrapper(this);
		mSecurityToken = new SecurityHelper.Token();

		setOrientation(VERTICAL);

		// Place for a generic message, such as filtering/search/error notifications.
		mStatusText = new TextView(getContext());
		LayoutParams statusTextLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, Services.Device.dipsToPixels(48));
		statusTextLayoutParams.setMargins(Services.Device.dipsToPixels(8), 0, 0, 0);
		mStatusText.setLayoutParams(statusTextLayoutParams);
		mStatusText.setGravity(Gravity.CENTER_VERTICAL);
		mStatusText.setVisibility(GONE);
		addView(mStatusText);

		// Use factory to create underlying control (e.g. ListView, ImageGallery...)
		// NOTE: Coordinator is passed to grid control so that it can call events, but the grid is not registered
		// as a view for the Coordinator. Otherwise the gesture listener will mess up the default touch events.
		mGrid = UcFactory.createGrid(context, mCoordinator, mDefinition);
		mGridView = (View)mGrid;
		LayoutControlFactory.setDefinition(mGridView, mDefinition);
		mGrid.addListener(mRequestDataListener);

		if (mDefinition.hasAutoGrow() || mDefinition.getDataSource() == null || !mDefinition.getDataSource().getOrders().hasAnyWithAlphaIndexer())
		{
			// Use MATCH_PARENT unless we have alpha indexer, that doesn't seem to work in that case.
			mGridView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}

		// if pull to refresh , add a SwipeRefreshLayout wrapper to gridview and add this view to the layout.
		if (mDefinition.getHasPullToRefresh())
		{
			mSwipeRefreshLayout = new SwipeRefreshLayout(context);
			mSwipeRefreshLayout.addView(mGridView);
			mSwipeRefreshLayout.setOnRefreshListener(this);
			addView(mSwipeRefreshLayout);

			Integer refreshIndicatorColor = ThemeUtils.getAndroidThemeColorId(context, R.attr.colorAccent);
			if (refreshIndicatorColor != null)
				mSwipeRefreshLayout.setColorSchemeColors(refreshIndicatorColor);
		}
		else
		{
			mSwipeRefreshLayout = null;
			addView(mGridView);
		}

		// Add indicator for "empty data set".
		mEmptyDataSetImage = new GxImageViewStatic(context);
		mEmptyDataSetImage.setVisibility(GONE);
		mEmptyDataSetImage.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		addView(mEmptyDataSetImage);

		// Add indicator for "empty data set" as text.
		mEmptyDataSetText = new GxTextBlockTextView(context);
		mEmptyDataSetText.setVisibility(GONE);
		mEmptyDataSetText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mEmptyDataSetText.setGravity(Gravity.CENTER);
		addView(mEmptyDataSetText);

		// Add "loading" indicator.
		mLoadingIndicator = new LoadingIndicatorView(getContext());
		mLoadingIndicator.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		addView(mLoadingIndicator);
		setGridVisivility(GONE);

		if (mDefinition.getSelectionMode() == GridDefinition.SELECTION_ALWAYS)
			setSelectionMode(true, null);
	}

	@Override
	public String getName()
	{
		return mDefinition.getName();
	}

	@Override
	public GridDefinition getDefinition()
	{
		return mDefinition;
	}

	@Override
	public String getDataSourceId()
	{
		IDataSourceDefinition dataSource = getDataSource();
		if (dataSource != null)
			return dataSource.getName();
		else
			return mDefinition.getName();
	}

	@Override
	public IDataSourceDefinition getDataSource()
	{
		return mDefinition.getDataSource();
	}

	@Override
	public String getDataSourceMember()
	{
		// SDT collection, for now.
		return mDefinition.getDataSourceMember();
	}

	@Override
	public int getDataSourceRowsPerPage()
	{
		return mDefinition.getRowsPerPage();
	}

	@Override
	public void setController(IDataSourceController controller)
	{
		mController = controller;
		mNeedsMoreData = true;
	}

	@Override
	public boolean isActive()
	{
		if (!isShown())
			return false;

		GxLayoutInTab parentTab = ViewHierarchyVisitor.getParent(GxLayoutInTab.class, this);
		if (parentTab != null && !parentTab.isActiveTab())
			return false;

		return true;
	}

	public void setAbsoluteSize(Size size)
	{
		// setBounds() is currently done by LayoutFragmentAdapter. Override setLayoutParams if you want to keep size properties.
		// AdaptersHelper.setBounds(mDefinition.getItemTable(), size.getWidth(), size.getHeight(), getContext());
		if (Cast.as(IGridSite.class, mGrid) != null)
			((IGridSite)mGrid).setAbsoluteSize(size);
	}

	@Override
	public void update(ViewData data)
	{
		// Always remove loading indicator. Will be replaced either by Grid or empty view.
		mLoadingIndicator.setVisibility(GONE);
		if (mSwipeRefreshLayout != null)
			mSwipeRefreshLayout.setRefreshing(false);

		if (data.getDataUnchanged())
			return;

		Activity activity = Cast.as(Activity.class, mGridContext.getBaseContext());
		if (activity != null && SecurityHelper.handleSecurityError(mGridContext.getCoordinator().getUIContext(), data.getStatusCode(), data.getStatusMessage(), mSecurityToken) != SecurityHelper.Handled.NOT_HANDLED)
			return;

		if (mDefinition.getSelectionMode() != GridDefinition.SELECTION_NONE)
			prepareForSelection(data);

		updateStatus(data);

		mCurrentData = data;
		mNeedsMoreData = false; // Reset before update(), since Grid may request extra data immediately after updating.
		mGrid.update(data);

		if (data.getEntities().size() != 0)
		{
			// Make grid control visible if it wasn't.
			setGridVisivility(VISIBLE);
			mEmptyDataSetImage.setVisibility(GONE);
			mEmptyDataSetText.setVisibility(GONE);

			if (mDefinition.hasAutoGrow() && mGrid instanceof GxListView)
			{
				ViewGroup.LayoutParams p = getLayoutParams();
				p.height = ((GxListView)mGrid).calculateAutoHeight();
				setLayoutParams(p);
			}
		}
		else
		{
			// Make empty data set text (or image) visible (if applicable).
			if (Services.Strings.hasValue(mDefinition.getEmptyDataSetText()))
			{
				setGridVisivility(GONE);
				mEmptyDataSetText.setText(Html.fromHtml(mDefinition.getEmptyDataSetText()));
				GxTheme.applyStyle(mEmptyDataSetText, mDefinition.getEmptyDataSetTextClass());
				mEmptyDataSetText.setVisibility(VISIBLE);
			}
			else if (Services.Strings.hasValue(mDefinition.getEmptyDataSetImage()))
			{
				setGridVisivility(GONE);
				ImageHelper.displayImage(mEmptyDataSetImage, mDefinition.getEmptyDataSetImage());
				GxTheme.applyStyle(mEmptyDataSetImage, mDefinition.getEmptyDataSetImageClass());
				mEmptyDataSetImage.setVisibility(VISIBLE);
			}
			else
				setGridVisivility(VISIBLE);
		}
	}

	private void setGridVisivility(int visibility)
	{
		mGridView.setVisibility(visibility);
		if (mSwipeRefreshLayout != null)
			mSwipeRefreshLayout.setVisibility(visibility);

	}

	private void updateStatus(ViewData data)
	{
		ArrayList<String> messages = new ArrayList<String>();

		// Update the "searched/filtered" indicator.
		GxUri dataUri = data.getUri();
		if (dataUri != null)
		{
			// Search
			if (Services.Strings.hasValue(dataUri.getSearchText()))
				messages.add(Services.Strings.getResource(R.string.GXM_DataSearched, dataUri.getSearchText()));

			// Filters
			if (dataUri.hasFilterValues())
			{
				ArrayList<String> filteredBy = new ArrayList<String>();
				for (FilterAttributeDefinition filterAttribute : dataUri.getDataSource().getFilter().getAttributes())
				{
					if (dataUri.getFilter(filterAttribute) != null)
						filteredBy.add(filterAttribute.getDescription());
				}

				messages.add(Services.Strings.getResource(R.string.GXM_DataFiltered, Services.Strings.join(filteredBy, ", ")));
			}
		}

		mStatusText.setText(Services.Strings.join(messages, Strings.SPACE));
		mStatusText.setVisibility(messages.size() != 0 ? VISIBLE : GONE);
	}

	@Override
	public EntityList getData()
	{
		return (mCurrentData != null ? mCurrentData.getEntities() : new EntityList());
	}

	public void saveEditValues()
	{
		if (mGrid instanceof ISupportsEditableControls)
			((ISupportsEditableControls)mGrid).saveEditValues();
	}

	@Override
	public IDataView getHost()
	{
		if (mHost == null)
		{
			// This is ugly as hell.
			// Ideally the GridContainer should receive the host as a constructor parameter,
			// but that is very difficult when creating nested grids. So we either use the provided
			// one or try to get it from the view hierarchy.
			for (ViewParent parent = getParent(); parent != null; parent = parent.getParent())
			{
				if (parent instanceof IDataViewHosted)
				{
					mHost = ((IDataViewHosted)parent).getHost();
					break; // Do not continue upwards, if previous hosted didn't have a host, no one else does either.
				}
			}
		}

		return mHost;
	}

	@Override
	public void setHost(IDataView host)
	{
		mHost = host;
	}

	private final IGridView.GridEventsListener mRequestDataListener = new IGridView.GridEventsListener()
	{
		@Override
		public void requestMoreData()
		{
			if (mController == null)
				return;

			// Ignore new request if a previous one is pending.
			if (mNeedsMoreData)
				return;

			// Ignore requests for more data if last request caused a network error.
			if (mCurrentData != null && mCurrentData.hasErrors())
				return;

			mNeedsMoreData = true;
			mController.onRequestMoreData();
		}

		@Override
		public boolean runDefaultAction(UIContext context, Entity entity)
		{
			IDataViewController hostController = getHostController();
			if (hostController == null)
				return false;

			if (hostController.handleSelection(entity))
				return true;

			// Execute the default action for the Grid.
			if (mDefinition == null || mDefinition.getDefaultAction() == null)
				return false;

			return runAction(context, mDefinition.getDefaultAction(), entity);
		}

		@Override
		public boolean runAction(UIContext context, ActionDefinition action, Entity entity)
		{
			IDataViewController hostController = getHostController();
			if (hostController == null)
				return false;

			if (mCurrentData == null || action == null)
				return false;

			// Set current entity to evaluate expressions like '&Collection.CurrentItem.X'
			if (entity != null)
				mCurrentData.getEntities().setCurrentEntity(entity);

			if (getHost() != null)
			{
				// Ask host to move values to its entity (needed if variables from form are used in action).
				Entity hostEntity = getHost().getContextEntity();

				// Context and entity may be null if executing an action that is NOT associated to a grid item.
				if (context == null)
					context = getHost().getUIContext();

				if (entity == null)
					entity = hostEntity;
			}

			hostController.runAction(context, action, entity);
			return true;
		}

		private IDataViewController getHostController()
		{
			if (mController != null)
				return mController.getParent();

			IDataView host = getHost();
			if (host != null && host.getController() != null)
				return host.getController();

			Services.Log.warning("GridContainer has neither a specific controller nor an associated host with a controller."); //$NON-NLS-1$
			return null;
		}

		@Override
		public UIContext getHostUIContext() {
			return mUIContext;
		}
	};

	@Override
	public boolean needsMoreData()
	{
		return mNeedsMoreData;
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		mThemeClass = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass()
	{
		return mThemeClass;
	}

	private void prepareForSelection(ViewData data)
	{
		// Notify items if a particular expression should be used to evaluate selection.
		String selectionExpression = mDefinition.getSelectionExpression();
		for (Entity item : data.getEntities())
			item.setSelectionExpression(selectionExpression);
	}

	@Override
	public void setSelectionMode(boolean enabled, ActionDefinition forAction)
	{
		if (!enabled && mCurrentData != null)
		{
			for (Entity entity : mCurrentData.getEntities())
				entity.setIsSelected(false);
		}

		if (mGrid != null)
		{
			if (mGrid instanceof ISupportsMultipleSelection)
				((ISupportsMultipleSelection)mGrid).setSelectionMode(enabled, forAction);
			else
				Services.Log.warning(String.format("'%s' does not support multiple selection.", mGrid.getClass().getName()));
		}

		// Selection cannot be fully disabled if working on "always on" selection mode.
		// So enabled=false clears selection, and we re-enable it afterwards.
		if (!enabled && mDefinition.getSelectionMode() == GridDefinition.SELECTION_ALWAYS)
			setSelectionMode(true, null);
	}

	@Override
	public void setFocus(boolean showKeyboard)
	{
		mControlWrapper.setFocus(showKeyboard);
	}

	@Override
	public void setVisible(boolean visible)
	{
		mControlWrapper.setVisible(visible);
	}

	@Override
	public void setCaption(String caption)
	{
		mControlWrapper.setCaption(caption);
	}

	@Override
	public void setExecutionContext(ExecutionContext context)
	{
		// Pass properties to the custom control if it supports them.
		if (mGrid instanceof IGxControlRuntimeContext)
			((IGxControlRuntimeContext)mGrid).setExecutionContext(context);
	}

	@Override
	public Object getProperty(String name)
	{
		// Retrieve properties from custom control if it supports them.
		if (mGrid instanceof IGxControlRuntime)
			return ((IGxControlRuntime)mGrid).getProperty(name);
		else
			return null;
	}

	@Override
	public void setProperty(String name, Object value)
	{
		// Pass properties to the custom control if it supports them.
		if (mGrid instanceof IGxControlRuntime)
			((IGxControlRuntime)mGrid).setProperty(name, value);
	}

	private static final String METHOD_REFRESH = "Refresh";

	@Override
	public void runMethod(String name, List<Object> parameters)
	{
		// Grid.Refresh() is a standard method, handle it here instead of passing it on to concrete grids.
		if (METHOD_REFRESH.equalsIgnoreCase(name))
		{
			refreshData(false);
		}
		else if (mGrid instanceof IGxControlRuntime)
		{
			// Pass on the method to the custom grid.
			((IGxControlRuntime)mGrid).runMethod(name, parameters);
		}
	}

	private void refreshData(boolean keepPosition)
	{
		if (mController != null)
			mController.getParent().getParent().onRefresh(mController, keepPosition);
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		if (mGrid instanceof IGxThemeable)
			((IGxThemeable)mGrid).setThemeClass(themeClass);
	}

	@Override
	public boolean isVisible()
	{
		return mControlWrapper.isVisible();
	}

	@Override
	public String getCaption()
	{
		return mControlWrapper.getCaption();
	}

	// SwipeRefreshLayout.OnRefreshListener.onRefresh()
	@Override
	public void onRefresh()
	{
		refreshData(true);
	}
}
