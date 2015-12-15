package com.artech.controls.common;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.utils.Strings;

public abstract class ValueItems<TItem extends ValueItem>
{
	private final ArrayList<TItem> mItems;
	private TItem mEmptyItem;

	public ValueItems()
	{
		mItems = new ArrayList<TItem>();
	}

	protected void add(TItem item)
	{
		mItems.add(item);
	}

	protected void setEmptyItem(TItem item)
	{
		mEmptyItem = item;
		mItems.add(0, item);
	}

	protected void clear()
	{
		mItems.clear();
	}

	public int size()
	{
		return mItems.size();
	}

	public int indexOf(String value)
	{
		for (int i = 0; i < mItems.size(); i++)
			if (mItems.get(i).Value.equalsIgnoreCase(value))
				return i;

		return -1;
	}

	public String getValue(int index)
	{
		ValueItem item = get(index);
		return (item != null ? item.Value : Strings.EMPTY);
	}

	public String getDescription(String value)
	{
		int index = indexOf(value);
		if (index != -1)
			return get(index).Description;
		else
			return (mEmptyItem != null ? mEmptyItem.Description : Strings.EMPTY);
	}

	public TItem get(int index)
	{
		if (index >= 0 && index < mItems.size())
			return mItems.get(index);

		return null;
	}

	public List<ValueItem> getItems()
	{
		return new ArrayList<ValueItem>(mItems);
	}
}
