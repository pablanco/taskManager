package com.artech.ui;

import android.view.View;

import com.artech.actions.UIContext;
import com.artech.base.metadata.ActionDefinition;
import com.artech.controls.IGxEdit;

/**
 * Interface for the utility class that controls can use to interact amongst themselves
 * or fire form events.
 * @author matiash
 */
public interface Coordinator
{
	/**
	 * Gets a variable/attribute value.
	 * @param name Variable/attribute name.
	 */
	Object getValue(String name);

	/**
	 * Sets a variable/attribute value.
	 * @param name Variable/attribute name.
	 * @param value Value to be assigned to the variable/attribute.
	 */
	void setValue(String name, Object value);

	/**
	 * Gets a variable/attribute value in string format.
	 * @param name Variable/attribute name.
	 */
	String getStringValue(String name);

	/**
	 * Used to signal that an edit's control value has changed (and possibly notify
	 * other controls that depend on the current one).
	 * @param edit Edit control.
	 * @param fireControlValueChanged fires the ControlValueChanged event.
	 */
	void onValueChanged(IGxEdit edit, boolean fireControlValueChanged);

	/**
	 * Used to fire an arbitrary action from a control.
	 * @param action Name of the action to be run.
	 * @param anchor Location of the view firing the action (used for callouts, for example).
	 */
	boolean runAction(String action, Anchor anchor);

	/**
	 * Used to fire an arbitrary action from a control.
	 * @param action Action to be run.
	 * @param anchor Location of the view firing the action (used for callouts, for example).
	 */
	boolean runAction(ActionDefinition action, Anchor anchor);

	/**
	 * Used to fire a control's UI event (such as 'Image.Tap' or '&CustomerName.Swipe').
	 * @param control Control that fires the event.
	 * @param eventName Event name.
	 */
	boolean runControlEvent(View control, String eventName);

	/**
	 * Used to fire a control's UI event (such as 'Image.Tap' or '&CustomerName.Swipe').
	 * @param control Control that fires the event.
	 * @param eventName Event name.
	 * @param preAction Action to be executed in the same thread of the action before the action
	 * @param postAction Action to be executed in the same thread of the action after the action
	 */
	boolean runControlEvent(View control, String eventName, Runnable preAction, Runnable postAction);

	/**
	 * Used to check if a control handle some of the eventNames
	 * @param control Control to check
	 * @param eventNames Event names to check
	 * @return
	 */
	boolean hasAnyEventHandler(View control, String[] eventNames);

	/**
	 * Used to get the action definition for the given event
	 * @param control Control to check
	 * @param eventName Event name to find
	 * @return null if the event is not found.
	 */
	ActionDefinition getControlEventHandler(View control, String eventName);

	/***
	 * Returns the View for the given control name
	 * @param name
	 * @return
	 */
	View getControl(String name);

	/***
	 * Returns the UI Site for this coordinator
	 * @return
	 */
	UIContext getUIContext();
}
