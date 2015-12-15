package com.artech.controls.grids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SectionIndexer;

import com.artech.adapters.AdaptersHelper;
import com.artech.adapters.GxAdapter;
import com.artech.android.layout.GridContext;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.OrderAttributeDefinition;
import com.artech.base.metadata.OrderDefinition;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.GridDefinition.SelectionType;
import com.artech.base.metadata.layout.TableDefinition;
import com.artech.base.metadata.theme.LayoutBoxMeasures;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.FormatHelper;
import com.artech.controllers.ViewData;

public class GridAdapter extends GxAdapter
	implements IGridAdapter, SectionIndexer
{
	private static final String EVENT_SELECTION_CHANGED = "SelectionChanged";
	private final GridHelper mHelper;
	private final GridDefinition mDefinition;
	private int mSelectedIndex = 0;


	private ViewData mViewData;
	private EntityList mData;
	private IDataSourceDefinition mCurrentDataSource;
	private OrderDefinition mCurrentOrder;
	private Activity activity;

	private String[] mSections;
	private HashMap<String, Integer> mAlphaIndexer;

	private boolean mInSelectionMode;
	private static final int CHECKBOX_WIDTH = 70; // dip

	public GridAdapter(Context context, GridHelper helper, GridDefinition definition)
	{
		super(context);
		mHelper = helper;
		mDefinition = definition;
		if (context instanceof Activity)
			activity = (Activity) context;
		else
			activity = (Activity) ((GridContext)context).getBaseContext();
	}
	
	public GridDefinition getDefinition() {
		return mDefinition;
	}

	public void setData(ViewData data)
	{
		mViewData = data;
		mData = data.getEntities();
		mCurrentDataSource = (data.getUri() != null ? data.getUri().getDataSource() : null);
		mCurrentOrder = (data.getUri() != null ? data.getUri().getOrder() : null);
		notifyDataSetChanged();
	}

	public void runDefaultAction(int index)
	{
		mHelper.runDefaultAction(getEntity(index));
	}

	@Override
	public ViewData getData() { return mViewData; }

	@Override
	public int getCount()
	{
		return (mData != null ? mData.size() : 0);
	}

	@Override
	public Object getItem(int position)
	{
		return getEntity(position);
	}

	@Override
	public Entity getEntity(int position)
	{
		return (mData != null ? mData.get(position) : null);
	}

	// this method  is called always with a value in entity
	public int getIndexOf(Entity item)
	{
		return (mData != null ? mData.indexOf(item) : -1);
	}
	
	public void setSelectedIndex(int selectedIndex) {
		mSelectedIndex = selectedIndex;
	}
	
	public int getSelectedIndex() {
		return mSelectedIndex;
	}
	
	public void selectIndex(int index, GridContext context, GridHelper helper, boolean raiseEvents) {
		if (index >= getCount())
			return;
		Entity item = getEntity(index);
		GridDefinition mDefinition = getDefinition();
		
		if (item != null && mDefinition.getSelectionMode() == GridDefinition.SELECTION_NONE)
		{
			Entity newSelection = item;
			Entity previousSelection = context.getSelection();
			boolean selectionChanged = false;

			if (newSelection != previousSelection)
			{
				context.setSelection(item);
				selectionChanged = true;
				setSelectedIndex(index);
				if (selectionChanged && raiseEvents)
					helper.getCoordinator().runControlEvent(helper.getGridView(),  GridAdapter.EVENT_SELECTION_CHANGED);
			}
			else
			{
				// Tapped on the selected item: Should it be deselected or remain selected?
				if (mDefinition.getSelectionType() == SelectionType.KeepUntilNewSelection)
				{
					context.setSelection(null);
					selectionChanged = true;
				}
			}

			// Force a re-layout, if necesssary.
			if (selectionChanged && (helper.hasDifferentLayoutWhenSelected(newSelection) || helper.hasDifferentLayoutWhenSelected(previousSelection))) {
				activity.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						notifyDataSetChanged();
						
					}
					
				});
				
				
			}
	
		}
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		GridItemViewInfo itemView = mHelper.getItemView(this, position, convertView, mInSelectionMode);

		// Header for alpha indexer.
		if (isGroupHeaderVisible(position))
		{
			itemView.getHeaderText().setVisibility(View.VISIBLE);
			itemView.getHeaderText().setText(getGroupHeaderText(position));
		}
		else
		{
			if (itemView.getHeaderText() != null)
				itemView.getHeaderText().setVisibility(View.GONE);
		}

		return itemView.getView();
	}

	@Override
	public int getItemViewType(int position)
	{
		Entity item = getEntity(position);
		if (item != null)
		{
			TableDefinition itemLayout = mHelper.getLayoutFor(item);
			if (itemLayout != null)
				return mDefinition.getItemLayouts().indexOf(itemLayout);
		}

		return 0;
	}

	@Override
	public int getViewTypeCount()
	{
		return mDefinition.getItemLayouts().size();
	}

	/**
	 * Returns whether the item's view is empty (i.e. either it has no possible layout,
	 * or its associated layout is an empty table with no controls), so that the Grid
	 * may decide to forgo showing it altogether.
	 */
	public boolean isItemViewEmpty(Entity item)
	{
		if (item == null)
			return true;

		TableDefinition itemLayout = mHelper.getLayoutFor(item);
		if (itemLayout == null)
			return true;

		return (itemLayout.Rows.size() == 0);
	}

	public void setSelectionMode(boolean value)
	{
		if (mInSelectionMode != value)
		{
			mInSelectionMode = value;

			// Update table bounds to reserve/free space for checkbox.
			mHelper.setReservedSpace(value ? CHECKBOX_WIDTH : 0);

			// Refresh so that checkbox is shown/hidden.
			notifyDataSetChanged();
		}
	}

	//reserve space for margin/padding in width, for scroll grids
	public void adjustSizeWithMarginPadding(GridDefinition mDefinition) {
		int widthToRemove = 0;
		ThemeClassDefinition themeClass = mDefinition.getThemeClass();
		 if (themeClass!=null && themeClass.hasMarginSet())
		 {
			LayoutBoxMeasures margins = themeClass.getMargins();
			widthToRemove = widthToRemove + (margins.left+margins.right);
		 }
		 if (themeClass!=null && themeClass.hasPaddingSet())
		 {
			LayoutBoxMeasures padding = themeClass.getPadding();
			widthToRemove = widthToRemove + (padding.left+padding.right);
		 }

		 if (widthToRemove>0)
			 mHelper.setReservedSpace(widthToRemove);
	}

	public void setBounds(int width, int height)
	{
		mHelper.setBounds(width, height);
	}

	public boolean isGroupHeaderVisible(int position)
	{
		if (mCurrentDataSource == null || !mCurrentDataSource.getOrders().hasBreakBy(mCurrentOrder))
			return false;

		List<DataItem> groupAttributes = mCurrentDataSource.getOrders().getBreakByAttributes(mCurrentOrder);
		if (groupAttributes.size() == 0)
			return false;

		if (position == 0)
			return true;

		// Compare position with position-1 item.
		Entity item = getEntity(position);
		Entity itemPrevious = getEntity(position - 1);

		boolean sameGroup = true;
		for (DataItem att : groupAttributes)
		{
			Object propertyValue = item.getProperty(att.getName());
			if (propertyValue != null)
			{
				if (!propertyValue.equals(itemPrevious.getProperty(att.getName())))
				{
					sameGroup = false;
					break;
				}
			}
		}

		return !sameGroup;
	}

	public CharSequence getGroupHeaderText(int position)
	{
		int index = 0;
		Entity item = getEntity(position);
		StringBuilder result = new StringBuilder();
		for (DataItem att : mCurrentDataSource.getOrders().getBreakByDescriptionAttributes(mCurrentOrder))
		{
			if (index != 0)
				result.append(" - "); //$NON-NLS-1$

			result.append(AdaptersHelper.getFormattedText(item, att.getName(), att));
			index++;
		}

		return result.toString();
	}

	private boolean showAlphaIndexer()
	{
		return (mCurrentOrder != null && mCurrentOrder.getEnableAlphaIndexer());
	}

	@Override
	public int getPositionForSection(int section)
	{
		if (showAlphaIndexer())
		{
			calculateSections();
			if (section < mSections.length)
				return mAlphaIndexer.get(mSections[section]);
		}
		return 0;
	}

	@Override
	public int getSectionForPosition(int position)
	{
		return 1;
	}

	@Override
	public Object[] getSections()
	{
		if (showAlphaIndexer())
		{
			calculateSections();
			return mSections;
		}
		return new Object[0];
	}

	@Override
	public void notifyDataSetChanged()
	{
		super.notifyDataSetChanged();

		if (showAlphaIndexer())
		{
			mSections = null;
			calculateSections();
		}
	}

	private void calculateSections()
	{
		if (mSections == null)
		{
			int count = getCount();
			mAlphaIndexer = new HashMap<String, Integer>();
			for (int x = count-1; x >= 0; x--)
			{
				Entity ent = mData.get(x);

				// get the first letter.
				String currentValue = getSectionText(ent);
				if (Services.Strings.hasValue(currentValue))
				{
					try
					{
						// Check if the currentValue is a number.
						//noinspection ResultOfMethodCallIgnored
						Integer.valueOf(currentValue);
						mAlphaIndexer.put(currentValue, x);
					}
					catch (NumberFormatException ex)
					{
						// The currentValue is a String.
						// Convert to uppercase, otherwise lowercase a-z will be sorted after upper A-Z.
						String ch = currentValue.substring(0, 1).toUpperCase(Locale.getDefault());

						// HashMap will prevent duplicates
						mAlphaIndexer.put(ch, x);
					}
				}
			}

			Set<String> sectionLetters = mAlphaIndexer.keySet();

			// create a list from the set to sort
			ArrayList<String> sectionList = new ArrayList<String>(sectionLetters);
			Collections.sort(sectionList);
			mSections = new String[sectionList.size()];
			sectionList.toArray(mSections);
		}
	}

	private String getSectionText(Entity item)
	{
		String result = Strings.EMPTY;
		if (mCurrentOrder != null && mCurrentOrder.getEnableAlphaIndexer())
		{
			for (OrderAttributeDefinition orderAtt : mCurrentOrder.getAttributes())
			{
				String attValue = item.optStringProperty(orderAtt.getName());
				result += FormatHelper.formatValue(attValue, orderAtt.getAttribute());
			}
		}

		return result;
	}
}