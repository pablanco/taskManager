package com.artech.base.metadata;

public class WWListDefinition extends DataViewDefinition
{
	private static final long serialVersionUID = 1L;

	private final WWLevelDefinition mParent;

	public WWListDefinition(WWLevelDefinition parent)
	{
		mParent = parent;
	}

	@Override
	public String getName()
	{
		// WWSDCustomer.Customer.List
		return String.format("%s.%s.%s", getPattern().getName(), mParent.getName(), "List"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public WorkWithDefinition getPattern() { return mParent.getParent(); }

	public WWLevelDefinition getParent() { return mParent; }
}
