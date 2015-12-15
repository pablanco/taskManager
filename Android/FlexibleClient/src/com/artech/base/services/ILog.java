package com.artech.base.services;

public interface ILog
{
	void Error(String tag, String message);
	void Error(String tag, String message, Throwable ex);
	void Error(String message);
	void Error(String message, Throwable ex);
	void error(Throwable ex);

	void warning(String tag, String message);
	void warning(String tag, String message, Throwable ex);
	void warning(String message);
	void warning(String message, Throwable ex);

	void info(String tag, String message);
	void info(String message);

	void debug(String tag, String message);
	void debug(String tag, String message, Throwable ex);
	void debug(String message);
	void debug(String message, Throwable ex);
}
