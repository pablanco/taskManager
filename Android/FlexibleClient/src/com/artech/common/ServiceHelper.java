package com.artech.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.Pair;
import android.webkit.MimeTypeMap;

import com.artech.R;
import com.artech.android.DebugService;
import com.artech.android.api.ClientInformation;
import com.artech.android.json.NodeObject;
import com.artech.android.media.ExifHelper;
import com.artech.android.media.utils.FileUtils;
import com.artech.application.MyApplication;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.settings.UploadSizeDefinition;
import com.artech.base.model.PropertiesObject;
import com.artech.base.providers.IApplicationServer;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.IContext;
import com.artech.base.services.IHttpService;
import com.artech.base.services.ServiceResponse;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.Strings;
import com.byarger.exchangeit.EasySSLSocketFactory;

public class ServiceHelper implements IHttpService
{
	private static String Token = Strings.EMPTY;

	private static final String CONTENT_TYPE_JSON = "application/json";

	private static final int DEFAULT_CONNECTION_TIMEOUT = 5000; // 5 seconds
	private static final int DEFAULT_SOCKET_TIMEOUT = 60000; // 60 seconds.

	public static class Headers {
		// Genexus headers
		public static final String GENEXUS_LANGUAGE = "GeneXus-Language";
		public static final String GENEXUS_AGENT = "GeneXus-Agent";
		public static final String GENEXUS_THEME = "GeneXus-Theme";
		public static final String GENEXUS_TIMEZONE = "GxTZOffset";
		public static final String GENEXUS_SYNC_VERSION = "GXSynchronizerVersion";

		// Device headers
		public static final String DEVICE_OS_NAME = "DeviceOSName";
		public static final String DEVICE_OS_VERSION = "DeviceOSVersion";
		public static final String DEVICE_ID = "DeviceId";
		public static final String DEVICE_NAME = "DeviceName";
		public static final String DEVICE_PLATFORM = "DevicePlatform";
		public static final String DEVICE_NETWORK_ID = "DeviceNetworkId";

		// Standard headers
		public static final String ACCEPT_LANGUAGE = "Accept-Language";
		public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
		public static final String AUTHORIZATION = "Authorization";

		// need to use from STD classes.
		public static Hashtable<String, String> getSecurityHeaders() {
			Hashtable<String, String> headers = new Hashtable<>();
			if (Token != null && Token.length() != 0)
				headers.put(Headers.AUTHORIZATION, "OAuth " + Token);
			return headers;
		}

		// need to use from STD classes.
		public static Hashtable<String, String> getMobileHeaders() {
			Hashtable<String, String> headers = new Hashtable<>();
			String language = Services.Resources.getCurrentLanguage();
			if (language != null)
				headers.put(Headers.GENEXUS_LANGUAGE, language);
			String themeName = PlatformHelper.getThemeName();
			if (themeName != null)
				headers.put(Headers.GENEXUS_THEME, themeName);
			headers.put(Headers.GENEXUS_AGENT, "SmartDevice Application");
			headers.put(Headers.ACCEPT_LANGUAGE, getLocaleString(Locale.getDefault()));
			headers.put(Headers.GENEXUS_TIMEZONE, StringUtil.TimeZoneOffsetID());
			headers.putAll(getClientInformationHeaders());
			return headers;
		}

		public static Hashtable<String, String> getClientInformationHeaders() {
			Hashtable<String, String> headers = new Hashtable<>();
			headers.put(Headers.DEVICE_OS_NAME, ClientInformation.osName());
			headers.put(Headers.DEVICE_OS_VERSION, ClientInformation.osVersion());
			headers.put(Headers.DEVICE_ID, ClientInformation.id());
			headers.put(Headers.DEVICE_NAME, ClientInformation.deviceName());
			headers.put(Headers.DEVICE_PLATFORM, ClientInformation.getPlatformName());
			if (Services.Strings.hasValue(ClientInformation.networkId()))
				headers.put(Headers.DEVICE_NETWORK_ID, ClientInformation.networkId());
			return headers;
		}

		private static void addHeadersToHttpBase(Hashtable<String, String> headers, HttpRequestBase httpBase) {
			for (Enumeration<String> en = headers.keys(); en.hasMoreElements();) {
				String key = en.nextElement();
				httpBase.setHeader(key, headers.get(key));
			}
		}

		public static void addSecurityHeaders(final HttpRequestBase httpBase) {
			addHeadersToHttpBase(getSecurityHeaders(), httpBase);
		}

		public static void addMobileHeaders(final HttpRequestBase httpBase) {
			addHeadersToHttpBase(getMobileHeaders(), httpBase);
		}

		// need to use from STD classes.
		public static void addSDHeader(String host, String baseUrl,	Hashtable<String, String> headerToSend) {
			URI uri;
			try {
				uri = new URI(MyApplication.getApp().UriMaker.getRootUri());

				// if are the same app root, add headers
				if (Services.Strings.hasValue(host) && Services.Strings.hasValue(baseUrl) &&
						host.equalsIgnoreCase(uri.getHost()) && baseUrl.startsWith(uri.getPath())) {
					headerToSend.putAll(getSecurityHeaders());
					headerToSend.putAll(getMobileHeaders());
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}

	public static JSONArray getJSONArrayFromUrl(String url)
	{
		ServiceDataResult result = getData(url, null, false);
		if (result.isOk() && result.getData().length() != 0)
			return result.getData();

		return null;
	}

	public static JSONObject getJSONFromUrl(String url)
	{
		ServiceDataResult result = getData(url, null, false);
		if (result.isOk() && result.getData().length() != 0)
		{
			try
			{
				return result.getData().getJSONObject(0);
			}
			catch (JSONException e)
			{
				// Should never happen, or isOk() would have returned false.
				e.printStackTrace();
				return null;
			}
		}
		else
			return null;
	}

	public static ServiceDataResult getEntityDataBC(String type, List<String> keys)
	{
		String url = MyApplication.getApp().UriMaker.MakeGetOneUriBC(type, keys);
		return getData(url, null, false);
	}

	public static ServiceDataResult getDataFromProvider(String uri, Date ifModifiedSince, boolean isCollection)
	{
		if (!Services.HttpService.isOnline())
		{
			String message = Services.Strings.getResource(R.string.GXM_NoInternetConnection);
			return ServiceDataResult.error(DataRequest.ERROR_NETWORK, message);
		}

		return getData(uri, ifModifiedSince, isCollection);
	}

	public static boolean createApplicationMetadata(IContext mycontext, Context context)
	{
		long remoteVersion = MetadataLoader.REMOTE_VERSION;
		SharedPreferences settings = context.getSharedPreferences(MetadataLoader.getPrefsName(), 0);
		long currentApi = settings.getLong("API_VERSION", 0); //$NON-NLS-1$

		// read if must read or if nothing in raw and is a new version
		if ((remoteVersion != currentApi && !MetadataLoader.FILES_IN_RAW && MetadataLoader.READ_RESOURCES) ||
			MetadataLoader.MUST_RELOAD_METADATA)
		{
			String appMetadata = MyApplication.getApp().UriMaker.makeAplicationMetadata(Services.Application.getAppEntry());
			Services.Log.debug(String.format("Downloading '%s'.", appMetadata));

			// If the app package is not there, also try the "old" package containing everything.
			InputStream stream = getInputStreamFromUrl(appMetadata);
			if (stream == null)
			{
				appMetadata = MyApplication.getApp().UriMaker.makeAplicationMetadata("app");
				Services.Log.debug(String.format("Downloading '%s'.", appMetadata));

				stream = getInputStreamFromUrl(appMetadata);
			}

			try
			{
				if (stream != null)
				{
					ZipHelper zipper = new ZipHelper(stream);
					zipper.unzip(context);

					SharedPreferences.Editor editor = settings.edit();
					editor.putLong("API_VERSION", remoteVersion); //$NON-NLS-1$
					editor.putString("DOWNLOADED_ZIP_VERSION", MetadataLoader.REMOTE_MAJOR_VERSION + "." + MetadataLoader.REMOTE_MINOR_VERSION ); //$NON-NLS-1$
					editor.commit();

					// now read from downloaded files
					MetadataLoader.READ_RESOURCES = false;
				}

			}
			finally
			{
				IOUtils.closeQuietly(stream);
			}
		}

		return true;
	}

	public static InputStream getInputStreamFromUrl(String url)
	{
		try
		{
			URL fileUrl = new URL(url);
			URLConnection urlConnection = fileUrl.openConnection();
			return urlConnection.getInputStream();
		}
		catch (IOException e)
		{
			Services.Log.error(e);
			return null;
		}
	}

	private static ServiceDataResult getData(String uri, Date ifModifiedSince, boolean parseList)
	{
		return getData(uri, ifModifiedSince, parseList, false);
	}

	private static ServiceDataResult getData(String uri, Date ifModifiedSince, boolean parseList, boolean isRetryAttempt)
	{
		final HttpClient client = getThreadSafeClient();
		final HttpGet get = new HttpGet(uri);

		Headers.addSecurityHeaders(get);
		Headers.addMobileHeaders(get);

		if (ifModifiedSince != null)
			get.setHeader(Headers.IF_MODIFIED_SINCE, StringUtil.dateToHttpFormat(ifModifiedSince));

		try
		{
			NetworkLogger.logRequest(get);
			DebugService.onHttpRequest(get);

			GxHttpResponse response = new GxHttpResponse(client.execute(get));
			NetworkLogger.logResponse(get, response);

			ServiceDataResult result = new ServiceDataResult(get, response, parseList);

			// Retry if it's a recoverable error (e.g. token expired but successfully renewed).
			if (!isRetryAttempt && shouldRetryRequest(result.getErrorType()))
				return getData(uri, ifModifiedSince, parseList, true);

			return result;
		}
		catch (IOException ex)
		{
			get.abort();
			NetworkLogger.logException(get, ex);
			return ServiceDataResult.networkError(ex);
		}
	}

	private static boolean shouldRetryRequest(int responseCode)
	{
		// In case we get an authentication error, see if it can be resolved without
		// asking for user name & password, and in that case signal to repeat the query.
		if (responseCode == DataRequest.ERROR_SECURITY_AUTHENTICATION)
			return SecurityHelper.tryAutomaticLogin();

		return false;
	}

	@Override
	public ServiceResponse saveEntityData(String type, List<String> key, INodeObject node)
	{
		JSONObject json = ((NodeObject) node).getInner();
		String url = MyApplication.getApp().UriMaker.MakeGetOneUriBC(type, key);
		try
		{
			return putJson(url, json);
		}
		catch (IOException e)
		{
			return new ServiceResponse(e);
		}
	}

	@Override
	public ServiceResponse insertEntityData(String type, List<String> key, INodeObject node)
	{
		JSONObject json = ((NodeObject)node).getInner();
		String url = MyApplication.getApp().UriMaker.MakeGetOneUriBC(type, key);
		try
		{
			return postJson(url, json);
		}
		catch (IOException e)
		{
			return new ServiceResponse(e);
		}
	}

	@Override
	public ServiceResponse deleteEntityData(String type, List<String> key)
	{
		String url = MyApplication.getApp().UriMaker.MakeGetOneUriBC(type, key);
		try
		{
			return delete(url);
		}
		catch (IOException e)
		{
			return new ServiceResponse(e);
		}
	}

	private static DefaultHttpClient sHttpClient = null;

	public static DefaultHttpClient getThreadSafeClient()
	{
		if (sHttpClient == null)
		{
			DefaultHttpClient client;

			if (MyApplication.getApp() != null &&
				MyApplication.getApp().getAPIUri().startsWith("https:") && //$NON-NLS-1$
				MyApplication.getApp().getAllowNotTrustedCertificate())
			{
				//New HttpClient for use with https
				SchemeRegistry schemeRegistry = new SchemeRegistry();
				schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80)); //$NON-NLS-1$
				schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443)); //$NON-NLS-1$

				HttpParams params = new BasicHttpParams();
				params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
				params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(30));
				params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
				HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

				ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
				client = new DefaultHttpClient(cm, params);
			}
			else
			{
				DefaultHttpClient tmpClient = new DefaultHttpClient();
				ClientConnectionManager mgr = tmpClient.getConnectionManager();
				HttpParams params = tmpClient.getParams();

				client = new DefaultHttpClient(new ThreadSafeClientConnManager(params, mgr.getSchemeRegistry()), params);
			}

			// Set timeouts.
			// Connection Timeout - timeout in milliseconds until a connection is established.
			// Socket Timeout - timeout in milliseconds while waiting for the server to send data.
			HttpParams httpParameters = client.getParams();
		    HttpConnectionParams.setConnectionTimeout(httpParameters, DEFAULT_CONNECTION_TIMEOUT);
		    HttpConnectionParams.setSoTimeout(httpParameters, DEFAULT_SOCKET_TIMEOUT);

		    enableCompression(client);
		    sHttpClient = client;
		}

		return sHttpClient;
	}

	private static void enableCompression(DefaultHttpClient client)
	{
		final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
		final String ENCODING_GZIP = "gzip";

		client.addRequestInterceptor(new HttpRequestInterceptor()
		{
			@Override
			public void process(HttpRequest request, HttpContext context)
			{
				// Add header to accept gzip content
				if (!request.containsHeader(HEADER_ACCEPT_ENCODING))
					request.setHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
			}
		});

		client.addResponseInterceptor(new HttpResponseInterceptor()
		{
			@Override
			public void process(HttpResponse response, HttpContext context)
			{
				// Inflate any responses compressed with gzip.
				final HttpEntity entity = response.getEntity();

				// Ignore null entity (e.g. 304 response) or Content-Length=0 because it causes GZIPInputStream to throw an exception.
				// (according to the docs Content-Length can be zero ONLY if the response is empty, an unknown size should be negative).
				// In both of those cases keep the original Entity object, otherwise add a decompressor in the middle.
				if (entity != null && entity.getContentLength() != 0)
				{
					final Header encoding = entity.getContentEncoding();
					if (encoding != null)
					{
						for (HeaderElement element : encoding.getElements())
						{
							if (element.getName().equalsIgnoreCase(ENCODING_GZIP))
							{
								response.setEntity(new InflatingEntity(response.getEntity()));
								break;
							}
						}
					}
				}
			}
		});
	}

	private static class InflatingEntity extends HttpEntityWrapper
	{
        public InflatingEntity(HttpEntity wrapped)
        {
            super(wrapped);
        }

        @Override
        public InputStream getContent() throws IOException
        {
            return new GZIPInputStream(wrappedEntity.getContent());
        }

        @Override
        public long getContentLength()
        {
            return -1;
        }
    }

	public static boolean resizeAndUploadImage(Context context, IApplicationServer appServer, Uri imageUri, String key, int maxUploadSizeMode, PropertiesObject entity, IProgressListener progressListener)
	{
		if (!Strings.hasValue(imageUri.getScheme()))
		{
			// if not has scheme try it as a file path.
			imageUri = Uri.parse("file://" + imageUri);
		}

		String mimeType = FileUtils.getMimeType(MyApplication.getAppContext(), imageUri);
		String imageExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
		String imagePath = FileUtils.getLocalFile(context, imageUri, null);
		String mediaExtension = FileUtils.getExtension(context, imageUri);

		try
		{
			UploadSizeDefinition uploadSize = Services.Application.getPatternSettings().getUploadSize(maxUploadSizeMode);
			byte[] bytes = ImageHelper.resizeImageIfNecessary(imagePath, uploadSize);
			if (bytes == null)
				return false; // Probably an invalid image.

			File tmpFile = StorageHelper.getNewCameraFile(imageExtension);
			try
			{
				// Write the resized image to a file, and include the original's EXIF data (especially orientation).
				org.apache.commons.io.FileUtils.writeByteArrayToFile(tmpFile, bytes, false);
				bytes = null; // We don't need the in-memory data anymore.
				new ExifHelper().copyExifInformation(imageUri, tmpFile);

				InputStream uploadImageData = new FileInputStream(tmpFile);
				try
				{
					return ServiceHelper.uploadFile(appServer, uploadImageData, tmpFile.length(), mediaExtension, mimeType, entity, key, progressListener);
				}
				finally
				{
					IOUtils.closeQuietly(uploadImageData);
				}
			}
			finally
			{
				org.apache.commons.io.FileUtils.deleteQuietly(tmpFile);
			}
		}
		catch (IOException ex)
		{
			Services.Exceptions.handle(ex);
			return false;
		}
	}

	public static boolean uploadFile(IApplicationServer appServer, InputStream data, long dataLength, String fileExtension, String fileMimeType, PropertiesObject entity, String key, IProgressListener progressListener)
	{
		String binaryToken = appServer.uploadBinary(fileExtension, fileMimeType, data, dataLength, progressListener);
		if (binaryToken != null)
		{
			entity.setProperty(key, binaryToken);
			return true;
		}
		else
			return false;
	}

	public static ServiceResponse uploadInputStreamToServer(String url, InputStream data, long dataLength, String mimeType, IProgressListener listener)
	{
		final HttpClient client = getThreadSafeClient();

		ProgressInputStreamEntity entity = new ProgressInputStreamEntity(data, dataLength, listener);
		entity.setContentType(mimeType);

		final HttpPost httpPost = new HttpPost(url);
		Headers.addMobileHeaders(httpPost);
		Headers.addSecurityHeaders(httpPost);
		httpPost.setEntity(entity);

		try
		{
			NetworkLogger.logRequest(httpPost);
			DebugService.onHttpRequest(httpPost);

			GxHttpResponse response = new GxHttpResponse(client.execute(httpPost));
			NetworkLogger.logResponse(httpPost, response);

			return responseToServiceResponse(httpPost, response.getEntity(), response, true);
		}
		catch (IOException e)
		{
			NetworkLogger.logException(httpPost, e);
			return new ServiceResponse(e);
		}
		finally
		{
			IOUtils.closeQuietly(data);
		}
	}

	public static void uploadFileFromPath(IApplicationServer server, PropertiesObject propObject, DataItem def, String filePath)
	{
		File file = new File(filePath);
		if (file.exists())
		{
			Uri imageUri = Uri.parse(filePath);
			if (!Strings.hasValue(imageUri.getScheme()))
			{
				// if not has scheme try it as a file path.
				imageUri = Uri.parse("file://" + imageUri);
			}
			String mediaMimeType = FileUtils.getMimeType(MyApplication.getAppContext(), imageUri);
			String mediaExtension = FileUtils.getExtension(MyApplication.getAppContext(), imageUri);

			FileInputStream data;
			try
			{
				data = new FileInputStream(file);
				ServiceHelper.uploadFile(server, data, file.length(), mediaExtension, mediaMimeType, propObject, def.getName(), null);
			} catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static ServiceResponse postJsonSyncResponse(String url, JSONArray jsonArray, String syncVersion) throws IOException
	{
		HttpPost post = new HttpPost(url);
		post.setHeader(Headers.GENEXUS_SYNC_VERSION, syncVersion);
		return doServerRequest(post, jsonArray.toString(), false);
	}

	public static ServiceResponse postJson(String url, JSONArray jsonArray) throws IOException
	{
		return doServerRequest(new HttpPost(url), jsonArray.toString(), true);
	}

	public static ServiceResponse postJson(String url, JSONObject jsonObject) throws IOException
	{
		return doServerRequest(new HttpPost(url), jsonObject.toString(), true);
	}

	public static ServiceResponse putJson(String url, JSONObject json) throws IOException
	{
		return doServerRequest(new HttpPut(url), json.toString(), true);
	}

	public static ServiceResponse delete(String url) throws IOException
	{
		return doServerRequest(new HttpDelete(url), null, false);
	}

	private static ServiceResponse doServerRequest(HttpRequestBase request, String content, boolean readJsonInResponse) throws IOException
	{
		return doServerRequest(request, content, readJsonInResponse, false);
	}

	private static ServiceResponse doServerRequest(HttpRequestBase request, String content, boolean readJsonInResponse, boolean isRetryAttempt) throws IOException
	{
		final HttpClient client = getThreadSafeClient();

		if (request instanceof HttpEntityEnclosingRequest && content != null)
		{
			StringEntity requestEntity = new StringEntity(content, HTTP.UTF_8);
			requestEntity.setContentType(CONTENT_TYPE_JSON);
			((HttpEntityEnclosingRequest)request).setEntity(requestEntity);
		}

		Headers.addSecurityHeaders(request);
		Headers.addMobileHeaders(request);

		DebugService.onHttpRequest(request);
		NetworkLogger.logRequest(request);

		GxHttpResponse response;
		try
		{
			response = new GxHttpResponse(client.execute(request));
			NetworkLogger.logResponse(request, response);
		}
		catch (IOException e)
		{
			// On exception, log *and* rethrow.
			NetworkLogger.logException(request, e);
			throw e;
		}

		HttpEntity responseEntity = response.getEntity();
		ServiceResponse serviceResponse = responseToServiceResponse(request, responseEntity, response, readJsonInResponse);

		// Retry if it's a recoverable error (e.g. token expired but successfully renewed).
		if (!isRetryAttempt && shouldRetryRequest(serviceResponse.StatusCode))
			return doServerRequest(request, content, readJsonInResponse, true);

		return serviceResponse;
	}

	public static String getLocaleString(Locale language)
	{
		// JvB: need to take sub-tag into account
		if (Strings.EMPTY.equals(language.getCountry())) {
			return language.getLanguage();
		} else {
			return language.getLanguage() + '-'
			+ language.getCountry() + Strings.COMMA + language.getLanguage();
		}
	}

	private static ServiceResponse responseToServiceResponse(HttpRequestBase getBase, HttpEntity entity, final HttpResponse response, boolean returnJson)
	{
		ServiceResponse serviceResponse = new ServiceResponse();

		serviceResponse.HttpCode = response.getStatusLine().getStatusCode();
		if (serviceResponse.HttpCode == HttpURLConnection.HTTP_UNAUTHORIZED)
		{
			Pair<Integer, String> error = ServiceErrorParser.parse(getBase, response);
			serviceResponse.StatusCode = error.first;
			serviceResponse.ErrorMessage = error.second;
			return serviceResponse;
		}

		if (entity != null)
		{
			try
			{
				if (returnJson)
					serviceResponse.Message = EntityUtils.toString(entity, HTTP.UTF_8);
				else
					serviceResponse.Stream = entity.getContent();
			}
			catch (Exception ex)
			{
				Services.Log.error(ex);
			}
		}

		try
		{
			if (serviceResponse.getResponseOk())
			{
				if (returnJson && serviceResponse.HttpCode != HttpURLConnection.HTTP_NO_CONTENT)
				{
					if (Services.Strings.hasValue(serviceResponse.Message))
						serviceResponse.Data = new NodeObject(new JSONObject(serviceResponse.Message));

					Header[] headers = response.getHeaders("Warning"); //$NON-NLS-1$
					if (headers != null)
					{
						serviceResponse.WarningMessage = Strings.EMPTY;
						for (Header header : headers)
						{
							readWarningFromHeader(serviceResponse, header);
						}
					}
				}
			}
			else
			{
				// Response is NOT ok. Try to read specific error message. If not present, return generic one.
				String errorMessage = null;

				String responseContent = serviceResponse.Message;
				if (!returnJson)
				{
					try
					{
						responseContent = IOUtils.toString(serviceResponse.Stream);
					}
					catch (IOException e)
					{
						responseContent = "";
					}
				}

				try
				{
					JSONObject jsonResponse = new JSONObject(responseContent);
					serviceResponse.Data = new NodeObject(jsonResponse);

					JSONObject errorObj = jsonResponse.optJSONObject("error"); //$NON-NLS-1$
					errorMessage = (errorObj != null ? errorObj.getString("message") : null);
				}
				catch (JSONException e) { } // An exception here means the response was not JSON or its format was unexpected.

				int httpStatusCode = response.getStatusLine().getStatusCode();
				if (errorMessage == null || httpStatusCode >= HttpURLConnection.HTTP_INTERNAL_ERROR)
				{
					// In case of error 500, ignore the returned message and show a generic one.
					if (errorMessage != null)
						Services.Log.Error(errorMessage);

					String errorDetail = String.valueOf(httpStatusCode) + " - " + response.getStatusLine().getReasonPhrase();
					errorMessage = Services.Strings.getResource(R.string.GXM_ApplicationServerError, errorDetail);
				}

				serviceResponse.ErrorMessage = errorMessage;
			}

			return serviceResponse;
		}
		catch (JSONException e)
		{
			Services.Log.error(e);
			return new ServiceResponse(e);
		}
	}

	private static void readWarningFromHeader(ServiceResponse serviceResponse, Header header)
	{
		String value = header.getValue();
		int start = value.indexOf("\"Encoded:User:"); //$NON-NLS-1$
		boolean encoded = true;
		if (start==-1)
		{
			start = value.indexOf("\"User:"); //$NON-NLS-1$
			encoded = false;
		}
		if (start > 0)
		{
			int end = value.indexOf("\"", start + 1); //$NON-NLS-1$
			if (end > start)
			{
				String messageWarning = value.substring(start + 6, end);
				if (encoded)
				{
					try
					{
						//Decode the message from server
						messageWarning = value.substring(start + 14, end);
						messageWarning = URLDecoder.decode(messageWarning, "UTF-8"); //$NON-NLS-1$
					}
					catch (UnsupportedEncodingException e)
					{
						e.printStackTrace();
					}
				}
				serviceResponse.WarningMessage += messageWarning + Strings.SPACE;
			}
		}
	}

	
	@Override
	public long getRemoteMetadataVersion()
	{
		if (!isOnline())
			return 0;

		String uri = MyApplication.getApp().UriMaker.MakeMetadataVersion();
		try
		{
			JSONObject obj = getJSONFromUrl(uri);
			if (obj != null)
				return Long.valueOf(obj.optString("version"));
			else
				Services.Log.Error(String.format("Could not read remote metadata version from '%s'.", uri)); //$NON-NLS-1$
		}
		catch (Exception ex)
		{
			Services.Log.Error("Exception in getRemoteMetadataVersion", ex); //$NON-NLS-1$
		}

		//return 0 if could not get version
		return 0;
	}

	@Override
	public boolean isOnline()
	{
		if (DebugService.isNetworkOffline())
			return false;

		ConnectivityManager cm = (ConnectivityManager)MyApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() == null)
			return false;

		return cm.getActiveNetworkInfo().isConnectedOrConnecting();
	}

	@Override
	public int connectionType()
	{
		// 0 None, 1 Wifi , 2 WAN
		if (DebugService.isNetworkOffline())
			return 0;

		ConnectivityManager cm = (ConnectivityManager)MyApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() == null)
			return 0;

		if ( cm.getActiveNetworkInfo().isConnectedOrConnecting())
		{
			if (cm.getActiveNetworkInfo().getType()==ConnectivityManager.TYPE_WIFI)
				return 1;
			else
				if (cm.getActiveNetworkInfo().isRoaming())
					return 3;
				else
					return 2;

		}
		return 0;
	}

	@Override
	public String UriEncode(String key) {
		return Uri.encode(key);
	}

	@Override
	public String UriDecode(String key) {
		return Uri.decode(key);
	}

	//Security

	//Login
	public static ServiceResponse StringToPostSecurity(String url, String parameters)
	{
		final HttpClient client = getThreadSafeClient();
		final HttpPost post = new HttpPost(url);

		try
		{
			if (Strings.hasValue(parameters))
			{
				StringEntity entitysend = new StringEntity(parameters, HTTP.UTF_8);
				entitysend.setContentType("application/x-www-form-urlencoded"); //$NON-NLS-1$
				post.setEntity(entitysend);
			}

			Headers.addMobileHeaders(post);
			Headers.addSecurityHeaders(post); // Needed for logout, at least.

			// Don't follow an HTTP redirect.
			HttpClientParams.setRedirecting(client.getParams(), false);
			try
			{
				NetworkLogger.logRequest(post);
				final GxHttpResponse response = new GxHttpResponse(client.execute(post));
				NetworkLogger.logResponse(post, response);

				if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_SEE_OTHER)
				{
					String newUrl = ServiceDataResult.parseRedirectOnHeader(response);
					ServiceResponse serviceResponse = new ServiceResponse();

					serviceResponse.HttpCode = response.getStatusLine().getStatusCode();
					serviceResponse.Message = newUrl;

					try
					{
						// Read entity anyway. Why?
						//noinspection ResultOfMethodCallIgnored
						EntityUtils.toString(response.getEntity(), HTTP.UTF_8);
					}
					catch (Exception ex) {}

					return serviceResponse;
				}
				else
				{
					HttpEntity entity = response.getEntity();
					return responseToServiceResponse(post, entity, response, true);
				}
			}
			finally
			{
				// Revert to default
				HttpClientParams.setRedirecting(client.getParams(), true);
			}
		}
		catch (IOException ex)
		{
			return new ServiceResponse(ex);
		}
	}

	public static String getToken()
	{
		return Token;
	}

	public static void setToken(String token)
	{
		Token = token.trim();
	}

	public static JSONObject getEntityDefaultsBC(String name)
	{
		String url = MyApplication.getApp().UriMaker.MakeGetDefaultUriBC(name);
		return getJSONFromUrl(url);
	}

	private static int MinorVersion = -1;
	private static int MajorVersion = -1;

	private static String AppStoreUrl = Strings.EMPTY;

	private void getRemoteApplicationInformation(String app)
	{
		if (isOnline() && (MinorVersion == -1 || MajorVersion == -1))
		{
			String uri = MyApplication.getApp().UriMaker.MakeApplicationVersion(app);
			try
			{
				JSONObject obj = getJSONFromUrl(uri);
				if (obj != null)
				{
					MajorVersion = Integer.valueOf(obj.optString("major")); //$NON-NLS-1$
					MinorVersion = Integer.valueOf(obj.optString("minor")); //$NON-NLS-1$
					AppStoreUrl = obj.optString("uri"); //$NON-NLS-1$
				}
				else
					Services.Log.Error(String.format("Could not read remote metadata version from '%s'.", uri)); //$NON-NLS-1$
			}
			catch (Exception ex)
			{
				Services.Log.Error("Exception in getRemoteVersions", ex); //$NON-NLS-1$
			}
		}
	}

	@Override
	public int getRemoteMinorVersion(String app) {
		getRemoteApplicationInformation(app);
		return MinorVersion;
	}

	@Override
	public int getRemoteMajorVersion(String app) {
		getRemoteApplicationInformation(app);
		return MajorVersion;
	}

	@Override
	public String getRemoteUrlVersion(String appEntry) {
		getRemoteApplicationInformation(appEntry);
		return AppStoreUrl;
	}

	@Override
	public String getNetworkErrorMessage(IOException e)
	{
		// The message is usually the exception's message.
		// If it doesn't have one, use the class name, but substitute "known" ones by a more descriptive message.
		String detail = e.getMessage();
		if (detail == null)
		{
			// Special cases
			if (e instanceof SocketTimeoutException)
				detail = "connection timed out";
			else
				detail = e.getClass().getName();
		}

		return Services.Strings.getResource(R.string.GXM_NetworkError, detail);
	}

	@Override
	public boolean isReachable(String url)
	{
		if (url == null)
			throw new IllegalArgumentException("Url cannot be null");

		if (!isOnline())
			return false;

		try
		{
			URL netUrl = new URL(url);
			HttpURLConnection connection = (HttpURLConnection)netUrl.openConnection();
			connection.setConnectTimeout(4000);
			connection.setReadTimeout(4000);
			try
			{
				// We aren't interested in the response. So long as we're able to connect, it's fine.
				connection.connect();
				return true;
			}
			finally
			{
				connection.disconnect();
			}
		}
		catch (Exception e)
		{
			// Should be MalformedURLException, IOException.
			Services.Log.debug("Exception during ServiceHelper.isReachable: " + e.toString());
			return false;
		}
	}
}
