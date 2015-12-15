package com.artech.common;

import java.io.IOException;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import com.artech.base.utils.Strings;

/**
 * Wrapper for HttpResponse to allow the Entity to be read multiple times.
 * @author matiash
 */
class GxHttpResponse implements HttpResponse
{
	private final HttpResponse mBase;
	private HttpEntity mEntity;

	public GxHttpResponse(HttpResponse base)
	{
		mBase = base;
	}

	@Override
	public HttpEntity getEntity()
	{
		if (NetworkLogger.getLevel() != NetworkLogger.Level.DETAILED)
		{
			return mBase.getEntity();
		}
		
		if (mEntity == null)
		{
			try
			{
				HttpEntity baseEntity = mBase.getEntity();
				if (baseEntity != null)
					mEntity = new BufferedHttpEntity(baseEntity);
				else
					mEntity = new StringEntity(Strings.EMPTY, HTTP.UTF_8);
			}
			catch (IOException e)
			{
				return null;
			}
		}

		return mEntity;
	}

	@Override
	public void addHeader(Header header) { mBase.addHeader(header); }

	@Override
	public void addHeader(String name, String value) { mBase.addHeader(name, value); }

	@Override
	public boolean containsHeader(String name) { return mBase.containsHeader(name); }

	@Override
	public Header[] getAllHeaders() { return mBase.getAllHeaders(); }

	@Override
	public Header getFirstHeader(String name) { return mBase.getFirstHeader(name); }

	@Override
	public Header[] getHeaders(String name) { return mBase.getHeaders(name); }

	@Override
	public Header getLastHeader(String name) { return mBase.getLastHeader(name); }

	@Override
	public HttpParams getParams() { return mBase.getParams(); }

	@Override
	public ProtocolVersion getProtocolVersion() { return mBase.getProtocolVersion(); }

	@Override
	public HeaderIterator headerIterator() { return mBase.headerIterator(); }

	@Override
	public HeaderIterator headerIterator(String name) { return mBase.headerIterator(name); }

	@Override
	public void removeHeader(Header header) { mBase.removeHeader(header); }

	@Override
	public void removeHeaders(String name) { mBase.removeHeaders(name); }

	@Override
	public void setHeader(Header header) { mBase.setHeader(header); }

	@Override
	public void setHeader(String name, String value) { mBase.setHeader(name, value); }

	@Override
	public void setHeaders(Header[] headers) { mBase.setHeaders(headers); }

	@Override
	public void setParams(HttpParams params) { mBase.setParams(params); }

	@Override
	public Locale getLocale() { return mBase.getLocale(); }

	@Override
	public StatusLine getStatusLine() { return mBase.getStatusLine(); }

	@Override
	public void setEntity(HttpEntity entity) { mBase.setEntity(entity); }

	@Override
	public void setLocale(Locale loc) { mBase.setLocale(loc); }

	@Override
	public void setReasonPhrase(String reason) throws IllegalStateException { mBase.setReasonPhrase(reason); }

	@Override
	public void setStatusCode(int code) throws IllegalStateException { mBase.setStatusCode(code); }

	@Override
	public void setStatusLine(StatusLine statusline) { mBase.setStatusLine(statusline); }

	@Override
	public void setStatusLine(ProtocolVersion ver, int code) { mBase.setStatusLine(ver, code); }

	@Override
	public void setStatusLine(ProtocolVersion ver, int code, String reason) { mBase.setStatusLine(ver, code, reason); }
}
