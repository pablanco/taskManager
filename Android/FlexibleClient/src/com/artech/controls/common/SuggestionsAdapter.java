package com.artech.controls.common;

import java.util.LinkedHashMap;
import java.util.List;

import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.artech.R;
import com.artech.application.MyApplication;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.providers.IApplicationServer;
import com.artech.base.utils.Strings;
import com.artech.ui.Coordinator;

public class SuggestionsAdapter extends ArrayAdapter<String>
{
	private final Coordinator mCoordinator;
	private final SuggestDefinition mDefinition;
	private final IApplicationServer mServer;

	private List<String> mCurrentSuggestions;

	public SuggestionsAdapter(Coordinator coordinator, LayoutItemDefinition definition)
	{
		super(coordinator.getUIContext(), R.layout.support_simple_spinner_dropdown_item);
		mCoordinator = coordinator;
		mDefinition = new SuggestDefinition(definition);
		mServer = MyApplication.getApplicationServer(coordinator.getUIContext().getConnectivitySupport());
	}

    @Override
    public int getCount()
    {
    	if (mCurrentSuggestions != null)
    		return mCurrentSuggestions.size();
    	else
    		return 0;
    }

    @Override
    public String getItem(int index)
    {
    	if (mCurrentSuggestions != null && index < mCurrentSuggestions.size())
    		return mCurrentSuggestions.get(index);
    	else
    		return Strings.EMPTY;
    }

    @Override
	public Filter getFilter()
    {
    	return new Filter()
    	{
			@Override
			protected FilterResults performFiltering(CharSequence constraint)
			{
				if (constraint == null)
					constraint = Strings.EMPTY;

				// Note: the first value is always the constraint.
				LinkedHashMap<String, String> conditionValues = new LinkedHashMap<String, String>();
				for (int i = 0; i < mDefinition.ServiceInput.size(); i++)
				{
					String inputName = mDefinition.ServiceInput.get(i);
					String inputValue = (i == 0 ? constraint.toString() : mCoordinator.getStringValue(inputName));
					conditionValues.put(inputName, inputValue);
				}

		    	// The performFiltering() method is called in another thread, so no need for an AsyncTask here.
				List<String> suggestions = mServer.getSuggestions(mDefinition.Service, conditionValues);

	            FilterResults results = new FilterResults();
	            results.values = suggestions;
				results.count = suggestions.size();
				return results;
			}

			@Override
			@SuppressWarnings("unchecked")
			protected void publishResults(CharSequence constraint, FilterResults results)
			{
				if (results != null)
					mCurrentSuggestions = (List<String>)results.values;
			    if (results != null && results.count > 0)
                    notifyDataSetChanged();
                else
                    notifyDataSetInvalidated();
			}
    	};
    }

	private static class SuggestDefinition extends ControlServiceDefinition
	{
		protected SuggestDefinition(LayoutItemDefinition itemDefinition)
		{
			super(itemDefinition, "_sg");

			// Error in the metadata, the first parameter expects the values attribute / description attribute name.
			if (ServiceInput.size() != 0)
			{
				ControlInfo controlInfo = itemDefinition.getControlInfo();

				String suggestInput;
				if (controlInfo.optStringProperty("@InputType").equalsIgnoreCase("Values"))
					suggestInput = controlInfo.optStringProperty("@ControlItemValues");
				else
					suggestInput = controlInfo.optStringProperty("@ControlItemDescription");

				if (Strings.hasValue(suggestInput))
					ServiceInput.set(0, suggestInput);
			}
		}
	}
}
