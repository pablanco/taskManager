package com.artech.layers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;

import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.genexus.GXDbFile;
import com.genexus.GXutil;

class LocalBinaryHelper
{
	public static String upload(String fileExtension, String fileMimeType, InputStream data, long dataLength)
	{
		try
		{
			try
			{
				String blobBasePath = com.genexus.Preferences.getDefaultPreferences().getBLOB_PATH();
				//String fileName = com.genexus.PrivateUtilities.getTempFileName(blobPath, "binary", fileType);

				//Convert extension.
				if (Strings.hasValue(fileExtension) && fileExtension.startsWith("."))
					fileExtension = fileExtension.substring(1);

				// path outside database, should be unique.
				String fileName = GXDbFile.addTokenToFileName("binary", fileExtension);

				String fileNameNew = blobBasePath + "/" + GXutil.getFileName(fileName)+ "." + GXutil.getFileType(fileName);

				File file = new File(fileNameNew);
				FileUtils.copyInputStreamToFile(data, file);
				return fileNameNew;
			}
			finally
			{
				data.close();
			}
		}
		catch (IOException e)
		{
			Services.Log.error(e);
			return null;
		}
	}
}
