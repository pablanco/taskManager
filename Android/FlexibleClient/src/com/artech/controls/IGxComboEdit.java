package com.artech.controls;

import java.util.List;

import com.artech.base.metadata.EnumValuesDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;

public interface IGxComboEdit extends IGxEdit
{
	LayoutItemDefinition getDefinition();
	
	// TODO: Should be "<? extends SomethingMoreGeneric>", to allow other option lists.
	void setComboValues(List<? extends EnumValuesDefinition> values);
}
