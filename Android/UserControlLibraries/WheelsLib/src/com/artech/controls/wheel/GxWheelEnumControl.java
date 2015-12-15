package com.artech.controls.wheel;

import com.artech.base.metadata.layout.ControlInfo;
import com.artech.controls.common.StaticValueItems;

public class GxWheelEnumControl implements IGxWheelControl
{
	private String[] mItemsKey = null;
	private String[] mItemsValue = null;

	public GxWheelEnumControl(ControlInfo info)
	{
		// Just use the new parsing mechanism for now, but refactor all this ASAP.
		StaticValueItems items = new StaticValueItems(info);
		setupValues(items);
	}

	private void setupValues(StaticValueItems items)
	{
		mItemsKey = new String[items.size()];
		mItemsValue = new String[items.size()];
		for (int i = 0; i < items.size(); i++)
		{
			mItemsKey[i] = items.get(i).Value;
			mItemsValue[i] = items.get(i).Description;
		}
	}

	@Override
	public String getDisplayInitialValue() {
		return mItemsValue[0];
	}

	@Override
	public String getGx_DisplayValue(String value) {
		return mItemsValue[GxWheelHelper.getPositionValue(mItemsKey, value)];
	}

	@Override
	public void setViewAdapter(String displayValue, GxWheelPicker wheelControlNumeric, GxWheelPicker wheelControlDecimal) {
		wheelControlNumeric.setViewAdapter(mItemsValue);
		wheelControlNumeric.setCurrentItem(GxWheelHelper.getPositionValue(mItemsValue, displayValue));
	}

	@Override
	public String getCurrentStringValue(GxWheelPicker wheelControlNumeric, GxWheelPicker wheelControlDecimal) {
		int itemPosition = wheelControlNumeric.getCurrentItem();
		return mItemsValue[itemPosition];
	}

	//return the key of value
	public String getGx_Value(String mDisplayValue) {
		return mItemsKey[GxWheelHelper.getPositionValue(mItemsValue, mDisplayValue)];
	}

}
