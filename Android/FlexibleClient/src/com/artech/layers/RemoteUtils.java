package com.artech.layers;

import java.util.ArrayList;
import java.util.List;

import com.artech.base.application.MessageLevel;
import com.artech.base.application.OutputMessage;
import com.artech.base.application.OutputResult;
import com.artech.base.serialization.INodeCollection;
import com.artech.base.serialization.INodeObject;
import com.artech.base.services.ServiceResponse;
import com.artech.base.services.Services;
import com.artech.common.ServiceDataResult;

class RemoteUtils
{
	public static OutputResult outputNoDefinition(String objectName)
	{
		return OutputResult.error(messageNoDefinition(objectName));
	}

	public static String messageNoDefinition(String objectName)
	{
		return String.format("The definition for object '%s' was not found in the application.", objectName);
	}

	public static OutputResult translateOutput(ServiceDataResult response)
	{
		if (response.isOk())
			return OutputResult.ok();

		// Process single error.
		OutputMessage msg = new OutputMessage(MessageLevel.ERROR, response.getErrorMessage());
		return new OutputResult(response.getErrorType(), msg);
	}

	public static OutputResult translateOutput(ServiceResponse response)
	{
		List<OutputMessage> messages = new ArrayList<OutputMessage>();

		if (Services.Strings.hasValue(response.WarningMessage))
			messages.add(new OutputMessage(MessageLevel.WARNING, response.WarningMessage));

		if (Services.Strings.hasValue(response.ErrorMessage))
			messages.add(new OutputMessage(MessageLevel.ERROR, response.ErrorMessage));

		if (response.getResponseOk())
		{
			if (response.Data != null)
			{
				// Special "message" field.
				String messageStr = response.Data.optString("message");
				if (Services.Strings.hasValue(messageStr))
					messages.add(new OutputMessage(MessageLevel.ERROR, messageStr));

				// Read the contents of the "Messages" special output variable.
				INodeCollection messageNodes = response.Data.optCollection("Messages"); //$NON-NLS-1$
				if (messageNodes.length() == 0)
					messageNodes = response.Data.optCollection("messages"); //$NON-NLS-1$

				for (INodeObject messageNode : messageNodes)
				{
					// Get text from gxmessage or Description
					String msgText = messageNode.optString("gxmessage"); //$NON-NLS-1$
					if (!Services.Strings.hasValue(msgText))
						msgText = messageNode.optString("Description"); //$NON-NLS-1$

					String type = messageNode.optString("Type"); //$NON-NLS-1$
					MessageLevel msgLevel = CommonUtils.translateMessageLevel(type);

					messages.add(new OutputMessage(msgLevel, msgText));
				}
			}
		}

		return new OutputResult(response.StatusCode, messages);
	}
}
