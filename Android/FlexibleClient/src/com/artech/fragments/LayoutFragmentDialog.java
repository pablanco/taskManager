package com.artech.fragments;

import android.support.v7.app.AppCompatDialog;

import com.artech.activities.IGxActivity;

class LayoutFragmentDialog extends AppCompatDialog
{
	private final LayoutFragment mContent;

	public LayoutFragmentDialog(LayoutFragment content)
	{
		super(content.getActivity(), content.getTheme());
		mContent = content;
	}

	@Override
	public void onBackPressed()
	{
		IGxActivity activity = mContent.getGxActivity();
		if (activity != null && activity.getController() != null && activity.getController().handleOnBackPressed(mContent))
			return;

		// Standard behavior, close the dialog.
		super.onBackPressed();
	}
}
