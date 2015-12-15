package com.artech.layers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

import com.artech.application.MyApplication;
import com.artech.base.controls.MappedValue;
import com.artech.base.utils.Strings;
import com.artech.common.ServiceHelper;

class RemoteServices
{
	public static LinkedHashMap<String, String> getDynamicComboValues(String service, Map<String, String> inputValues)
	{
		JSONArray jsonValues = getJsonValues(service, inputValues);
		return CommonUtils.jsonToMap(jsonValues);
	}

	public static List<String> getDependentValues(String service, Map<String, String> input)
	{
		JSONArray jsonValues = getJsonValues(service, input);
		return CommonUtils.jsonToList(jsonValues);
	}

	public static List<String> getSuggestions(String serviceName, Map<String, String> input)
	{
		JSONArray jsonValues = getJsonValues(serviceName, input);
		return CommonUtils.jsonToList(jsonValues);
	}

	public static MappedValue getMappedValue(String serviceName, Map<String, String> input)
	{
		JSONArray jsonValues = getJsonValues(serviceName, input);
		return CommonUtils.jsonToMappedValue(jsonValues);
	}

	private static JSONArray getJsonValues(String service, Map<String, String> parameters)
	{
		if (Strings.hasValue(service))
		{
			String uri = MyApplication.getApp().UriMaker.getObjectUri(service, parameters);
			return ServiceHelper.getJSONArrayFromUrl(uri);
		}
		else
			return new JSONArray();
	}
}
