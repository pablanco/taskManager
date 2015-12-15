package com.artech.base.metadata.layout;

import java.util.Vector;

import com.artech.base.serialization.INodeObject;

public class RowDefinition extends LayoutItemDefinition
{
	private static final long serialVersionUID = 1L;

	public final Vector<CellDefinition> Cells = new Vector<CellDefinition>();
	// The parent table of this Row, rows elements are allowed only on Tables.
	private final TableDefinition mTableDefinition;

	public RowDefinition(LayoutDefinition layout, LayoutItemDefinition itemParent)
	{
		super(layout, itemParent);
		mTableDefinition = (TableDefinition) itemParent;
	}
	
	public int getIndex() {
		return mTableDefinition.Rows.indexOf(this);
	}

	@Override
	public TableDefinition getParent()
	{
		return (TableDefinition)super.getParent();
	}

	@Override
	public void readData(INodeObject rowNode)
	{
		super.readData(rowNode);
		String height = rowNode.optString("@rowHeight"); //$NON-NLS-1$
		mTableDefinition.Rows.add(this);
	}

	public int getEndY()
	{
		int y = 0;
		for (CellDefinition cell : Cells)
		{
			y = cell.getAbsoluteY() + cell.getAbsoluteHeight();
			if (y != 0)
				break;
		}

		return y;
	}

}
