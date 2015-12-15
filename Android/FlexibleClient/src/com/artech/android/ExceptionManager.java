package com.artech.android;

import com.artech.base.services.IExceptions;
import com.artech.base.services.Services;

public class ExceptionManager implements IExceptions {

	@Override
	public void handle(Exception ex) {
		if (ex != null && ex.getMessage() != null) {
			Services.Log.Error("ERROR:", ex.getMessage()); //$NON-NLS-1$
		}
		if (ex != null)
			ex.printStackTrace();
		else
			Services.Log.Error("ERROR:", " invalid exception was caught"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void printStackTrace(Exception ex) {
		ex.printStackTrace();
	}

}
