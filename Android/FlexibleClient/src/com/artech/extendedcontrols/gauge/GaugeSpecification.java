package com.artech.extendedcontrols.gauge;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.Color;

import com.artech.base.utils.Strings;
import com.artech.utils.ThemeUtils;

public class GaugeSpecification {

	public int Height;
	public float MaxValue;
	public float MinValue;
	public float CurrentValue;
	public boolean ShowMinMax;

	public ArrayList<RangeSpec> Ranges = new ArrayList<RangeSpec>();

	public void deserialize(String value) {
		/*
		{"Title":0,"Height":10,"MaxValue":80.0,"MinValue":60.0,"Value":75.0,"ShowMinMax":false,"Ranges":[{"Color":"#0000FF","Name":"Baja","Length":7.0},{"Color":"#008000","Name":"Media","Length":7.0},{"Color":"#660000","Name":"Alta","Length":6.0}]}
		*/
		try {
			JSONObject obj = new JSONObject(value);
			Height = (int) obj.optDouble("Height"); //$NON-NLS-1$
			MaxValue = (float) obj.optDouble("MaxValue", 100); //$NON-NLS-1$
			MinValue = (float) obj.optDouble("MinValue", 0); //$NON-NLS-1$
			CurrentValue = (float) obj.optDouble("Value", 50); //$NON-NLS-1$
			ShowMinMax = obj.optBoolean("ShowMinMax"); //$NON-NLS-1$

			JSONArray ranges = obj.optJSONArray("Ranges"); //$NON-NLS-1$
			for (int i = 0; i < ranges.length() ; i++) {
				JSONObject range = ranges.getJSONObject(i);
				RangeSpec rangeSpec = new RangeSpec();
				rangeSpec.Name = range.optString("Name"); //$NON-NLS-1$
				rangeSpec.Length = (float) range.optDouble("Length"); //$NON-NLS-1$
				String colorStr = range.optString("Color", "red"); //$NON-NLS-1$ //$NON-NLS-2$
				if (!colorStr.startsWith("#")) //$NON-NLS-1$
					colorStr = "#" + colorStr; //$NON-NLS-1$
				rangeSpec.Color = ThemeUtils.getColorId(colorStr);
				Ranges.add(rangeSpec);
			}
		} catch (Exception e) {
			MaxValue = 100;
			MinValue = 0;
			CurrentValue = 0;
			Height = 10;
			RangeSpec rangeSpec = new RangeSpec();
			rangeSpec.Color = Color.RED;
			rangeSpec.Length = 100;
			if (value.length() != 0)
				rangeSpec.Name = "Wrong Json Format"; //$NON-NLS-1$
			else
				rangeSpec.Name = Strings.EMPTY;
			Ranges.add(rangeSpec);
		}


	}
}
