package com.artech.android.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.artech.base.metadata.enums.LayoutItemsTypes;
import com.artech.base.metadata.layout.ILayoutVisitor;
import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.layout.TableDefinition;

public class TablesLayoutVisitor implements ILayoutVisitor
{
	private List<TableDefinition> mTables = new ArrayList<TableDefinition>();
	private boolean mHasAdsTable = false;
	
	private static String SD_ADS = "GoogleAdsControl";  //$NON-NLS-1$
	
	private TablesLayoutVisitor()
	{
	}

	public static boolean hasAdsTable(LayoutDefinition layout)
	{
		if (layout != null)
		{
			TablesLayoutVisitor visitor = new TablesLayoutVisitor();
			layout.getTable().accept(visitor);
			return visitor.hasAdsTable();
		}
		else
			return false;
	}
	
	@Override
	public void enterVisitor(LayoutItemDefinition visitable) {	}

	@Override
	public void visit(LayoutItemDefinition visitable)
	{
		if (visitable.getType().equalsIgnoreCase(LayoutItemsTypes.Table))
		{
			TableDefinition table = (TableDefinition)visitable;
			mTables.add(table);
			
			if (table.getName().equalsIgnoreCase(SD_ADS))
			{
				mHasAdsTable = true;
			}
		}
	}
	
	private boolean hasAdsTable() { return mHasAdsTable; }
	
	public Collection<TableDefinition> getGrids() { return mTables; }

	@Override
	public void leaveVisitor(LayoutItemDefinition visitable) { }
}
