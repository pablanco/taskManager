package com.artech.actions;

import java.util.HashMap;
import java.util.List;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionDefinition.DependencyInfo;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.utils.Strings;

public class DependentValuesAction extends Action
{
	private final String mService;
	private final List<String> mInput;
	private final List<String> mOutput;

	@SuppressWarnings("unchecked")
	public DependentValuesAction(UIContext context, ActionDefinition definition, ActionParameters parameters)
	{
		super(context, definition, parameters);
		mService = definition.optStringProperty(DependencyInfo.SERVICE);
		mInput = (List<String>)definition.getProperty(DependencyInfo.SERVICE_INPUT);
		mOutput = (List<String>)definition.getProperty(DependencyInfo.SERVICE_OUTPUT);
	}

	public static boolean isAction(ActionDefinition action)
	{
		return Strings.hasValue(action.optStringProperty(DependencyInfo.SERVICE));
	}

	@Override
	public boolean Do()
	{
		HashMap<String, String> inputValues = new HashMap<String, String>();
		for (String inputName : mInput)
		{
			Object inputValue = getParameterValue(new ActionParameter(inputName));
			if (inputValue != null)
				inputValues.put(inputName, inputValue.toString());
			else
				inputValues.put(inputName, Strings.EMPTY);
		}

		// Call service (local or remote).
		List<String> output = getApplicationServer().getDependentValues(mService, inputValues);

		for (int i = 0; i < mOutput.size() && i < output.size(); i++)
			setOutputValue(mOutput.get(i), output.get(i));

		return true;
	}
}
