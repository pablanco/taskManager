package com.artech.app;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.utils.NameMap;

public class ComponentParameters
{
	public final ComponentType Type;
	public final IViewDefinition Object;
	public final short Mode;
	public final List<String> Parameters;
	public final NameMap<String> NamedParameters;
	public final String Url;

	public ComponentParameters(IViewDefinition object)
	{
		this(object, DisplayModes.VIEW, null);
	}
	
	public ComponentParameters(IViewDefinition object, short mode, List<String> parameters)
	{
		this(object, mode, parameters, null);
	}

	public ComponentParameters(IViewDefinition object, short mode, List<String> parameters, Map<String, String> namedParameters)
	{
		Type = ComponentType.Form;
		Object = object;
		Mode = mode;
		Parameters = (parameters != null ? parameters : Collections.<String>emptyList());
		NamedParameters = new NameMap<String>(namedParameters);
		Url = null;
	}
	
	public ComponentParameters(String url)
	{
		Type = ComponentType.Web;
		Object = null;
		Mode = DisplayModes.VIEW;
		Parameters = Collections.emptyList();
		NamedParameters = new NameMap<String>();
		Url = url;
	}
}
