package com.artech.android.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.artech.R;
import com.artech.base.metadata.DimensionValue;
import com.artech.base.metadata.layout.CellDefinition;
import com.artech.base.metadata.layout.ILayoutContainer;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.layout.RowDefinition;
import com.artech.base.metadata.layout.TableDefinition;
import com.artech.base.metadata.theme.LayoutBoxMeasures;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.controls.DataBoundControl;
import com.artech.controls.GxHorizontalSeparator;
import com.artech.controls.IGxThemeable;
import com.artech.fragments.GridContainer;
import com.artech.ui.Coordinator;
import com.artech.utils.BackgroundOptions;
import com.artech.utils.Cast;
import com.artech.utils.ThemeUtils;

public class GxLayout extends ViewGroup implements IGxThemeable
{
	/***
	 *  The TableDefinition for this Layout, this is set on constructor or using setDefinition function
	 */
	private TableDefinition mLayout;
	/***
	 * Margins measures for this layout
	 */
	private LayoutBoxMeasures mMargins;
	/***
	 * Coordinator know all associated controls and events for this layout.
	 */
	private Coordinator mCoordinator;
	/***
	 * Map with the views for each horizontal line. This map is used when show horizontal lines = true in a GeneXus theme
	 */
	private final Hashtable<RowDefinition, View> mHorizontalLines = new Hashtable<RowDefinition, View>();

	private ThemeClassDefinition mThemeClass;

	public GxLayout(Context context, TableDefinition layout, Coordinator coordinator)
	{
		super(context);
		mLayout = layout;
		mCoordinator = coordinator;
		setTag(layout.getName());
	}

	public GxLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GxLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	protected Coordinator getCoordinator()
	{
		return mCoordinator;
	}

	public void setLayout(Coordinator coordinator, TableDefinition layout)
	{
		mCoordinator = coordinator;
		mLayout = layout;
	}

	// Preallocated variables used by onMeasure().
	private final Hashtable<Integer, Integer> mOnMeasureOffsets = new Hashtable<Integer, Integer>();
	private final SparseIntArray mOnMeasureHiddens = new SparseIntArray();
	private final Rect mOnMeasureHr = new Rect();
	private final Rect mOnMeasureFrame = new Rect();
	private final RowLayoutContext mRowContext = new RowLayoutContext();
	private final ArrayList<Integer> mOnMeasureKeys = new ArrayList<Integer>();

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		// Find out how big every child wants to be
		measureChildren(widthMeasureSpec, heightMeasureSpec);
		if (mLayout == null)
			Services.Log.debug("GxLayout with out definition"); //$NON-NLS-1$
		RowLayoutContext rowContext = mRowContext;
		mRowContext.clear();
		if (mLayout != null)
		{
			// reset hidden space and autogrow space
			mOnMeasureOffsets.clear();
			mOnMeasureHiddens.clear();

			// traverse all rows calculating positions for each cell
			// taking into account visibility and autogrow properties
			for (RowDefinition row : mLayout.Rows )
			{
				initializeRowContext(rowContext);
				layoutRow(row, rowContext);
				updateHiddenSpace(row, rowContext);
			}
			// Shift controls affected by autoHeight and consider hidden controls
			adjustSizes();
		}
		setDimensions(widthMeasureSpec, heightMeasureSpec);
	}

	private void updateHiddenSpace(RowDefinition row,
			RowLayoutContext rowContext) {
		if (rowContext.MaxHeightHiddenInCurrentRow > 0 && rowContext.MaxHeightHiddenInCurrentRow > rowContext.MaxHeightVisibleInCurrentRow ) {
			rowContext.RowsHiddenHeightSum += Math.max(0, rowContext.MaxHeightHiddenInCurrentRow - rowContext.MaxHeightVisibleInCurrentRow);
		}
		mOnMeasureHiddens.put(row.getIndex() + 1, rowContext.RowsHiddenHeightSum);
	}

	private void initializeRowContext(RowLayoutContext rowContext) {
		rowContext.MaxHeightVisibleInCurrentRow = 0;
		rowContext.MaxHeightHiddenInCurrentRow = 0;
	}

	private void setDimensions(int widthMeasureSpec, int heightMeasureSpec)
	{
		int maxHeight = 0;
		int maxWidth = 0;
		int childCount = getChildCount();

		// Find rightmost and bottom-most child
		int invisibleHeight = 0;
		for (int i = 0; i < childCount; i++)
		{
			View child = getChildAt(i);
			GxLayout.LayoutParams lp = (GxLayout.LayoutParams) child.getLayoutParams();

			// Consider this child space if it is visible or if needs to keep space when invisible
			if (child.getVisibility() == VISIBLE || lp.cell.keepSpace)
			{
				int childRight;
				int childBottom;

				childRight = lp.x + Math.max(child.getMeasuredWidth(), lp.width);
				childBottom = lp.y + Math.max(child.getMeasuredHeight(), lp.height);

				maxWidth = Math.max(maxWidth, childRight);
				maxHeight = Math.max(maxHeight, childBottom);
			}
			else
			{
				// We already set the minimum height to the design time size, but this can change when we set visibility off to some inner control.
				// So aggregate all the hidden space for this control.
				invisibleHeight += Math.max(child.getMeasuredHeight(), lp.height);
			}
		}

		// Account for padding too
		maxWidth += getPaddingLeft() + getPaddingRight();
		maxHeight += getPaddingTop() + getPaddingBottom();

		// Special case: Do not shrink a main table that should occupy the whole screen.
		if (mLayout != null && mLayout.isMainTable() && mLayout.getHeight() != null && mLayout.getHeight().equals(DimensionValue.HUNDRED_PERCENT))
			invisibleHeight = 0;

		// Update the minimum height.
		if (invisibleHeight > 0 && (getSuggestedMinimumHeight() - invisibleHeight) > 0)
			setMinimumHeight(getSuggestedMinimumHeight() - invisibleHeight);

		// Check against minimum height and width
		maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
		maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

		setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec), resolveSize(maxHeight, heightMeasureSpec));
	}

	private void adjustSizes()
	{
		shiftOffsets();

		for (RowDefinition row : mLayout.Rows)
		{
			CellMeasures lastCell = null;
			int maxCellHeight = 0;
			for (CellDefinition cell : row.Cells)
			{
				View control = findViewFromCell(cell);
				if (control != null)
				{
					GxLayout.LayoutParams lp = (GxLayout.LayoutParams) control.getLayoutParams();
					lastCell = lp.cell;

					Integer previousHiddenSpace = mOnMeasureHiddens.get(lp.cell.row);

					lp.y = lp.cell.y + mOnMeasureOffsets.get(lp.cell.row) - ((previousHiddenSpace != null) ? previousHiddenSpace : 0);

					//ignore controls with row span to calculate line position
					if (lp.cell.rowSpan == 1)
					{
						int height = Math.max(lp.cell.height, control.getMeasuredHeight());
						height = considerMargin(control, height);
						maxCellHeight = Math.max(maxCellHeight, height);
					}
				}
			}

			if (lastCell != null)
				setLinePosition(row, lastCell, maxCellHeight);
		}
	}

	private void setLinePosition(RowDefinition row, CellMeasures lastCell, int maxCellHeight)
	{
		if (lastCell == null)
			throw new IllegalArgumentException("lastCell must not be null");  //$NON-NLS-1$

		//set line position
		View control =  mHorizontalLines.get(row);
		Integer previousHiddenSpace = mOnMeasureHiddens.get(row.getIndex());

		Integer rowOffset = mOnMeasureOffsets.get(lastCell.row);
		if (control != null && rowOffset != null)
		{
			GxLayout.LayoutParams lp = (GxLayout.LayoutParams) control.getLayoutParams();

			lp.y = lastCell.y + rowOffset + maxCellHeight - ((previousHiddenSpace != null) ? previousHiddenSpace : 0);
			if (lp.y > 0)
				lp.y = lp.y - 1;
		}
	}

	private int considerMargin(View control, int height) {
		// add margin for DataBoundControl
		if (control instanceof DataBoundControl)
		{
			DataBoundControl dbControl = (DataBoundControl)control;
			if (dbControl.getFormItemDefinition()!=null)
			{
				ThemeClassDefinition themeClass = dbControl.getFormItemDefinition().getThemeClass();
				if (themeClass!=null && themeClass.hasMarginSet())
				{
					LayoutBoxMeasures margins = themeClass.getMargins();
					height = height + margins.bottom;
				}
			}
		}
		return height;
	}

	private void shiftOffsets()
	{
		// TODO: Optimize this method! Should be MUCH simpler if using a SparseIntArray for mOnMeasureOffsets.
		ArrayList<Integer> keys = mOnMeasureKeys;
		keys.clear();
		keys.addAll(mOnMeasureOffsets.keySet());
		Collections.sort(keys);

		int previous = 0;
		for (Integer key : keys) {
			Integer o1 = mOnMeasureOffsets.get(key);
			previous = previous + o1;
			mOnMeasureOffsets.put(key, previous);
		}

		// shift offset to next row
		previous = 0;
		for (Integer key : keys) {
			int current = mOnMeasureOffsets.get(key);
			if (current >= 0)
			{
				mOnMeasureOffsets.put(key, previous);
			}
			previous = current;
		}
	}

	private void layoutRow(RowDefinition row, RowLayoutContext rowContext) {
		Rect hr = mOnMeasureHr;
		hr.setEmpty();
		List<CellDefinition> vector = row.Cells;
		for (CellDefinition cell : vector)
		{
			View cellView = findViewFromCell(cell);
			if (cellView != null)
			{
				CellMeasures cellMeasures = ((GxLayout.LayoutParams)cellView.getLayoutParams()).cell;
				layoutCell(cellView, cellMeasures, hr, rowContext);
			}
		}
	}

	private View findViewFromCell(CellDefinition cell)
	{
		if (cell != null && cell.getContent() != null)
			return mCoordinator.getControl(cell.getContent().getName());

		return null;
	}

	private void layoutCell(View control, CellMeasures cell, Rect hr, RowLayoutContext rowContext)
	{
		boolean isControlHiddenAndCollapseMode = (control.getVisibility() != VISIBLE && !cell.keepSpace);

		Rect frame = getDesignTimeFrame(cell);
		// ensure row in offsets dictionary
		if (!mOnMeasureOffsets.containsKey(cell.row))
			mOnMeasureOffsets.put(cell.row, 0);

		int frameHeight = frame.height();
		if (frame.top + frameHeight > hr.top)
			hr.top = frame.top + frameHeight;

		ViewGroup.LayoutParams parms = control.getLayoutParams();
		if (parms instanceof GxLayout.LayoutParams)
		{
			GxLayout.LayoutParams lp = (GxLayout.LayoutParams) control.getLayoutParams();
			int height = lp.cell.height;

			if (lp.cell.autoHeight && control instanceof DataBoundControl)
				lp.height = LayoutParams.WRAP_CONTENT;

			if (lp.height == LayoutParams.WRAP_CONTENT && !lp.cell.autoHeight)
				lp.height = height;

			// Calculate auto height event if the control is hidden because the adjustSizes method
			// will decrement the full size of hidden controls
			if (lp.cell.autoHeight)
			{
				if (height < control.getMeasuredHeight())
				{
					Integer add = control.getMeasuredHeight() - height;
					Integer value = mOnMeasureOffsets.get(cell.row);
					if (add > value)
					{
						mOnMeasureOffsets.put(cell.row, add);
						frame.bottom = frame.bottom + add;
					}
				}
				else
				{
					if (lp.height != LayoutParams.WRAP_CONTENT )
					{
						// If not wrap context , set current size.
						// we set setMinimumHeight to all controls so only set height ig now wrap_content
						lp.height = height;
					}
				}
			}

			if (isControlHiddenAndCollapseMode)
			{
				if (cell.rowSpan == 1)
					rowContext.MaxHeightHiddenInCurrentRow = Math.max(rowContext.MaxHeightHiddenInCurrentRow, (parms.height > 0)? parms.height : frame.height());
				else
					rowContext.MaxHeightHiddenInCurrentRow = Math.max(rowContext.MaxHeightHiddenInCurrentRow, frame.height());
			}
		}

		if (!isControlHiddenAndCollapseMode)
			rowContext.MaxHeightVisibleInCurrentRow = Math.max(rowContext.MaxHeightVisibleInCurrentRow, (parms.height > 0)? parms.height : control.getMeasuredHeight());
	}

	private Rect getDesignTimeFrame(CellMeasures cell)
	{
		Rect frame = mOnMeasureFrame;
		frame.setEmpty();
		// offset x & y with padding?
		frame.left = cell.x;
		frame.top = cell.y;
		frame.bottom = frame.top + cell.height;
		return frame;
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		int count = getChildCount();

		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			if (child.getVisibility() != GONE) {

				GxLayout.LayoutParams lp =
						(GxLayout.LayoutParams) child.getLayoutParams();

				int childLeft = getPaddingLeft() + lp.x;
				int childTop = getPaddingTop() + lp.y;
				child.layout(childLeft, childTop,
						childLeft + child.getMeasuredWidth(),
						childTop + child.getMeasuredHeight());
			}
		}
	}

	@Override
	public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new GxLayout.LayoutParams(getContext(), attrs);
	}

	// Override to allow type-checking of LayoutParams.
	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof GxLayout.LayoutParams;
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new GxLayout.LayoutParams(p);
	}

	public static class LayoutParams extends ViewGroup.MarginLayoutParams
	{
		public int x;
		public int y;
		public final CellMeasures cell;

		public LayoutParams(int width, int height, int x, int y)
		{
			super(width, height);
			this.x = x;
			this.y = y;
			cell = new CellMeasures();
		}

		public LayoutParams(Context c, AttributeSet attrs)
		{
			super(c, attrs);
			x = 0;
			y = 0;
			cell = new CellMeasures();
		}

		public LayoutParams(ViewGroup.LayoutParams source)
		{
			super(source);
			cell = new CellMeasures();
		}

		public LayoutParams(CellDefinition layoutCell, LayoutItemDefinition item, View view)
		{
			super(layoutCell.getAbsoluteWidth(), layoutCell.getAbsoluteHeight());
			cell = new CellMeasures(layoutCell);

			x = cell.x;
			y = cell.y;

	        if (view instanceof GridContainer && item.hasAutoGrow())
	        {
	        	// Do NOT set WRAP_CONTENT, it does nothing for ListView.
	        	cell.autoHeight = true;
	        }
	        else if (item instanceof ILayoutContainer || (item.hasAutoGrow() && !(view instanceof GridContainer)))
			{
				//auto grow for tables, not work for grid/list yet
				//sections ? also ContentContainer

				height = LayoutParams.WRAP_CONTENT;
				cell.autoHeight = true;

				// Set MinimumHeight for all Autogrow controls
				// Table layout should height all the size to align vertical to work. This is a design time value.
				view.setMinimumHeight(cell.height);
			}
		}
	}

	public static class CellMeasures
	{
		public final int width;
		public final int height;
		public final int x;
		public final int y;
		public final boolean keepSpace;
		public final int row;
		public final int rowSpan;
		public boolean autoHeight;

		public CellMeasures()
		{
			width = 0;
			height = 0;
			x = 0;
			y = 0;
			row = 0;
			rowSpan = 1;
			keepSpace = true;
		}

		public CellMeasures(CellDefinition cell)
		{
			width = cell.getAbsoluteWidth();
			height = cell.getAbsoluteHeight();
			x = cell.getAbsoluteX();
			y = cell.getAbsoluteY();
			row = cell.getRow();
			rowSpan = cell.getRowSpan();
			keepSpace = (cell.getContent() != null && cell.getContent().getKeepSpace());
		}
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		mThemeClass = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass() {
		return mThemeClass;
	}

	public void updateHorizontalSeparators(GxHorizontalSeparator separator)
	{
		// Add separator between rows (i.e. after all rows but the last one).
		int rowCountWithSeparators = mLayout.Rows.size() - 1;

		for (int i = 0; i < rowCountWithSeparators; i++)
		{
			RowDefinition row = mLayout.Rows.get(i);

			// Remove previous separator, if any.
			View oldSeparator = mHorizontalLines.get(row);
			if (oldSeparator != null) {
				removeView(oldSeparator);
				mHorizontalLines.remove(row);
			}

			// Add new separator, if applicable.
			if (separator.isVisible())
			{
				View separatorView;
				if (!separator.isDefault())
				{
					ImageView imgView = new ImageView(getContext());
					imgView.setImageDrawable(separator.getDrawable());
					imgView.setScaleType(ScaleType.FIT_XY);
					separatorView = imgView;
				}
				else
					separatorView = LayoutInflater.from(getContext()).inflate(R.layout.listdivider, null);

				separatorView.setLayoutParams(new GxLayout.LayoutParams(LayoutParams.MATCH_PARENT, separator.getHeight(), 0, row.getEndY()));
				separatorView.setTag(row);
				mHorizontalLines.put(row, separatorView);
				addView(separatorView);
			}
		}
	}

	@Override
	public void setLayoutParams(ViewGroup.LayoutParams params)
	{
		if (mMargins != null && Cast.as(MarginLayoutParams.class, params) != null)
			((MarginLayoutParams)params).setMargins(mMargins.left, mMargins.top, mMargins.right, mMargins.bottom);

		super.setLayoutParams(params);
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass) {
		// Padding
		LayoutBoxMeasures padding = themeClass.getPadding();
		if (padding != null)
			setPadding(padding.left, padding.top, padding.right, padding.bottom);

		// Margins
		LayoutBoxMeasures margins = themeClass.getMargins();
		if (margins!=null)
		{
			ViewGroup.LayoutParams lp = getLayoutParams(); // The layout could not be on site yet, differ the setting to the setLayoutParams
			if (lp != null)
			{
				MarginLayoutParams marginParms = Cast.as(MarginLayoutParams.class, lp); // does its site support margins?
				if (marginParms != null)
				{
					marginParms.setMargins( margins.left, margins.top,margins.right, margins.bottom);
					setLayoutParams(lp);
				}
			}
			else
				mMargins = margins;
		}

		// Background and Border.
		ThemeUtils.setBackgroundBorderProperties(this, themeClass, BackgroundOptions.defaultFor(mLayout));

		// Horizontal separators.
		updateHorizontalSeparators(new GxHorizontalSeparator(mLayout, themeClass));

	}

}

