package com.artech.base.serialization;

public interface INodeCollection extends Iterable<INodeObject>
{
	int length();
	INodeObject getNode(int index);
	void put(INodeObject value);
}
