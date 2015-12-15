package com.artech.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.view.View;

import com.artech.actions.ActionDefinitionWithHandlers;
import com.artech.actions.UIContext;
import com.artech.adapters.AdaptersHelper;
import com.artech.android.layout.ControlViewHelper;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.DataItemHelper;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.NameMap;
import com.artech.base.utils.Strings;
import com.artech.common.PromptHelper;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.controls.DataBoundControl;
import com.artech.controls.GxDragListener;
import com.artech.controls.GxGestureListener;
import com.artech.controls.GxTouchEvents;
import com.artech.controls.IGxEdit;
import com.artech.controls.IGxEditWithDependencies;
import com.artech.utils.Cast;

public abstract class CoordinatorBase implements CoordinatorAdvanced
{
	private final UIContext mContext;
	private final ArrayList<View> mControls;
	private final NameMap<View> mControlsByName = new NameMap<View>();
	private final TreeMap<String, List<IGxEditWithDependencies>> mCachedDependencies;

	private Entity mData;
	private final Entity.OnPropertyValueChangeListener mPropertyValueChangeListener;
	private final View.OnFocusChangeListener mViewFocusChangeListener;
	private GxDragListener mDragDropListener = null;
	private final GxGestureListener mGestureListener;

	protected CoordinatorBase(UIContext context)
	{
		mContext = context;
		mControls = new ArrayList<View>();

		mCachedDependencies = new TreeMap<String, List<IGxEditWithDependencies>>(String.CASE_INSENSITIVE_ORDER);
		mPropertyValueChangeListener = new EntityPropertyValueChangeListener();
		mViewFocusChangeListener = new ViewFocusChangeListener();
		if (CompatibilityHelper.isHoneycomb())
			mDragDropListener = new GxDragListener(this);

		mGestureListener = new GxGestureListener(mContext, this);
 	}

	protected abstract IViewDefinition getContainerDefinition();

	public Entity getData() { return mData; }

	public void setData(Entity data)
	{
		if (mData != data)
		{
			if (mData != null)
				mData.setOnPropertyValueChangeListener(null);

			mData = data;

			if (mData != null)
				mData.setOnPropertyValueChangeListener(mPropertyValueChangeListener);
		}
	}


	@Override
	public UIContext getUIContext()
	{
		return mContext;
	}

	public List<View> getControlViews() {
		return Collections.unmodifiableList(mControls);
	}

	@SuppressLint("NewApi")
	@Override
	public void addControl(View control, LayoutItemDefinition definition)
	{
		mControls.add(control);
		mControlsByName.put(definition.getName(), control);
		mGestureListener.addView(control);

		View focusView = control;
		if (control instanceof DataBoundControl)
		{
			View editView = Cast.as(View.class, ((DataBoundControl)control).getEdit());
			if (editView != null)
				focusView = editView;
		}

		if (CompatibilityHelper.isHoneycomb() && hasAnyEventHandler(control, new String[] { GxTouchEvents.DROP }))
			control.setOnDragListener(mDragDropListener);

		focusView.setOnFocusChangeListener(mViewFocusChangeListener);

		// Clear calculated dependencies if new controls are registered.
		mCachedDependencies.clear();
	}

	@Override
	public final Object getValue(String name)
	{
		if (mData == null)
		{
			Services.Log.warning(String.format("Asking for '%s' before Coordinator's data is set.", name));
			return null;
		}

		return mData.getProperty(name);
	}

	@Override
	public void setValue(String name, Object value)
	{
		if (mData == null)
		{
			Services.Log.warning(String.format("Trying to set '%s' and no Coordinator's data is set.", name));
			return;
		}

		mData.setProperty(name, value);
	}

	@Override
	public View getControl(String name)
	{
		return mControlsByName.get(name);
	}

	@Override
	public final String getStringValue(String name)
	{
		Object value = getValue(name);
		return (value != null ? value.toString() : Strings.EMPTY);
	}

	@Override
	public final void onValueChanged(IGxEdit edit, boolean fireControlValueChanged)
	{
		// Post the supplied value.
		// This will probably fire onDataValueChanged() below.
		if (mData != null)
			AdaptersHelper.saveEditValue(edit, mData);

		if (fireControlValueChanged) {
			runControlEvent((View) edit, "ControlValueChanged");
		}
	}

	private class ViewFocusChangeListener implements View.OnFocusChangeListener
	{
		@Override
		public void onFocusChange(View v, boolean hasFocus)
		{
			if (v instanceof IGxEdit && !hasFocus)
			{
				// Leaving an edit control, post its (possibly changed) value.
				// This will probably fire onDataValueChanged() below.
				//onValueChanged((IGxEdit)v, true);
			}
		}
	}

	private class EntityPropertyValueChangeListener implements Entity.OnPropertyValueChangeListener
	{
		@Override
		public void onPropertyValueChange(String propertyName, Object oldValue, Object newValue)
		{
			onDataValueChanged(propertyName, newValue);
		}
	}

	private void onDataValueChanged(String name, Object value)
	{
		// A data value has changed.
		// This may have been fired by exiting a control, by the control itself, or by modifying a value via code.
		// If this property has dependant controls, update the value in the Entity and notify them.
		List<IGxEditWithDependencies> dependantControls = getDependantControls(name);
		for (IGxEditWithDependencies dependant : dependantControls)
			dependant.onDependencyValueChanged(name, value);
	}

	private List<IGxEditWithDependencies> getDependantControls(String propertyName)
	{
		// Ignore attribute/variable names mismatch.
		propertyName = DataItemHelper.getNormalizedName(propertyName);

		List<IGxEditWithDependencies> dependants = mCachedDependencies.get(propertyName);
		if (dependants == null)
		{
			// Search for edits which can have dependencies and match them against the supplied control.
			dependants = new ArrayList<IGxEditWithDependencies>();
			for (IGxEditWithDependencies otherEdit : Cast.iterateAs(IGxEditWithDependencies.class, mControls))
			{
				if (isDependant(otherEdit, propertyName))
					dependants.add(otherEdit);
			}

			// Store calculated dependencies in cache for later reuse.
			mCachedDependencies.put(propertyName, dependants);
		}

		return dependants;
	}

	private static boolean isDependant(IGxEditWithDependencies edit, String onProperty)
	{
		if (edit.getDependencies() != null)
		{
			for (String dependency : edit.getDependencies())
			{
				// Ignore attribute/variable names mismatch.
				dependency = DataItemHelper.getNormalizedName(dependency);

				if (dependency.equalsIgnoreCase(onProperty))
					return true;
			}
		}

		return false;
	}

	@Override
	public final boolean hasAnyEventHandler(View control, String[] eventNames)
	{
		for (String eventName : eventNames)
		{
			if (getControlEventHandler(control, eventName) != null)
				return true;
		}

		// Some controls have implicit Tap events (such as those that act as prompt controls).
		//noinspection RedundantIfStatement
		if (Strings.arrayContains(eventNames, GxTouchEvents.TAP, true) && hasImplicitTapHandler(control))
			return true;

		return false;
	}

	@Override
	public ActionDefinition getControlEventHandler(View control, String eventName)
	{
		LayoutItemDefinition controlDefinition = ControlViewHelper.getDefinition(control);
		if (controlDefinition != null)
			return controlDefinition.getEventHandler(eventName);
		else
			return null;
	}

	@Override
	public final boolean runAction(String action, Anchor anchor)
	{
		IDataViewDefinition containerDefinition = Cast.as(IDataViewDefinition.class, getContainerDefinition());
		if (containerDefinition != null)
		{
			ActionDefinition actionDefinition = containerDefinition.getEvent(action);
			if (actionDefinition != null)
				return runAction(actionDefinition, anchor);
		}

		return false;
	}

	@Override
	public final boolean runControlEvent(View control, String eventName, Runnable preAction, Runnable postAction)
	{
		if (GxTouchEvents.TAP.equals(eventName) && runImplicitTapHandler(control))
			return true;

		ActionDefinition action = getControlEventHandler(control, eventName);
		if (preAction != null || postAction != null)
			action = new ActionDefinitionWithHandlers(action, preAction, postAction);

		if (action != null)
			return runAction(action, new Anchor(control));

		return false;
	}

	@Override
	public final boolean runControlEvent(View control, String eventName)
	{
		if (GxTouchEvents.TAP.equals(eventName) && runImplicitTapHandler(control))
			return true;

		ActionDefinition action = getControlEventHandler(control, eventName);
		if (action != null)
			return runAction(action, new Anchor(control));

		return false;
	}

	private boolean hasImplicitTapHandler(View control)
	{
		// Only prompts, for now.
		return PromptHelper.hasPrompt(control);
	}

	private boolean runImplicitTapHandler(View control)
	{
		// Only prompts, for now.
		return PromptHelper.callPrompt(this, control);
	}
}
