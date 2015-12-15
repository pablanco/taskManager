package com.artech.actions;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.artech.R;
import com.artech.android.api.ProgressIndicatorApi;
import com.artech.android.layout.OrientationLock;
import com.artech.application.MyApplication;
import com.artech.base.application.MessageLevel;
import com.artech.base.application.OutputMessage;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.services.Services;
import com.artech.common.SecurityHelper;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.utils.Cast;

public class ActionExecution extends AsyncTask<Void, Integer, Void>
{
	private final Action mAction;
	private ActionResult mContinuationResult = null;

	private static ActionExecution Current;
	private final static ArrayList<CompositeAction> StackPending = new ArrayList<>();

	public ActionExecution(Action action)
	{
		mAction = action;

		CompositeAction previous = getCurrentCompositeAction();

		// When is the previous action pushed into the stack?
		// 1) when there are more actions afterwards (e.g. msg(), call(), msg()), or.
		// 2) when the last executed action expects result data (e.g. call() with output parameters).
		if (previous != null && !previous.equals(mAction) && (!previous.isDone() || previous.catchOnActivityResult()))
		{
			if (!StackPending.contains(previous))
			{
				//logging.
				///Services.Log.debug("ActionExecution", "create add previous to pending " + previous.toString());
				StackPending.add(previous);
			}
		}

		//logging.
		//Services.Log.debug("ActionExecution", "create current action " + action.toString());
		
		Current = this;
	}

	public static ActionResult continueCurrent(Activity currentActivity, boolean continueFromPendings)
	{
		return continueCurrent(RequestCodes.ACTION, Activity.RESULT_OK, null, currentActivity, continueFromPendings);
	}
	
	private static ActionResult continueCurrent(int requestCode, int resultCode, Intent resultData, Activity currentActivity, boolean continueFromPendings)
	{
		if (Current != null)
			return Current.continueAction(requestCode, resultCode, resultData, currentActivity, continueFromPendings);
		else
			return ActionResult.SUCCESS_CONTINUE;
	}
	
	public static ActionResult continueCurrentFromActivityResult(int requestCode, int resultCode, Intent resultData, Activity currentActivity)
	{
		return continueCurrent(requestCode, resultCode, resultData, currentActivity, true);
	}

	public static void cancelCurrent()
	{
		if (Current != null)
			cleanCurrentPendingAsDone();
	}

	private static CompositeAction getCurrentCompositeAction()
	{
		if (Current != null)
			return Cast.as(CompositeAction.class, Current.getAction());
		else
			return null;
	}

	private static void setCurrentActionExecution(ActionExecution myCurrent)
	{
		Current = myCurrent;
	}

	public static void cleanCurrentOrLastPendingActionFromActivityResult(int requestCode, int resultCode, Intent resultData, Activity currentActivity)
	{
		// The unique action that continue on activity result not OK is scan barcode loop
		CompositeAction compositeInner = getCurrentCompositeAction();
		ApiAction apiAction = null;
		if (compositeInner!=null)
		{
			Action action = compositeInner.getCurrentActionExecuted();
			if (action!=null && action instanceof ApiAction)
			{
				apiAction = (ApiAction) action;
				if (apiAction.isScanInLoopAction())
				{
					if	(ActionExecution.Current!=null)
					{
						ActionExecution.Current.continueActionLocal(requestCode, resultCode, resultData, currentActivity, true);
						return;
					}
				}
			}

			//Return action also continue on activity not ok, special case, for example on activity redirect (dynamic call).
			if (action instanceof ApiAction)
			{
				apiAction = (ApiAction) action;
				if (apiAction.isReturnAction())
				{
					// if Return Action, finish current event
					// Services.Log.info("onPostExecute", "finishCurrentEvent for return action " + composite.toString()); //$NON-NLS-1$ //$NON-NLS-2$
					if	(ActionExecution.Current!=null)
					{
						ActionExecution.Current.finishCurrentEventAndSetPreviousAsCurrent(action, compositeInner, apiAction.finishReturnRequestCode, apiAction.finishReturnResultCode, apiAction.finishReturnResult, apiAction.finishReturnCurrentActivity);
					}
				}
			}
		}

		// call Activity result with resultCode fail and then fail if necessary 
		// Services.Log.debug("cleanCurrentOrLastPendingActionFromActivityResult afterActivityResult mAction" + mAction.toString());
		ActionResult result = ActionResult.SUCCESS_CONTINUE;
		if (Current != null)
		{
			result = Current.getAction().afterActivityResult(requestCode, resultCode, resultData);
		}

		// Only don't fail if ActionResult is REPEAT, same as OnAction Continuation
		if (result != ActionResult.SUCCESS_WAIT)
		{
			//finish this action unsuccessfully
			cleanCurrentPendingAsDone();

			//	Clean last pending action or if cancel set fail called event.
			if (compositeInner==null || (apiAction!=null && apiAction.isCancelAction()))
			{
				removeLastPendingEventFromPendings();
			}
		}
		
	}

	private static void removeLastPendingEventFromPendings() {
		//	TODO: should only clean one level of Pending actions on stack?
		//	StackPending.clear();
		if (StackPending.size() > 0)
		{
			CompositeAction pendingAction = StackPending.get(StackPending.size() - 1);
			StackPending.remove(pendingAction);
		
			//logging.
			//Services.Log.debug("cleanCurrentOrLastPendingAction", "cancel action remove from pending " + pendingAction.toString());

			//finish this action unsuccessfully
			pendingAction.setCurrentActionFail(true);

			// Event ended unsuccessfully. , set Current to null
			onEndEvent(pendingAction, false);
		}
	}

	public static void cleanCurrentPendingAsDone()
	{
		//set current as done.
		CompositeAction compositeInner = getCurrentCompositeAction();
		if (compositeInner != null)
		{
			// finish this action unsuccessfully
			compositeInner.setCurrentActionFail(true);

			// Event ended unsuccessfully. , set Current to null
			onEndEvent(compositeInner, false);
		}
	}

	public static void movePendingActions(int delta)
	{
		CompositeAction current = getCurrentCompositeAction();
		if (current != null)
			current.move(delta);
	}

	public void executeAction()
	{
		CompatibilityHelper.executeAsyncTask(this);
	}

	// Call when composite action is done.
	public static void onEndEventAsDone(CompositeAction action, boolean success)
	{
		onEndEvent(action, success);
	}

	/**
	 * Called whenever an user event ends.
	 * @param success True if composite ended successfully; false if it was interrupted.
	 */
	private static void onEndEvent(CompositeAction event, boolean success)
	{
		//logging.
		//Services.Log.debug("onEndEvent", "onEndEvent " + event.getCurrentActionExecuted().toString() + " , "+ event.toString());
		
		// Notify ProgressIndicator so it can be removed if not done so already.
		ProgressIndicatorApi.onEndEvent(event, success);

		// Activity orientation was locked to prevent rotation during action, can be unlocked now.
		OrientationLock.unlock(event.getActivity(), OrientationLock.REASON_RUN_EVENT);

		// Current event is finished, remove its reference
		setCurrentActionExecution(null);

		if (event.getEventListener() != null)
			event.getEventListener().onEndEvent(event, success);
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		mAction.Do();
		return null;
	}

	@Override
	protected void onPostExecute(Void result)
	{
		// Handle composite action
		if (!mAction.catchOnActivityResult() && mAction instanceof CompositeAction)
		{
			CompositeAction composite = (CompositeAction) mAction;
			boolean continueAction = true;
			if (composite.getCurrentActionExecuted() != null)
			{
				// Services.Log.debug("onPostExecute", "handlePostExecutedSingleAction " + composite.getCurrentActionExecuted().toString());
				continueAction = handlePostExecutedSingleAction(composite.getCurrentActionExecuted());
			}

			if (continueAction)
			{
				// Services.Log.info("onPostExecute", "Continue exec onpost action " + composite.toString()); //$NON-NLS-1$ //$NON-NLS-2$
				continueExecNextAction(composite, RequestCodes.ACTION, Activity.RESULT_OK, null, null, false);
			}
			else
			{

				Action action = composite.getCurrentActionExecuted();
				if (action instanceof ApiAction)
				{
					ApiAction apiAction = (ApiAction) action;
					if (apiAction.isReturnAction())
					{
						// if Return Action, finish current event
						// Services.Log.info("onPostExecute", "finishCurrentEvent for return action " + composite.toString()); //$NON-NLS-1$ //$NON-NLS-2$
						finishCurrentEventAndSetPreviousAsCurrent(action, composite, apiAction.finishReturnRequestCode, apiAction.finishReturnResultCode, apiAction.finishReturnResult, apiAction.finishReturnCurrentActivity);
					}
				}
			}
			return;
		}

		handlePostExecutedSingleAction(mAction);
	}

	private void finishCurrentEventAndSetPreviousAsCurrent(Action action, CompositeAction composite, int requestCode, int resultCode, Intent result, Activity currentActivity)
	{
		// This method is called twice, one when return is finished , one when return to caller activity
		// The order is not always the same, so finish the event the second time.
		if (action instanceof ApiAction)
		{
			ApiAction apiAction = (ApiAction) action;
			if ((apiAction.isReturnAction()) && apiAction.finishReturn)
			{
				// Services.Log.info("finishCurrentEventAndSetPreviousAsCurrent", "call next event after return " + composite.toString()); //$NON-NLS-1$ //$NON-NLS-2$
				composite.setAsDone();

				// check if its a sub
				boolean isSubroutine = composite.isSubRoutine();
				
				if (composite.isDone() && !isSubroutine)
					onEndEvent(composite, !composite.isCurrentActionFail());

				// return in a sub, should cancel this and previous event (caller).
				if (isSubroutine)
				{
					// Current event is finished, remove its reference
					setCurrentActionExecution(null);
					removeLastPendingEventFromPendings();
				}
				
				if (!composite.isCurrentActionFail())
				{
					continueExecNextActionFromPendings(requestCode, resultCode, result, currentActivity);
				}
			}
			else if (apiAction.isReturnAction())
			{
				// Services.Log.info("finishCurrentEventAndSetPreviousAsCurrent", "set return as done " + composite.toString()); //$NON-NLS-1$ //$NON-NLS-2$
				apiAction.finishReturn = true;
				apiAction.finishReturnRequestCode = requestCode;
				apiAction.finishReturnResultCode = resultCode;
				apiAction.finishReturnResult = result;
				apiAction.finishReturnCurrentActivity = currentActivity;
			}
		}
	}

	private boolean handlePostExecutedSingleAction(Action action)
	{
		if (!action.catchOnActivityResult())
		{
			// Handle action with output (procedure, multicall, BC).
			if (action instanceof IActionWithOutput)
				handleActionOutput((IActionWithOutput)action);

			// Handle Login action
			if (action instanceof CallLoginAction)
				handleLoginOutputMessage(action.getContext(), ((CallLoginAction)action).getErrorMessage());

			//handle Login External action
			if (action instanceof CallLoginExternalAction)
				handleLoginOutputMessage(action.getContext(), ((CallLoginExternalAction)action).getErrorMessage());

			// Handle return continuation, refresh
			if (action instanceof ApiAction)
			{
				ApiAction apiAction = (ApiAction) action;
				if (apiAction.isReturnAction())
				{
					if (mAction instanceof CompositeAction)
					{
						return false;
						// if return action not continue event.
					}
				}
			}
		}

		//handle scanbarcode message if could not be loaded
		if (mAction instanceof CompositeAction)
		{
			CompositeAction composite = (CompositeAction)mAction;
			if (composite.getCurrentActionExecuted() != null)
				handlePostExecutedScanSingleActionMessage(composite.getCurrentActionExecuted());
		}
		else
			handlePostExecutedScanSingleActionMessage(mAction);

		// If the action has finished the activity, then the current event is done too.
		if (action.isActivityEnding())
			setCurrentActionExecution(null);

		return true;
	}

	private static void handlePostExecutedScanSingleActionMessage(Action singleAction)
	{
		if ((singleAction instanceof NotImplementedAction))
		{
			String eventText = singleAction.getDefinition().optStringProperty("@eventText"); //$NON-NLS-1$
			String message = String.format(Services.Strings.getResource(R.string.GXM_InvalidEvent), eventText);

			if (Services.Strings.hasValue(eventText))
			{
				try
				{
					MyApplication.getInstance().showMessageDialog(singleAction.getContext(), Services.Strings.getResource(R.string.GXM_errtitle), message);
				}
				catch (Exception ex) { }
			}
			else
			{
				Services.Log.Error("NotImplementedAction Do " + message); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Handles action output (shows messages if necessary).
	 * @param action Action just executed.
	 * @return True if action execution should stop after this; otherwise false.
	 */
	private static boolean handleActionOutput(IActionWithOutput action)
	{
		OutputResult output = action.getOutput();
		if (output != null)
		{
			// If error message is "token expired", redirect to login.
			if (SecurityHelper.handleSecurityError(action.getContext(), output.getStatusCode(), output.getErrorText(), null) != SecurityHelper.Handled.NOT_HANDLED)
				return true;

			 // No special messages, just show them.
			int index = 0;
			for (OutputMessage message : output.getMessages())
			{
				if (message.getLevel() == MessageLevel.ERROR && action.getActivity()!=null)
					MyApplication.getInstance().showError(action.getActivity(), message.getText());
				else
					MyApplication.getInstance().showMessage(message.getText());
				index++;
				// if already show 50 message, ignore the rest. has no sense and could crash the app.
				if (index>50)
					break;
			}
		}

		return false;
	}

	private static void handleLoginOutputMessage(Context actionContext, String errorMessage)
	{
		if (actionContext != null && Services.Strings.hasValue(errorMessage))
		{
			//Show login output messages
			MyApplication.getInstance().showError(actionContext , errorMessage);
			//MyApplication.getInstance().showMessage(errorMessage);
		}
	}

	@Override
	protected void onPreExecute()
	{
		//handle composite action
		if (mAction instanceof CompositeAction)
		{
			CompositeAction composite = (CompositeAction) mAction;
			if (composite.getNextActionToExecute() != null)
			{
				if (composite.mCurrentActivity != null)
					composite.getNextActionToExecute().mCurrentActivity = composite.mCurrentActivity;
			}
		}
	}

	private Action getAction()
	{
		return mAction;
	}

	private ActionResult continueAction(int requestCode, int resultCode, Intent resultData, Activity currentActivity, boolean continueFromPendings)
	{
		// Continue action from onActivityResult().
		// Services.Log.debug("continueAction afterActivityResult mAction" + mAction.toString());
		ActionResult result = mAction.afterActivityResult(requestCode, resultCode, resultData);

		if (result != ActionResult.SUCCESS_WAIT)
		{
			// Go ahead with next action, if any.
			continueActionLocal(requestCode, resultCode, resultData, currentActivity, continueFromPendings);

			// Hack. The mAction above is not the correct one when returning to an activity.
			if (mContinuationResult == ActionResult.SUCCESS_CONTINUE_NO_REFRESH)
				result = ActionResult.SUCCESS_CONTINUE_NO_REFRESH;
		}

		mContinuationResult = null;
		return result;
	}

	private void continueActionLocal(int requestCode, int resultCode, Intent result, Activity currentActivity, boolean continueFromPendings)
	{
		if (mAction instanceof CompositeAction)
		{
			CompositeAction composite = (CompositeAction) mAction;
			if (composite.catchOnActivityResult())
			{
				// Services.Log.info("continueAction", "Continue exec action "+ composite.catchOnActivityResult() + composite.toString()); //$NON-NLS-1$ //$NON-NLS-2$
				continueExecNextAction(composite, requestCode, resultCode, result, currentActivity, continueFromPendings);
			}
			else
			{
				mAction.mCurrentActivity = currentActivity;
				// Services.Log.warning("continueAction", "Not Continue exec action not catch on activity result"+ composite.toString()); //$NON-NLS-1$ //$NON-NLS-2$

				
				if (composite.getCurrentActionExecuted() != null)
				{
					// if Return Action, finish current event
					// Services.Log.info("continueAction", "finishCurrentEventAndSetPreviousAsCurrent "+ composite.catchOnActivityResult() + composite.toString()); //$NON-NLS-1$ //$NON-NLS-2$
					finishCurrentEventAndSetPreviousAsCurrent(composite.getCurrentActionExecuted(), composite, requestCode, resultCode, result, currentActivity);
				}
			}
		}
		else
		{
			if (StackPending.size() > 0 && continueFromPendings)
				continueExecNextActionFromPendings(requestCode, resultCode, result, currentActivity);
		}
	}

	private void continueExecNextAction(CompositeAction composite, int requestCode, int resultCode, Intent result, Activity currentActivity, boolean continueFromPendings)
	{
		if (!composite.isDone())
		{
			ActionExecution exec = new ActionExecution( mAction);
			setCurrentActionExecution(exec);
			if (currentActivity!=null)
				mAction.mCurrentActivity = currentActivity;
			//exec.setAction(mAction);
			// Services.Log.info("continueExecNextAction", "exec action " + composite.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			exec.executeAction();
		}
		else
		{
			// check if its a sub
			boolean isSubroutine = composite.isSubRoutine();
			
			// if current action fail , already called "OnEndEvent", so not call it again.
			// dont endEvent in a sub
			if (!composite.isCurrentActionFail() && !isSubroutine)
				onEndEvent(composite, !composite.isCurrentActionFail());

			if (isSubroutine)
			{
				continueFromPendings = true;
				if (!composite.isCurrentActionFail())
				{
					// Current event is finished, remove its reference
					setCurrentActionExecution(null);
				}
			}
			
			// Not continue action from pendings in an end of a event.
			// Only when return to caller activity
			// if is a sub, continue the caller event
			if (!composite.isCurrentActionFail() && continueFromPendings)
				continueExecNextActionFromPendings(requestCode, resultCode, result, currentActivity);
			else if (composite.isCurrentActionFail() && isSubroutine)
			{
				// if currentActionFail, and isSubrutine, remove caller from pendings
				removeLastPendingEventFromPendings();
			}
			
		}
	}


	private void continueExecNextActionFromPendings(int requestCode, int resultCode, Intent result, Activity currentActivity)
	{
		// If current run if done, check for pending ones
		if (StackPending.size() > 0)
		{
			CompositeAction pendingAction = StackPending.get(StackPending.size() - 1);
			StackPending.remove(pendingAction);

			//logging.
			//Services.Log.debug("continueExecNextActionFromPendings", "continue from pendings " + pendingAction.toString());

			// Notify last pending executed action of activity result.
			// Services.Log.debug("continueExecNextActionFromPendings pendingAction" + pendingAction.toString() );
			// Services.Log.debug("intent is null " + (result==null));
			if (result != null && pendingAction.catchOnActivityResult())
			{
				// Services.Log.debug("continueExecNextActionFromPendings result" + result.toString() );
				mContinuationResult = pendingAction.afterActivityResult(requestCode, resultCode, result);
			}

			// Run pending action.
			pendingAction.mCurrentActivity = currentActivity;
			ActionExecution exec = new ActionExecution(pendingAction);
			setCurrentActionExecution(exec);
			exec.executeAction();
		}
	}


}
