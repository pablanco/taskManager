package com.artech.activities;

import com.artech.base.metadata.IDataViewDefinition;
import com.artech.fragments.IDataView;

public interface IGxActivity extends IGxBaseActivity
{
	ActivityModel getModel();
	ActivityController getController();
	Iterable<IDataView> getActiveDataViews();

	IDataViewDefinition getMainDefinition();
	void refreshData(boolean keepPosition);

	void setReturnResult();
}
