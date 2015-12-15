package com.artech.controls.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;

public abstract class ControlServiceDefinition
{
	public final LayoutItemDefinition LayoutItem;
	public final String Service;
	public final List<String> ServiceInput;

	protected ControlServiceDefinition(LayoutItemDefinition itemDefinition, String serviceSuffix)
	{
		LayoutItem = itemDefinition;
		ControlInfo info = itemDefinition.getControlInfo();
		if (info != null)
		{
			Service = info.optStringProperty("@service" + serviceSuffix); //$NON-NLS-1$

			String serviceInputParameters = info.optStringProperty("@service" + serviceSuffix + "Inputs"); //$NON-NLS-1$ //$NON-NLS-2$
			if (Services.Strings.hasValue(serviceInputParameters))
				ServiceInput = Arrays.asList(Services.Strings.split(serviceInputParameters, ",")); //$NON-NLS-1$
			else
				ServiceInput = new ArrayList<String>();
		}
		else
		{
			Service = null;
			ServiceInput = new ArrayList<String>();
		}
	}
}
