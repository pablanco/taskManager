package com.artech.controls.common;

/**
 * Value item for a control that provides options, either static or dynamic
 * (such as combo box, radio group, dynamic combo).
 * @author matiash
 */
public class ValueItem
{
	public final String Value;
	public final String Description;
	// TODO: Add support for public final String Image;

	public ValueItem(String value, String description)
	{
		Value = value;
		Description = description;
	}

	@Override
	public String toString() { return Description; }
}
