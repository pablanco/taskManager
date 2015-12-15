package com.artech.controls;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.RecyclerListener;

import com.artech.R;
import com.artech.android.layout.GridContext;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.layout.CellDefinition;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.GridDefinition.SelectionType;
import com.artech.base.metadata.layout.ILayoutActionDefinition;
import com.artech.base.metadata.layout.TableDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.base.providers.GxUri;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.UIActionHelper;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.controllers.ViewData;
import com.artech.controls.grids.GridAdapter;
import com.artech.controls.grids.GridItemLayout;
import com.artech.controls.grids.ISupportsEditableControls;
import com.artech.controls.grids.ISupportsMultipleSelection;

public class GxListView extends android.widget.ListView
	implements IGridView, IGxThemeable, ISupportsMultipleSelection, ISupportsEditableControls, RecyclerListener
{
	private ListViewHelper mHelper;

	private GridContext mContext;
	private GridDefinition mDefinition;
	private GridAdapter mAdapter;

	private boolean mInSelectionMode;
	private ActionDefinition mInSelectionForAction;
	private boolean mWithSelection;

	private Drawable mDefaultSelector;
	private Drawable mDefaultDivider;

	private ThemeClassDefinition mThemeClass;

	public GxListView(Context context, GridDefinition definition)
	{
		super(context);
		init(context, definition);
	}

	public GxListView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public GxListView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	private void init(Context context, GridDefinition definition)
	{
		mContext = (GridContext) context;
		mDefinition = definition;
		mHelper = new ListViewHelper(this, mDefinition);

		// Store default drawables from style, if they need to be restored later.
		mDefaultSelector = getSelector();
		mDefaultDivider = getDivider();

		// Although descendant views may receive focus, the ListView itself doesn't need it.
		setFocusable(false);

		setRecyclerListener(this);

		// Important: BEFORE setAdapter().
		mHelper.showFooter(true, Strings.EMPTY);

		// Fix transparency for background color.
		// See: http://android-developers.blogspot.com/2009/01/why-is-my-list-black-android.html
		setCacheColorHint(0);

		// Set the separator from theme class, keep default from style.
		updateSeparator(new GxHorizontalSeparator(definition));
	}

	public GridDefinition getDefinition()
	{
		return mDefinition;
	}

	public boolean handlesClicksOn(GridItemLayout itemView)
	{
		return (mDefinition.getDefaultAction() != null ||
				mHelper.hasDifferentLayoutWhenSelected(itemView.getEntity()));
	}

	@Override
	public boolean performItemClick(View view, int position, long id)
	{
		// Base handling of item click is IGNORED in case we are selecting items in a pre-honeycomb device,
		// or with a current selection in honeycomb; otherwise the default action would be ignored for
		// grids with selection.
		boolean ignoreBase = (!CompatibilityHelper.isHoneycomb() && mInSelectionMode);
		boolean continueAfterBase = (CompatibilityHelper.isHoneycomb() && mInSelectionMode && !mWithSelection);

		if (!ignoreBase && super.performItemClick(view, position, id))
		{
			if (!continueAfterBase)
				return true;
		}

		Entity item = mAdapter.getEntity(position);
		if (item != null && mDefinition.getSelectionMode() == GridDefinition.SELECTION_NONE)
		{
			Entity newSelection = item;
			Entity previousSelection = mContext.getSelection();
			boolean selectionChanged = false;

			if (newSelection != previousSelection)
			{
				mContext.setSelection(item);
				selectionChanged = true;
			}
			else
			{
				// Tapped on the selected item: Should it be deselected or remain selected?
				if (mDefinition.getSelectionType() == SelectionType.KeepUntilNewSelection)
				{
					mContext.setSelection(null);
					selectionChanged = true;
				}
			}

			// Force a re-layout, if necesssary.
			if (selectionChanged && (mHelper.hasDifferentLayoutWhenSelected(newSelection) || mHelper.hasDifferentLayoutWhenSelected(previousSelection)))
				mAdapter.notifyDataSetChanged();
		}

		return mHelper.runDefaultAction(item);
	}

	@Override
	public void setLayoutParams(ViewGroup.LayoutParams params)
	{
		mHelper.adjustMargins(params);
		super.setLayoutParams(params);
	}

	@Override
	public void update(ViewData data)
	{
		setFastScrollEnabled(data.getUri());
		prepareAdapter();
		mAdapter.setData(data);
		updateSelection(data);

		// Important: AFTER setAdapter().
		// removeFooterView() cannot be called before setting adapter, and addFoterView()
		// has already been called.
		mHelper.showFooter(data.isMoreAvailable(), data.getStatusMessage());
	}

	private void prepareAdapter()
	{
		if (mAdapter == null)
		{
			mAdapter = new GridAdapter(mContext.getBaseContext(), mHelper, mDefinition);
			mAdapter.setSelectionMode(mInSelectionMode);
			mAdapter.adjustSizeWithMarginPadding(mDefinition);
			setAdapter(mAdapter);
		}
	}

	private void setFastScrollEnabled(GxUri uri)
	{
		if (uri != null && uri.getOrder() != null && uri.getOrder().getEnableAlphaIndexer())
		{
			setFastScrollEnabled(true);
			//TODO : see how to fix alpha index with fill_parent
			// http://code.google.com/p/android/issues/detail?id=9054
			// http://stackoverflow.com/questions/2912082/section-indexer-overlay-is-not-updating-as-the-adapters-data-changes
			//ViewGroup.LayoutParams params = this.getLayoutParams();
			//params.height = LayoutParams.WRAP_CONTENT;
			//params.width = LayoutParams.WRAP_CONTENT;
			//this.setLayoutParams(params);
		}
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

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		mHelper.setThemeClass(themeClass);
		updateSeparator(new GxHorizontalSeparator(mDefinition, themeClass));
		updateSelector(themeClass);
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	private void updateSeparator(GxHorizontalSeparator separator)
	{
		setHeaderDividersEnabled(false);
		setFooterDividersEnabled(false);

		if (separator.isVisible())
		{
			if (!separator.isDefault())
			{
				setDivider(separator.getDrawable());
				setDividerHeight(separator.getHeight());
			}
			else
				setDivider(mDefaultDivider);
		}
		else
		{
			setDivider(null);
			setDividerHeight(0);
		}
	}

	private void updateSelector(ThemeClassDefinition themeClass)
	{
		if (mDefinition.getDefaultAction() != null)
		{
			if (themeClass != null)
			{
				if ((themeClass.getThemeGridEvenRowClass() != null && themeClass.getThemeGridEvenRowClass().hasHighlightedBackground()) ||
					(themeClass.getThemeGridOddRowClass() != null && themeClass.getThemeGridOddRowClass().hasHighlightedBackground()))
				{
					// Remove the default selector if the row classes have highlighted background.
					setSelector(android.R.color.transparent);
				}
				else
				{
					// Use the default selector to act as highlighted background color.
					setSelector(mDefaultSelector);
				}
			}
		}
		else
		{
			// Disable selector if the grid doesn't have a default action.
			setSelector(android.R.color.transparent);
		}
	}

	@Override
	public void addListener(GridEventsListener listener)
	{
		mHelper.setListener(listener);
		setOnScrollListener(mOnScrollList);
	}

	private final OnScrollListener mOnScrollList = new OnScrollListener()
	{
		@Override
		public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3)
		{
			mHelper.onScroll();
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) { }
	};

	@Override
	public void onMovedToScrapHeap(View view)
	{
		mHelper.discardItemView(view);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		mHelper.beginOnMeasure();
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		mHelper.endOnMeasure();
	}

	/**
	 * Calculates the height in pixels necessary to fit the current grid data:
	 * size of items (including their margins, paddings, separators and group headers)
	 * plus grid padding and margin.
	 */
	public int calculateAutoHeight()
	{
		int itemCount = mAdapter.getCount();
		if (itemCount == 0)
			return 0;

		int totalItemHeight = 0;
		int maxPaddingMarginsGrid = getGridMaxPaddingMarginsOddEven(mDefinition);
		if (mDefinition.getItemLayouts().size() > 1)
		{
			// Iterate over each item, because they may have different item layouts (possibly with different heights).
			for (Entity dataItem : mAdapter.getData().getEntities())
			{
				TableDefinition itemLayout = mHelper.getLayoutFor(dataItem);
				totalItemHeight += calculateItemAutoHeight(itemLayout, maxPaddingMarginsGrid);
			}
		}
		else
		{
			// Simply multiply the number of items by the height of the only possible layout.
			totalItemHeight = itemCount * calculateItemAutoHeight(mDefinition.getDefaultItemLayout(), maxPaddingMarginsGrid);
		}

		// Add space for "Break by" separators, if used.
		if (mDefinition.hasBreakBy())
		{
			// We create a group header TextView to measure it _for each time the header appears_
			// since a long text may produce word wrap, and hence a different height.
			GridItemLayout dummyItem = (GridItemLayout)LayoutInflater.from(getContext()).inflate(R.layout.grid_item_with_break, this, false);
			GxTextView dummyHeader = (GxTextView)dummyItem.findViewById(R.id.grid_item_header_text);
			mHelper.applyGroupHeaderClass(dummyHeader);

			int headerWidth = ((CellDefinition)mDefinition.getParent()).getAbsoluteWidth();

			for (int i = 0; i < itemCount; i++)
			{
				if (mAdapter.isGroupHeaderVisible(i))
				{
					// Set the group header text to measure the view.
					dummyHeader.setText(mAdapter.getGroupHeaderText(i));
					dummyHeader.measure(MeasureSpec.makeMeasureSpec(headerWidth, MeasureSpec.AT_MOST), MeasureSpec.UNSPECIFIED);
					int headerHeight = dummyHeader.getMeasuredHeight();

					totalItemHeight += headerHeight;
				}
			}
		}

		// Add space for separators between items.
		int separatorHeight = getDividerHeight();
		int totalSeparatorHeight = separatorHeight * (itemCount - 1);

		int gridPaddingHeight = 0;
		if (mDefinition.getThemeClass() != null)
			gridPaddingHeight = mDefinition.getThemeClass().getPadding().getTotalVertical();

		//temp, TODO, check this, margins should not be necessary but works in an example.
		int gridMarginsHeight = 0;
		if (mDefinition.getThemeClass() != null)
			gridMarginsHeight = mDefinition.getThemeClass().getMargins().getTotalVertical();

		// Height of the grid is: padding of the grid + item heights + separator heights.
		return gridMarginsHeight + gridPaddingHeight + totalItemHeight + totalSeparatorHeight;
	}

	private static int calculateItemAutoHeight(TableDefinition itemLayout, int maxPaddingMarginsGrid)
	{
		int rowHeight = itemLayout.getAbsoluteHeight();

		int maxPaddingMargins = 0;
		if (itemLayout.getThemeClass() != null)
		{
			// Add top/bottom paddings from theme class.
			maxPaddingMargins += itemLayout.getThemeClass().getPadding().getTotalVertical();

			// Add top/bottom margins from theme class.
			// This must be considered for items (because the whole item view, including margins, is part of the grid)
			// However, not for the grid itself, because its margins are "outside".
			maxPaddingMargins += itemLayout.getThemeClass().getMargins().getTotalVertical();
		}
		maxPaddingMargins = Math.max(maxPaddingMarginsGrid, maxPaddingMargins);

		rowHeight += maxPaddingMargins;
		return rowHeight;
	}

	private static int getGridMaxPaddingMarginsOddEven(GridDefinition gridDefinition) {
		int maxPaddingMarginsGridResult = 0;
		int maxPaddingMarginsGrid = 0;
		if (gridDefinition.getThemeClass()!=null)
		{
			// Padding in Even
			if (gridDefinition.getThemeClass().getThemeGridEvenRowClass()!=null)
			{
				maxPaddingMarginsGrid += gridDefinition.getThemeClass().getThemeGridEvenRowClass().getPadding().getTotalVertical();
				maxPaddingMarginsGrid += gridDefinition.getThemeClass().getThemeGridEvenRowClass().getMargins().getTotalVertical();
			}

			maxPaddingMarginsGridResult = maxPaddingMarginsGrid;

			// Padding in Odd
			maxPaddingMarginsGrid = 0;
			if (gridDefinition.getThemeClass().getThemeGridOddRowClass()!=null)
			{
				maxPaddingMarginsGrid += gridDefinition.getThemeClass().getThemeGridOddRowClass().getPadding().getTotalVertical();
				maxPaddingMarginsGrid += gridDefinition.getThemeClass().getThemeGridOddRowClass().getMargins().getTotalVertical();

			}
			maxPaddingMarginsGridResult = Math.max(maxPaddingMarginsGrid, maxPaddingMarginsGridResult);

		}
		return maxPaddingMarginsGridResult;
	}

	@Override
	public void saveEditValues()
	{
		mHelper.saveEditValues();
	}

	@Override
	@TargetApi(11)
	public void setSelectionMode(boolean enabled, ActionDefinition forAction)
	{
		mInSelectionMode = enabled;

		// Notify adapter, needed to redraw views (to add or remove checkbox).
		if (mAdapter != null)
			mAdapter.setSelectionMode(enabled);

		if (enabled)
		{
			if (CompatibilityHelper.isHoneycomb())
			{
				setChoiceMode(CHOICE_MODE_MULTIPLE_MODAL);
				setMultiChoiceModeListener(new MultiChoiceModeListener());
				mInSelectionForAction = forAction;
			}
			else
				setChoiceMode(CHOICE_MODE_MULTIPLE);
		}
		else
		{
			setChoiceMode(CHOICE_MODE_NONE);
			clearChoices();
		}
	}

	private void updateSelection(ViewData data)
	{
		for (int i = 0; i < data.getEntities().size(); i++)
			setItemChecked(i, data.getEntities().get(i).isSelected());
	}

	@Override
	public void setItemSelected(int position, boolean selected)
	{
		setItemChecked(position, selected);
	}

	// Multi choice listener; ONLY for Honeycomb or above.
	@TargetApi(11)
	private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener
	{
		private List<ILayoutActionDefinition> mActions;

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu)
		{
			updateTitle(mode);
			mWithSelection = true;

			// Get the list of actions that apply to this multiple selection to build menu
			// (or show a single action if selection was started for a particular one).
			mActions = new ArrayList<ILayoutActionDefinition>();
			if (mInSelectionForAction != null)
			{
				// Find the LAYOUT action given its associated event, to get UI properties (icon, caption).
				for (ILayoutActionDefinition layoutAction : mDefinition.getMultipleSelectionActions())
				{
					if (layoutAction.getEvent() == mInSelectionForAction)
					{
						mActions.add(layoutAction);
						break;
					}
				}
			}
			else
				mActions.addAll(mDefinition.getMultipleSelectionActions());

			for (int i = 0; i < mActions.size(); i++)
			{
				ILayoutActionDefinition action = mActions.get(i);
				MenuItem item = menu.add(Menu.NONE, i, Menu.NONE, action.getCaption());
				UIActionHelper.setMenuItemImage(getContext(), item, action);
			}

			return true;
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
		{
			updateTitle(mode);
		}

		private void updateTitle(ActionMode mode)
		{
			int count = getCheckedItemCount();
			String title = String.format(Services.Strings.getResource(com.artech.R.string.GXM_SelectedItems), count);
			mode.setTitle(title);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu)
		{
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item)
		{
			// Perform the multiple selection action.
			ILayoutActionDefinition action = mActions.get(item.getItemId());
			mHelper.runExternalAction(action.getEvent());
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode)
		{
			mWithSelection = false;
		}
	}
}