package com.artech.externalapi;

public class ExternalApiDefinition
{
	public ExternalApiDefinition(String name, String className)
	{
		Name = name;
		ClassName = className;
	}

	public ExternalApiDefinition(String name, Class<? extends ExternalApi> clazz)
	{
		this(name, clazz.getName());
	}

	public final String Name;
	public final String ClassName;
}
