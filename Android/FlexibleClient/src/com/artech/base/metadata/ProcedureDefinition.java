package com.artech.base.metadata;

import com.artech.base.metadata.enums.GxObjectTypes;

public class ProcedureDefinition extends GxObjectDefinition
{
	public ProcedureDefinition(String name)
	{
		super(GxObjectTypes.PROCEDURE, name);
	}
}
