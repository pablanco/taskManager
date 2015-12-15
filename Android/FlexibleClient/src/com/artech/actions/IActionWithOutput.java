package com.artech.actions;

import android.app.Activity;

import com.artech.base.application.OutputResult;

public interface IActionWithOutput
{
	Activity getActivity();
	UIContext getContext();
	OutputResult getOutput();
}
