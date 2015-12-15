package com.artech.android.layout;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.artech.actions.UIContext;
import com.artech.base.utils.DoubleMap;
import com.artech.base.utils.Triplet;
import com.artech.common.ExecutionContext;
import com.artech.controls.IGxControl;

public class ControlProperties
{
	private final DoubleMap<String, String, Object> mProperties = DoubleMap.newStringMap();
	private final List<Triplet<String, String, List<Object>>> mMethods = new LinkedList<Triplet<String, String, List<Object>>>();

	public void apply(UIContext context)
	{
		applyProperties(context);
		runMethods(context);
	}

	private void applyProperties(UIContext context)
	{
		for (Map.Entry<String, Map<String, Object>> controlProperties : getProperties())
		{
			IGxControl control = context.findControl(controlProperties.getKey());
			if (control != null)
			{
				// Set all specified properties for this control.
				ControlHelper.setProperties(ExecutionContext.base(context), control, controlProperties.getValue());
			}
		}
	}

	private void runMethods(UIContext context)
	{
		for (Triplet<String, String, List<Object>> controlMethod : mMethods)
		{
			IGxControl control = context.findControl(controlMethod.first);
			if (control != null)
				ControlHelper.runMethod(ExecutionContext.base(context), control, controlMethod.second, controlMethod.third);
		}
	}

	private Object getProperty(String control, String propertyName)
	{
		return mProperties.get(control, propertyName);
	}

	public String getStringProperty(String control, String propertyName)
	{
		Object value = getProperty(control, propertyName);
		return (value != null ? value.toString() : null);
	}

	Iterable<Map.Entry<String, Map<String, Object>>> getProperties()
	{
		return mProperties.entrySet();
	}

	public void putProperty(String controlName, String propName, Object propValue)
	{
		mProperties.put(controlName, propName, propValue);
	}

	void putMethod(String controlName, String methodName, List<Object> methodParameters)
	{
		mMethods.add(new Triplet<String, String, List<Object>>(controlName, methodName, methodParameters));
	}

	public void putAll(ControlProperties other)
	{
		mProperties.putAll(other.mProperties);
		mMethods.addAll(other.mMethods);
	}
}
