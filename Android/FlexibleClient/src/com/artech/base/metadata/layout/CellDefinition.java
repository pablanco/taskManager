package com.artech.base.metadata.layout;

import java.util.Vector;

import com.artech.base.metadata.enums.LayoutItemsTypes;
import com.artech.base.metadata.theme.LayoutBoxMeasures;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.MathUtils;

public class CellDefinition extends LayoutItemDefinition
{
	private static final long serialVersionUID = 1L;

	// All intermediate and final fields are in pixels
	private int x;
	private int y;
	private float relativex; // [0..1]
	private float relativey; // [0..1]
	private int width;
	private int height;
	private float relativewidth; // [0..1]
	private float relativeheight; // [0..1]

	private float mCalculatedAbsoluteX;
	private float mCalculatedAbsoluteY;
	private float mCalculatedAbsoluteWidth;
	private float mCalculatedAbsoluteHeight;
	private Integer mRow;

	public CellDefinition(LayoutDefinition parent, LayoutItemDefinition itemParent)
	{
		super(parent, itemParent);
	}

	@Override
	public RowDefinition getParent()
	{
		return (RowDefinition)super.getParent();
	}

	void calculateBounds(float containerWidth, float containerHeight)
	{
		mCalculatedAbsoluteX = (relativex * containerWidth) + x;
		mCalculatedAbsoluteY = (relativey * containerHeight) + y;
		mCalculatedAbsoluteWidth = (relativewidth * containerWidth) + width;
		mCalculatedAbsoluteHeight = (relativeheight * containerHeight) + height;

		if (mCalculatedAbsoluteWidth < 0)
			mCalculatedAbsoluteWidth = 0;
		if (mCalculatedAbsoluteHeight < 0)
			mCalculatedAbsoluteHeight = 0;

		Vector<LayoutItemDefinition> vector = getChildItems();
		for (LayoutItemDefinition child : vector)
		{
			String itemTypeName = child.getType();
			if (itemTypeName.equalsIgnoreCase(LayoutItemsTypes.Data) ||
				itemTypeName.equalsIgnoreCase(LayoutItemsTypes.TextBlock) ||
				itemTypeName.equalsIgnoreCase(LayoutItemsTypes.Tab)	||
				child instanceof LayoutActionDefinition /* Button */)
			{
				// remove the space of the margin for this cell
				adjustSizeWithMargin(child);
			}
			if (child instanceof ILayoutContainer)
				((ILayoutContainer)child).calculateBounds(mCalculatedAbsoluteWidth, mCalculatedAbsoluteHeight);
			if (child instanceof GridDefinition)
				((GridDefinition) child).calculateBounds(mCalculatedAbsoluteWidth, mCalculatedAbsoluteHeight);
			if (child instanceof TabControlDefinition)
				((TabControlDefinition) child).calculateBounds(mCalculatedAbsoluteWidth, mCalculatedAbsoluteHeight);
		}
	}

	private void adjustSizeWithMargin(LayoutItemDefinition child)
	{
		ThemeClassDefinition themeClass = child.getThemeClass();
		if (themeClass!=null && themeClass.hasMarginSet())
		{
			LayoutBoxMeasures margins = themeClass.getMargins();
			mCalculatedAbsoluteHeight = mCalculatedAbsoluteHeight - (margins.top+margins.bottom);
			mCalculatedAbsoluteWidth = mCalculatedAbsoluteWidth - (margins.left+margins.right);
			mCalculatedAbsoluteX = mCalculatedAbsoluteX + margins.left;
			mCalculatedAbsoluteY = mCalculatedAbsoluteY + margins.top;
		}

		//TODO: should subtract the border size to the cell size?
		if (mCalculatedAbsoluteWidth < 0)
			mCalculatedAbsoluteWidth = 0;
		if (mCalculatedAbsoluteHeight < 0)
			mCalculatedAbsoluteHeight = 0;
	}

	@Override
	public void readData(INodeObject cellNode)
	{
		super.readData(cellNode);

		// Set Bounds, calculate is differed to runtime
		setBounds(cellNode);
	}

	private void setBounds(INodeObject cellNode) {
		INodeObject node = cellNode.optNode("CellBounds"); //$NON-NLS-1$

		/* "@x": "0",
         "@y": "0",
         "@xRelative": "0",
         "@yRelative": "0",
         "@width": "0",
         "@height": "0",
         "@widthRelative": "100",
         "@heightRelative": "100"*/
		if (node!=null)
		{
			// Convert from dips to pixels position and dimensions
			x 				= Services.Device.dipsToPixels((int) (Float.parseFloat(node.optString("@x")))); //$NON-NLS-1$
			y 				= Services.Device.dipsToPixels((int) (Float.parseFloat(node.optString("@y")))); //$NON-NLS-1$
			width 			= Services.Device.dipsToPixels((int) (Float.parseFloat(node.optString("@width")))); //$NON-NLS-1$
			height 			= Services.Device.dipsToPixels((int) (Float.parseFloat(node.optString("@height")))); //$NON-NLS-1$

			// Percentages so no conversion to pixels!
			relativex 		= Float.parseFloat(node.optString("@xRelative")) / 100f; //$NON-NLS-1$
			relativey 		= Float.parseFloat(node.optString("@yRelative")) / 100f; //$NON-NLS-1$
			relativeheight	= Float.parseFloat(node.optString("@heightRelative")) / 100f; //$NON-NLS-1$
			relativewidth	= Float.parseFloat(node.optString("@widthRelative")) / 100f; //$NON-NLS-1$
		}
		else
		{
			x 				= 0;
			y 				= 0;
			relativex 		= 0;
			relativey 		= 0;
			width 			= 0;
			height 			= 0;
			relativeheight 	= 1f;
			relativewidth	= 1f;
		}

		RowDefinition parent = getParent();
		parent.Cells.addElement(this);
	}

	public LayoutItemDefinition getContent()
	{
		if (getChildItems().size() != 0)
			return getChildItems().get(0);
		else
			return null;
	}

	public int getRowSpan() {
		return optIntProperty("@rowSpan");
	}

	public Size getAbsoluteSize()
	{
		return new Size(getAbsoluteWidth(), getAbsoluteHeight());
	}

	public int getRow()
	{
		if (mRow == null)
			mRow = getParent().getIndex();

		return mRow;
	}

	public int getAbsoluteX()
	{
		return MathUtils.round(mCalculatedAbsoluteX);
	}

	public int getAbsoluteY()
	{
		return MathUtils.round(mCalculatedAbsoluteY);
	}

	public int getAbsoluteWidth()
	{
		// Reason for this strange calculation: a double rounding may produce differences.
		// For example, say cell1 has x = 1.4 and width = 1.4. This means that cell2 has x = 2.8
		// Rounding each separately ends up with x = 1 and width = 1, while cell2 has x = 3 and there is a blank pixel there.
		// With this calculation, right = round(2.8) = 3, which means that width = 2.
		int right = MathUtils.round(mCalculatedAbsoluteX + mCalculatedAbsoluteWidth);
		return right - getAbsoluteX();
	}

	public int getAbsoluteHeight()
	{
		// See above method for explanation.
		int bottom = MathUtils.round(mCalculatedAbsoluteY + mCalculatedAbsoluteHeight);
		return bottom - getAbsoluteY();
	}
}
