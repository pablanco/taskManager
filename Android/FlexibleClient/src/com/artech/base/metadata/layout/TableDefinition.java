package com.artech.base.metadata.layout;

import java.util.Vector;

import com.artech.base.metadata.DimensionValue;
import com.artech.base.metadata.DimensionValue.ValueType;
import com.artech.base.metadata.theme.LayoutBoxMeasures;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.MathUtils;

public class TableDefinition extends LayoutItemDefinition implements ILayoutContainer
{
	private static final long serialVersionUID = 1L;

	// Metadata Values
	private DimensionValue mWidth;
	private DimensionValue mHeight;
	private int mFixedWidthCellSum;
	private int mFixedHeightCellSum;

	// Real Size
	private float AbsoluteHeightForTable;
	private float AbsoluteWidthForTable;
	private float AbsoluteHeight;
	private float AbsoluteWidth;

	public Vector<RowDefinition> Rows = new Vector<RowDefinition>();
	private Vector<CellDefinition> mCells;
	private String mBackground;


	public TableDefinition(LayoutDefinition parent,
			LayoutItemDefinition itemParent) {
		super(parent, itemParent);
	}

	@Override
	public TableDefinition getContent() {
		return this;
	}

	public boolean isMainTable()
	{
		return (getLayout() != null && getLayout().getTable() == this);
	}

	private Vector<CellDefinition> getCells()
	{
		if (mCells == null)
		{
			mCells = new Vector<CellDefinition>();
			for (RowDefinition row : Rows)
				for (CellDefinition cell : row.Cells)
					mCells.add(cell);
		}

		return mCells;
	}

	@Override
	public void readData(INodeObject tableNode)
	{
		super.readData(tableNode);

		String width = tableNode.optString("@width"); //$NON-NLS-1$
		String height = tableNode.optString("@height"); //$NON-NLS-1$
		String fixedHeight = tableNode.optString("@FixedHeightSum"); //$NON-NLS-1$
		String fixedWidth = tableNode.optString("@FixedWidthSum"); //$NON-NLS-1$

		mWidth = DimensionValue.parse(width);
		mHeight = DimensionValue.parse(height);

		// Just to be safe, in case parsing fails.
		if (mWidth == null)
			mWidth = DimensionValue.ZERO;
		if (mHeight == null)
			mHeight = DimensionValue.ZERO;

		mFixedHeightCellSum = 0;
		if (fixedHeight.length() > 0)
			mFixedHeightCellSum = Services.Device.dipsToPixels((int) Float.parseFloat(fixedHeight));

		mFixedWidthCellSum = 0;
		if (fixedWidth.length() > 0)
			mFixedWidthCellSum = Services.Device.dipsToPixels((int) Float.parseFloat(fixedWidth));

		mBackground = tableNode.optString("@background"); //$NON-NLS-1$
	}

	public void recalculateBounds(int reservedWidth)
	{
		if (mSuppliedSize != null)
		{
			Size newSize = mSuppliedSize.minusWidth(reservedWidth);
			calculateBoundsInternal(newSize.getWidth(), newSize.getHeight());
		}
		else
			Services.Log.warning("Cannot recalculate size of table because calculateBounds() hasn't been called yet.");
	}

	// Size last passed to calculateBounds().
	private transient Size mSuppliedSize;

	/***
	 * Calculate absolute bounds of this table
	 * @param widthReal Available width, in pixels.
	 * @param heightReal Available height, in pixels.
	 */
	@Override
	public void calculateBounds(float widthReal, float heightReal)
	{
		mSuppliedSize = new Size((int)widthReal, (int)heightReal);
		calculateBoundsInternal(widthReal, heightReal);
	}

	private void calculateBoundsInternal(float widthReal, float heightReal)
	{
		AbsoluteWidth = DimensionValue.toPixels(mWidth, widthReal);
		float absoluteWidthAvailable = AbsoluteWidth - mFixedWidthCellSum;

		AbsoluteHeight = DimensionValue.toPixels(mHeight, heightReal);
		float absoluteHeightAvailable = AbsoluteHeight - mFixedHeightCellSum;

		AbsoluteHeightForTable = AbsoluteHeight;
		AbsoluteWidthForTable = AbsoluteWidth;

		if (getThemeClass() != null)
		{
			// subtract margings, border and padding.
			// subtract margin only if size is not fixed.
			LayoutBoxMeasures margins = getThemeClass().getMargins();
			if (mHeight.Type == ValueType.PERCENT)
			{
				absoluteHeightAvailable -= margins.getTotalVertical();
				AbsoluteHeightForTable -= margins.getTotalVertical();
			}

			if (mWidth.Type == ValueType.PERCENT)
			{
				absoluteWidthAvailable -= margins.getTotalHorizontal();
				AbsoluteWidthForTable -= margins.getTotalHorizontal();
			}

			// subtract padding and border, padding now include border.
			LayoutBoxMeasures padding = getThemeClass().getPadding();
			absoluteWidthAvailable = absoluteWidthAvailable - padding.getTotalHorizontal();
			absoluteHeightAvailable = absoluteHeightAvailable - padding.getTotalVertical();
		}

		if (absoluteWidthAvailable < 0)
			absoluteWidthAvailable = 0;

		if (absoluteHeightAvailable < 0)
			absoluteHeightAvailable = 0;

		for (CellDefinition cell : getCells())
			cell.calculateBounds(absoluteWidthAvailable, absoluteHeightAvailable);
	}

	boolean hasDipHeight()
	{
		return (mHeight.Type == DimensionValue.ValueType.PIXELS);
	}

	public DimensionValue getWidth()
	{
		return mWidth;
	}

	public DimensionValue getHeight()
	{
		return mHeight;
	}

	public String getBackground()
	{
		return mBackground;
	}

	void addToFixedHeightSum(int add)
	{
		mFixedHeightCellSum += add;
	}

	int getFixedHeightSum()
	{
		return mFixedHeightCellSum;
	}

	public int getAbsoluteHeight()
	{
		return MathUtils.round(AbsoluteHeight);
	}

	public int getAbsoluteWidthForTable()
	{
		return MathUtils.round(AbsoluteWidthForTable);
	}

	public int getAbsoluteWidth()
	{
		return MathUtils.round(AbsoluteWidth);
	}

	public int getAbsoluteHeightForTable()
	{
		return MathUtils.round(AbsoluteHeightForTable);
	}

	public boolean isCanvas()
	{
		return optBooleanProperty("@AbsolutLayout");
	}

	public boolean getEnableHeaderRowPattern() { return optBooleanProperty("@enableHeaderRowPattern"); }

	public String getHeaderRowApplicationBarClass() { return optStringProperty("@headerRowApplicationBarsClass"); }


}
