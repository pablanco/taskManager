package com.artech.base.metadata.layout;

import com.artech.base.model.PropertiesObject;
import com.artech.base.services.Services;

public class ControlInfo extends PropertiesObject
{
	private static final long serialVersionUID = 1L;

	private String mControl;

	public String getControl() { return mControl; }
	public void setControl(String value) { mControl = value; }

	public String getTranslatedProperty(String propName)
	{
		String value = optStringProperty(propName);
		return Services.Resources.getTranslation(value);
	}
}
