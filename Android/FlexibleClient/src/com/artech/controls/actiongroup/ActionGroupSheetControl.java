package com.artech.controls.actiongroup;

import java.util.ArrayList;
import java.util.List;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.artech.base.metadata.layout.ActionGroupDefinition;
import com.artech.base.metadata.layout.ActionGroupItemDefinition;
import com.artech.base.metadata.layout.ILayoutActionDefinition;
import com.artech.controls.IGxControl;
import com.artech.fragments.IDataView;

class ActionGroupSheetControl extends ActionGroupBaseControl
{
	private final ArrayList<ActionGroupSheetItemControl> mItems;

	public ActionGroupSheetControl(IDataView dataView, ActionGroupDefinition definition)
	{
		super(dataView, definition);

		// For now only actions are supported.
		mItems = new ArrayList<ActionGroupSheetItemControl>();
		for (ILayoutActionDefinition action : definition.getActions())
			mItems.add(new ActionGroupSheetItemControl((ActionGroupItemDefinition)action));
	}

	@Override
	protected void showActionGroup()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		ArrayList<String> options = new ArrayList<String>();
		ArrayList<ActionGroupSheetItemControl> items = new ArrayList<ActionGroupSheetItemControl>();

		for (ActionGroupSheetItemControl item : mItems)
		{
			if (item.isVisible())
			{
				items.add(item);
				options.add(item.getCaption());
			}
		}

		builder.setTitle(getCaption());
		builder.setItems(options.toArray(new CharSequence[options.size()]), new OptionClickListener(items));
		builder.show();
	}

	@Override
	protected void hideActionGroup()
	{
		// It's modal and closes immediately, so it's not needed.
	}

	private class OptionClickListener implements DialogInterface.OnClickListener
	{
		private final List<ActionGroupSheetItemControl> mSheetItems;

		public OptionClickListener(List<ActionGroupSheetItemControl> items)
		{
			mSheetItems = items;
		}

		@Override
		public void onClick(DialogInterface dialog, int which)
		{
			if (which < mSheetItems.size())
			{
				ActionGroupSheetItemControl item = mSheetItems.get(which);
				if (item.getAction() != null)
					runAction(item.getAction().getEvent());
			}
		}
	}

	private class ActionGroupSheetItemControl extends ActionGroupBaseItemControl<ActionGroupSheetItemControl>
	{
		public ActionGroupSheetItemControl(ActionGroupItemDefinition definition)
		{
			super(definition);
		}

		// There is no need to override setVisible or setEnabled because
		// the sheet is closed immediately after an option is chosen.
	}

	@Override
	public IGxControl getControl(String name)
	{
		for (ActionGroupSheetItemControl control : mItems)
		{
			if (control.getName().equalsIgnoreCase(name))
				return control;
		}

		return null;
	}
 }
