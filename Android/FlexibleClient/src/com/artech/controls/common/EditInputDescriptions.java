package com.artech.controls.common;

import java.util.LinkedHashMap;

import android.os.AsyncTask;

import com.artech.application.MyApplication;
import com.artech.base.controls.MappedValue;
import com.artech.base.metadata.DataTypeDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.providers.IApplicationServer;
import com.artech.base.services.IValuesFormatter;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.ui.Coordinator;

public class EditInputDescriptions extends EditInput
{
	private final LayoutItemDefinition mLayoutItem;
	private final Coordinator mCoordinator;

	private final IApplicationServer mServer;
	private final ControlServiceDefinition mGetValueFromDescriptionDefinition;
	private final ControlServiceDefinition mGetDescriptionFromValueDefinition;

	private String mValue;
	private String mText;
	private GetMappedValueTask mCurrentTask;

	public EditInputDescriptions(Coordinator coordinator, LayoutItemDefinition layoutItem)
	{
		if (!isInputTypeDescriptions(layoutItem))
			throw new IllegalArgumentException("LayoutItemDefinition does not have input type descriptions");

		mLayoutItem = layoutItem;
		mCoordinator = coordinator;
		mServer = MyApplication.getApplicationServer(coordinator.getUIContext().getConnectivitySupport());

		mGetValueFromDescriptionDefinition = new GetValueFromDescriptionService(layoutItem);
		mGetDescriptionFromValueDefinition = new GetDescriptionFromValueService(layoutItem);

		mValue = Strings.EMPTY;
		mText = Strings.EMPTY;
	}

	public static boolean isInputTypeDescriptions(LayoutItemDefinition layoutItem)
	{
		return (layoutItem != null &&
				layoutItem.getControlInfo() != null &&
				layoutItem.getControlInfo().optStringProperty("@InputType").equalsIgnoreCase("Descriptions"));
	}

	@Override
	public void setValue(String value, OnMappedAvailable onTextAvailable)
	{
		mValue = value;
		GetMappedValueTask task = new GetDescriptionFromValueTask(onTextAvailable);
		runTask(task, value);
	}

	@Override
	public void setText(String text, OnMappedAvailable onValueAvailable)
	{
		mText = text;
		GetMappedValueTask task = new GetValueFromDescriptionTask(onValueAvailable);
		runTask(task, text);
	}

	@Override
	public String getValue()
	{
		return mValue;
	}

	@Override
	public String getText()
	{
		return mText;
	}

	private void runTask(GetMappedValueTask task, String input)
	{
		if (mCurrentTask != null)
		{
			mCurrentTask.cancel(true);
			mCurrentTask = null;
		}

		mCurrentTask = task;
		CompatibilityHelper.executeAsyncTask(task, input);
	}

	private class GetValueFromDescriptionTask extends GetMappedValueTask
	{
		public GetValueFromDescriptionTask(OnMappedAvailable onValueAvailable)
		{
			super(mGetValueFromDescriptionDefinition, onValueAvailable);
		}

		@Override
		protected void onPostExecute(MappedValue result)
		{
			mValue = result.value;
			super.onPostExecute(result);
		}
	}

	private class GetDescriptionFromValueTask extends GetMappedValueTask
	{
		public GetDescriptionFromValueTask(OnMappedAvailable onValueAvailable)
		{
			super(mGetDescriptionFromValueDefinition, onValueAvailable);
		}

		@Override
		protected void onPostExecute(MappedValue result)
		{
			mText = result.value;
			super.onPostExecute(result);
		}
	}

	private abstract class GetMappedValueTask extends AsyncTask<String, Void, MappedValue>
	{
		private final ControlServiceDefinition mService;
		private final OnMappedAvailable mOnResultAvailable;

		public GetMappedValueTask(ControlServiceDefinition service, OnMappedAvailable onResultAvailable)
		{
			mService = service;
			mOnResultAvailable = onResultAvailable;
		}

		@Override
		protected MappedValue doInBackground(String... params)
		{
			String value = params[0];

			if (Strings.hasValue(value))
			{
				// Note: the first value is always the value to be mapped.
				LinkedHashMap<String, String> inputValues = new LinkedHashMap<String, String>();
				for (int i = 0; i < mService.ServiceInput.size(); i++)
				{
					String inputName = mService.ServiceInput.get(i);
					String inputValue = (i == 0 ? value : mCoordinator.getStringValue(inputName));
					inputValues.put(inputName, inputValue);
				}

				return mServer.getMappedValue(mService.Service, inputValues);
			}
			else
				return MappedValue.exact(Strings.EMPTY);
		}

		@Override
		protected void onPostExecute(MappedValue result)
		{
			if (!isCancelled() && mOnResultAvailable != null)
				mOnResultAvailable.run(result);
		}
	}

	private static class GetValueFromDescriptionService extends ControlServiceDefinition
	{
		public GetValueFromDescriptionService(LayoutItemDefinition itemDefinition)
		{
			super(itemDefinition, "_hc");

			// Error in the metadata, the first parameter expects the attribute name.
			if (ServiceInput.size() != 0)
				ServiceInput.set(0, itemDefinition.getControlInfo().optStringProperty("@ControlItemDescription"));
		}
	}

	private static class GetDescriptionFromValueService extends ControlServiceDefinition
	{
		public GetDescriptionFromValueService(LayoutItemDefinition itemDefinition)
		{
			super(itemDefinition, "_hc_rev");
		}
	}

	@Override
	public boolean getSupportsAutocorrection()
	{
		return false;
	}

	@Override
	public Integer getEditLength()
	{
		String itemDescriptions = mLayoutItem.getControlInfo().optStringProperty("@ControlItemDescription");
		if (Strings.hasValue(itemDescriptions))
		{
			DataTypeDefinition itemDescriptionsDefinition = Services.Application.getAttribute(itemDescriptions);
			if (itemDescriptionsDefinition != null)
			{
				int length = itemDescriptionsDefinition.getLength();
				if (length != 0)
					return length;
			}
		}

		return null;
	}

	@Override
	public IValuesFormatter getValuesFormatter()
	{
		return new ValuesFormatter();
	}

	private class ValuesFormatter implements IValuesFormatter
	{
		@Override
		public boolean needsAsync()
		{
			return true;
		}

		@Override
		public CharSequence format(String value)
		{
			try
			{
				// This is called from background, so calling doInBackground() is ok.
				return new GetDescriptionFromValueTask(null).doInBackground(value).value;
			}
			catch (Exception e)
			{
				return Strings.EMPTY;
			}
		}
	}
}