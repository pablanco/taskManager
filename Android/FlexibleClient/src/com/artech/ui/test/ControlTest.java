package com.artech.ui.test;

import android.view.View;

import com.artech.application.MyApplication;
import com.artech.base.metadata.layout.ILayoutItem;

public class ControlTest implements IControlsTest
{
	@Override
	public void onGxControlCreated(View control, ILayoutItem definition)
	{
		// Implementation of control created for test purpose.
		//for now set gx control name to content-desc property

		if (MyApplication.getApp().getUseTestMode())
		{
			control.setContentDescription(definition.getName());
		}
	}
}