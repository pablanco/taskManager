package com.artech.fragments;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import com.artech.actions.UIContext;
import com.artech.activities.IGxActivity;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.layout.Size;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.ReflectionHelper;
import com.artech.ui.Anchor;

public abstract class BaseFragment extends DialogFragment
{
	private final ArrayList<ActionDefinition> mPendingActions = new ArrayList<ActionDefinition>();
	private Anchor mAnchor;
	private Size mDesiredSize;

	@Override
	public void onStart()
	{
		super.onStart();
		if (getDialog() == null)
			return;

		if (mAnchor != null)
		{
			// A callout. Not modal, and (TODO) should be shown at specified position.
			getDialog().setCanceledOnTouchOutside(true);

			// WindowManager.LayoutParams p = getDialog().getWindow().getAttributes();
			// p.x =  mAnchorRect.left;
			// p.y = mAnchorRect.top + (mAbsoluteHeightForTable / 2) - mAnchorRect.height() / 2 - (screenSize.getHeight()/2); // 0 is the center of the screen
			// getDialog().getWindow().setAttributes(p);
		}
		else
		{
			// A popup is modal.
		    getDialog().setCanceledOnTouchOutside(false);
		}

		if (mDesiredSize != null)
		{
			// Set the dialog size. The frame adds padding (actually, insets); see
			// android\support\v7\appcompat\res\drawable\abc_dialog_material_background_dark.xml
			// so we need to add these pixels back to account for it; otherwise the content is cut.
			final int DIALOG_INSETS = Services.Device.dipsToPixels(16);
			int dialogWidth = mDesiredSize.getWidth() + 2 * DIALOG_INSETS;
			int dialogHeight = mDesiredSize.getHeight() + 2 * DIALOG_INSETS;

			// Normally the dialog would auto-size itself to match its content, but unfortunately
			// it does not grow beyond a certain width (hence, this code).
			getDialog().getWindow().setLayout(dialogWidth, dialogHeight);
		}
	}

	// BUGFIX: Workaround for https://code.google.com/p/android/issues/detail?id=170053
	// (marked as fixed in "future release"). Remove it with the release of AppCompat > v22.1.1
	public LayoutInflater getLayoutInflater(Bundle savedInstanceState)
	{
		if (getShowsDialog())
		{
			try
			{
				AppCompatDialog dialog = (AppCompatDialog)onCreateDialog(savedInstanceState);
				dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
				ReflectionHelper.setField(this, "mDialog", dialog);

				return LayoutInflater.from(dialog.getContext());
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		else
			return super.getLayoutInflater(savedInstanceState);
	}

	public void setDialogAnchor(Anchor anchor)
	{
		mAnchor = anchor;
	}

	public void setDesiredSize(Size size)
	{
		mDesiredSize = size;
	}

	protected Size getDesiredSize()
	{
		return mDesiredSize;
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		if (mPendingActions.size() != 0)
		{
			ArrayList<ActionDefinition> pendingActions = new ArrayList<ActionDefinition>(mPendingActions);
			mPendingActions.clear();

			for (ActionDefinition action : pendingActions)
				runAction(action, null);
		}
	}

	public void runAction(ActionDefinition action, Anchor anchor)
	{
		// Enqueue action if it's fired before the fragment is attached to the activity
		// (Activity is necessary for building UIContext).
		if (getActivity() != null)
		{
			UIContext context = getUIContext();
			context.setAnchor(anchor);
			((IGxActivity)getActivity()).getController().runAction(context, action, getContextEntity());
		}
		else
			mPendingActions.add(action);
	}

	public abstract IViewDefinition getDefinition();
	public abstract UIContext getUIContext();
	public abstract Entity getContextEntity();
	public abstract void setActive(boolean active);

	public abstract void saveFragmentState(LayoutFragmentState state);
	public abstract void restoreFragmentState(LayoutFragmentState state);
	public abstract List<BaseFragment> getChildFragments();
	
	public abstract List<View> getControlViews();
}
