package com.artech.base.metadata.loader;

import com.artech.base.utils.Strings;

public class LoadResult
{
	private final int mCode;
	private final Throwable mError;

	public final static int RESULT_OK = 0;
	public final static int RESULT_UPDATE = 1;
	public final static int RESULT_ERROR = 2;
	public final static int RESULT_UPDATE_RELAUNCH = 3;

	private LoadResult(int code, Throwable error)
	{
		mCode = code;
		mError = error;
	}

	public static LoadResult result(int code)
	{
		return new LoadResult(code, null);
	}

	public static LoadResult error(Throwable ex)
	{
		return new LoadResult(RESULT_ERROR, ex);
	}

	public int getCode()
	{
		return mCode;
	}

	public String getErrorMessage()
	{
		if (mError != null)
		{
			if (mError.getMessage() != null && mError.getMessage().length() > 0)
				return mError.getMessage();
			else
				return mError.toString();
		}
		else
			return Strings.EMPTY;
	}

	public Throwable getErrorDetail()
	{
		if (mError instanceof LoadException)
			return ((LoadException)mError).getDetail();

		return mError;
	}
}
