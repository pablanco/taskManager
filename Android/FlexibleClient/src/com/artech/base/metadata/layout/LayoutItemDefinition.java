package com.artech.base.metadata.layout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.DataTypeName;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.Properties;
import com.artech.base.metadata.RelationDefinition;
import com.artech.base.metadata.enums.Alignment;
import com.artech.base.metadata.enums.ControlTypes;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.metadata.enums.ImageUploadModes;
import com.artech.base.metadata.enums.LayoutItemsTypes;
import com.artech.base.metadata.enums.LayoutModes;
import com.artech.base.metadata.rules.PromptRuleDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.metadata.types.IStructuredDataType;
import com.artech.base.model.PropertiesObject;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.Strings;

public class LayoutItemDefinition extends PropertiesObject implements ILayoutItem, ILayoutItemVisitable, Serializable
{
	private static final long serialVersionUID = 1L;
	private String mControlName;
	private String mType;
	private LayoutDefinition mLayout;
	private LayoutItemDefinition mItemParent;
	private ControlInfo mControlInfo;
	private String mCaption;
	private String mThemeClass;
	private final Vector<LayoutItemDefinition> mChildItems = new Vector<LayoutItemDefinition>();
	private boolean mIsVisible;
	private boolean mIsBox;
	private boolean mIsEnabled;
	private boolean mIsAutogrow;
	private boolean mIsHtml;
	private String mLabelPosition;
	private int mLevel;

	public int CellGravity = Alignment.NONE;
	private RelationDefinition mRelationToNavigate;

	// Effective data item points to "real" field if mDataItem is an SDT. Otherwise they are the same.
	private DataItem mDataItem;
	private DataItem mEffectiveDataItem;

	public LayoutItemDefinition(DataItem data)
	{
		mDataItem = data;
	}

	public LayoutItemDefinition(LayoutDefinition layout, LayoutItemDefinition itemParent)
	{
		mLayout = layout;
		mItemParent = itemParent;
	}

	@Override
	public String toString()
	{
		String name = getName();
		return (Services.Strings.hasValue(name) ? name : "<missing control name>");	//$NON-NLS-1$
	}

	/**
	 * Get the data source associated to this layout item.
	 * Can be the component's datasource or the grid's datasource if the item belongs to a grid.
	 * By default it's inherited from parent, and then from component if its the root layout item;
	 * some layout items may override it to return their own datasource (which is then inherited
	 * downwards).
	 */
	public IDataSourceDefinition getDataSource()
	{
		if (mItemParent == null)
			return mLayout.getDataSource();

		return mItemParent.getDataSource();
	}

	@Override
	public String getName()
	{
		return mControlName;
	}

	public void setFK(RelationDefinition relation) {
		mRelationToNavigate = relation;
	}
	public RelationDefinition getFK()	{
		return mRelationToNavigate;
	}

	private void loadDataItem(INodeObject node)
	{
		if (mDataItem != null)
			return;

		String dataElement = node.optString("@attribute"); //$NON-NLS-1$
		if (getDataSource() != null && dataElement.length() > 0)
			mDataItem = getDataSource().getDataItem(dataElement);
	}

	public void readData(INodeObject node)
	{
		mControlName = node.optString("@controlName"); //$NON-NLS-1$
		if (mType.equals("textblock")) //$NON-NLS-1$
			mCaption = node.optString("@caption"); //$NON-NLS-1$
		else if (mType.equals("data")) //$NON-NLS-1$
			mCaption = node.optString("@labelCaption"); //$NON-NLS-1$
		else
			mCaption = Strings.EMPTY;

		mIsVisible = node.optBoolean("@visible", true); //$NON-NLS-1$
		mIsBox = node.optString("@invisibleMode", "Keep Space").equalsIgnoreCase("Keep Space"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		mIsEnabled = node.optBoolean("@enabled", true); //$NON-NLS-1$
		mIsHtml = node.optString("@format", "Text").equalsIgnoreCase("HTML");

		mThemeClass = node.optString("@class"); //$NON-NLS-1$

		// Set Alignment
		String hAlign = node.optString("@hAlign"); //$NON-NLS-1$
		String vAlign = node.optString("@vAlign"); //$NON-NLS-1$
		setHAlign(hAlign);
		setVAlign(vAlign);

		loadDataItem(node);

		INodeObject customProps = node.optNode("CustomProperties"); //$NON-NLS-1$
		if (mControlInfo == null && customProps != null)
		{
			mControlInfo = new ControlInfo();
			String controlType = node.optString("@ControlType"); //$NON-NLS-1$
			if (Services.Strings.hasValue(controlType))
				mControlInfo.setControl(controlType);
			else
				mControlInfo.setControl(customProps.optString("@ControlType")); //$NON-NLS-1$
			mControlInfo.deserialize(customProps);
		}

		mIsAutogrow = (mControlInfo != null) ? mControlInfo.optBooleanProperty("@AutoGrow") : node.optBoolean("@AutoGrow"); //$NON-NLS-1$
	}

	public boolean hasAutoGrow()
	{
		return mIsAutogrow;
	}

	public ControlInfo getControlInfo()
	{
		return mControlInfo;
	}

	private void setVAlign(String vAlign)
	{
		if (vAlign.equals(Properties.VerticalAlignType.Bottom)) {
			CellGravity |= Alignment.BOTTOM;
		} else if (vAlign.equals(Properties.VerticalAlignType.Middle)) {
			CellGravity |= Alignment.CENTER_VERTICAL;
		} else if (vAlign.equals(Properties.VerticalAlignType.Top)) {
			CellGravity |= Alignment.TOP;
		}
	}

	private void setHAlign(String hAlign)
	{
		if (hAlign.equals(Properties.HorizontalAlignType.Left)) {
			CellGravity |= Alignment.LEFT;
		} else if (hAlign.equals(Properties.HorizontalAlignType.Center)) {
			CellGravity |= Alignment.CENTER_HORIZONTAL;
		} else if (hAlign.equals(Properties.HorizontalAlignType.Right)) {
			CellGravity |= Alignment.RIGHT;
		}
	}

	public LayoutDefinition getLayout() { return mLayout; }
	public LayoutItemDefinition getParent() { return mItemParent; }

	/**
	 * Goes up the parent chain until an item is found of the specified type.
	 * @return The nearest ancestor of the specified type, or null if none is found.
	 */
	public <TType extends LayoutItemDefinition> TType findParentOfType(Class<TType> itemType)
	{
		LayoutItemDefinition parent = getParent();
		if (parent == null)
			return null;

		if (itemType.isInstance(parent))
			return itemType.cast(parent);

		return parent.findParentOfType(itemType);
	}

	/**
	 * Goes down the children chain collecting all items of the specified type.
	 * @return The items of the specified type directly or indirectly contained by this one.
	 */
	public <TType extends LayoutItemDefinition> List<TType> findChildrenOfType(Class<TType> itemType)
	{
		ArrayList<TType> list = new ArrayList<TType>();

		if (itemType.isInstance(this))
			list.add(itemType.cast(this));

		for (LayoutItemDefinition child : getChildItems())
		{
			List<TType> childList = child.findChildrenOfType(itemType);
			list.addAll(childList);
		}

		return list;
	}

	private boolean isInsideGrid()
	{
		return (findParentOfType(GridDefinition.class) != null);
	}

	public Vector<LayoutItemDefinition> getChildItems() {
		return mChildItems;
	}

	public void setType(String type) {
		mType = type;
	}

	public String getType() {
		return mType;
	}

	@Override
	public void accept(ILayoutVisitor visitor)
	{
		if (mChildItems.size() > 0)
		{
			visitor.enterVisitor(this);
			visitor.visit(this);
			for (int i = 0; i < mChildItems.size() ; i++)
				mChildItems.elementAt(i).accept(visitor);

			// Ignore Cells as containers because always contains only one element
			if (!getType().equals(LayoutItemsTypes.Cell))
				visitor.leaveVisitor(this);
		}
		else
			visitor.visit(this);
	}

	public String getCaption()
	{
		return Services.Resources.getTranslation(mCaption);
	}

	public void setCaption(String caption)
	{
		mCaption = caption;
	}

	private ThemeClassDefinition mClassDefinition;

	public ThemeClassDefinition getThemeClass()
	{
		// Don't cache the ThemeDefinition if LiveEditing is on.
		if (mClassDefinition == null || Services.Application.isLiveEditingEnabled()) {
			mClassDefinition = PlatformHelper.getThemeClass(mThemeClass);
		}
		return mClassDefinition;
	}

	public DataTypeName getDataTypeName()
	{
		return getDataItem().getDataTypeName();
	}

	public DataItem getDataItem()
	{
		// Effective data item points to "real" field if mDataItem is an SDT. Otherwise they are the same.
		// Cache calculation, since it might be costly.
		if (mEffectiveDataItem == null && mDataItem != null)
			mEffectiveDataItem = getEffectiveDataItem();

		if (mEffectiveDataItem == null)
			Services.Log.Error(String.format("Control '%s' does not have an associated data item.", getName())); //$NON-NLS-1$

		return mEffectiveDataItem;
	}

	private DataItem getEffectiveDataItem()
	{
		String fieldSpecifier = getFieldSpecifier();
		if (Services.Strings.hasValue(fieldSpecifier))
		{
			// An SDT (or BC) field. Get info from SDT structure.
			IStructuredDataType structureType = mDataItem.getTypeInfo(IStructuredDataType.class);
			if (structureType != null)
			{
				fieldSpecifier = fieldSpecifier.replace("item(0).", Strings.EMPTY); //$NON-NLS-1$
				DataItem subItem = structureType.getItem(fieldSpecifier);
				if (subItem != null)
					return subItem;
			}

			Services.Log.warning(String.format("LayoutDataItem '%s' has field specifier '%s', but field information could not be obtained from data item definition.", getName(), fieldSpecifier)); //$NON-NLS-1$
		}

		return mDataItem;
	}

	public String getLabelPosition()
	{
		if (mLabelPosition == null)
		{
			String position = optStringProperty("@labelPosition"); //$NON-NLS-1$

			// Missing means 'platform default'.
			if (!Services.Strings.hasValue(position))
				position = Properties.LabelPositionType.PlatformDefault;

			// And 'platform default' means 'top' if the control is stand-alone and 'left' inside a Grid.
			// At least on Android! Move to a Service if it needs to be different on Blackberry.
			if (position.equals(Properties.LabelPositionType.PlatformDefault))
			{
				if (isInsideGrid())
				{
					if (!getControlType().equals(ControlTypes.PhotoEditor))
						position = Properties.LabelPositionType.Left;
					else
						position =	Properties.LabelPositionType.None;
				}
				else
				{
					position = Properties.LabelPositionType.Top;
				}
			}
			mLabelPosition = position;
		}

		return mLabelPosition;
	}

	public String getInviteMessage()
	{
		String inviteMessage = optStringProperty("@InviteMessage"); //$NON-NLS-1$
		return Services.Resources.getTranslation(inviteMessage);
	}

	public boolean getAutoLink()
	{
		return optBooleanProperty("@autolink");  //$NON-NLS-1$
	}

	public String getControlType()
	{
		DataItem dataItem = getDataItem();
		return (dataItem != null ? getDataItem().getControlType() : ControlTypes.TextBox);
	}

	public boolean getReadOnly(short layoutMode, short trnMode)
	{
		if (optStringProperty("@readonly").equalsIgnoreCase("Auto")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			if (layoutMode == LayoutModes.LIST)
				return true;

			if (layoutMode == LayoutModes.VIEW)
			{
				//var editables / att readonly in view
				return !optStringProperty("@attribute").startsWith("&"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			if (layoutMode == LayoutModes.EDIT)
			{
				//var readonly / att editables in edit
				return optStringProperty("@attribute").startsWith("&"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		if (trnMode == DisplayModes.DELETE)
			return true;

		if (getDataItem().getAutoNumber())
			return true;

		if (trnMode == DisplayModes.EDIT && getDataItem().isKey())
			return true;

		if (optBooleanProperty("@readonly")) //$NON-NLS-1$
			return true;

		return getDataItem().getReadOnly();
	}

	public String getDataId()
	{
		// Replaces item(0) by item(0), so same as before.
		return getDataId(0);
	}

	public String getDataId(int position)
	{
		if (mDataItem == null)
			return null;

		String idName = mDataItem.getName();
		String fieldSpecifier = getFieldSpecifier();

		if (Services.Strings.hasValue(fieldSpecifier))
		{
			fieldSpecifier = fieldSpecifier.replace("item(0)", String.format("item(%d)", position)); //$NON-NLS-1$ //$NON-NLS-2$
			idName += Strings.DOT + fieldSpecifier;
		}

		return idName;
	}

	private String getFieldSpecifier()
	{
		return optStringProperty("@fieldSpecifier"); //$NON-NLS-1$
	}

	public boolean isVisible()
	{
		return mIsVisible;
	}

	public boolean getKeepSpace() {
		return mIsBox;
	}

	public boolean isEnabled()
	{
		return mIsEnabled;
	}

	public int getZOrder()
	{
		return optIntProperty("@zOrder");
	}

	public boolean isHtml() { return mIsHtml; }

	public boolean hasPrompt(short layoutMode, short trnMode)
	{
		if (mLayout != null)
			return mLayout.getPrompts().hasPrompt(this, layoutMode, trnMode);
		else
			return false;
	}

	public PromptRuleDefinition getPrompt(short layoutMode, short trnMode)
	{
		if (mLayout != null)
			return mLayout.getPrompts().getPromptOn(this, layoutMode, trnMode);
		else
			return null;
	}

	public int getMaximumUploadSizeMode()
	{
		String maxUploadSize = (mControlInfo != null) ? mControlInfo.optStringProperty("@MaximumUploadSize") : optStringProperty("@MaximumUploadSize"); //$NON-NLS-1$
		if (maxUploadSize.equalsIgnoreCase("small"))
			return ImageUploadModes.SMALL;
		else if (maxUploadSize.equalsIgnoreCase("medium"))
			return ImageUploadModes.MEDIUM;
		else if (maxUploadSize.equalsIgnoreCase("actualsize") || maxUploadSize.equalsIgnoreCase("actual"))
			return ImageUploadModes.ACTUALSIZE;
		//default
		return ImageUploadModes.LARGE;
	}

	public void setLevel(int level)
	{
		mLevel = level;
	}

	public int getLevel()
	{
		return mLevel;
	}

	public ActionDefinition getEventHandler(String eventName)
	{
		if (mLayout != null && mLayout.getParent() != null)
		{
			String actionName = getName() + "." + eventName;  // String.format("%s.%s", controlDefinition.getName(), eventName) is slooooooooooow.
			return mLayout.getParent().getEvent(actionName);
		}
		else
			return null;
	}
}
