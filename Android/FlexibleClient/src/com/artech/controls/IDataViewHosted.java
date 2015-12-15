package com.artech.controls;

import com.artech.fragments.IDataView;

public interface IDataViewHosted
{
	IDataView getHost();
	void setHost(IDataView host);
}
