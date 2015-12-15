package com.artech.base.metadata;

import java.io.Serializable;

import com.artech.base.services.Services;


public class EnumValuesDefinition implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String m_Value;
	private String m_Description;
	private String m_Name;

	public void setValue(String myValue) {
		this.m_Value = myValue;
	}
	public String getValue() {
		return m_Value;
	}
	public void setDescription(String myDescription) {
		this.m_Description = myDescription;
	}
	public String getDescription() {
		return Services.Resources.getTranslation(m_Description);
	}
	public void setName(String string) {
		m_Name = string;
	}
	public String getName() {
		return m_Name;
	}

	@Override
	public String toString(){
		return getName();
	}
}
