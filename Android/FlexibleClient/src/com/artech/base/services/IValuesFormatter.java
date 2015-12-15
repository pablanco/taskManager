package com.artech.base.services;

public interface IValuesFormatter
{
	boolean needsAsync();
	CharSequence format(String value);
}
