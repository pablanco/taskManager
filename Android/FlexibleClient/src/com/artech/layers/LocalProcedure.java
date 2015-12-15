package com.artech.layers;

import java.util.ArrayList;
import java.util.List;

import com.artech.application.MyApplication;
import com.artech.base.application.IProcedure;
import com.artech.base.application.MessageLevel;
import com.artech.base.application.OutputMessage;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.ObjectParameterDefinition;
import com.artech.base.metadata.ProcedureDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.PropertiesObject;
import com.artech.base.services.IGxProcedure;
import com.artech.base.services.Services;
import com.artech.utils.Cast;

public class LocalProcedure implements IProcedure
{
	private final String mName;
	private final IGxProcedure mImplementation;

	public LocalProcedure(String name)
	{
		mName = name;
		mImplementation = GxObjectFactory.getProcedure(name);
	}

	@Override
	public OutputResult execute(PropertiesObject parameters)
	{
		if (mImplementation != null)
		{
			LocalUtils.beginTransaction();
			
			try {
				mImplementation.execute(parameters);
			}
			finally {
				LocalUtils.endTransaction();
			}
			LocalBusinessComponent.postSendBCToServer();
			return translateOutput(parameters);
		}
		else
			return LocalUtils.outputNoImplementation(mName);
	}

	@Override
	public OutputResult executeMultiple(List<PropertiesObject> parameters)
	{
		if (mImplementation != null)
		{
			for (PropertiesObject item : parameters)
			{
				LocalUtils.beginTransaction();
				try
				{
					mImplementation.execute(item);
				}
				finally
				{
					LocalUtils.endTransaction();
				}
				// TODO: Accumulate procedure warnings/errors. Not right now, to maintain compatibility with online behavior.
			}
			// TODO: Return procedure warnings/errors. Not right now, to maintain compatibility with online behavior.
			LocalBusinessComponent.postSendBCToServer();
			return OutputResult.ok();
		}
		else
			return LocalUtils.outputNoImplementation(mName);
	}

	private OutputResult translateOutput(PropertiesObject parameters)
	{
		// See if there are any output parameters of type "Messages".
		ObjectParameterDefinition outputParameter = getOutputParameter(MyApplication.getInstance().getProcedure(mName));

		if (outputParameter == null)
			return OutputResult.ok(); // No output means the call is successful, unless it crashes or something.

		// Since this is a collection SDT, it should have been converted to a collection of Entities.
		Object outputValue = parameters.getProperty(outputParameter.getName());
		List<?> procedureMessages = Cast.as(List.class, outputValue);
		if (procedureMessages != null)
		{
			ArrayList<OutputMessage> messages = new ArrayList<OutputMessage>();
			for (Object objProcedureMessage : procedureMessages)
			{
				Entity procedureMessage = Cast.as(Entity.class, objProcedureMessage);
				if (procedureMessage != null)
				{
					MessageLevel msgLevel = CommonUtils.translateMessageLevel(procedureMessage.optStringProperty("Type"));
					String msgText = procedureMessage.optStringProperty("Description");
					messages.add(new OutputMessage(msgLevel, msgText));
				}
			}

			return new OutputResult(messages);
		}
		else
		{
			Services.Log.warning(String.format("Could not read output messages after calling procedure '%s'.", mName));
			return OutputResult.ok();
		}
	}

	private static ObjectParameterDefinition getOutputParameter(ProcedureDefinition procedure)
	{
		final String MESSAGES_NAME = "Messages";

		if (procedure == null)
			return null;

		for (ObjectParameterDefinition parameter : procedure.getOutParameters())
			if (MESSAGES_NAME.equalsIgnoreCase(parameter.getName()))
				return parameter;

		return null;
	}
}
