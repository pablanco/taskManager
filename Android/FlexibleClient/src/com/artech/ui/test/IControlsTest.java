package com.artech.ui.test;

import android.view.View;

import com.artech.base.metadata.layout.ILayoutItem;

// Interface for testing purposes, it allow to change control properties after create
// UI controls.
public interface IControlsTest {

	void onGxControlCreated(View control, ILayoutItem definition);

}
