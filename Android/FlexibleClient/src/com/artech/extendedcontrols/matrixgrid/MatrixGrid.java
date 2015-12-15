package com.artech.extendedcontrols.matrixgrid;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsoluteLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.artech.R;
import com.artech.android.layout.GridContext;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.Size;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.services.Services;
import com.artech.base.utils.MathUtils;
import com.artech.base.utils.Strings;
import com.artech.base.utils.Triplet;
import com.artech.common.ImageHelper;
import com.artech.controllers.ViewData;
import com.artech.controls.GxLinearLayout;
import com.artech.controls.GxTextView;
import com.artech.controls.IGridView;
import com.artech.controls.IGxThemeable;
import com.artech.controls.grids.GridAdapter;
import com.artech.controls.grids.GridHelper;
import com.artech.controls.grids.GridItemLayout;
import com.artech.controls.grids.GridItemViewInfo;
import com.artech.controls.grids.IGridSite;
import com.artech.ui.Anchor;
import com.artech.ui.Coordinator;
import com.artech.utils.BackgroundOptions;
import com.artech.utils.ThemeUtils;

@SuppressWarnings("deprecation")
public class MatrixGrid extends LinearLayout implements ScrollViewListener, IGridView, IGridSite, IGxThemeable, IGxControlRuntime
{
	/*
	 * For each grid item the control will hold a ViewHolder with its rect and
	 * its view This rect is used to be intersected with the current visible
	 * area of the content area
	 */
	public class ViewHolder
	{
		private GxLinearLayout mView;
		private Rect mRect;
		private RecyclerKey mRecyclerKey;

		public Rect getRect()
		{
			return mRect;
		}

		public GxLinearLayout getView()
		{
			return mView;
		}

		public void setView(GxLinearLayout view)
		{
			mView = view;
		}

		public void setTypeAndRect(int itemType, Rect rect)
		{
			mRect = rect;
			mRecyclerKey = new RecyclerKey(itemType, rect.width(), rect.height());
		}

		public RecyclerKey getRecyclerKey()
		{
			return mRecyclerKey;
		}
	}

	private class RecyclerKey
	{
		private final Triplet<Integer, Integer, Integer> mKeyValues;

		public RecyclerKey(int viewType, int viewWidth, int viewHeight)
		{
			mKeyValues = new Triplet<Integer, Integer, Integer>(viewType, viewWidth, viewHeight);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;

			if (obj == null || getClass() != obj.getClass())
				return false;

			RecyclerKey other = (RecyclerKey)obj;
			return mKeyValues.equals(other.mKeyValues);
		}

		@Override
		public int hashCode()
		{
			return mKeyValues.hashCode();
		}

		@Override
		public String toString()
		{
			return mKeyValues.toString();
		}
	}

	// Composition, see init comments or grid.xml to understand the control
	// layout.
	private TableLayout frozenTableHeader;
	private TableLayout contentHeaderTable;
	private TableLayout frozenTable;
	private ObservableVerticalScrollView frozenColumnView;
	private ObservableHorizontalScrollView headerScrollView;
	private GxAbsoluteLayout contentTable;
	private MatrixTwoDScrollView contentScrollView;
	// This flag is used to avoid send scrolling messages while scrolling
	// programatically
	private boolean mForcingScroll = false;
	// Because we load the header and first column on update data we need to
	// control if we already load them because pagination can occur
	private boolean mFirstUpdate = true;
	// Grid helpers
	private final GridHelper mHelper;
	private final Coordinator mCoordinator;
	private GridAdapter mAdapter;
	// Grid control definition, some definition is from metadata and some come
	// on update data.
	private final ScheduleGridDefinition mDefinition;
	private ThemeClassDefinition mThemeClass;
	private Rect mVisibleRect = new Rect();
	// ViewHolder for each data item
	private final ArrayList<ViewHolder> mViews = new ArrayList<ViewHolder>();
	// Recyclable views by view type and dimensions.
	private final ViewRecycler<RecyclerKey, GxLinearLayout> mRecycleBin = new ViewRecycler<RecyclerKey, GxLinearLayout>();

	private Size mSize;
	private boolean mDataArrived;
	private boolean mGlobalLayoutOccurred;
	private RowAndColumn mPendingScrollToRowAndColumn;

	public MatrixGrid(GridContext context, Coordinator coor, GridDefinition def) {
		super(context);
		mCoordinator = coor;
		mDefinition = new ScheduleGridDefinition(context, def);
		mHelper = new GridHelper(this, def);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.grid, this, true);
		init();
	}

	private void prepareAdapter()
	{
		if (mAdapter == null)
			mAdapter = new GridAdapter(getContext(), mHelper, mDefinition.getGrid());
	}

	private void init() {
		// The Layout :
		// frozenTableHeader | headerScrollView ( contentHeaderTable )
		// ---------------------------------------------------------------------------------------
		// frozenColumnView( frozenTable ) | contentScrollView ( contentTable )
		//

		// The frozen column
		frozenColumnView = (ObservableVerticalScrollView) findViewById(R.id.frozenColumn);
		frozenColumnView.setScrollViewListener(this);
		ViewCompat.setOverScrollMode(frozenColumnView, ViewCompat.OVER_SCROLL_NEVER);
		frozenTable = (TableLayout) findViewById(R.id.frozenTable);
		frozenTableHeader = (TableLayout) findViewById(R.id.frozenTableHeader);

		// The header
		headerScrollView = (ObservableHorizontalScrollView) findViewById(R.id.contentTableHeaderHorizontalScrollView);
		headerScrollView.setScrollViewListener(this);
		ViewCompat.setOverScrollMode(headerScrollView, ViewCompat.OVER_SCROLL_NEVER);
		contentHeaderTable = (TableLayout) findViewById(R.id.contentTableHeader);
		contentHeaderTable.setHorizontalScrollBarEnabled(false);

		// The content
		contentScrollView = (MatrixTwoDScrollView) findViewById(R.id.contentTableHorizontalScrollView);
		contentScrollView.setScrollViewListener(this);
		ViewCompat.setOverScrollMode(contentScrollView, ViewCompat.OVER_SCROLL_ALWAYS);
		contentScrollView.setHorizontalScrollBarEnabled(false); // Only show the scroll bar on the header table (so that there aren't two).
		contentTable = (GxAbsoluteLayout) findViewById(R.id.contentTable);
	}

	/***
	 * Create the first column (frozen) by traversing the entities and creating
	 * a Cell with a text view inside for each item
	 *
	 * @param list
	 */
	protected void populateFrozenColumn(EntityList list)
	{
		TableLayout.LayoutParams parms = new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT);

		int rowNumber = 0;
		for (Entity entity : list)
		{
			TableRow row = new TableRow(getContext());
			row.setLayoutParams(parms);
			View frozenCell = createMarginCell(entity, rowNumber);
			rowNumber++;
			row.addView(frozenCell);
			frozenTable.addView(row);
		}

		if (mMatrixClass.getYAxisTableClass() != null)
			ThemeUtils.setBackgroundBorderProperties(frozenTable, mMatrixClass.getYAxisTableClass(), BackgroundOptions.DEFAULT);

		frozenTableHeader.setMinimumWidth(mDefinition.getYAxisWidth());
	}

	private int mStartSelection;

	private View createMarginCell(Entity entity, int rowNumber)
	{
		int marginCellWidth = mDefinition.getYAxisWidth();
		int rowHeight = mDefinition.getCellHeight();

		GxLinearLayout frozenCell = new GxLinearLayout(getContext());
		frozenCell.setOrientation(LinearLayout.VERTICAL);

		// Add Title Label
		GxTextView title = new GxTextView(getContext());
		title.setText(entity.optStringProperty(mDefinition.getYValuesFieldTitle()));
		title.applyClass(mMatrixClass.getYAxisTitleLabelClass());
		title.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

		// Add Description Label
		GxTextView desc = new GxTextView(getContext());
		desc.setText(entity.optStringProperty(mDefinition.getYValuesFieldDescription()));
		desc.applyClass(mMatrixClass.getYAxisDescriptionLabelClass());
		desc.setPadding(5, 0, 0, 0);
		desc.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

		frozenCell.addView(title);
		frozenCell.addView(desc);

		// Add the entire row for this frozen cell
		GxLinearLayout row = new GxLinearLayout(getContext());
		row.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

		ThemeClassDefinition cls = (rowNumber % 2 == 0) ? mMatrixClass.getRowTableClassReferenceEvenClass() : mMatrixClass.getRowTableClassReferenceOddClass();

		if (mDefinition.isSelectedRow(rowNumber))
		{
			cls = mMatrixClass.getSelectedRowClass();
			rowHeight = mDefinition.getSelectedRowHeight();
		}

		int rowOffset = 0;
		if (mDefinition.hasSelectedRow() && mDefinition.getSelectedRowIndex() < rowNumber)
			rowOffset = mDefinition.getSelectedRowExtraHeight();

		row.applyClass(cls);
		contentTable.addViewInLayout(row,
			new AbsoluteLayout.LayoutParams(mDefinition.getTotalContentWidth(),
						rowHeight,
						0,
						(rowNumber * mDefinition.getCellHeight()) + rowOffset));

		frozenCell.setVerticalGravity(Gravity.CENTER_VERTICAL);
		frozenCell.setMinimumWidth(marginCellWidth);
		frozenCell.setMinimumHeight(rowHeight);
		frozenCell.setLayoutParams(new TableRow.LayoutParams(marginCellWidth, rowHeight));
		frozenCell.applyClass(cls);

		return frozenCell;
	}

	/***
	 * Create the header (frozen) by traversing the entities and creating a Cell
	 * with a text view inside for each item
	 *
	 * @param list
	 */
	protected void populateHeader(EntityList list)
	{
		TableLayout.LayoutParams parms = new TableLayout.LayoutParams(
				mDefinition.getCellWidth(),
				TableLayout.LayoutParams.WRAP_CONTENT);

		parms.setMargins(1, 1, 1, 1);
		parms.weight = 1;
		TableRow row = new TableRow(getContext());
		row.setLayoutParams(parms);

		for (Entity entity : list)
		{
			View frozenCell = createHeaderCell(entity);
			row.addView(frozenCell);
		}

		if (mMatrixClass.getXAxisTableClass() != null)
			ThemeUtils.setBackgroundBorderProperties(contentHeaderTable, mMatrixClass.getXAxisTableClass(), BackgroundOptions.DEFAULT);

		contentHeaderTable.addView(row);
	}

	private View createHeaderCell(Entity entity)
	{
		LinearLayout layout = new LinearLayout(getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setMinimumWidth(mDefinition.getCellWidth());
		layout.setMinimumHeight(mDefinition.getXAxisHeight());

		GxTextView title = new GxTextView(getContext());
		if (Strings.hasValue(mDefinition.getXValuesFieldTitle()))
			title.setText(entity.optStringProperty(mDefinition.getXValuesFieldTitle()));

		title.setPadding(5, 0, 5, 0);
		title.applyClass(mMatrixClass.getXAxisLabelClass());

		GxTextView description = new GxTextView(getContext());
		if (Strings.hasValue(mDefinition.getXValuesFieldDescription()))
			description.setText(entity.optStringProperty(mDefinition.getXValuesFieldDescription()));

		description.setPadding(5, 0, 5, 0);
		description.applyClass(mMatrixClass.getXAxisLabelClass());

		layout.addView(title);
		layout.addView(description);
		return layout;
	}

	/***
	 * The site give the control the actual size. Save for further use later.
	 */
	@Override
	public void setAbsoluteSize(Size size)
	{
		mSize = size;
	}

	/***
	 * Scroll between header, column and content must be in sync, so scroll
	 * programatically: manual scroll on header -> programatically on content
	 * manual scroll on frozencolumn -> programatically on content manual scroll
	 * on content -> programatically on header and frozencolumn
	 */
	@Override
	public void onScrollChanged(View scrollView, int x, int y, int oldX, int oldY)
	{
		if (scrollView == headerScrollView && !mForcingScroll)
		{
			mForcingScroll = true;
			contentScrollView.scrollTo(x, contentScrollView.getScrollY());
			mForcingScroll = false;

			updateVisibleViews();
		}

		if (scrollView == frozenColumnView && !mForcingScroll)
		{
			mForcingScroll = true;
			contentScrollView.scrollTo(contentScrollView.getScrollX(), y);
			mForcingScroll = false;

			updateVisibleViews();
		}

		if (scrollView == contentScrollView && !mForcingScroll)
		{
			mForcingScroll = true;
			if (x != oldX)
				headerScrollView.scrollTo(x, headerScrollView.getScrollY());
			if (y != oldY)
				frozenColumnView.scrollTo(frozenColumnView.getScrollX(), y);
			mForcingScroll = false;

			updateVisibleViews();
		}
	}

	/***
	 * Traverse all rectangles from the data and intersects with the visible
	 * area of the content.
	 *
	 */
	private void updateVisibleViews()
	{
		if (contentTable.getLocalVisibleRect(mVisibleRect))
		{
			// Take a bigger rectangle so that we have more intersections with the data rects so
			// we are adding views in advance because will have more hits with the given rect.
			mVisibleRect.left -= 30;
			mVisibleRect.right += 30;
			mVisibleRect.top -= 30;
			mVisibleRect.bottom += 30;
			// Traverse the rects using the ViewHolder, if the View for the holder is null it means is not present
			// on the visible area.
			for (int i = 0; i < mViews.size(); i++)
			{
				ViewHolder holder = mViews.get(i);
				Rect f = holder.getRect();
				if (mVisibleRect.intersects(f.left, f.top, f.right, f.bottom))
				{
					// The rect should be visible, if not created yet try to add
					// using addView
					if (holder.getView() == null)
						addView(i, holder);
				}
				else
				{
					// The view is not visible anymore, so we can reuse it in another view.
					removeView(holder);
				}
			}
		}
	}

	@Override
	public void addListener(GridEventsListener listener)
	{
		mHelper.setListener(listener);
	}

	@Override
	public void update(final ViewData data)
	{
		// If it is the first data set, we need to create Header and first
		// column definition based on data
		// We should do this in other event
		if (mFirstUpdate)
		{
			mDefinition.updateSize(mCoordinator, mSize);
			contentTable.setMinimumHeight(mDefinition.getTotalContentHeight());
			contentTable.setMinimumWidth(mDefinition.getTotalContentWidth());

			// GridLines lines = new GridLines(mDefinition.getCellHeight(), mDefinition.getCellWidth(), mDefinition.getRowCount(), mDefinition.getColumnCount());
			// contentTable.setBackgroundDrawable(lines);

			populateHeader(mDefinition.getXAxis());
			populateFrozenColumn(mDefinition.getYAxis());

			mFirstUpdate = false;
		}

		// Create ViewHolders for each data item.
		prepareAdapter();
		mAdapter.setData(data);
		populateData(data);

		mDataArrived = true;

		ThemeClassDefinition selectedClassDef = mMatrixClass.getSelectedCellClass();
		Bitmap bitmapSelector = null;
		if (selectedClassDef != null)
		{
			Drawable draw = ImageHelper.getStaticImage(selectedClassDef.getBackgroundImage(), true);
			if (draw instanceof BitmapDrawable)
				bitmapSelector = ((BitmapDrawable) draw).getBitmap();
		}

		if (bitmapSelector == null)
			bitmapSelector = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.bgrow);

		if (mDefinition.hasSelectedRow())
		{
			contentScrollView.setIndicatorBitmap(bitmapSelector);
			contentScrollView.setSelectedRow(mStartSelection, mDefinition.getSelectedRowHeight());
		}
		else
			contentScrollView.setIndicatorBitmap(null);

		// Add only visible views to the content area. If the update is before the layout then visible rect
		// is going to be empty so we subscribe to do the updatevisibleviews on layout ready.
		if (!mGlobalLayoutOccurred)
		{
			getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
			{
				@Override
				public void onGlobalLayout()
				{
					MatrixGrid.this.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					updateVisibleAreaAfterDataArrived(data);
					mGlobalLayoutOccurred = true;
				}
			});
		}
		else
			updateVisibleAreaAfterDataArrived(data);
	}

	private void updateVisibleAreaAfterDataArrived(ViewData data)
	{
		if (mPendingScrollToRowAndColumn != null)
		{
			scrollToRowAndColumn(mPendingScrollToRowAndColumn);
			mPendingScrollToRowAndColumn = null;
		}
		else
		{
			// We only call this if we don't have a pending scroll because
			// scrollToRowAndColumn() indirectly updates the grid.
			updateVisibleViews();
		}

		if (data.isMoreAvailable())
			mHelper.requestMoreData();
	}

	/***
	 * Traverse data and save and create a Holder with its rect for each data
	 * item.
	 *
	 * @param data
	 */
	private void populateData(ViewData data)
	{
		if (mDefinition.hasSelectedRow())
			mStartSelection = (mDefinition.getSelectedRowIndex() * mDefinition.getCellHeight());

		int position = 0;
		for (Entity entitiy : data.getEntities())
		{
			// xFrom -> xTo
			float xFrom = mDefinition.getXDataValue(entitiy.optStringProperty(mDefinition.getXFromFieldName()));
			float xTo = xFrom;
			if (mDefinition.getXToFieldName() != null)
				xTo = mDefinition.getXDataValue(entitiy.optStringProperty(mDefinition.getXToFieldName()));

			// yFrom -> yTo
			float yFrom = mDefinition.getYDataValue(entitiy.optStringProperty(mDefinition.geYFromFieldName()));
			float yTo = yFrom;
			if (mDefinition.getYToFieldName() != null)
				yTo = mDefinition.getYDataValue(entitiy.optStringProperty(mDefinition.getYToFieldName()));

			// xFrom, xTo, yFrom, yTo to pixels using a resolver
			Rect rect = mDefinition.createCellRect(xFrom, xTo, yFrom, yTo, mStartSelection);
			if (mDefinition.hasSelectedRow() && rect.top == mStartSelection)
				entitiy.setIsSelected(true);

			// Save the holder, used for further intersection with the visible area when needed.
			ViewHolder holder;

			// Create a new one if we didn't have enough.
			if (position >= mViews.size())
			{
				holder = new ViewHolder();
				mViews.add(holder);
			}
			else
			{
				holder = mViews.get(position);
				removeView(holder);
			}

			holder.setTypeAndRect(mAdapter.getItemViewType(position), rect);
			position++;
		}

		// Clear extra holders (e.g. after doing a refresh operation).
		while (position < mViews.size())
			mViews.remove(position);
	}

	private void removeView(ViewHolder holder)
	{
		if (holder.getView() != null)
		{
			// The view is not visible anymore, so we can reuse it in another view.
			// However, since it may come into the visible again without having been reused yet,
			// also mark it as INVISIBLE.
			mHelper.discardItemView(holder.getView().getChildAt(0));
			mRecycleBin.put(holder.getRecyclerKey(), holder.getView());
			holder.getView().setVisibility(INVISIBLE);
			holder.setView(null);
		}
	}

	/***
	 * Add View to the visible area, this can be a new View or just set a new
	 * position for a recycled view
	 *
	 * @param index
	 * @param holder
	 */
	private void addView(int index, ViewHolder holder)
	{
		// set the size for the new view
		Rect rect = holder.getRect();
		mAdapter.setBounds(rect.width(), rect.height());

		// try to get a reusable view
		View previousView = null;
		GxLinearLayout previousContainer = mRecycleBin.get(holder.getRecyclerKey());
		if (previousContainer != null)
		{
			previousView = previousContainer.getChildAt(0);
			previousContainer.setVisibility(VISIBLE);
		}

		Entity item = mAdapter.getEntity(index);

		// ask the adapter for a new view or existing view
		GridItemViewInfo itemViewInfo = mHelper.getItemView(mAdapter, index, previousView, false);
		GridItemLayout itemLayout = itemViewInfo.getView();
		itemLayout.setOnClickListener(mOnItemClickListener);

		GxLinearLayout itemContainer;
		AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(rect.width(), rect.height(), rect.left, rect.top);

		if (previousView == null)
		{
			// A new item, without reusing anything (this also means previousContainer == null)
			itemContainer = new GxLinearLayout(getContext());
			itemContainer.addView(itemLayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

			// Add the new container to the grid.
			contentTable.addViewInLayout(itemContainer, params);
		}
		else
		{
			// Attempted to reuse. This also means we already had a previousContainer (that can
			// always be reused, even if the item itself cannot). Move it to its new position.
			itemContainer = previousContainer;
			itemContainer.setLayoutParams(params);

			if (previousView != itemLayout)
			{
				// The itemView itself was not reused. Discard the old one and put the new one inside the container.
				itemContainer.removeAllViews();
				itemContainer.addView(itemLayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				previousView.setOnClickListener(null);
			}
		}

		itemContainer.applyClass((item.isSelected()) ? mMatrixClass.getSelectedCellClass() : mMatrixClass.getCellClass());

		// ensure we know it's a visible view by setting to the holder the associated view
		holder.setView(itemContainer);
	}

	// Handle default action on each data item
	private final OnClickListener mOnItemClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			GridItemLayout item = (GridItemLayout) v;
			Entity entity = item.getEntity();

			mHelper.runDefaultAction(entity, new Anchor(v));
		}
	};
	private MatrixGridThemeClass mMatrixClass;

	// IGxThemeable Implementation
	@Override
	public void setThemeClass(ThemeClassDefinition themeClass) {
		mThemeClass = themeClass;
		mMatrixClass = new MatrixGridThemeClass(themeClass);
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass() {
		return mThemeClass;
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass) {
		mHelper.setThemeClass(themeClass);
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void setProperty(String name, Object value) { }

	@Override
	public Object getProperty(String name) { return null; }

	private class RowAndColumn
	{
		public final int row;
		public final int column;

		public RowAndColumn(int r, int c)
		{
			row = r;
			column = c;
		}
	}

	@Override
	public void runMethod(String name, List<Object> parameters)
	{
		final String METHOD_SCROLL_TO = "ScrollToCoordinates";

		if (name.equalsIgnoreCase(METHOD_SCROLL_TO) && parameters.size() >= 2)
		{
			int row = Services.Strings.tryParseInt(parameters.get(1).toString(), 0);
			int column = Services.Strings.tryParseInt(parameters.get(0).toString(), 0);
			scrollToRowAndColumn(new RowAndColumn(row, column));
		}
	}

	private void scrollToRowAndColumn(RowAndColumn rowCol)
	{
		if (!mDataArrived)
		{
			mPendingScrollToRowAndColumn = rowCol;
			return;
		}

		// Adjust gx 1-based indexes to our 0-based.
		rowCol = new RowAndColumn(rowCol.row - 1, rowCol.column - 1);

		int x = (rowCol.column * mDefinition.getCellWidth());
		int y = (rowCol.row * mDefinition.getCellHeight());

		if (mDefinition.hasSelectedRow() && rowCol.row > mDefinition.getSelectedRowIndex()) // Offset if the selected row is previous to this one.
			y += mDefinition.getSelectedRowExtraHeight();

		x = MathUtils.constrain(x, 0, mDefinition.getTotalContentWidth());
		y = MathUtils.constrain(y, 0, mDefinition.getTotalContentHeight());

		headerScrollView.scrollTo(x, 0);
		frozenColumnView.scrollTo(0, y);
	}
}
