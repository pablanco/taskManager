package com.artech.base.providers;

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.artech.application.MyApplication;
import com.artech.base.metadata.DataItemHelper;
import com.artech.base.metadata.DataSourceMemberDefinition;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.ObjectParameterDefinition;
import com.artech.base.metadata.OrderDefinition;
import com.artech.base.metadata.filter.FilterAttributeDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

/**
 * URI for requests to a SD Data Provider.
 * Can be used to build the URI string or to access its components piecemeal.
 * @author matiash
 *
 */
public class GxUri
{
	private final IDataSourceDefinition mDataSource;

	// DP explicit parameters
	private Hashtable<String, Object> mParameters;

	// Order, Filter, Search.
	private int mOrder; // 0-based.
	private Hashtable<FilterAttributeDefinition, Object[]> mFilterValues;
	private String mSearchText;
	private int mSearchField;

	private static final String PARAMETER_JSON_NAME = "fmt"; //$NON-NLS-1$
	private static final String PARAMETER_JSON_VALUE = "json"; //$NON-NLS-1$

	private static final String PARAMETER_SESSION_ID = "gxid"; //$NON-NLS-1$

	private static final String PARAMETER_ORDER = "Orderedby"; //$NON-NLS-1$
	private static final int PARAMETER_ORDER_DEFAULT = 0;

	private static final String PARAMETER_SEARCH_TEXT = "Searchtext";  //$NON-NLS-1$
	private static final String PARAMETER_SEARCH_FIELD = "Searchfield"; //$NON-NLS-1$
	private static final int PARAMETER_SEARCH_FIELD_DEFAULT = 0;

	private static final String PARAMETER_PAGING_START = "start"; //$NON-NLS-1$
	private static final String PARAMETER_PAGING_COUNT = "count"; //$NON-NLS-1$
	private static final int SPECIAL_EMPTY = 0;

	public GxUri(IDataSourceDefinition dataSource)
	{
		mDataSource = dataSource;
		mFilterValues = new Hashtable<FilterAttributeDefinition, Object[]>();
		mParameters = new Hashtable<String, Object>();
	}

	public GxUri setParameters(List<String> values)
	{
		if (values != null)
		{
			// Suppose that the order is the same as the definition.
			for (int i = 0; i < mDataSource.getParameters().size(); i++)
			{
				if (i < values.size())
					setParameter(mDataSource.getParameters().get(i).getName(), values.get(i));
			}
		}

		return this;
	}

	public GxUri setParameter(String name, Object value)
	{
		mParameters.put(name, value);
		return this;
	}

	public void reset()
	{
		mOrder = 0;
		resetFilter();
		resetSearch();
	}

	public boolean resetFilter()
	{
		boolean changed = (mFilterValues.size() != 0);
		mFilterValues.clear();
		return changed;
	}

	public boolean resetSearch()
	{
		boolean changed = (Services.Strings.hasValue(mSearchText) || mSearchField != 0);
		mSearchText = Strings.EMPTY;
		mSearchField = 0;
		return changed;
	}

	public GxUri setOrder(int order)
	{
		mOrder = order;
		return this;
	}

	public GxUri setFilter(String filter, Object... values)
	{
		FilterAttributeDefinition filterDef = mDataSource.getFilter().getAttribute(filter);
		if (filterDef == null)
			throw new IllegalArgumentException(String.format("filter attribute '%s' is not present in data source '%s'.", filter, mDataSource.getName())); //$NON-NLS-1$

		return setFilter(filterDef, values);
	}

	public GxUri setFilter(FilterAttributeDefinition filter, Object... values)
	{
		checkMember(filter);

		if (values == null || values.length == 0)
			mFilterValues.remove(filter);
		else {
			if (values.length != filter.getParameterNames().size())
				throw new IllegalArgumentException(String.format("Wrong number of parameters for setFilter (%s). Expected %s, received %s.", filter.getName(), filter.getParameterNames().size(), values.length)); //$NON-NLS-1$

			mFilterValues.put(filter, values);
		}
		return this;
	}

	public GxUri setSearch(String searchText)
	{
		return setSearch(searchText, PARAMETER_SEARCH_FIELD_DEFAULT);
	}

	public GxUri setSearch(String searchText, int searchField)
	{
		mSearchText = searchText;
		mSearchField = searchField;
		return this;
	}

	private void checkMember(DataSourceMemberDefinition member)
	{
		if (member == null)
			throw new IllegalArgumentException(String.format("Null member supplied to data source '%s'.", mDataSource.getName())); //$NON-NLS-1$

		if (member.getParent() != mDataSource)
			throw new IllegalArgumentException(String.format("Member '%s' is not part of this data source (%s).", member.getName(), mDataSource.getName())); //$NON-NLS-1$
	}

	public IDataSourceDefinition getDataSource() { return mDataSource; }
	public Map<String, Object> getParameters() { return mParameters; }

	public OrderDefinition getOrder()
	{
		if (mOrder < mDataSource.getOrders().size())
			return mDataSource.getOrders().get(mOrder);

		return  null;
	}

	public String getSearchText() { return mSearchText; }
	public int getSearchField() { return mSearchField; }

	public boolean hasFilterValues()
	{
		return mFilterValues.size() != 0;
	}

	public Object[] getFilter(FilterAttributeDefinition attribute)
	{
		return mFilterValues.get(attribute);
	}

	public String getName()
	{
		return mDataSource.getName();
	}

	public String getQuery()
	{
		Map<String, Object> queryValues = getQueryValues(SPECIAL_EMPTY, SPECIAL_EMPTY, SPECIAL_EMPTY);
		return valuesToUrlQuery(queryValues);
	}

	public Map<String, Object> getQueryValues(int sessionId, int start, int count)
	{
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();

		for (ObjectParameterDefinition parameter : mDataSource.getParameters())
		{
			String parameterName = parameter.getName();
			parameterName = DataItemHelper.getNormalizedName(parameterName);

			Object parameterValue = mParameters.get(parameter.getName());
			if (parameterValue != null)
				map.put(parameterName, parameterValue);
		}

		// Order
		if (mOrder > PARAMETER_ORDER_DEFAULT)
			map.put(PARAMETER_ORDER, mOrder);

		// Filters.
		for (FilterAttributeDefinition filterAtt : mDataSource.getFilter().getAttributes())
		{
			Object[] filterValues = mFilterValues.get(filterAtt);
			if (filterValues != null)
			{
				List<String> filterNames = filterAtt.getParameterNames();
				for (int i = 0; i < filterNames.size() && i < filterValues.length; i++)
					map.put(filterNames.get(i), filterValues[i]);
			}
		}

		// Search Text.
		if (mDataSource.getFilter().getSearch() != null)
		{
			if (Services.Strings.hasValue(mSearchText))
			{
				map.put(PARAMETER_SEARCH_TEXT, mSearchText);
				if (mSearchField != 0)
					map.put(PARAMETER_SEARCH_FIELD, mSearchField);
			}
		}

		// Session parameter
		if (sessionId > 0)
			map.put(PARAMETER_SESSION_ID, sessionId);

		// Paging parameters.
		if (start > 0 || count > 0)
		{
			map.put(PARAMETER_PAGING_START, start);
			map.put(PARAMETER_PAGING_COUNT, count);
		}

		return map;
	}

	@Override
	public String toString()
	{
		return toString(0, 0, 0);
	}

	public String toString(int sessionId, int start, int count)
	{
		StringBuilder sb = new StringBuilder(250);
		sb.append(MyApplication.getApp().UriMaker.getBaseUri());
		sb.append("/"); //$NON-NLS-1$

		sb.append(getName().replace('.', '/'));
		sb.append(Strings.QUESTION);

		// "Normal" query parameters (including gxid/start/count);
		Map<String, Object> queryValues = getQueryValues(sessionId, start, count);

		// Special "fmt" parameter.
		queryValues.put(PARAMETER_JSON_NAME, PARAMETER_JSON_VALUE);

		sb.append(valuesToUrlQuery(queryValues));
		return sb.toString();
	}

	private static String valuesToUrlQuery(Map<String, Object> values)
	{
		StringBuilder sb = new StringBuilder(150);
		for (Map.Entry<String, Object> entry : values.entrySet())
		{
			if (sb.length() != 0)
				sb.append(Strings.AND);

			sb.append(entry.getKey()).append(Strings.EQUAL).append(Services.HttpService.UriEncode(entry.getValue().toString()));
		}

		return sb.toString();
	}
}
