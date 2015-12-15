package com.artech.controls;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.Gravity;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.ui.Coordinator;



public class GxEditTextPhone extends GxEditText{

	public GxEditTextPhone(Context context, Coordinator coordinator, LayoutItemDefinition def) {
		super(context, coordinator, def);
		this.setInputType(InputType.TYPE_CLASS_PHONE);
		setGravity(Gravity.RIGHT);
		setMaxEms(10);
	}
	public GxEditTextPhone(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public GxEditTextPhone(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	
}
