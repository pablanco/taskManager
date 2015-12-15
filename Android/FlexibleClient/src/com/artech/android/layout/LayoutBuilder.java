package com.artech.android.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.artech.R;
import com.artech.android.layout.GxLayout.LayoutParams;
import com.artech.base.metadata.Properties;
import com.artech.base.metadata.layout.CellDefinition;
import com.artech.base.metadata.layout.ILayoutContainer;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.layout.RowDefinition;
import com.artech.base.metadata.layout.TableDefinition;
import com.artech.base.utils.MultiMap;
import com.artech.controllers.IDataSourceBoundView;
import com.artech.controls.DataBoundControl;
import com.artech.controls.GxHorizontalSeparator;
import com.artech.controls.GxLinearLayout;
import com.artech.controls.GxTextView;
import com.artech.controls.tabs.GxTabControl;
import com.artech.fragments.ComponentContainer;
import com.artech.fragments.GridContainer;
import com.artech.ui.CoordinatorAdvanced;
import com.artech.utils.Cast;
import com.artech.utils.ThemeUtils;
import com.fedorvlasov.lazylist.ImageLoader;

/***
 * The main class for expanding a Layout
 * @author GMilano
 *
 */
public class LayoutBuilder
{
	private final Context mContext;
	private final CoordinatorAdvanced mCoordinator;
	private final ImageLoader mImageLoader;

	private short mLayoutMode;
	private short mTrnMode;
	private boolean mAddDomainActions;

	private ArrayList<View> mBoundViews;
	private ArrayList<IDataSourceBoundView> mDataSourceBoundViews;
	private ArrayList<ComponentContainer> mComponentContainers;

	public LayoutBuilder(Context context, CoordinatorAdvanced coordinator, ImageLoader imageLoader, short layoutMode, short trnMode, boolean addDomainActions)
	{
		mContext = context;
		mCoordinator = coordinator;
		mImageLoader = imageLoader;

		mBoundViews = new ArrayList<View>();
		mDataSourceBoundViews = new ArrayList<IDataSourceBoundView>();
		mComponentContainers = new ArrayList<ComponentContainer>();

		// Maybe these could be changed later instead of fixed in constructor?
		mLayoutMode = layoutMode;
		mTrnMode = trnMode;
		mAddDomainActions = addDomainActions;
	}

	// Get collected results.
	public List<View> getBoundViews() { return mBoundViews; }
	public List<IDataSourceBoundView> getDataSourceBoundViews() { return mDataSourceBoundViews; }
	public List<ComponentContainer> getComponentContainers() { return mComponentContainers; }

	/**
	 * Create views from layout definition.
	 */
	public void expandLayout(GxLayout parent, TableDefinition table)
	{
		if (table == null)
		{
			putErrorNoLayout(parent);
			return;
		}

		initialize();
		LayoutControlFactory.setDefinition(parent, table);
		mCoordinator.addControl(parent, table);
		if (table.hasAutoGrow())
		{
			// Table layout should height all the size to align vertical to work
			int minHeight = table.getAbsoluteHeight();

			// However, if contained inside a GxLinearLayout that adds margins, the minimum size must go _there_.
			View viewWithHeight = parent;
			if (table.getThemeClass() != null && table.getThemeClass().hasMarginSet() && parent.getParent() instanceof GxLinearLayout)
				viewWithHeight = (GxLinearLayout)parent.getParent();

			viewWithHeight.setMinimumHeight(minHeight);
		}
		expandInnerLayout(parent, table);
		alignFields();
	}

	private void initialize()
	{
		mBoundViews.clear();
		mDataSourceBoundViews.clear();
		mComponentContainers.clear();
	}

	private GxLayout expandInnerLayout(ViewGroup parent, ILayoutContainer container)
	{
		// ViewGroup parent is GxTableLayout or GxLinearLayout
		// Get the main table for the container (all container MUST have a TableDefinition inside
		TableDefinition def = container.getContent();

		// Create a layout control to hold this definition.
		GxLayout layout = new GxLayout(mContext, def, mCoordinator);

		// Expand the definition inside the layout.
		expandInnerLayout(layout, container);

		// Finally add the view to the given container.
		ViewGroup.LayoutParams lpParent = parent.getLayoutParams();

		LinearLayout.LayoutParams lp;
		// if auto grow table, then set wrap content to GxLayout inside it
		if (lpParent!=null && lpParent.height == LayoutParams.WRAP_CONTENT)
			lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		else
			lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		parent.addView(layout, lp);

		return layout;
	}

	private GxLayout expandInnerLayout(GxLayout parent, ILayoutContainer container)
	{
		// Get the main table for the container (all container MUST have a TableDefinition inside
		TableDefinition def = container.getContent();

		// Expand Rows inside it the specified parent.
		expandRows(def, parent);

		// After processing the layout apply style. (should we do this before? in order to take into account style settings
		GxTheme.applyStyle(parent, def.getThemeClass());
		ThemeUtils.setBackground(def, parent, def.getThemeClass());

		return parent;
	}

	private void expandRows(TableDefinition table, GxLayout layout)
	{
		for (RowDefinition row : table.Rows)
			expandRow(table, layout, row);

		layout.updateHorizontalSeparators(new GxHorizontalSeparator(table));
	}

	private void expandRow(TableDefinition table, GxLayout layout, RowDefinition row)
	{
		ArrayList<CellDefinition> rowCells = new ArrayList<CellDefinition>(row.Cells);

		if (table.isCanvas())
		{
			// Canvas tables have only one row with all the canvas controls inside it. Create them
			// in their Z-order, since Android treats the order of a view's children as their z-order.
			// IMPORTANT: The sort MUST be stable (controls with equal z-order should not be shuffled).
			// Collections.sort() does (http://docs.oracle.com/javase/tutorial/collections/algorithms/#sorting)
			Collections.sort(rowCells, new Comparator<CellDefinition>()
			{
				@Override
				public int compare(CellDefinition lhs, CellDefinition rhs)
				{
					return Integer.valueOf(lhs.getZOrder()).compareTo(rhs.getZOrder());
				}
			});
		}

		for (CellDefinition cell : rowCells)
			expandCell(layout, cell);
	}

	private void expandCell(GxLayout layout, CellDefinition cell)
	{
		// Actually we support only one control per cell, so just take the first one
		if (cell.getChildItems().size() == 0)
			return;

		LayoutItemDefinition item = cell.getContent();
		if (item != null)
		{
			item.CellGravity = cell.CellGravity;
			View view = LayoutControlFactory.createView(mContext, mCoordinator, item, mImageLoader, mLayoutMode, mTrnMode, mAddDomainActions);
			if (view != null)
			{
				// Register as a view for this screen.
				mCoordinator.addControl(view, item);

				GxLayout.LayoutParams parms = new GxLayout.LayoutParams(cell, item, view);
				if (view instanceof GridContainer)
				{
					GridContainer grid = (GridContainer)view;
					grid.setAbsoluteSize(cell.getAbsoluteSize());
					mDataSourceBoundViews.add(grid);
				}
				if (view instanceof ComponentContainer)
				{
					ComponentContainer component = (ComponentContainer)view;
					component.setComponentSize(cell.getAbsoluteSize());
					mComponentContainers.add(component);
				}
				view.setLayoutParams(parms);
				layout.addView(view);

				if (item instanceof ILayoutContainer &&	!LayoutControlFactory.isAdsTable(item))
				{
					ILayoutContainer itemContainer = (ILayoutContainer) item;
					GxLayout innerLayout;
					if (view instanceof GxLayout)
						innerLayout = expandInnerLayout((GxLayout) view, itemContainer);
					else
						innerLayout = expandInnerLayout((ViewGroup) view, itemContainer);
					//Should also return a containers in a list?

					//resize container size if its a table.
					//TODO: Remove it if autogrow not work.
					TableDefinition def = itemContainer.getContent();
					ViewGroup.LayoutParams params = innerLayout.getLayoutParams();

					//minus margin
					params.width = def.getAbsoluteWidthForTable();
					//to autogrow to work in tables
					if (params.height != LayoutParams.WRAP_CONTENT)
						params.height = def.getAbsoluteHeightForTable();
					innerLayout.setLayoutParams(params);
				}
				else
					mBoundViews.add(view);

				if (view instanceof GxTabControl)
				{
					GxTabControl tabControl = (GxTabControl)view;
					for (GxTabControl.TabItemInfo tabItemInfo : tabControl.getTabItems())
					{
						GxLayout innerLayout = expandInnerLayout(tabItemInfo.contentView, tabItemInfo.definition.getTable());

						//resize container size if its a table.
						//TODO: Remove it if auto grow not work.
						TableDefinition def = tabItemInfo.definition.getTable().getContent();
						ViewGroup.LayoutParams params = innerLayout.getLayoutParams();
						params.width = def.getAbsoluteWidth();
						params.height = def.getAbsoluteHeight() ;
						innerLayout.setLayoutParams(params);
					}

					if (item.getThemeClass() != null)
						GxTheme.applyStyle(tabControl, item.getThemeClass());
					else
						GxTheme.applyStyle(tabControl, "Tab"); // hardcoded for Tabs created on the fly //$NON-NLS-1$
				}
			}
		}
	}

	private void alignFields()
	{
		MultiMap<Pair<TableDefinition, Integer>, DataBoundControl> fieldsToAlign = new MultiMap<Pair<TableDefinition, Integer>, DataBoundControl>();

		// Collect the DataBoundControls located in each table that have to be aligned (i.e. label position = left).
		// Also match by their containing cells "X" position, so that different columns are differently aligned.
		for (View v : mBoundViews)
		{
			DataBoundControl field = Cast.as(DataBoundControl.class, v);
			if (field != null && field.getFormItemDefinition() != null)
			{
				LayoutItemDefinition layoutItem = field.getFormItemDefinition();
				CellDefinition cell = Cast.as(CellDefinition.class, layoutItem.getParent());

				if (cell != null && layoutItem.getLabelPosition().equals(Properties.LabelPositionType.Left))
				{
					Pair<TableDefinition, Integer> column = new Pair<TableDefinition, Integer>(cell.getParent().getParent(), cell.getAbsoluteX());
					fieldsToAlign.put(column, field);
				}
			}
		}

		// Process each table column separately for alignment.
		for (Pair<TableDefinition, Integer> column : fieldsToAlign.keySet())
		{
			// Don't align when there is just one.
			if (fieldsToAlign.getCount(column) <= 1)
				continue;

			// Measure every label's width to get maximum.
			int maxLabelWidth = 0;
			ArrayList<GxTextView> columnLabels = new ArrayList<GxTextView>();
			for (DataBoundControl field : fieldsToAlign.get(column))
			{
				GxTextView label = field.getLabel();
				if (label != null && label.getText() != null)
				{
					int labelWidth = (int)label.getPaint().measureText(label.getText().toString());
					labelWidth += label.getTotalPaddingLeft() + label.getTotalPaddingRight();

					columnLabels.add(label);
					maxLabelWidth = Math.max(maxLabelWidth, labelWidth);
				}
			}

			// Apply size to label. Since edit fills remaining space, this should align them.
			if (maxLabelWidth != 0)
			{
				for (GxTextView label : columnLabels)
					label.setWidth(maxLabelWidth);
			}
		}
	}

	private void putErrorNoLayout(ViewGroup parent)
	{
		TextView txtMessage = new TextView(mContext);
		txtMessage.setText(R.string.GXM_NoLayout);
		parent.addView(txtMessage);
	}
}