package com.artech.controls.grids;

import java.util.WeakHashMap;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.TextView;

import com.artech.R;
import com.artech.actions.UIContext;
import com.artech.activities.ActivityHelper;
import com.artech.adapters.AdaptersHelper;
import com.artech.android.ViewHierarchyVisitor;
import com.artech.android.layout.ControlProperties;
import com.artech.android.layout.ControlPropertiesDefaults;
import com.artech.android.layout.DynamicProperties;
import com.artech.android.layout.GridContext;
import com.artech.android.layout.GxLayout;
import com.artech.android.layout.LayoutBuilder;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.metadata.enums.LayoutModes;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.TableDefinition;
import com.artech.base.metadata.theme.LayoutBoxMeasures;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.controls.DataBoundControl;
import com.artech.controls.GxImageViewData;
import com.artech.controls.IGridView;
import com.artech.controls.IGxActionControl;
import com.artech.ui.Anchor;
import com.artech.ui.Coordinator;
import com.artech.ui.CoordinatorAdvanced;
import com.artech.ui.GridItemCoordinator;
import com.artech.utils.BackgroundOptions;
import com.artech.utils.Cast;
import com.artech.utils.ThemeUtils;
import com.fedorvlasov.lazylist.ImageLoader;

/**
 * Helper class for common functionality for grid controls.
 */
public class GridHelper
{
	public static final String PROPERTY_ITEM_LAYOUT = "ItemLayout";
	public static final String PROPERTY_ITEM_SELECTED_LAYOUT = "ItemSelectedLayout";

	private final View mGrid;
	private final GridDefinition mDefinition;
	private final boolean mSupportReuse;
	private Integer mItemViewResourceId;

	private Activity mActivity;
	private LayoutBoxMeasures mDeferredMargins;
	private IGridView.GridEventsListener mEventsListener;
	private WeakHashMap<Entity, GridItemUIContext> mGridItemContext;
	private ControlPropertiesDefaults mControlPropertiesDefaults;

	private GridItemLayoutVersion mItemLayoutVersion;
	private Coordinator mCoordinator;

	public GridHelper(View grid, GridDefinition definition)
	{
		this(grid, definition, true);
	}
	

	public void setCoordinator(Coordinator coordinator) {
		mCoordinator = coordinator;
	}
	
	public Coordinator getCoordinator() {
		return mCoordinator;
	}

	public GridHelper(View grid, GridDefinition definition, boolean supportItemViewReuse)
	{
		mGrid = grid;
		mDefinition = definition;
		mSupportReuse = !Services.Application.isLiveEditingEnabled() && supportItemViewReuse;

		mGridItemContext = new WeakHashMap<Entity, GridItemUIContext>();
		mControlPropertiesDefaults = new ControlPropertiesDefaults(definition.getLayout());

		// Don't care about actual values, just need to detect when changed.
		mItemLayoutVersion = new GridItemLayoutVersion(-1, -1, -1);
	}

	private Context getContext()
	{
		return mGrid.getContext();
	}

	public GridDefinition getDefinition()
	{
		return mDefinition;
	}

	public View getGridView()
	{
		return mGrid;
	}

	public void setBounds(int width, int height)
	{
		if (width != mItemLayoutVersion.itemWidth || height != mItemLayoutVersion.itemHeight)
		{
			for (TableDefinition itemLayout : mDefinition.getItemLayouts())
				AdaptersHelper.setBounds(itemLayout, width, height);

			mItemLayoutVersion = new GridItemLayoutVersion(width, height, mItemLayoutVersion.itemReservedWidth);
		}
	}

	public void setReservedSpace(int reservedWidth)
	{
		if (reservedWidth != mItemLayoutVersion.itemReservedWidth)
		{
			for (TableDefinition itemLayout : mDefinition.getItemLayouts())
				itemLayout.recalculateBounds(reservedWidth);

			mItemLayoutVersion = new GridItemLayoutVersion(mItemLayoutVersion.itemWidth, mItemLayoutVersion.itemHeight, reservedWidth);
		}
	}

	public void setListener(IGridView.GridEventsListener listener)
	{
		mEventsListener = listener;
	}

	// ********************************************************************
	// Event handlers for requesting data, default action and buttons.

	public void requestMoreData()
	{
		if (mEventsListener != null)
			mEventsListener.requestMoreData();
	}

	/**
	 * Run an action that is NOT associated to the grid (e.g. a multiple selection action).
	 * @return
	 */
	public boolean runExternalAction(ActionDefinition action)
	{
		saveEditValues();
		if (mEventsListener != null)
			return mEventsListener.runAction(null, action, null);
		else
			return false;
	}

	/**
	 * Run an action in the context of the grid item.
	 * @param action Action definition.
	 * @param entity Entity corresponding to the grid item.
	 */
	public boolean runAction(ActionDefinition action, Entity entity, Anchor anchor)
	{
		saveEditValues();
		if (mEventsListener != null)
			return mEventsListener.runAction(getUIContextFor(entity, anchor), action, entity);
		else
			return false;
	}

	/**
	 * Run the default action associated to the grid.
	 * @param entity Entity corresponding to the grid item.
	 */
	public boolean runDefaultAction(Entity entity)
	{
		return runDefaultAction(entity, null);
	}

	public boolean runDefaultAction(Entity entity, Anchor anchor)
	{
		saveEditValues();
		if (mEventsListener != null)
			return mEventsListener.runDefaultAction(getUIContextFor(entity, anchor), entity);
		else
			return false;
	}

	// Executed when an "in grid action" is fired.
	private OnClickListener mActionHandler = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			if (mEventsListener == null)
				return;

			if (!(v instanceof IGxActionControl))
				return;

			IGxActionControl action = (IGxActionControl)v;
			runAction(action.getAction(), action.getEntity(), new Anchor(v));
		}
	};

	private UIContext getUIContextFor(Entity entity, Anchor anchor)
	{
		UIContext itemContext = mGridItemContext.get(entity);
		if (itemContext == null)
			itemContext = mEventsListener.getHostUIContext();

		itemContext.setAnchor(anchor);
		return itemContext;
	}

	public Activity getActivity()
	{
		// Try to get activity from grid view context (by casting or via base context).
		// As a last resort fall on ActivityHelper's current activity.
		if (mActivity == null)
		{
			Context gridContext = getContext();
			if (gridContext instanceof Activity)
				mActivity = (Activity)gridContext;
			else if ((gridContext instanceof ContextWrapper) && ((ContextWrapper)gridContext).getBaseContext() instanceof Activity)
				mActivity = (Activity)((ContextWrapper)gridContext).getBaseContext();
			else
				mActivity = ActivityHelper.getCurrentActivity();
		}

		return mActivity;
	}

	// Executed when an "domain" action is fired.
	private OnClickListener mDomainActionHandler = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			if (mEventsListener == null)
				return;

			if (!(v instanceof DataBoundControl))
				return;

			saveEditValues();
			DataBoundControl actionDomainControl = (DataBoundControl)v;
			AdaptersHelper.launchDomainAction(getUIContextFor(actionDomainControl.getEntity(), new Anchor(v)), v, actionDomainControl.getEntity());
		}
	};

	public void saveEditValues()
	{
		// Post pending edits in visible items (those that were recycled were previously posted).
		for (GridItemLayout gridItem : ViewHierarchyVisitor.getViews(GridItemLayout.class, mGrid))
			saveEditValues(gridItem.getItemInfo());
	}

	private void saveEditValues(GridItemViewInfo gridItem)
	{
		// Post any pending edits to the underlying data.
		// Necessary before running any actions that may depend on typed values (otherwise
		// the old value will be used), and before reusing the view (otherwise the edited value is lost).
		if (gridItem.getData() != null)
			AdaptersHelper.saveEditValues(gridItem.getBoundViews(), gridItem.getData());
	}

	// ********************************************************************
	// Expand view.

	private LayoutInflater mInflater;
	private boolean mNotReuseViews;

	/**
	 * Called to create the view corresponding to an item.
	 * @param previousView View to reuse, if the control supports it.
	 */
	public GridItemViewInfo getItemView(IGridAdapter adapter, int position, View previousView, boolean inSelectionMode)
	{
		if (previousView != null && previousView.getTag(R.id.tag_grid_item_view_in_use) != null)
			throw new IllegalArgumentException("If passing in a previousView for reusing, make sure discardItemView() was called on it before.");

		// Initialize LayoutInflater only once.
		if (mInflater == null)
			mInflater = LayoutInflater.from(getContext());

		Entity item = adapter.getEntity(position);

		View view;
		boolean shouldExpandLayout;
		GridItemViewInfo itemView = GridItemViewInfo.fromView(previousView);

		// A GridItemView keeps references to children views to avoid repeated calls to findViewById() on each row.
		// If the supplied view is not null, reuse it directly (unless drawListItem instructs us not to do so).
		if (previousView == null || itemView == null  || itemView.getVersion() == null || !itemView.getVersion().equals(mItemLayoutVersion))
		{
			int layoutResId = getItemViewResourceId();
			view = mInflater.inflate(layoutResId, null);
			shouldExpandLayout = true;
		}
		else
		{
			view = previousView;
			shouldExpandLayout = mNotReuseViews;
		}

		if (shouldExpandLayout)
		{
			// Create a coordinator for this item view.
			GridItemCoordinator coordinator = new GridItemCoordinator(getUIContextFor(item, new Anchor(view)), this, item);

			itemView = createNewItemView((GridItemLayout)view, coordinator, getLayoutFor(item), adapter.getImageLoader());
			itemView.assignTo(view);
			itemView.setCoordinator(coordinator);

			// Set theme for group separator header, only the first time we expand the layout
			if (itemView.getHeaderText() != null)
				applyGroupHeaderClass(itemView.getHeaderText());
		}

		itemView.setData(mGrid, position, item);

		// If this row is being recreated, then get previous UI context and previous set properties, if any,
		// but associate to new view. Otherwise, create a new context and associate it.
		GridItemUIContext uiContext = mGridItemContext.get(item);
		if (uiContext == null)
		{
			uiContext = new GridItemUIContext(mEventsListener.getHostUIContext(), this, itemView);
			mGridItemContext.put(item, uiContext);
		}
		else
			uiContext.setGridItem(itemView);

		ThemeClassDefinition itemClass = null;
		if (mDefinition.getThemeClass() != null)
			itemClass = (position % 2 == 0 ? mDefinition.getThemeClass().getThemeGridOddRowClass() : mDefinition.getThemeClass().getThemeGridEvenRowClass());

		// If this is a recycled view then we need to reset any changed visual properties.
		if (!shouldExpandLayout)
			applyProperties(uiContext, mControlPropertiesDefaults);

		boolean drawNeedsDisableReuse = AdaptersHelper.drawListItem(adapter, itemView, position, item, itemClass,
			mActionHandler, mDomainActionHandler, mDefinition.getDataSource(), mNotReuseViews, inSelectionMode);

		applyDynamicProperties(uiContext, item);

		mNotReuseViews |= drawNeedsDisableReuse;

		itemView.getView().setTag(R.id.tag_grid_item_view_in_use, true);
		return itemView;
	}

	public void applyGroupHeaderClass(TextView groupHeader)
	{
		if (mDefinition.getThemeClass() != null)
		{
			ThemeClassDefinition groupHeaderClass = mDefinition.getThemeClass().getThemeGridGroupSeparatorClass();
			if (groupHeaderClass != null)
			{
				// Apply font and background.
				ThemeUtils.setFontProperties(groupHeader, groupHeaderClass);
				ThemeUtils.setBackgroundBorderProperties(groupHeader, groupHeaderClass, BackgroundOptions.defaultFor(mDefinition));

				// Apply padding.
				LayoutBoxMeasures padding = groupHeaderClass.getPadding();
				if (padding.isEmpty())
					groupHeader.setPadding(Services.Device.dipsToPixels(4), 0, 0, 0); // Default if empty.
				else
					groupHeader.setPadding(padding.left, padding.top, padding.right, padding.bottom);
			}
		}
	}

	@SuppressWarnings("ConstantConditions")
	private int getItemViewResourceId()
	{
		if (mItemViewResourceId == null)
		{
			boolean hasBreakBy = mDefinition.hasBreakBy();
			boolean hasSelection = (mDefinition.getSelectionMode() != GridDefinition.SELECTION_NONE);

			// Use a simple layout if possible, to avoid nesting controls.
			if (hasBreakBy && hasSelection)
				mItemViewResourceId = R.layout.grid_item_with_all;
			else if (hasBreakBy && !hasSelection)
				mItemViewResourceId = R.layout.grid_item_with_break;
			else if (!hasBreakBy && hasSelection)
				mItemViewResourceId = R.layout.grid_item_with_checkbox;
			else
				mItemViewResourceId = R.layout.grid_item_basic;
		}

		return mItemViewResourceId;
	}

	private GridItemViewInfo createNewItemView(GridItemLayout convertView, CoordinatorAdvanced coordinator, TableDefinition table, ImageLoader imageLoader)
	{
		GxLayout holderLayout = (GxLayout) convertView.findViewById(R.id.grid_item_content);
		holderLayout.setTag(table.getName());
		//remove and re add views
		holderLayout.removeAllViews();
		//clean previews view theme settings
		// background and borders
		CompatibilityHelper.setBackground(holderLayout, null);

		holderLayout.setLayout(coordinator, table.getContent());

		LayoutBuilder builder = new LayoutBuilder(convertView.getContext(), coordinator, imageLoader, LayoutModes.LIST, DisplayModes.VIEW, true);
		builder.expandLayout(holderLayout, table);

		// Creates a ViewHolder and store references to the children views we want to bind data to.
		return new GridItemViewInfo(convertView, mItemLayoutVersion, builder.getBoundViews(), holderLayout);
	}

	private void applyDynamicProperties(GridItemUIContext rowContext, Entity data)
	{
		// 1) Read dynprops from row data. These are the visual properties set in Load event.
		DynamicProperties dynProps = DynamicProperties.get(data);

		// 2) If we are about to change a visual property, then we must take note its default value,
		// so that it can be restored later when the view for that row is recycled.
		// If this is unsuccessful we disable reuse from now on.
		if (!mControlPropertiesDefaults.putDefaultsFor(dynProps))
			disableViewReuse();

		// 3) Construct the set of visual properties to apply. These comprise the ones assigned in the Load event,
		// plus any properties assigned by actions on this row, before its view was recycled.
		ControlProperties rowProps = new ControlProperties();
		rowProps.putAll(dynProps);
		rowProps.putAll(rowContext.getAssignedControlProperties());

		// 4) Apply the full set. Don't consider these ase "properties assigned by actions on this row".
		applyProperties(rowContext, rowProps);
	}

	private static void applyProperties(GridItemUIContext rowContext, ControlProperties properties)
	{
		rowContext.setControlPropertiesTrackingEnabled(false);
		properties.apply(rowContext);
		rowContext.setControlPropertiesTrackingEnabled(true);
	}

	public TableDefinition getLayoutFor(Entity item)
	{
		return getLayoutFor(item, isSelected(item));
	}

	public boolean hasDifferentLayoutWhenSelected(Entity item)
	{
		if (item != null)
		{
			TableDefinition standardLayout = getLayoutFor(item, false);
			TableDefinition selectedLayout = getLayoutFor(item, true);

			return (selectedLayout != standardLayout);
		}
		else
			return false;
	}

	private TableDefinition getLayoutFor(Entity item, boolean isSelected)
	{
		DynamicProperties dynProps = DynamicProperties.get(item);
		String itemLayoutName = dynProps.getStringProperty(mDefinition.getName(), PROPERTY_ITEM_LAYOUT);
		String selectedItemLayoutName = dynProps.getStringProperty(mDefinition.getName(), PROPERTY_ITEM_SELECTED_LAYOUT);

		if (isSelected)
		{
			TableDefinition selectedItemLayout = mDefinition.getItemLayout(selectedItemLayoutName);
			if (selectedItemLayout == null)
				selectedItemLayout = mDefinition.getDefaultSelectedItemLayout();

			if (selectedItemLayout != null)
				return selectedItemLayout;
		}

		// Not selected, or was selected but didn't have a "selected layout" set, nor a default one.
		TableDefinition itemLayout = mDefinition.getItemLayout(itemLayoutName);
		if (itemLayout == null)
			itemLayout = mDefinition.getDefaultItemLayout();

		return itemLayout;
	}

	private boolean isSelected(Entity item)
	{
		if (item.isSelected())
			return true;

		// TODO: matiash/gmilano, verify isSelected()! GridContext should not be necessary.
		GridContext context = Cast.as(GridContext.class, getContext());
		if (context != null)
		{
			if (item == context.getSelection())
				return true;
		}

		return false;
	}

	/**
	 * Called before a view is about to be destroyed. Must be called for grids that support editing.
	 */
	public void discardItemView(View view)
	{
		if (view != null)
			view.setTag(R.id.tag_grid_item_view_in_use, null);

		GridItemViewInfo viewInfo = GridItemViewInfo.fromView(view);
		if (viewInfo != null)
		{
			// Check all properties that were set on this row to ensure that they CAN be returned to their
			// default values. If not, it means that the row cannot be reused, so we disable reuse from here on.
			GridItemUIContext rowContext = mGridItemContext.get(viewInfo.getData());
			if (rowContext != null && !mControlPropertiesDefaults.putDefaultsFor(rowContext.getAssignedControlProperties()))
				disableViewReuse();

			// Before reusing the view, post its edited values to the underlying Entity.
			if (!isMeasuring())
				saveEditValues(viewInfo);

			// Release memory.
			viewInfo.setData(-1, null);
			for (GxImageViewData imageView : ViewHierarchyVisitor.getViews(GxImageViewData.class, view))
			{
				imageView.setGx_Value(null);
				imageView.setImageDrawable(null);
			}

			if (!mSupportReuse)
				GridItemViewInfo.discard(view);
		}
	}

	protected boolean isMeasuring()
	{
		return false;
	}

	void disableViewReuse()
	{
		mNotReuseViews = true;
	}

	// ********************************************************************
	// Theme and layout parameters.

	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		// Margins
		LayoutBoxMeasures margins = themeClass.getMargins();
		if (margins!=null)
		{
			ViewGroup.LayoutParams lp = mGrid.getLayoutParams(); // The layout could not be on site yet, differ the setting to the setLayoutParams
			if (lp != null)
			{
				MarginLayoutParams marginParms = Cast.as(MarginLayoutParams.class, lp); // does its site support margins?
				if (marginParms != null)
				{
					marginParms.setMargins( margins.left, margins.top,margins.right, margins.bottom);
					mGrid.setLayoutParams(lp);
				}
			}
			else
				mDeferredMargins = margins;
		}

		// Padding
		LayoutBoxMeasures padding = themeClass.getPadding();
		if (padding != null)
			mGrid.setPadding(padding.left, padding.top, padding.right, padding.bottom);

		// Background and Border.
		ThemeUtils.setBackgroundBorderProperties(mGrid, themeClass, BackgroundOptions.defaultFor(mDefinition));
	}

	public void adjustMargins(ViewGroup.LayoutParams params)
	{
		if (mDeferredMargins != null && Cast.as(MarginLayoutParams.class, params) != null)
			((MarginLayoutParams)params).setMargins(mDeferredMargins.left, mDeferredMargins.top,mDeferredMargins.right, mDeferredMargins.bottom);
	}
}
