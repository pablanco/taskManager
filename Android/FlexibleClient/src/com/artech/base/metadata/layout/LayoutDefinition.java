package com.artech.base.metadata.layout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.artech.android.layout.TablesLayoutVisitor;
import com.artech.application.MyApplication;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.ILayoutDefinition;
import com.artech.base.metadata.SectionDefinition;
import com.artech.base.metadata.enums.LayoutItemsTypes;
import com.artech.base.metadata.enums.Orientation;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.settings.PlatformDefinition;
import com.artech.base.metadata.theme.ThemeApplicationBarClassDefinition;
import com.artech.base.metadata.theme.ThemeFormClassDefinition;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.Strings;
import com.artech.common.LayoutHelper;

public class LayoutDefinition
	implements ILayoutDefinition, ILayoutItemVisitable, Serializable
{
	private static final long serialVersionUID = 1L;

	private String mType;
	private String mPlatform;
	private String mOrientation;
    private String mId;

	private Orientation mActualOrientation;
	private boolean mIsOrientationSwitchable;

	private final IDataViewDefinition mParent;
	private final Vector<LayoutItemDefinition> mItems = new Vector<LayoutItemDefinition>();
	private ActionBarDefinition mActionBar;
	private ArrayList<ActionGroupDefinition> mActionGroups;
	private PlatformDefinition mPlatformDefinition;
	private String mThemeClass;
	private String mEmptyDataSetBackground;
	private INodeObject mContent;

	private LayoutPromptsDefinition mLayoutPrompts;
	private LayoutItemLookup mItemLookup;

	public static final String TYPE_ANY = Strings.EMPTY;
	public static final String TYPE_VIEW = "View"; //$NON-NLS-1$
	public static final String TYPE_EDIT = "Edit"; //$NON-NLS-1$

	private static final String ORIENTATION_PORTRAIT = "Portrait"; //$NON-NLS-1$
	private static final String ORIENTATION_LANDSCAPE = "Landscape"; //$NON-NLS-1$

    public LayoutDefinition(IDataViewDefinition parent)
	{
		mParent = parent;
	}

	public TableDefinition getTable()
	{
		return (mItems.size() != 0 ? (TableDefinition)mItems.get(0) : null);
	}

	public IDataViewDefinition getParent() { return mParent; }

	@Override
	public String toString()
	{
		return String.format("[%s] %s (Orientation: %s)", getType(), getPlatformDefinition(), getOrientation());
	}

	public IDataSourceDefinition getDataSource()
	{
		return getParent().getMainDataSource();
	}

    public String getId()
    {
        if (mContent == null)
            return mId;

        return mContent.optString("@id", Strings.EMPTY);
    }

	public String getType()
	{
		if (mContent == null)
			return mType;

		return mContent.optString("@Type", Strings.EMPTY); //$NON-NLS-1$
	}

	public PlatformDefinition getPlatformDefinition()
	{
		if (mPlatformDefinition == null)
		{
			if (mContent != null)
				mPlatform = mContent.optString("@Platform", Strings.EMPTY); //$NON-NLS-1$

			mPlatformDefinition = Services.Application.getPatternSettings().getPlatform(mPlatform);
		}

		return mPlatformDefinition;
	}

	public Orientation getOrientation()
	{
		if (mOrientation == null && mContent != null)
			mOrientation = mContent.optString("@Orientation");  //$NON-NLS-1$

		if (mOrientation != null)
		{
			if (mOrientation.equalsIgnoreCase(ORIENTATION_PORTRAIT))
				return Orientation.PORTRAIT;
			else if (mOrientation.equalsIgnoreCase(ORIENTATION_LANDSCAPE))
				return Orientation.LANDSCAPE;
		}

		return Orientation.UNDEFINED; // Any.
	}

	public void setActualOrientation(Orientation orientation, boolean canRotate)
	{
		mActualOrientation = orientation;
		mIsOrientationSwitchable = canRotate;
	}

	public Orientation getActualOrientation()
	{
		return (mActualOrientation != null ? mActualOrientation : getOrientation());
	}

	public boolean isOrientationSwitchable()
	{
		return mIsOrientationSwitchable;
	}

	public ActionBarDefinition getActionBar() { return mActionBar; }
	public List<ActionGroupDefinition> getActionGroups() { return mActionGroups; }

	private ArrayList<ILayoutActionDefinition> mAllActions = null;

	/**
	 * Gets the list of all actions associated to this layout (either in the action bar, or in the form itself).
	 */
	List<ILayoutActionDefinition> getAllActions()
	{
		if (mAllActions == null)
		{
			mAllActions = new ArrayList<ILayoutActionDefinition>();

			 // Don't duplicate if same event is associated to two controls.
			ArrayList<ActionDefinition> events = new ArrayList<ActionDefinition>();

			// From action bar.
			for (ILayoutActionDefinition action : getActionBar().getActions())
			{
				if (action.getEvent() != null && !events.contains(action.getEvent()))
				{
					mAllActions.add(action);
					events.add(action.getEvent());
				}
			}

			if (getTable() != null)
			{
				for (ILayoutActionDefinition action : getTable().findChildrenOfType(LayoutActionDefinition.class))
				{
					if (action.getEvent() != null && !events.contains(action.getEvent()))
						mAllActions.add(action);
				}
			}
		}

		return mAllActions;
	}

	@Override
	public boolean getShowApplicationBar()
	{
		return mActionBar.isVisible();
	}

	@Override
	public boolean getEnableHeaderRowPattern()
	{
		if (getTable()!=null)
		{
			return getTable().getEnableHeaderRowPattern();
		}
		return false;
	}

	@Override
	public ThemeApplicationBarClassDefinition getHeaderRowApplicationBarClass()
	{
		if (getTable()!=null)
		{
			return PlatformHelper.getThemeClass(ThemeApplicationBarClassDefinition.class, getTable().getHeaderRowApplicationBarClass());
		}
		return null;
	}

	@Override
	public ThemeApplicationBarClassDefinition getApplicationBarClass()
	{
		return (ThemeApplicationBarClassDefinition)mActionBar.getThemeClass();
	}

	public void getDataItems(LayoutItemDefinition parentItem, Vector<LayoutItemDefinition> data)
	{
		Vector<LayoutItemDefinition> items;
		if (parentItem != null)
			items = parentItem.getChildItems();
		else
			items = mItems;

		for (int i = 0; i < items.size(); i++)
		{
			LayoutItemDefinition item = items.elementAt(i);
	    	if (item.getType().equalsIgnoreCase(LayoutItemsTypes.Data))
	    	{
	    		data.add(item);
	    	}
	    	else
	    	{
	    		getDataItems(item, data);
	    	}
		}
	}

	@Override
	public void accept(ILayoutVisitor visitor)
	{
		// We just take the first item because the definition always has only one parent (a table)
		if (mItems.size() > 0)
			mItems.elementAt(0).accept(visitor);
	}

	public ThemeFormClassDefinition getThemeClass()
	{
		 return PlatformHelper.getThemeClass(ThemeFormClassDefinition.class, mThemeClass);
	}

	public void setContent(INodeObject jsonLayout)
	{
		mContent = jsonLayout;
	}

	public void deserialize()
	{
		if (mContent != null)
		{
			mType = mContent.optString("@Type", Strings.EMPTY); //$NON-NLS-1$
			mPlatform = mContent.optString("@Platform", Strings.EMPTY); //$NON-NLS-1$
			mOrientation = mContent.optString("@Orientation"); //$NON-NLS-1$
			mThemeClass = mContent.optString("@class", Strings.EMPTY); //$NON-NLS-1$
            mId = mContent.optString("@id", Strings.EMPTY);
			mEmptyDataSetBackground = MetadataLoader.getObjectName(mContent.optString("@emptyDataSetBackground")); //$NON-NLS-1$

			mActionBar = new ActionBarDefinition(this, mContent.optNode("actionBar")); //$NON-NLS-1$

			mActionGroups = new ArrayList<ActionGroupDefinition>();
			readActionGroups(mContent, this, mActionGroups);

			readLayoutItems(mContent, this, null, 0);

			//if layout has ads add it here
			if (MyApplication.getApp().getUseAds() &&
				getParent().getShowAds() &&
				!(getParent() instanceof SectionDefinition))
			{
				//add another row to main table
				if (getTable()!=null && !TablesLayoutVisitor.hasAdsTable(this))
				{
					//get ads size in this device in dpi
					int AdsSizeDpi = LayoutHelper.AdsSizeDpi;
					//get position at the bottom.
					int positionY = Services.Device.pixelsToDips(getTable().getFixedHeightSum());

					String cellContent = "\"table\": { \"@controlName\": \"GoogleAdsControl\", \"@width\": \"100%\",\"@height\": \"100%\", "+ //$NON-NLS-1$
		            "  \"@visible\": \"True\", \"@FixedHeightSum\": \"0\", \"@FixedWidthSum\": \"0\" }" ; //$NON-NLS-1$

					LayoutItemDefinition layoutRowItemDef = LayoutHelper.getRowWithCell(this, getTable(), cellContent, String.valueOf(AdsSizeDpi) + "dpi", //$NON-NLS-1$
							Strings.ZERO, String.valueOf(positionY), Strings.ZERO, "100",  //$NON-NLS-1$
							Strings.ZERO , String.valueOf(AdsSizeDpi), "100", Strings.ZERO); //$NON-NLS-1$

					//TODO at the top must move all controls y down.
					if (layoutRowItemDef!=null)
					{
						getTable().getChildItems().add(layoutRowItemDef);
						//	change fix FixedHeightSum
						getTable().addToFixedHeightSum(Services.Device.dipsToPixels(AdsSizeDpi));
					}
				}
			}

			mContent = null;
		}

		if (mItemLookup == null)
			mItemLookup = new LayoutItemLookup(this);

		if (mLayoutPrompts == null)
			mLayoutPrompts = new LayoutPromptsDefinition(this);
	}

	public String getEmptyDataSetBackground()
	{
		return mEmptyDataSetBackground;
	}

	public LayoutItemDefinition getControl(String name)
	{
		return mItemLookup.getControl(name);
	}

	public LayoutItemDefinition getDataControl(String dataId)
	{
		return mItemLookup.getDataControl(dataId);
	}

	LayoutPromptsDefinition getPrompts()
	{
		return mLayoutPrompts;
	}

	public static void readLayoutItems(INodeObject json, LayoutDefinition layout, LayoutItemDefinition itemParent, int level)
	{
		// Read child in Order
		for (String attName : json.names())
		{
			if (!attName.startsWith("@")) //$NON-NLS-1$
			{
				// Check if we can create this kind of node
				LayoutItemDefinition dummyLayoutItem = LayoutItemDefinitionFactory.createDefinition(layout, itemParent, attName);
				if (dummyLayoutItem != null)
				{
					// Actually, might be many instances of this node, read each one.
					INodeCollection layoutArray = json.optCollection(attName);
					for (int k = 0; k < layoutArray.length() ; k++)
					{
						LayoutItemDefinition layoutItem = LayoutItemDefinitionFactory.createDefinition(layout, itemParent, attName);
						if (layoutItem == null)
							throw new IllegalStateException("layoutItem cannot be null if dummyLayoutItem wasn't!");

						layoutItem.setType(attName);
						layoutItem.setLevel(level);

						INodeObject singleJson = layoutArray.getNode(k);
						layoutItem.readData(singleJson);
						readLayoutItems(singleJson, layout, layoutItem, level + 1);

						// Add to parent (or as root)
						if (itemParent != null)
							itemParent.getChildItems().add(layoutItem);
						else
							layout.mItems.add(layoutItem);
					}
				}
				else
				{
					if (itemParent != null)
						itemParent.setProperty(attName, json.getString(attName));
				}
			}
			else
			{
				if (itemParent != null)
					itemParent.setProperty(attName, json.getString(attName));
			}
		}
	}

	private static void readActionGroups(INodeObject content, LayoutDefinition layout, ArrayList<ActionGroupDefinition> collection)
	{
		collection.clear();

		INodeObject groupsNode = content.optNode("actionGroups");
		if (groupsNode != null)
		{
			for (INodeObject groupNode : groupsNode.optCollection("actionGroup"))
			{
				ActionGroupDefinition group = new ActionGroupDefinition(layout, groupNode);
				collection.add(group);
			}
		}
	}
}
