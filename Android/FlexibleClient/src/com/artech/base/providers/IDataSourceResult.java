package com.artech.base.providers;

import java.util.Date;
import java.util.List;

import com.artech.base.model.Entity;

public interface IDataSourceResult
{
	boolean isOk();
	boolean isUpToDate();

	Date getLastModified();
	int getErrorType();
	String getErrorMessage();

	List<Entity> getData();
}
