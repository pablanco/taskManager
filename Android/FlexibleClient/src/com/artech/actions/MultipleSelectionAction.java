package com.artech.actions;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;

import com.artech.application.MyApplication;
import com.artech.base.application.IProcedure;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ProcedureDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.model.PropertiesObject;
import com.artech.base.services.Services;
import com.artech.controls.IGxGridControl;
import com.artech.utils.Cast;

class MultipleSelectionAction extends Action implements IActionWithOutput
{
	private final ActionDefinition.MultipleSelectionInfo mInfo;
	private final IGxGridControl mGrid;
	private final ProcedureDefinition mTargetProcedure;

	private boolean mSelectionOnDemand;

	private OutputResult mOutput;

	public MultipleSelectionAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
		mInfo = definition.getMultipleSelectionInfo();
		mGrid = Cast.as(IGxGridControl.class, getContext().findControl(mInfo.getGrid()));
		mTargetProcedure = MyApplication.getInstance().getProcedure(mInfo.getTarget());

		// TODO: Enable this when "selection on demand" works.
		// Action needs a continuation if grid selection mode = "on action" and it's a "for each SELECTED line" action.
		// Otherwise Do() already has all the information needed and can proceed.
		// mSelectionOnDemand = (mInfo.useSelection() && mGrid != null && mGrid.getDefinition().getSelectionMode() == GridDefinition.SELECTION_ON_ACTION);
		mSelectionOnDemand = false;
	}

	public static boolean isAction(ActionDefinition definition)
	{
		return (definition.getMultipleSelectionInfo() != null);
	}

	@Override
	List<Action> getPreActions()
	{
		List<Action> list = super.getPreActions();

		if (mSelectionOnDemand)
			list.add(new MultipleSelectionPreAction(this)); // To enable selection.

		return list;
	}

	@Override
	List<Action> getPostActions()
	{
		List<Action> list = super.getPostActions();
		list.add(new MultipleSelectionPostAction(this)); // To finish/clear selection.
		return list;
	}

	@Override
	public boolean Do()
	{
		if (mTargetProcedure == null)
			return false;

		if (mGrid != null)
		{
			// This action is always the confirmation step, even if the selection mode is "on action".
			// Therefore, just perform the server call.
			EntityList gridData = mGrid.getData();
			IProcedure procedure = getApplicationServer().getProcedure(mTargetProcedure.getName());
			
			List<PropertiesObject> values = getMultiCallParameters(procedure , gridData);

			mOutput = procedure.executeMultiple(values);

			return mOutput.isOk();
		}
		else
		{
			Services.Log.Error(String.format("Grid '%s' not found on UI context.", mInfo.getGrid()));
			return false;
		}
	}

	private List<PropertiesObject> getMultiCallParameters(IProcedure procedure, EntityList entities)
	{
		// Get the list of entities that make up the action (all or selected), and evaluate field expressions for each one.
		ArrayList<PropertiesObject> allValues = new ArrayList<PropertiesObject>();

		for (Entity entity : entities)
		{
			if (!mInfo.useSelection() || entity.isSelected())
			{
				// Set "CurrentItem" so that selection can be evaluated over SDTs.
				entities.setCurrentEntity(entity);

				PropertiesObject itemValues = CallGxObjectAction.prepareCallParameters(procedure, this, getDefinition(), mTargetProcedure, entity);
				allValues.add(itemValues);
			}
		}

		return allValues;
	}

	IGxGridControl getGrid()
	{
		return mGrid;
	}

	boolean isOnDemand()
	{
		return mSelectionOnDemand;
	}

	@Override
	public Activity getActivity()
	{
		return super.getActivity();
	}

	@Override
	public OutputResult getOutput()
	{
		return mOutput;
	}
}
