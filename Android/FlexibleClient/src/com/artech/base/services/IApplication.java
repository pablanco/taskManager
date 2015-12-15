package com.artech.base.services;

import android.content.Intent;

import com.artech.actions.UIContext;
import com.artech.base.metadata.ApplicationDefinition;
import com.artech.base.metadata.AttributeDefinition;
import com.artech.base.metadata.DomainDefinition;
import com.artech.base.metadata.GxObjectDefinition;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.IPatternMetadata;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.ProcedureDefinition;
import com.artech.base.metadata.StructureDataType;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.WorkWithDefinition;
import com.artech.base.metadata.loader.LoadResult;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.settings.WorkWithSettings;
import com.artech.base.metadata.theme.ThemeDefinition;
import com.artech.base.model.Entity;
import com.artech.base.synchronization.SynchronizationHelper.DataSyncCriteria;

public interface IApplication
{
	LoadResult initialize();
	void resetLoad();

	String getName();
	ApplicationDefinition getDefinition(); // We should remove  every put/get defined below...
	boolean isLoaded();

	// Get definitions.
	WorkWithSettings getPatternSettings();
	IPatternMetadata getPattern(String name);
	IViewDefinition getView(String name);
	WorkWithDefinition getWorkWithForBC(String bcName);
	IDataViewDefinition getDataView(String name);
	IDataSourceDefinition getDataSource(String name);
	GxObjectDefinition getGxObject(String name);
	ProcedureDefinition getProcedure(String name);
	StructureDefinition getBusinessComponent(String name);
	StructureDataType getSDT(String name);
	AttributeDefinition getAttribute(String name);
	DomainDefinition getDomain(String name);
	ThemeDefinition getTheme(String name);

	// Set definitions.
	void setPatternSettings(WorkWithSettings settings);
	void putPattern(IPatternMetadata data, MetadataLoader loader, String filename);
	void putGxObject(GxObjectDefinition gxObject);
	void putBusinessComponent(StructureDefinition bc);
	void putSDT(StructureDataType sdt);
	void putAttribute(AttributeDefinition attribute);
	void putDomain(DomainDefinition domain);
	void putTheme(ThemeDefinition theme);

	String getAppEntry();
	void setRootUri(String serverUrl);
	void setBaseUri(String string);
	void setAppEntry(String string);

	UriBuilder getUriMaker();

	// This is used as a moniker to identify an application
	void setApplicationId(String string);

	// Specify the kind of server that is attending this app, take into account that this can change dynamically
	void setServerSideType(int serverType);

	String link(String objName);
	String linkObjectUrl(String objPartialUrl);

	//Synchronizer
	String getSynchronizer();
	DataSyncCriteria getSynchronizerDataSyncCriteria();
	long getSynchronizerMinTimeBetweenSync();
	
	/*
	 * Try to handle an application Intent, traversing all registered handlers. (ie: App Indexing, App Links, etc)
	 */
	boolean handleIntent(UIContext ctx, Intent intent, Entity entity);
	/*
	 * The protocol handler for this application
	 */
	String getAppsLinksProtocol();

	boolean isLiveEditingEnabled();
}
