package com.artech.actions;

import java.util.Vector;

import android.content.Intent;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class CompositeAction extends Action
{
	private Vector<Action> mActions;
	private int mCurrentAction = 0;

	private boolean mCurrentActionFail = false;
	private Action mCurrentActionExecuted = null;
	private IEventListener mEventListener;

	public interface IEventListener
	{
		// Not needed, for now.
		// void onStartEvent(CompositeAction event);
		void onEndEvent(CompositeAction event, boolean successful);
	}

	public CompositeAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
		mActions = new Vector<Action>();
	}

	public void addAction(Action action)
	{
		if (!(action instanceof NotImplementedAction))
			mActions.add(action);
	}

	void addActions(Iterable<Action> actions)
	{
		for (Action action : actions)
			addAction(action);
	}

	Action getCurrentActionExecuted()
	{
		return mCurrentActionExecuted;
	}

	Action getNextActionToExecute()
	{
		if (mCurrentAction < mActions.size())
			return mActions.get(mCurrentAction);

		return null;
	}

	@Override
	public boolean catchOnActivityResult()
	{
		if (mCurrentActionExecuted != null)
			return mCurrentActionExecuted.catchOnActivityResult();

		return false;
	}

	@Override
	public boolean isActivityEnding()
	{
		if (mCurrentActionExecuted != null)
			return mCurrentActionExecuted.isActivityEnding();
		else
			return false;
	}

	@Override
	public boolean Do()
	{
		if (mCurrentAction < mActions.size())
		{
			Action action = mActions.get(mCurrentAction);
			if (mCurrentActivity != null)
				action.mCurrentActivity = mCurrentActivity;

//			Services.Log.info("DoComposite", "Do Action"+ action.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			mCurrentActionExecuted = action;
			if (!action.Do())
			{
				mCurrentActionFail = true;
				//TODO Clean actions pending stack, did should not be necessary if cancel of an
				//action dont need to clean stack.
				//Only clean current action stack leave the rest alone, so a retry could work
				ActionExecution.cleanCurrentPendingAsDone();
				return false;
			}

//			Services.Log.info("DoComposite", "Action ++ "+ action.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			mCurrentAction++;
		}
		else
		{
			if (isDone() && getCurrentActionExecuted() != null)
				 ActionExecution.onEndEventAsDone(this, !isCurrentActionFail());
		}
		return true;
	}

	public void move(int delta)
	{
		mCurrentAction += delta;
	}

	public boolean isDone()
	{
		if (mCurrentActionFail)
			return true;

		return mCurrentAction >= mActions.size();
	}

	public void setAsDone()
	{
		mCurrentAction = mActions.size();
	}

	public void setCurrentActionFail(boolean fail)
	{
		mCurrentActionFail = fail;
	}

	public boolean isCurrentActionFail()
	{
		return mCurrentActionFail;
	}

	@Override
	public UIContext getContext()
	{
		if (mCurrentActionExecuted != null)
			return mCurrentActionExecuted.getContext();

		return super.getContext();
	}

	@Override
	public ActionResult afterActivityResult(int requestCode, int resultCode, Intent result)
	{
		if (mCurrentActionExecuted != null)
		{
			//logging.
			Services.Log.debug("afterActivityResult afterActivityResult mCurrentActionExecuted" + mCurrentActionExecuted.toString());
			//Services.Log.debug("afterActivityResult requestCode is " + requestCode + " resultCode " + resultCode);
			//Services.Log.debug("afterActivityResult intent is null " + (result==null));
			return mCurrentActionExecuted.afterActivityResult(requestCode, resultCode, result);
		}

		return ActionResult.SUCCESS_CONTINUE;
	}

	@Override
	public String toString()
	{
		String result ="";
		for (Action action : mActions)
			result += " " + action.toString();

		return result;
	}

	public Iterable<Action> getComponents()
	{
		return mActions;
	}

	public boolean isSubRoutine()
	{
		if (getDefinition() != null && Strings.hasValue(getDefinition().getActionType()))
		{
			if (getDefinition().getActionType().equalsIgnoreCase("Subroutine"))  //$NON-NLS-1$
				return true;
		}
		return false;	
	}

	public void setEventListener(IEventListener listener)
	{
		mEventListener = listener;
	}

	public IEventListener getEventListener()
	{
		return mEventListener;
	}
}
