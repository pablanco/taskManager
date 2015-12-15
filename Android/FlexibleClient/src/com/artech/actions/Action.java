package com.artech.actions;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;

import com.artech.activities.ActivityHelper;
import com.artech.adapters.AdaptersHelper;
import com.artech.application.MyApplication;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.expressions.Expression;
import com.artech.base.metadata.expressions.Expression.Type;
import com.artech.base.metadata.expressions.Expression.Value;
import com.artech.base.metadata.expressions.ExpressionValueBridge;
import com.artech.base.metadata.expressions.IAssignableExpression;
import com.artech.base.metadata.expressions.IExpressionContext;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.providers.IApplicationServer;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.DataRequest;
import com.artech.common.ExecutionContext;
import com.artech.controllers.IDataSourceBoundView;
import com.artech.controllers.ViewData;
import com.artech.controls.IGxControl;
import com.artech.controls.IGxEdit;

public abstract class Action
{
	private final UIContext mContext;
	private final ActionDefinition mDefinition;
	private final ActionParameters mParameters;

	protected Activity mCurrentActivity;

	protected Action(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		if (context == null)
			throw new IllegalArgumentException("Null UIContext passed to Action constructor."); //$NON-NLS-1$

		mContext = context;
		mDefinition = definition;
		mParameters = (parameters != null ? parameters : ActionParameters.EMPTY);
	}

	protected Action(Action baseAction)
	{
		this(baseAction.getContext(), baseAction.getDefinition(), baseAction.getParameters());
	}

	public UIContext getContext() { return mContext; }

	protected Activity getActivity()
	{
		Activity activity = mContext.getActivity();

		if (activity == null)
			activity = ActivityHelper.getCurrentActivity();

		return activity;
	}

	public Activity getMyActivity()
	{
		if (mCurrentActivity != null && mCurrentActivity != getActivity())
			Services.Log.Error("MyActivity different from context Activity. Action: " + mDefinition.getName());

		return (mCurrentActivity != null)? mCurrentActivity : getActivity();
	}

	public ActionDefinition getDefinition() { return mDefinition; }
	public IApplicationServer getApplicationServer() {
		return MyApplication.getApplicationServer(getContext().getConnectivitySupport());
	}

	protected ActionParameters getParameters() { return mParameters; }
	public Entity getParameterEntity() { return mParameters.getEntity(); }

	public abstract boolean Do();

	/**
	 * If this action needs to be executed in a multi-step fashion, returns the list of actions
	 * that must execute BEFORE the current one.
	 */
	List<Action> getPreActions()
	{
		return new ArrayList<Action>();
	}

	/**
	 * If this action needs to be executed in a multi-step fashion, returns the list of actions
	 * that must execute AFTER the current one.
	 */
	List<Action> getPostActions()
	{
		return new ArrayList<Action>();
	}

	/**
	 * Indicates that the action will wait for (and handle) the parent activity's OnActivityResult().
	 * Needed for actions that need to pause for user input by calling another activity.
	 */
	public boolean catchOnActivityResult() { return false; }

	/**
	 * Indicates that the current activity will finish when the action is executed
	 * (e.g. a call to another panel with CallOptions.Type = Replace).
	 */
	public boolean isActivityEnding() { return false; }

	/**
	 * Called after activity's onActivityResult() to process result.
	 * @param result Intent returned by the called activity.
	 * @return Desired action after processing intent.
	 */
	public ActionResult afterActivityResult(int requestCode, int resultCode, Intent result) { return ActionResult.SUCCESS_CONTINUE; }

	/**
	 * Gets the list of parameters values of the action (evaluated on the
	 * context of the parameter entity).
	 */
	protected List<Object> getParameterValues()
	{
		ArrayList<Object> parameterValues = new ArrayList<Object>();
		for (ActionParameter parameter : mDefinition.getParameters())
		{
			Object value = getParameterValue(parameter);
			parameterValues.add(value);
		}

		return parameterValues;
	}

	/**
	 * Evaluates a parameter to get its value (on the context of the parameter entity).
	 * Uses the expression (if supplied), otherwise the value.
	 */
	public Object getParameterValue(ActionParameter parameter)
	{
		return getParameterValue(parameter, null);
	}

	/**
	 * Evaluates a parameter to get its value (on the context of the supplied entity).
	 * Uses the expression (if supplied), otherwise the value.
	 */
	Object getParameterValue(ActionParameter parameter, Entity contextEntity)
	{
		if (contextEntity == null)
			contextEntity = getParameterEntity();

		if (parameter.getExpression() != null)
		{
			try
			{
				Expression.Value value = parameter.getExpression().eval(new ExpressionContext(contextEntity));
				return ExpressionValueBridge.convertValueToEntityFormat(value);
			}
			catch (Exception e)
			{
				// Log exception and fall through to "old" evaluation.
				Services.Log.Error(String.format("An exception occurred while evaluating expression '%s'.", parameter.getExpression()), e);
			}
		}

		// Old evaluation, supports constants and attributes/variables.
		return ActionParametersHelper.getParameterValue(contextEntity, parameter.getValue());
	}

	/**
	 * Sets a variable with the value of the action output.
	 * e.g. after the action "&Var = Proc1.Udp()" or "&Var = SDActions.ScanBarcode()"
	 */
	protected void setOutputValue(final ActionParameter target, Object value)
	{
		if (target.getExpression() instanceof IAssignableExpression)
		{
			IAssignableExpression expression = (IAssignableExpression)target.getExpression();
			Entity entity = getParameterEntity();
			ExpressionContext context = new ExpressionContext(entity);

			// Set the value, then get the name of the "affected" variable/attribute to refresh its UI.
			if (expression.setValue(context, value))
			{
				String outputName = expression.getRootName();
				if (Strings.hasValue(outputName))
					updateUIAfterOutput(entity, outputName);
			}
			else
				Services.Log.warning("IAssignableExpression.setValue() failed. Expression is: " + expression.toString());
		}
		else
			setOutputValue(target.getValue(), value);
	}

	/**
	 * Sets a variable with the value of the action output.
	 * e.g. after the action "&Var = Proc1.Udp()" or "&Var = SDActions.ScanBarcode()"
	 */
	protected void setOutputValue(String name, Object value)
	{
		if (Strings.hasValue(name) && value != null)
		{
			final Entity entity = getParameterEntity();

			if (entity != null)
			{
				entity.setProperty(name, value);
				updateUIAfterOutput(entity, name);
			}
		}
	}

	/**
	 * Refresh the screen control(s) associated to the output, if any.
	 */
	private void updateUIAfterOutput(final Entity entity, final String outputName)
	{
		if (getContext() == null || getActivity() == null)
			return;

		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				// Update individual fields.
				for (IGxEdit edit : findControlsBoundTo(getContext(), outputName))
					AdaptersHelper.setEditValue(edit, entity);

				// Update (SDT) grids.
				updateGridAfterOutput(entity, outputName);

			}
		});
	}

	private void updateGridAfterOutput(Entity entity, String outputName)
	{
		// TODO: Currently won't work on nested grids.
		Object outputValue = entity.getProperty(outputName);
		if (outputValue != null && outputValue instanceof EntityList)
		{
			EntityList gridData = (EntityList)outputValue;
			for (IDataSourceBoundView grid : findGridsBoundTo(getContext(), outputName))
			{
				grid.update(ViewData.customData(gridData, DataRequest.RESULT_SOURCE_SERVER));
				break; // Don't look for other grids.
			}
		}
		else if (Strings.hasValue(outputName) && outputName.contains(Strings.DOT) && Strings.toLowerCase(outputName).contains("item"))
		{
			outputName = outputName.substring(0, outputName.indexOf(Strings.DOT));
			updateGridAfterOutput(entity, outputName);
		}
	}

	private static List<IGxEdit> findControlsBoundTo(UIContext context, String name)
	{
		if (context == null)
			return new ArrayList<IGxEdit>();

		List<IGxEdit> boundControls = context.findControlsBoundTo(name);
		if (boundControls.size() != 0)
			return boundControls;

		return findControlsBoundTo(context.getParent(), name);
	}

	private static List<IDataSourceBoundView> findGridsBoundTo(UIContext context, String name)
	{
		ArrayList<IDataSourceBoundView> list = new ArrayList<IDataSourceBoundView>();

		if (Strings.hasValue(name))
		{
			while (context != null)
			{
				for (IDataSourceBoundView grid : context.findBoundGrids())
				{
					if (name.equalsIgnoreCase(grid.getDataSourceMember()))
						list.add(grid);
				}

				context = context.getParent();
			}
		}

		return list;
	}

	protected IGxControl findControl(String name)
	{
		IGxControl control = findControl(mContext, name);

		if (control == null)
			Services.Log.warning(String.format("Control '%s' not found in the form.", name));

		return control;
	}

	private static IGxControl findControl(UIContext context, String name)
	{
		if (context == null)
			return null;

		IGxControl control = context.findControl(name);
		if (control != null)
			return control;

		// Search in parent context (e.g. update form control property from grid item action).
		return findControl(context.getParent(), name);
	}

	private class ExpressionContext implements IExpressionContext
	{
		private final Entity mExpressionContextEntity;

		private ExpressionContext(Entity expressionContextEntity)
		{
			mExpressionContextEntity = expressionContextEntity;
		}

		private Entity getExpressionContextEntity()
		{
			if (mExpressionContextEntity != null)
				return mExpressionContextEntity;

			return getParameterEntity();
		}

		@Override
		public ExecutionContext getExecutionContext()
		{
			return ExecutionContext.inAction(Action.this);
		}

		@Override
		public Value getValue(String name, Type expectedType)
		{
			return ExpressionValueBridge.convertEntityFormatToValue(getExpressionContextEntity(), name, expectedType);
		}

		@Override
		public IGxControl getControl(String name)
		{
			return findControl(name);
		}
	}
}
