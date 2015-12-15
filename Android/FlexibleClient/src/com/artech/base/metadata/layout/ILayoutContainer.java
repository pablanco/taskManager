package com.artech.base.metadata.layout;

public interface ILayoutContainer
{
	TableDefinition getContent();
	void calculateBounds(float absoulteWidth, float absoluteHeight);
}
