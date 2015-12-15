package com.artech.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.InputStreamEntity;

class ProgressInputStreamEntity extends InputStreamEntity
{
	private static final int CHUNK_SIZE = 1024;
	private InputStream mInputStream;
	private IProgressListener mListener;
	
	public ProgressInputStreamEntity(InputStream inputStream, long length, IProgressListener listener)
	{
		super(inputStream, length);
		mInputStream = inputStream;
		mListener = listener;

		if (mListener != null && length != -1)
			mListener.setCount(length / CHUNK_SIZE);
	}
	
	@Override
	public void writeTo(OutputStream outputStream) throws IOException
	{
		byte[] buffer = new byte[CHUNK_SIZE];
		while (mInputStream.read(buffer) > 0)
		{
			outputStream.write(buffer);
			if (mListener != null)
				mListener.step();
		}
	}
}
