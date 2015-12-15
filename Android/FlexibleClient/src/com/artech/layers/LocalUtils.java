package com.artech.layers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.sqldroid.SQLDroidDriver;

import com.artech.application.MyApplication;
import com.artech.base.application.MessageLevel;
import com.artech.base.application.OutputMessage;
import com.artech.base.application.OutputResult;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.services.IPropertiesObject;
import com.artech.base.services.Services;
import com.genexus.Application;
import com.genexus.ClientContext;
import com.genexus.internet.MsgList;

public class LocalUtils
{
	public static OutputResult outputNoImplementation(String objectName)
	{
		return OutputResult.error(messageNoImplementation(objectName));
	}

	public static String messageNoImplementation(String objectName)
	{
		return String.format("An implementation for object '%s' was not found in package.", objectName);
	}

	public static OutputResult translateOutput(boolean success, MsgList gxMessages)
	{
		ArrayList<OutputMessage> messages = new ArrayList<OutputMessage>();
		for (int i = 1; i <= gxMessages.size(); i++)
		{
			if (success && i==1)
				continue;
			String text = gxMessages.getItemText(i);
			MessageLevel level = CommonUtils.translateMessageLevel(gxMessages.getItemType(i));
			messages.add(new OutputMessage(level, text));
		}

		if (!success && messages.size() == 0)
			messages.add(new OutputMessage(MessageLevel.ERROR, "Unknown error"));

		return new OutputResult(messages);
	}

	public static Object toParameter(Object value)
	{
		if (value == null)
			return null;

		// Entity -> IPropertiesObject.
		if (value instanceof Entity)
			return value;

		// EntityList -> List<IPropertiesObject>
		if (value instanceof EntityList)
			return new LinkedList<IPropertiesObject>((EntityList)value);

		// TODO: Missing simple collections!
		// All other values -> String.
		return value.toString();
	}
	
	
	public static void beginTransaction()
	{
		try {
		if (SQLDroidDriver.getCurrentConnection()!=null &&
				!SQLDroidDriver.getCurrentConnection().getAutoCommit())
			{
				SQLDroidDriver.getCurrentConnection().onlyBeginTransaction();
			}	
		} catch (SQLException e) {
				Services.Log.Error("Sqlexception " + e.getErrorCode() + " " + e.getMessage() + " " + e.toString());
				e.printStackTrace();
				
				//MsgList mList = new MsgList();
				//mList.addItem("SQL Exception ." + e.getMessage(), 1, "");
				//return LocalUtils.translateOutput(false, mList);
			}
	}
	
	public static void endTransaction()
	{
		try {
			if (SQLDroidDriver.getCurrentConnection()!=null &&
					!SQLDroidDriver.getCurrentConnection().getAutoCommit())
			{
			
				SQLDroidDriver.getCurrentConnection().onlyEndTransaction();
			}
		} catch (SQLException e) {
			Services.Log.Error("Sqlexception " + e.getErrorCode() + " " + e.getMessage() + " " + e.toString());
			e.printStackTrace();
			
			//MsgList mList = new MsgList();
			//mList.addItem("SQL Exception ." + e.getMessage(), 1, "");
			//return LocalUtils.translateOutput(false, mList);
		} catch (IllegalStateException  e) {
			Services.Log.Error("IllegalStateException " + e.getMessage() + " " + e.toString());
			e.printStackTrace();
		}
		
	}
	
	public static void commit()
	{
		Application.commit(ClientContext.getModelContext(), MyApplication.getApp().getRemoteHandle(), "DEFAULT"); //$NON-NLS-1$
	}
	
}
