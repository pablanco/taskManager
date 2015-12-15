package com.artech.controls.wheel;

import com.artech.utils.ThemeUtils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.LinearLayout.LayoutParams;
import antistatic.spinnerwheel.WheelVerticalView;
import antistatic.spinnerwheel.adapters.AbstractWheelTextAdapter;
import antistatic.spinnerwheel.adapters.ArrayWheelAdapter;
import antistatic.spinnerwheel.adapters.NumericWheelAdapter;
import antistatic.spinnerwheel.adapters.WheelViewAdapter;

public class GxWheelPicker extends WheelVerticalView {
	private WheelViewAdapter mAdapter; 
	
	public GxWheelPicker(Context context) {
		super(context);
		setInterpolator(new AnticipateOvershootInterpolator());
	}
	
	public GxWheelPicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		setInterpolator(new AnticipateOvershootInterpolator());
	}
	
	public void extendToFillParent() {
		LayoutParams param = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		setLayoutParams(param);
	}
	
	public String getCurrentItemText()
	{
		WheelViewAdapter adapter = getAdapter();
		if (adapter instanceof NumericWheelAdapter) {
			return (String) ((NumericWheelAdapter) adapter).getItemText(getCurrentItem());
		} else if (adapter instanceof ArrayWheelAdapter<?>) {
			return (String) ((ArrayWheelAdapter<?>) adapter).getItemText(getCurrentItem());
		}
		return "";
	}
	
	public void setViewAdapter(String[] mItemsValue)
	{
		setAdapter(new ArrayWheelAdapter<String>(getContext(), mItemsValue));
	}
	
	public void setViewAdapter(int mMinValue, int mMaxValue)
	{
		setAdapter(new NumericWheelAdapter(getContext(), mMinValue, mMaxValue));
	}	

	private WheelViewAdapter getAdapter() {
		return mAdapter;
	}

	private void setAdapter(AbstractWheelTextAdapter adapter) {
		int textColor = ThemeUtils.getAndroidThemeColorId(getContext(), android.R.attr.textColorPrimary);
		adapter.setTextColor(textColor);
		super.setViewAdapter(adapter);
		mAdapter = adapter;
	}
}
