package com.artech.controls.maps.common.kml;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import android.os.AsyncTask;

import com.artech.base.services.Services;
import com.artech.controls.maps.common.IMapsProvider;

public class KmlReaderAsyncTask extends AsyncTask<String, Void, MapLayer>
{
	private final IMapsProvider mMapsProvider;
	
	public KmlReaderAsyncTask(IMapsProvider mapsProvider)
	{
		mMapsProvider = mapsProvider;
	}
	
	@Override
	protected MapLayer doInBackground(String... params)
	{
		if (params == null || params.length == 0)
			return null;
		
		IKmlDeserializer deserializer = null;
		try
		{
			Class<?> klass = Class.forName("com.artech.controls.maps.common.kml.KmlDeserializerImpl");
			if (klass != null)
				deserializer = (IKmlDeserializer)klass.newInstance();
		}
		catch (Exception e)
		{
			Services.Log.warning("Error instantiating KmlDeserializerImpl", e);
			return null;
		}
		
		if (deserializer != null)
		{
			try
			{
				String kmlString = params[0];
				// InputStream is = new FileInputStream(filename); To read as a file. For now the KML is passed as text.
				InputStream is = IOUtils.toInputStream(kmlString);
				try
				{
					return deserializer.deserialize(mMapsProvider, is);
				}
				finally
				{
					IOUtils.closeQuietly(is);
				}
			}
			catch (Exception e)
			{
				Services.Log.warning("Error deserializing KML file", e);
				return null;
			}
		}
		else
			return null;
	}
}
