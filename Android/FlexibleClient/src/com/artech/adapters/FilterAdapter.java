package com.artech.adapters;

import java.util.Vector;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.artech.R;
import com.artech.android.ResourceManager;
import com.artech.base.metadata.filter.FilterAttributeDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.FiltersHelper;
import com.artech.controls.GxLinearLayout;
import com.artech.controls.GxTextBlockTextView;

public class FilterAdapter extends BaseAdapter
{
	private final LayoutInflater mInflater;
	private final Context mContext;
    private final Vector<FilterAttributeDefinition> mArrayFilter;
    private final Vector<String> mFilterRangeBegin;
    private final Vector<String> mFilterRangeEnd;

    private static class ViewHolder
    {
        TextView description;
        TextView filter;
        // ImageView icon;
    }

    public FilterAdapter(Context context, Vector<FilterAttributeDefinition> arrayFilter, Vector<String> filterRangeBegin, Vector<String>  filterRangeEnd)
    {
    	// Cache the LayoutInflate to avoid asking for a new one each time.
    	mInflater = LayoutInflater.from(context);
    	//Metadata
    	mArrayFilter = arrayFilter;
    	mFilterRangeBegin = filterRangeBegin;
    	mFilterRangeEnd = filterRangeEnd;

    	mContext = context;
    }

	@Override
	public int getCount() {
		return mArrayFilter.size();
	}

	@Override
	public Object getItem(int position) {
		return mArrayFilter.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewHolder holder;
		if (convertView == null)
		{
	    	convertView = mInflater.inflate(R.layout.filterrow , parent, false);

	    	holder = new ViewHolder();
	    	GxTextBlockTextView textDescription = (GxTextBlockTextView) convertView.findViewById(R.id.description );
	    	GxTextBlockTextView searchFilter = (GxTextBlockTextView) convertView.findViewById(R.id.searchfilter );
	    	textDescription.setGravity(Gravity.CENTER_VERTICAL);
	    	searchFilter.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
	    	holder.description= textDescription;
            holder.filter = searchFilter;
            holder.description.setGravity(Gravity.CENTER_VERTICAL);
    		holder.filter.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            convertView.setTag(holder);

            ImageView icon = (ImageView)convertView.findViewById(R.id.icon);
            icon.setImageResource(ResourceManager.getResource(mContext, R.drawable.gx_field_prompt_dark, R.drawable.gx_field_prompt_light));

            GxLinearLayout linearLayoutFilterRow = (GxLinearLayout) convertView.findViewById(R.id.GxLinearLayoutFilterRow);
            FiltersHelper.setThemeFilters(null, linearLayoutFilterRow, searchFilter, textDescription, null, null, null);

		}
		else
			holder = (ViewHolder) convertView.getTag();

		FilterAttributeDefinition filterAtt = mArrayFilter.get(position);
		CharSequence stringFiter = filterAtt.getDescription();

		String strRangeBegin = mFilterRangeBegin.get(position);
		String strRangeEnd = mFilterRangeEnd.get(position);
        if (holder.description != null) {
        	holder.description.setText(stringFiter);
       	}
        if(holder.filter != null)
        {
        	String toView = strRangeBegin;
        	if (filterAtt.getType().equalsIgnoreCase(FilterAttributeDefinition.TYPE_STANDARD))
        	{
        		if (strRangeBegin.length() == 0)
        			toView = (String)mContext.getResources().getText(R.string.GX_AllItems);
        	}
        	else if (filterAtt.getType().equalsIgnoreCase(FilterAttributeDefinition.TYPE_RANGE))
        	{
        		if (isNull(strRangeBegin) && isNull(strRangeEnd))
        			toView = (String)mContext.getResources().getText(R.string.GX_AllItems);
        		else if (!isNull(strRangeBegin) && !isNull(strRangeEnd))
        			toView = String.format(Services.Strings.getResource(R.string.GXM_FilterRange), strRangeBegin, strRangeEnd);
        		else if (!isNull(strRangeBegin) && isNull(strRangeEnd))
	    			toView = String.format(Services.Strings.getResource(R.string.GXM_FilterRangeFrom), strRangeBegin);
	    		else if (isNull(strRangeBegin) && !isNull(strRangeEnd))
	    			toView = String.format(Services.Strings.getResource(R.string.GXM_FilterRangeTo), strRangeEnd);
        	}

    		holder.filter.setText(toView);
        }

		return convertView;
	}

	private static boolean isNull(String filterValue)
	{
		// Null or empty.
		if (!Services.Strings.hasValue(filterValue))
			return true;

		// Default value of the datatype.
		// TODO: This is a horrendous hack. The FilterAttributeDefinition should have the type, currently it does not.
		return filterValue.equals(Strings.ZERO);

	}


}
