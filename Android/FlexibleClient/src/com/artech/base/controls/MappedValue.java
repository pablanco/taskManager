package com.artech.base.controls;

/**
 * Mapped value, used by the input type descriptions translation methods.
 * @author matiash
 *
 */
public class MappedValue
{
	public enum Type { EXACT, AMBIGUOUS, NOT_FOUND }

	public final Type type;
	public final String value;

	public MappedValue(Type aType, String aValue)
	{
		type = aType;
		value = aValue;
	}

	public static MappedValue exact(String aValue)
	{
		return new MappedValue(Type.EXACT, aValue);
	}
}
