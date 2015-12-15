package com.artech.base.metadata.layout;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.Properties;
import com.artech.base.metadata.enums.LayoutItemsTypes;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.types.IStructuredDataType;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.base.utils.Version;
import com.artech.controls.ControlPropertiesDefinition;
import com.artech.usercontrols.UcFactory;
import com.artech.usercontrols.UserControlDefinition;

public class GridDefinition extends LayoutItemDefinition
{
	private static final long serialVersionUID = 1L;

	private String mDataSourceName;
	private String mAssociatedCollection;
	private String mDefaultAction;
	private String mEmptyDataSetImage;
	private String mEmptyDataSetImageClass;
	private String mEmptyDataSetText;
	private String mEmptyDataSetTextClass;
	private String mRowsPerPageStr;
	private Integer mRowsPerPage;

	private String mShowSelector;
	private SelectionType mSelectionType;
	private String mSelectionTypeStr;

	private Boolean mHasAutoGrow;
	private static final Version VERSION_WITH_SUPPORT_FOR_AUTOGROW = new Version(0, 8);

	private ArrayList<TableDefinition> mItemLayouts;
	private String mDefaultItemLayout;
	private String mDefaultSelectedItemLayout;

	private ArrayList<ILayoutActionDefinition> mMultipleSelectionActions;

	public static final int SELECTION_NONE = 0;
	public static final int SELECTION_ALWAYS = 1;
	public static final int SELECTION_ON_ACTION = 2;

	private boolean mHasPullToRefresh;

	public enum SelectionType
	{
		AutoDeselect,
		KeepWhileExecuting,
		KeepUntilNewSelection
	}

	public GridDefinition(LayoutDefinition layout, LayoutItemDefinition itemParent)
	{
		super(layout, itemParent);
	}

	@Override
	public void readData(INodeObject node)
	{
		super.readData(node);
		mAssociatedCollection = node.optString("@collection"); //$NON-NLS-1$
		mDefaultAction = node.optString("@defaultAction"); //$NON-NLS-1$
		mDataSourceName = node.optString("@DataProvider"); //$NON-NLS-1$
		mEmptyDataSetImage = MetadataLoader.getObjectName(node.optString("@emptyDataSetBackground")); //$NON-NLS-1$
		mEmptyDataSetImageClass = node.optString("@emptyDataSetBackgroundClass"); //$NON-NLS-1$
		mEmptyDataSetText = node.optString("@emptyDataSetText"); //$NON-NLS-1$
		mEmptyDataSetTextClass = node.optString("@emptyDataSetTextClass"); //$NON-NLS-1$
		mRowsPerPageStr = node.optString("@rows"); //$NON-NLS-1$
		mShowSelector = node.optString("@showSelector"); //$NON-NLS-1$
		mDefaultItemLayout = node.optString("@defaultTable"); //$NON-NLS-1$
		mDefaultSelectedItemLayout = node.optString("@defaultSelectedItemLayout"); //$NON-NLS-1$

		// Selection type is not calculated here because calculation depends on item layouts (loaded later).
		mSelectionTypeStr = node.optString("@selectionType"); //$NON-NLS-1$

		mHasPullToRefresh = node.optBoolean("@pullToRefresh", false); //$NON-NLS-1$
	}

	@Override
	public IDataSourceDefinition getDataSource()
	{
		// Return the data source (DP) associated to the grid.
		if (Services.Strings.hasValue(mDataSourceName))
			return getLayout().getParent().getDataSources().get(mDataSourceName);

		return super.getDataSource();
	}

	public Iterable<DataItem> getDataSourceItems()
	{
		if (getDataSource() == null)
			return new ArrayList<DataItem>();

		if (Services.Strings.hasValue(mAssociatedCollection))
		{
			// It's an SDT. Return the members of the SDT type.
			DataItem collectionDataItem = getDataSource().getDataItem(mAssociatedCollection);
			if (collectionDataItem == null)
			{
				// Member information not found.
				Services.Log.warning(String.format("Collection data item (%s) information was not found in specification of data source '%s'.", mAssociatedCollection, getDataSource().getName())); //$NON-NLS-1$
				return new ArrayList<DataItem>();
			}

			IStructuredDataType collectionType = collectionDataItem.getTypeInfo(IStructuredDataType.class);
			if (collectionType == null)
			{
				// Member information does not point to an SDT definition.
				Services.Log.warning(String.format("Data item '%s' is not based on an SDT in specification of data source '%s'.", mAssociatedCollection, getDataSource().getName())); //$NON-NLS-1$
				return new ArrayList<DataItem>();
			}

			return collectionType.getItems();
		}
		else
			return getDataSource().getDataItems();
	}

	public String getDataSourceMember()
	{
		return mAssociatedCollection;
	}

	private static final int ROWS_PER_PAGE_DEFAULT = 10;
	private static final int ROWS_PER_PAGE_UNLIMITED = 0;
	private static String CONTROL_SD_MAPS = "SD Maps";  //$NON-NLS-1$
	private static String CONTROL_SD_CHARTS = "SD Charts";  //$NON-NLS-1$

	public int getRowsPerPage()
	{
		if (mRowsPerPage == null)
			mRowsPerPage = calculateRowsPerPage();

		return mRowsPerPage;
	}

	private int calculateRowsPerPage()
	{
		// Map controls do not support paging, so return UNLIMITED for that case.
		if (getControlInfo() != null && (CONTROL_SD_MAPS.equalsIgnoreCase(getControlInfo().getControl())
				|| CONTROL_SD_CHARTS.equalsIgnoreCase(getControlInfo().getControl())))
			return ROWS_PER_PAGE_UNLIMITED;

		if (Strings.hasValue(mRowsPerPageStr))
		{
			if (mRowsPerPageStr.equalsIgnoreCase("<default>")) //$NON-NLS-1$
				return ROWS_PER_PAGE_DEFAULT;

			if (mRowsPerPageStr.equalsIgnoreCase("<unlimited>")) //$NON-NLS-1$
				return ROWS_PER_PAGE_UNLIMITED;

			Integer intValue = Services.Strings.tryParseInt(mRowsPerPageStr);
			return (intValue != null ? intValue : ROWS_PER_PAGE_DEFAULT);
		}

		return ROWS_PER_PAGE_DEFAULT;
	}

	public ActionDefinition getDefaultAction()
	{
		if (Services.Strings.hasValue(mDefaultAction))
			return getLayout().getParent().getEvent(mDefaultAction);

		return null;
	}

	public String getEmptyDataSetImage()
	{
		return mEmptyDataSetImage;
	}

	public String getEmptyDataSetImageClass()
	{
		return mEmptyDataSetImageClass;
	}

	public String getEmptyDataSetText()
	{
		return Services.Resources.getTranslation(mEmptyDataSetText);
	}

	public String getEmptyDataSetTextClass()
	{
		return mEmptyDataSetTextClass;
	}

	public SelectionType getSelectionType()
	{
		if (mSelectionType == null)
		{
			if (getSelectionMode() == SELECTION_NONE)
			{
				if (Strings.hasValue(mSelectionTypeStr))
				{
					if (mSelectionTypeStr.equalsIgnoreCase("Auto deselect") && getDefaultSelectedItemLayout() == null) //$NON-NLS-1$
						mSelectionType = SelectionType.AutoDeselect;
					else if (mSelectionTypeStr.equalsIgnoreCase("Keep selection while executing") && getDefaultAction() != null) //$NON-NLS-1$
						mSelectionType = SelectionType.KeepWhileExecuting;
					else if (mSelectionTypeStr.equalsIgnoreCase("Keep until new selection")) //$NON-NLS-1$
						mSelectionType = SelectionType.KeepUntilNewSelection;
				}
				else
					mSelectionType = SelectionType.AutoDeselect; // Compatibility with previous behavior (Ev2).
			}
			else
				mSelectionType = SelectionType.KeepUntilNewSelection; // For multiple selection.
		}

		if (mSelectionType == null)
		{
			// "Platform Default" or an invalid combination (e.g. "Keep selection while executing" but no default action).
			if (Strings.hasValue(mDefaultAction))
				mSelectionType = SelectionType.KeepWhileExecuting;
			else if (getDefaultSelectedItemLayout() != null)
				mSelectionType = SelectionType.KeepUntilNewSelection;
			else
				mSelectionType = SelectionType.AutoDeselect;
		}

		return mSelectionType;
	}

	public int getSelectionMode()
	{
		if (Services.Strings.hasValue(mShowSelector))
		{
			if (mShowSelector.equalsIgnoreCase("Always") || mShowSelector.equalsIgnoreCase(Properties.PLATFORM_DEFAULT)) //$NON-NLS-1$
				return SELECTION_ALWAYS;

			// TODO: Enable this when "selection on action" works (support it's incomplete for now).
			if (mShowSelector.equalsIgnoreCase("On Action")) //$NON-NLS-1$
				return SELECTION_ALWAYS;

//			if (mShowSelector.equalsIgnoreCase("On Action"))
//				return SELECTION_ON_ACTION;
		}

		return SELECTION_NONE; // Either "no" or property not present (before GX version that enabled it).
	}

	public List<ILayoutActionDefinition> getMultipleSelectionActions()
	{
		if (mMultipleSelectionActions == null)
		{
			mMultipleSelectionActions = new ArrayList<ILayoutActionDefinition>();
			for (ILayoutActionDefinition action : getLayout().getAllActions())
				if (isMultipleSelectionAction(action.getEvent()))
					mMultipleSelectionActions.add(action);
		}

		return mMultipleSelectionActions;
	}

	private boolean isMultipleSelectionAction(ActionDefinition action)
	{
		if (action != null)
		{
			// See if this action (or any of its components) has multiple selection over THIS grid.
			ActionDefinition.MultipleSelectionInfo msInfo = action.getMultipleSelectionInfo();
			if (msInfo != null && msInfo.useSelection() && msInfo.getGrid() != null && msInfo.getGrid().equalsIgnoreCase(getName()))
				return true;

			for (ActionDefinition component : action.getActions())
			{
				if (isMultipleSelectionAction(component))
					return true;
			}
		}

		return false;
	}

	/**
	 * Returns the expression that, when evaluated, indicates that an item is selected.
	 */
	public String getSelectionExpression()
	{
		return new ControlPropertiesDefinition(this).readDataExpression("@selectionFlag", "@selectionFlagFieldSpecifier"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void calculateBounds(float absoluteWidth, float absoluteHeight)
	{
		for (TableDefinition itemLayout : getItemLayouts())
			itemLayout.calculateBounds(absoluteWidth, absoluteHeight);
	}

	@Override
	public boolean hasAutoGrow()
	{
		if (mHasAutoGrow == null)
		{
			mHasAutoGrow = super.hasAutoGrow();

			// Autogrow is not supported in all cases, filter them here.
			if (mHasAutoGrow && mustDisableAutogrow())
				mHasAutoGrow = false;
		}

		return mHasAutoGrow;
	}

	private boolean mustDisableAutogrow()
	{
		// AutoGrow was, mistakenly, true by default a short while ago.
		// So ignore that value unless it comes from a newer version.
		Version version = getLayout().getParent().getPattern().getInstanceProperties().getDefinitionVersion();
		if (version.isLessThan(VERSION_WITH_SUPPORT_FOR_AUTOGROW))
			return true;

		// For now only default grid supports autogrow.
		if (getControlInfo() != null)
		{
			UserControlDefinition gridControl = UcFactory.getControlDefinition(getControlInfo().getControl());
			if (gridControl != null)
				return true;
		}

		// Item layouts cannot have a percentage height.
		for (TableDefinition itemLayout : getItemLayouts())
		{
			if (!itemLayout.hasDipHeight())
				return true;
		}

		// No controls contained in this grid can have autogrow.
		// We disregards non-data ones, since they shouldn't grow unless they contain a data control that does.
		if (AutogrowFinder.containsDataControlsWithAutogrow(this))
			return true;

		// All ok.
		return false;
	}

	private static class AutogrowFinder implements ILayoutVisitor
	{
		public static boolean containsDataControlsWithAutogrow(GridDefinition grid)
		{
			AutogrowFinder finder = new AutogrowFinder();
			finder.mRoot = grid;
			finder.mOnlyDataControls = true;

			grid.accept(finder);
			return finder.mContainsAutogrow;
		}

		private GridDefinition mRoot;
		private boolean mOnlyDataControls;
		private boolean mContainsAutogrow;

		@Override
		public void enterVisitor(LayoutItemDefinition visitable) { }

		@Override
		public void visit(LayoutItemDefinition visitable)
		{
			if (!mContainsAutogrow)
			{
				if (visitable != mRoot && visitable.hasAutoGrow())
				{
					if (!mOnlyDataControls || LayoutItemsTypes.Data.equalsIgnoreCase(visitable.getType()))
						mContainsAutogrow = true;
				}
			}
		}

		@Override
		public void leaveVisitor(LayoutItemDefinition visitable) { }
	}

	public List<TableDefinition> getItemLayouts()
	{
		if (mItemLayouts == null)
		{
			ArrayList<TableDefinition> itemLayouts = new ArrayList<TableDefinition>();
			for (LayoutItemDefinition itemLayout : getChildItems())
			{
				if (itemLayout instanceof TableDefinition)
					itemLayouts.add((TableDefinition)itemLayout);
			}

			mItemLayouts = itemLayouts;
		}

		return mItemLayouts;
	}

	public TableDefinition getItemLayout(String name)
	{
		if (Strings.hasValue(name))
		{
			for (TableDefinition itemLayout : getItemLayouts())
			{
				if (name.equalsIgnoreCase(itemLayout.optStringProperty("@layoutName"))) //$NON-NLS-1$
					return itemLayout;
			}
		}

		return null;
	}

	/**
	 * Gets the default layout for grid items. Never returns null.
	 */
	public TableDefinition getDefaultItemLayout()
	{
		TableDefinition itemLayout = getItemLayout(mDefaultItemLayout);

		// If the default layout is not specified (or not found), use the first one.
		if (itemLayout == null)
			itemLayout = getItemLayouts().get(0);

		return itemLayout;
	}

	/**
	 * Gets the default layout for grid items when they are selected. May return null.
	 */
	public TableDefinition getDefaultSelectedItemLayout()
	{
		// Bugfix: DefaultSelectedItemLayout is *always* set.
		// Therefore, ignore it when it's the same as the default item layout (otherwise a custom layout is lost when selecting).
		if (mDefaultSelectedItemLayout.equalsIgnoreCase(mDefaultItemLayout))
			return null;

		return getItemLayout(mDefaultSelectedItemLayout);
	}

	public boolean hasBreakBy()
	{
		IDataSourceDefinition dataSource = getDataSource();
		if (dataSource != null)
			return dataSource.getOrders().hasAnyWithBreakBy();
		else
			return false;
	}

	public boolean getHasPullToRefresh()
	{
		return mHasPullToRefresh;
	}

}
