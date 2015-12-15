package com.artech.base.metadata.layout;


public interface ILayoutItemVisitable {
	void accept(ILayoutVisitor visitor);
}
