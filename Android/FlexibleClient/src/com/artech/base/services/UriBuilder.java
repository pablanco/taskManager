package com.artech.base.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.artech.application.MyApplication;
import com.artech.base.utils.Strings;

public class UriBuilder
{
	private String mBaseUri; // Base URI for REST services.
	private String mRootUri; // Root URI of the website.

	private final static String mMetadataZipFormat = "gxmetadata/%s.android.gxsd"; //$NON-NLS-1$
	private final static String mGetMetadataVersion = "gxmetadata/gxversion"; //$NON-NLS-1$
	private final static String mExtraParameters = "fmt=json"; //$NON-NLS-1$

	public final static int RUBY_SERVER = 22;
	public final static int NET_SERVER = 15;
	public final static int JAVA_SERVER = 12;

	public String MakeGetAllUriBC(String entity)
	{
		StringBuilder sb = new StringBuilder(mBaseUri);
		sb.append('/');
		sb.append(entity.replace('.', '/'));
		if (mExtraParameters != null)
		{
			sb.append('?');
			sb.append(mExtraParameters);
		}

		return sb.toString();
	}

	private static String getEncodedParameterList(List<String> keys)
	{
		List<String> encodedKeys = new ArrayList<String>();

		for (String key : keys)
			encodedKeys.add(Services.HttpService.UriEncode(key));

		return Services.Strings.join(encodedKeys, Strings.COMMA);
	}

	public String MakeGetOneUriBC(String entity, List<String> keys)
	{
		StringBuilder sb = new StringBuilder(mBaseUri);
		sb.append('/');
		sb.append(entity.replace('.', '/'));
		sb.append('/');
		sb.append(getEncodedParameterList(keys));

		if (mExtraParameters != null){
			sb.append('?');
			sb.append(mExtraParameters);
		}
		return sb.toString();
	}

	public String link(String object, int serverType, boolean addExtension) {
		StringBuilder sb = new StringBuilder(mRootUri);
		if (serverType == RUBY_SERVER) {
			sb.append("/"); //$NON-NLS-1$
			sb.append(Strings.toLowerCase(object));
			if (addExtension)
				sb.append(".html"); //$NON-NLS-1$
		} else if (serverType == NET_SERVER) {
			sb.append("/"); //$NON-NLS-1$
			sb.append(object);
			if (addExtension)
				sb.append(".aspx"); //$NON-NLS-1$
		} else if (serverType == JAVA_SERVER) {
			sb.append("/servlet/"); //$NON-NLS-1$
			sb.append(Strings.toLowerCase(object));
		}
		return sb.toString();
	}

	public void setBaseUri(String baseUri) {
		mBaseUri = baseUri;
	}
	public String getBaseUri() {
		return mBaseUri;
	}

	public void setRootUri(String rootUri)
	{
		mRootUri = rootUri;
	}

	public String getRootUri() {
		return mRootUri;
	}

	public String getBaseImagesUri()
	{
		//Images get by http if invalid https certificate
		if (mRootUri!=null && MyApplication.getApp()!=null && MyApplication.getApp().getAllowNotTrustedCertificate())
			return mRootUri.replace("https:", "http:"); //$NON-NLS-1$ //$NON-NLS-2$
		return mRootUri;
	}

	public String MakeImagePath(String imageResource) {
		if (imageResource.startsWith("http://") || imageResource.startsWith("https://")) //$NON-NLS-1$ //$NON-NLS-2$
			return imageResource;
		StringBuilder sb = new StringBuilder(getBaseImagesUri());
		sb.append('/');
		String lastSegment = imageResource.replace('\\', '/');
		int pos = lastSegment.lastIndexOf('/') + 1;
		if (pos>1)
			lastSegment = lastSegment.substring(0, pos) + Services.HttpService.UriEncode(lastSegment.substring(pos) );
		else
			lastSegment = Services.HttpService.UriEncode(lastSegment );
		sb.append( lastSegment);
		return sb.toString();
	}

	public String MakeMetadataVersion()
	{
		StringBuilder sb = new StringBuilder(mRootUri);
		sb.append('/');
		sb.append(mGetMetadataVersion);
		sb.append(".json"); //$NON-NLS-1$
		return sb.toString();
	}

	public String getLoginUrl()
	{
		StringBuilder sb = new StringBuilder(mRootUri);
		sb.append("/oauth/access_token"); //$NON-NLS-1$
		return sb.toString();
	}

	public String getUserInformationUrl()
	{
		StringBuilder sb = new StringBuilder(mRootUri);
		sb.append("/oauth/userinfo"); //$NON-NLS-1$
		return sb.toString();
	}

	public String getLogoutUrl()
	{
		StringBuilder sb = new StringBuilder(mRootUri);
		sb.append("/oauth/logout"); //$NON-NLS-1$
		return sb.toString();
	}

	public String GetTokenParameters(String clientId, String secret, String type, String username, String password, String refreshToken)
	{
		StringBuilder sb = new StringBuilder();
 		sb.append("client_id="); //$NON-NLS-1$
		sb.append(clientId);
		sb.append("&client_secret="); //$NON-NLS-1$
		sb.append(secret);
		sb.append("&grant_type="); //$NON-NLS-1$
		sb.append(type);

		if (Services.Strings.hasValue(refreshToken))
		{
			sb.append("&refresh_token="); //$NON-NLS-1$
			sb.append(Services.HttpService.UriEncode(refreshToken));
		}
		else if (Services.Strings.hasValue(username))
		{
			sb.append("&username="); //$NON-NLS-1$
			sb.append(Services.HttpService.UriEncode(username));
			sb.append("&password="); //$NON-NLS-1$
			sb.append(Services.HttpService.UriEncode(password));
		}

		sb.append("&scope=FullControl"); //$NON-NLS-1$
		return sb.toString();
	}

	public String MakeImagesServer()
	{
		return mBaseUri.replace("/rest", "/gxobject"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public String getObjectUri(String name, Map<String, ?> parameters)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(mBaseUri);
		sb.append("/");
		sb.append(name);

		if (parameters != null && parameters.size() != 0)
		{
			sb.append(Strings.QUESTION);
			boolean isNotFirst = false;
			for (Map.Entry<String, ?> parameter : parameters.entrySet())
			{
				if (isNotFirst)
					sb.append(Strings.AND);

				String key = parameter.getKey();
				if (key.startsWith("&"))
					key = key.substring(1);

				sb.append(key);
				sb.append(Strings.EQUAL);
				sb.append(Services.HttpService.UriEncode(parameter.getValue().toString()));
				isNotFirst = true;
			}
		}

		return sb.toString();
	}

	public String MakeGetDefaultUriBC(String entity)
	{
		StringBuilder sb = new StringBuilder(mBaseUri);
		sb.append('/');
		sb.append(entity.replace('.', '/'));
		sb.append("/_default"); //$NON-NLS-1$
		return sb.toString();
	}

	public String MakeApplicationVersion(String app)
	{
		StringBuilder sb = new StringBuilder(mRootUri);
		sb.append('/');
		sb.append("gxmetadata");
		sb.append("/").append(Strings.toLowerCase(app)).append(".android.json");
		return sb.toString();
	}

	public String makeAplicationMetadata(String app)
	{
		StringBuilder sb = new StringBuilder(mRootUri);
		sb.append('/');
		sb.append(String.format(mMetadataZipFormat, Strings.toLowerCase(app)));
		return sb.toString();
	}

	public String makeMultiCallUri(String procedure)
	{
		StringBuilder sb = new StringBuilder(mRootUri);
		sb.append("/gxmulticall?");
		sb.append(procedure.replace('.', '/'));

		return sb.toString();
	}
}
