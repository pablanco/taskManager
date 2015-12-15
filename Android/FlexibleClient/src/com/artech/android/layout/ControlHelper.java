package com.artech.android.layout;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.ResultRunnable;
import com.artech.base.utils.Strings;
import com.artech.common.ExecutionContext;
import com.artech.controls.IGxControl;

public class ControlHelper
{
	// Known properties than can be set in events.
	public static final String PROPERTY_CLASS = "Class"; //$NON-NLS-1$
	public static final String PROPERTY_ENABLED = "Enabled"; //$NON-NLS-1$
	public static final String PROPERTY_FOCUSED = "SetFocus"; //$NON-NLS-1$
	public static final String PROPERTY_VISIBLE = "Visible"; //$NON-NLS-1$
	public static final String PROPERTY_CAPTION = "Caption"; //$NON-NLS-1$

	private static final Set<String> KNOWN_PROPERTIES = Strings.newSet(PROPERTY_CLASS, PROPERTY_ENABLED, PROPERTY_FOCUSED, PROPERTY_VISIBLE, PROPERTY_CAPTION);

	public static boolean isKnownProperty(String name)
	{
		return KNOWN_PROPERTIES.contains(name);
	}

	public static void setProperties(ExecutionContext context, IGxControl control, Map<String, Object> properties)
	{
		for (Map.Entry<String, Object> property : properties.entrySet())
			setProperty(context, control, property.getKey(), property.getValue());
	}

	public static boolean setProperty(final ExecutionContext context, final IGxControl control, final String propertyName, final Object propertyValue)
	{
		if (control == null || propertyName == null || propertyValue == null)
			return false;

		// Post if from a background thread, run directly otherwise.
		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				control.setExecutionContext(context);
				internalSetProperty(control, propertyName, propertyValue);
			}
		});

		return true;
	}

	public static Object getProperty(final ExecutionContext context, final IGxControl control, final String propertyName)
	{
		if (control == null || propertyName == null)
			return null;

		// Always invoke and wait, since we need the result.
		return Services.Device.invokeOnUiThread(new ResultRunnable<Object>()
		{
			@Override
			public Object run()
			{
				control.setExecutionContext(context);
				return internalGetProperty(control, propertyName);
			}
		});
	}

	public static boolean runMethod(final ExecutionContext context, final IGxControl control, final String methodName, final List<Object> parameters)
	{
		if (control == null || methodName == null)
			return false;

		// Post if from a background thread, run directly otherwise.
		Services.Device.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				control.setExecutionContext(context);
				internalRunMethod(control, methodName, parameters);
			}
		});

		return true;
	}

	private static void internalSetProperty(IGxControl control, String propertyName, Object propertyValue)
	{
		if (propertyName.equalsIgnoreCase(PROPERTY_CLASS))
		{
			ThemeClassDefinition themeClass = PlatformHelper.getThemeClass(propertyValue.toString());
			if (themeClass != null)
				control.setThemeClass(themeClass);
		}
		else if (propertyName.equalsIgnoreCase(PROPERTY_VISIBLE))
		{
			control.setVisible(Services.Strings.parseBoolean(propertyValue.toString()));
			control.requestLayout();
		}
		else if (propertyName.equalsIgnoreCase(PROPERTY_ENABLED))
		{
			control.setEnabled(Services.Strings.parseBoolean(propertyValue.toString()));
		}
		else if (propertyName.equalsIgnoreCase(PROPERTY_FOCUSED))
		{
			// Focus & show keyboard.
			control.setFocus(true);
		}
		else if (propertyName.equalsIgnoreCase(PROPERTY_CAPTION))
		{
			control.setCaption(propertyValue.toString());
		}
		else
		{
			// Possibly a custom property.
			control.setProperty(propertyName, propertyValue);
		}
	}

	private static Object internalGetProperty(IGxControl control, String propertyName)
	{
		if (propertyName.equalsIgnoreCase(PROPERTY_CLASS))
		{
			return control.getThemeClass();
		}
		else if (propertyName.equalsIgnoreCase(PROPERTY_VISIBLE))
		{
			return control.isVisible();
		}
		else if (propertyName.equalsIgnoreCase(PROPERTY_ENABLED))
		{
			return control.isEnabled();
		}
		else if (propertyName.equalsIgnoreCase(PROPERTY_CAPTION))
		{
			return control.getCaption();
		}
		else
		{
			// Possibly a custom property.
			return control.getProperty(propertyName);
		}
	}

	private static void internalRunMethod(IGxControl control, String methodName, List<Object> parameters)
	{
		// Handle "SetFocus" as a special case (it is a dual property/method).
		if (PROPERTY_FOCUSED.equalsIgnoreCase(methodName))
		{
			control.setFocus(true);
		}
		else
		{
			// Probably a custom method.
			control.runMethod(methodName, parameters);
		}
	}
}
