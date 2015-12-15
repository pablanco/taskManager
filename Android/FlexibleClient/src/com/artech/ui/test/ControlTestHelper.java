package com.artech.ui.test;

import android.view.View;

import com.artech.base.metadata.layout.ILayoutItem;
import com.artech.controls.GxButton;
import com.artech.fragments.GridContainer;

public class ControlTestHelper
{
	private static IControlsTest sControlTest = null;

	public static void onGxControlCreated(View control, ILayoutItem definition)
	{
		if (sControlTest == null)
		{
			//create control Test instance
			sControlTest = new ControlTest();
		}

		// handle special cases here.
		if (control instanceof GxButton)
		{
			GxButton gxButton = (GxButton)control;
			if (gxButton.getInnerControl()!=null)
			{
				sControlTest.onGxControlCreated(gxButton.getInnerControl(), definition);
				return;
			}
		}
		if (control instanceof GridContainer)
		{
			return;
		}
		sControlTest.onGxControlCreated(control, definition);
	}
}