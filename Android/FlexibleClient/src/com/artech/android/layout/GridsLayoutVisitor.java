package com.artech.android.layout;

import com.artech.base.metadata.enums.ControlTypes;
import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.metadata.enums.LayoutItemsTypes;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.ILayoutVisitor;
import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.layout.TableDefinition;
import com.artech.base.utils.Strings;
import com.artech.usercontrols.UcFactory;
import com.artech.usercontrols.UserControlDefinition;

public class GridsLayoutVisitor implements ILayoutVisitor
{
	private boolean mHasScrollableGrid = false;
	private boolean mHasScrollableView = false;
	private boolean mHasScrollableSection = false;
	private boolean mHasScrollableWebView = false;
	private boolean mHasTab = false;

	public static boolean hasScrollableViews(LayoutDefinition layout)
	{
		if (layout != null)
			return hasScrollableViews(layout.getTable());
		else
			return false;
	}

	public static boolean hasScrollableViews(TableDefinition table)
	{
		if (table != null)
		{
			GridsLayoutVisitor visitor = new GridsLayoutVisitor();
			table.accept(visitor);
			return visitor.hasScrollableView();
		}
		else
			return false;
	}
	
	@Override
	public void enterVisitor(LayoutItemDefinition visitable) {	}

	@Override
	public void visit(LayoutItemDefinition visitable)
	{
		if (visitable.getType().equalsIgnoreCase(LayoutItemsTypes.Grid))
		{
			GridDefinition grid = (GridDefinition)visitable;

			UserControlDefinition gridControl = null;
			if (grid.getControlInfo() != null)
				gridControl = UcFactory.getControlDefinition(grid.getControlInfo().getControl());

			if (gridControl != null)
			{
				if (gridControl.IsScrollable)
					mHasScrollableGrid = true;
			}
			else
			{
				// A default ListView supports autogrow and in that case it will not have scroll.
				if (!grid.hasAutoGrow())
					mHasScrollableGrid = true;
			}
		}

		if (visitable.getType().equalsIgnoreCase(LayoutItemsTypes.Data))
		{
			//sd chart can be associated to an att or var too.
			ControlInfo controlInfo = visitable.getControlInfo();
			if (controlInfo != null)
			{
				String name = controlInfo.getControl();
				UserControlDefinition attView = UcFactory.getControlDefinition(name);
				if (attView != null && attView.IsScrollable)
					mHasScrollableView = true;
				//custom for gxwheel control inline
				if (attView != null &&  attView.Name.equalsIgnoreCase("SDWheel")
						&& controlInfo.optStringProperty("@SDWheelDisplayStyle").equalsIgnoreCase("inline"))
				{
					mHasScrollableView = true;
				}
			}

			if (visitable.getControlType().equals(ControlTypes.WebView) )
			{
				String domainDataType = Strings.EMPTY;
				if (visitable.getDataTypeName()!=null)
					domainDataType = visitable.getDataTypeName().GetDataType();

				 if (domainDataType.equals(DataTypes.html) || domainDataType.equals(DataTypes.component))
				 {
					 if (!visitable.hasAutoGrow())
					 {
						 mHasScrollableWebView = true;
					 }
				 }

			}

		}
		//Temp, tab cannot scroll outside it , to allow scroll inside.
		if (visitable.getType().equalsIgnoreCase(LayoutItemsTypes.Tab))
		{
			mHasScrollableView = true;
			mHasTab = true;
		}
		//Temp , section cannot scroll outside it, to allow scroll inside.
		if (visitable.getType().equalsIgnoreCase(LayoutItemsTypes.OneContent) || visitable.getType().equalsIgnoreCase(LayoutItemsTypes.Component))
		{
			// TODO: see how to decide it
			mHasScrollableSection = true;
		}
		//Temp , section cannot scroll outside it, to allow scroll inside.
		if (visitable.getType().equalsIgnoreCase(LayoutItemsTypes.AllContent))
		{
			//should be a tab with sections or one section.
			mHasScrollableView = true;
		}
	}

	public boolean hasTab() { return mHasTab; }
	public boolean hasScrollableGrid() { return mHasScrollableGrid; }
	public boolean hasScrollableView() { return mHasScrollableGrid || mHasScrollableView; }
	public boolean hasScrollableSection() { return mHasScrollableSection; }
	public boolean hasScrollableWebView() { return mHasScrollableWebView; }

	public boolean hasAnyScrollable()
	{
		return (mHasScrollableGrid || mHasScrollableView || mHasScrollableSection || mHasScrollableWebView);
	}

	@Override
	public void leaveVisitor(LayoutItemDefinition visitable) { }
}
