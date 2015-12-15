package com.artech.ui.navigation;

import android.app.Activity;
import android.content.Intent;

import com.artech.adapters.AdaptersHelper;
import com.artech.android.animations.Transition;
import com.artech.android.animations.Transitions;
import com.artech.base.metadata.DimensionValue;
import com.artech.base.metadata.DimensionValue.ValueType;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.metadata.layout.Size;
import com.artech.base.metadata.layout.TableDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.MathUtils;
import com.artech.base.utils.NameMap;

import static com.artech.ui.navigation.CallOptions.OPTION_ENTER_EFFECT;
import static com.artech.ui.navigation.CallOptions.OPTION_EXIT_EFFECT;
import static com.artech.ui.navigation.CallOptions.OPTION_TARGET;
import static com.artech.ui.navigation.CallOptions.OPTION_TARGET_HEIGHT;
import static com.artech.ui.navigation.CallOptions.OPTION_TARGET_SIZE;
import static com.artech.ui.navigation.CallOptions.OPTION_TARGET_WIDTH;
import static com.artech.ui.navigation.CallOptions.OPTION_TYPE;

public class CallOptionsHelper
{
	private static final String INTENT_EXTRA_CALL_OPTIONS = "com.artech.ui.navigation.CallOptionsHelper.CallOptions"; //$NON-NLS-1$

	// Options set via assignments to <Panel>.CallOptions.<option>.
	private static NameMap<CallOptions> sConfiguredOptions = new NameMap<CallOptions>();

	/**
	 * Gets the currently configured CallOptions for an object. Never returns null.
	 */
	public static CallOptions getCallOptions(IViewDefinition view, short mode)
	{
		CallOptions callOptions = new CallOptions();

		// Read standard call options from object's theme class.
		if (view instanceof IDataViewDefinition)
		{
			IDataViewDefinition dataView = (IDataViewDefinition)view;

			LayoutDefinition layout = dataView.getLayoutForMode(mode);
			if (layout != null && layout.getThemeClass() != null)
				callOptions.setFromClass(layout.getThemeClass());
		}

		// Set custom call options configured via previous <Panel>.CallOptions.X assignments.
		if (view != null)
		{
			CallOptions configuredCallOptions = sConfiguredOptions.get(view.getObjectName());
			if (configuredCallOptions != null)
				callOptions.setOverrides(configuredCallOptions);
		}

		return callOptions;
	}

	/**
	 * Assigns a CallOptions property for the specified target object. Will be used when said object is called.
	 */
	public static void setCallOption(String targetObject, String optionName, String optionValue)
	{
		// Reuse or initialize a CallOptions instance for this target object.
		CallOptions options = sConfiguredOptions.get(targetObject);
		if (options == null)
		{
			options = new CallOptions();
			sConfiguredOptions.put(targetObject, options);
		}

		if (OPTION_TYPE.equalsIgnoreCase(optionName))
		{
			CallType callType = CallType.tryParse(optionValue);
			if (callType != null)
				options.setCallType(callType);
		}
		else if (OPTION_ENTER_EFFECT.equalsIgnoreCase(optionName))
		{
			Transition enterEffect = Transitions.get(optionValue);
			if (enterEffect != null)
				options.setEnterEffect(enterEffect);
		}
		else if (OPTION_EXIT_EFFECT.equalsIgnoreCase(optionName))
		{
			Transition exitEffect = Transitions.get(optionValue);
			if (exitEffect != null)
				options.setExitEffect(exitEffect);
		}
		else if (OPTION_TARGET.equalsIgnoreCase(optionName))
		{
			options.setTargetName(optionValue);
		}
		else if (OPTION_TARGET_SIZE.equalsIgnoreCase(optionName))
		{
			options.setTargetSize(optionValue);
		}
		else if (OPTION_TARGET_HEIGHT.equalsIgnoreCase(optionName))
		{
			DimensionValue value = DimensionValue.parse(optionValue);
			if (value != null)
				options.setTargetHeight(value);
		}
		else if (OPTION_TARGET_WIDTH.equalsIgnoreCase(optionName))
		{
			DimensionValue value = DimensionValue.parse(optionValue);
			if (value != null)
				options.setTargetWidth(value);
		}
		else
			Services.Log.warning(String.format("Unknown CallOptions parameter: '%s'.", optionName));
	}

	/**
	 * Clears all assignments to targetObject.CallOptions. Should be used immediately after calling it.
	 */
	public static void resetCallOptions(String targetObject)
	{
		if (targetObject != null)
			sConfiguredOptions.remove(targetObject);
	}

	/**
	 * Sets the call options for a particular intent. The invoked Activity can then use
	 * getCurrentCallOptions() to retrieve them from the intent.
	 */
	public static void setCurrentCallOptions(Intent intent, CallOptions options)
	{
		intent.putExtra(INTENT_EXTRA_CALL_OPTIONS, options);
	}

	/**
	 * Gets the call options that were set in the intent (as parameters to the Activity).
	 * May return null, but probably only for "external" intents.
	 */
	public static CallOptions getCurrentCallOptions(Intent intent)
	{
		if (intent != null)
			return (CallOptions)intent.getSerializableExtra(INTENT_EXTRA_CALL_OPTIONS);
		else
			return null;
	}

	public static Size getTargetSize(UIObjectCall call, CallOptions options)
	{
		if (options.getCallType() == CallType.POPUP || options.getCallType() == CallType.CALLOUT)
		{
			Activity activity = call.getContext().getActivity();
			int displayWidth = AdaptersHelper.getDisplayWidth(activity);
			int displayHeight = AdaptersHelper.getDisplayHeight(activity, call.getObjectLayout());

			// Read the layout that will be shown. If a size is not specified in the CallOptions,
			// and the layout has fixed width/height, use it.
			TableDefinition popupTable = null;
			if (call.getObjectLayout() instanceof LayoutDefinition)
				popupTable = ((LayoutDefinition)call.getObjectLayout()).getTable();

			int width = MathUtils.round(getTargetWidth(options, popupTable, displayWidth));
			int height = MathUtils.round(getTargetHeight(options, popupTable, displayHeight));

			return new Size(width, height);
		}
		else
			return null;
	}

	private static float getTargetWidth(CallOptions options, TableDefinition popupTable, int displayWidth)
	{
		if (options.getTargetWidth() != null)
		{
			// Use specified width.
			return DimensionValue.toPixels(options.getTargetWidth(), displayWidth);
		}
		else
		{
			// No width specified. Try to get width from layout (if not in percentage), otherwise use a default value.
			if (popupTable != null && popupTable.getWidth().Type == ValueType.PIXELS)
				return popupTable.getWidth().Value;
			else
				return DimensionValue.toPixels(CallOptions.getDefaultTargetWidth(), displayWidth);
		}
	}

	private static float getTargetHeight(CallOptions options, TableDefinition popupTable, int displayHeight)
	{
		if (options.getTargetHeight() != null)
		{
			// Use specified height.
			return DimensionValue.toPixels(options.getTargetHeight(), displayHeight);
		}
		else
		{
			// No height specified. Try to get height from layout (if not in percentage), otherwise use a default value.
			if (popupTable != null && popupTable.getHeight().Type == ValueType.PIXELS)
				return popupTable.getHeight().Value;
			else
				return DimensionValue.toPixels(CallOptions.getDefaultTargetHeight(), displayHeight);
		}
	}
}
