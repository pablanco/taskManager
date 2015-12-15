package com.artech.base.services;

import java.io.IOException;
import java.util.List;

import com.artech.base.serialization.INodeObject;

public interface IHttpService
{
	
	boolean isOnline();
	boolean isReachable(String url);
	int connectionType();

	long getRemoteMetadataVersion();

	int getRemoteMinorVersion(String app);
	int getRemoteMajorVersion(String app);

	String UriEncode(String key);
	String UriDecode(String key);

	ServiceResponse saveEntityData(String type, List<String> key, INodeObject node);
	ServiceResponse insertEntityData(String type, List<String> key, INodeObject node);
	ServiceResponse deleteEntityData(String type, List<String> key);

	String getRemoteUrlVersion(String appEntry);

	String getNetworkErrorMessage(IOException e);
}
