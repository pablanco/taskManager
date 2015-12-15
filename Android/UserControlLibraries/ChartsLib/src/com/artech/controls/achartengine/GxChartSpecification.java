package com.artech.controls.achartengine;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.controllers.ViewData;
import com.artech.controls.TimeLineControlDefinition;

public class GxChartSpecification  {

    public Vector<String> mArrayTitles = null;
    public String[] mTitles = null;

    //TimeLine
    public Vector<String> mXValue = null;
    public Vector<String> mYValue = null;
    private Vector<String> mAnnotationTitle = null;
    private Vector<String> mAnnotationText = null;

    //Pie
    public String[] mValues = null;

    public int[] mArrayData;
    public int mTotalData = 0;
    public int mTotalCountData = 0;
	private String mChartsAttribute = Strings.EMPTY;
	private String mChartsNameAttribute = Strings.EMPTY;

	public void deserializeTimeLine(String value) {

		mXValue = new Vector<String>();
	    mYValue = new Vector<String>();
	    mAnnotationTitle = new Vector<String>();
	    mAnnotationText = new Vector<String>();

		try {
			JSONArray dataChartJSONArray = new JSONArray(value);
			JSONObject dataItemObject = (JSONObject) dataChartJSONArray.get(0);
			String dataItemName = dataItemObject.optString("Name"); //$NON-NLS-1$
			if (dataItemName.length() > 0) {
				//the values is of the form:
				//[{"Name": "Peso","Data": "[{"AnnotationTitle":"Fvdvf","YValue":"80.1","AnnotationText":"","XValue":"2011-09-20"},{"AnnotationTitle":"","YValue":"80.1","AnnotationText":"","XValue":"2011-10-23"}]"}]
				deserializeTimeLine1(dataChartJSONArray);
			} else {
				//the values is of the form:
				//[{"ClientAccountBudgetValue": "1","ClientAccountBudgetAcceptedValue": "1","ClientAccountBudgetDate": "2011-12-15"},{"ClientAccountBudgetValue": "2","ClientAccountBudgetAcceptedValue": "2","ClientAccountBudgetDate": "2011-12-16"}]
				deserializeTimeLine2(dataChartJSONArray);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void deserializeTimeLine1(JSONArray dataChartJSONArray) {
		//the values is of the form:
		//[{"Name": "Peso","Data": "[{"AnnotationTitle":"Fvdvf","YValue":"80.1","AnnotationText":"","XValue":"2011-09-20"},{"AnnotationTitle":"","YValue":"80.1","AnnotationText":"","XValue":"2011-10-23"}]"}]

		mTotalData = dataChartJSONArray.length();
		mArrayTitles = new Vector<String>(mTotalData);
		mArrayData = new int[mTotalData];

		try {
			for (int i = 0; i < mTotalData; i++)
			{
				JSONObject dataItemObject;
				dataItemObject = (JSONObject) dataChartJSONArray.get(i);
				String dataItemName = dataItemObject.optString("Name"); //$NON-NLS-1$
				mArrayTitles.add(i, dataItemName);
				JSONArray dataItemData = new JSONArray(dataItemObject.getString("Data")); //$NON-NLS-1$
				if (dataItemData!=null)
				{
					mTotalCountData = dataItemData.length();
					mArrayData[i]= mTotalCountData;
					for (int j = 0; j < dataItemData.length(); j++)
					{
						JSONObject objData = dataItemData.optJSONObject(j);
						mXValue.add(objData.optString("XValue")); //$NON-NLS-1$
						mYValue.add(objData.optString("YValue")); //$NON-NLS-1$
						mAnnotationTitle.add(objData.optString("AnnotationTitle")); //$NON-NLS-1$
						mAnnotationText.add(objData.optString("AnnotationText")); //$NON-NLS-1$
					}
				}
			}

			//Titles
			mTitles =new String[mTotalData];
	        int i =0;
			for(String str : mArrayTitles)
				mTitles[i++] = str;

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	void deserializeTimeLine(ViewData data, TimeLineControlDefinition def) {
		mTotalData = def.getSeriesAttributeCollection().length;

		mTitles = def.getSeriesLabelCollection();
		if (mTitles.length == 0)
			mTitles = def.getSeriesAttributeCollection();

		List<String[]> yListValues = new Vector<String[]>();
		Vector<String> xValueAux = new Vector<String>();
		int totalRegister = data.getCount();
		for (int i = 0; i < totalRegister ; i++) {
			Entity e = data.getEntities().get(i);

			if (e.getLevel("Data") != null) // Here we realize that actually it is a AnnotatedTimeLine
			{
				deserializeTimeLine(data.getEntities());
				return;
			}

			String dateValue = e.optStringProperty(def.getTimeAttribute());
			Date date = Services.Strings.getDate(dateValue);
			if ( date != null)
				xValueAux.add(dateValue);
			String[] yValues = new String[def.getSeriesAttributeCollection().length];

			for (int j = 0; j < def.getSeriesAttributeCollection().length ; j++) {
				String yValue = e.optStringProperty(def.getSeriesAttributeCollection()[j]);
				yValues[j] = yValue;
			}
			yListValues.add(yValues);
		}
		if (yListValues.size() == 0)
			return;
		//Generate the vector X and Y
		String[] yValuesAux = yListValues.get(0);
		int totalData = yValuesAux.length * yListValues.size();

		//Set mArrayData
		mArrayData = new int[yValuesAux.length];
		for (int i = 0; i < yValuesAux.length; i++) {
			mArrayData[i] =  yListValues.size();
		}

		mXValue = new Vector<String>(totalData);
		mYValue = new Vector<String>(totalData);
		for (int i = 0; i < totalData; i++) {
			if (i < xValueAux.size())
				mXValue.add(i, xValueAux.get(i));
			else
				mXValue.add(i, Strings.EMPTY);
			mYValue.add(i, Strings.EMPTY);
		}
		for (int i = 0; i < yListValues.size(); i++) {
			String[] yValues = yListValues.get(i);
			for (int j = 0; j < yValues.length; j++) {
				mXValue.remove(i + j*(yListValues.size()));
				mXValue.add(i + j*(yListValues.size()), xValueAux.get(i));

				mYValue.remove(i + j*(yListValues.size()));
				mYValue.add(i + j*(yListValues.size()), yValues[j]);
			}
		}

	}

	private void deserializeTimeLine2(JSONArray dataChartJSONArray) {
		//the values is of the form:
		//[{"ClientAccountBudgetValue": "1","ClientAccountBudgetAcceptedValue": "1","ClientAccountBudgetDate": "2011-12-15"},{"ClientAccountBudgetValue": "2","ClientAccountBudgetAcceptedValue": "2","ClientAccountBudgetDate": "2011-12-16"}]

		List<String[]> yListValues = new Vector<String[]>();
		int totalRegister = dataChartJSONArray.length();
		Vector<String> xValueAux = new Vector<String>();
		//mArrayTitles = new Vector<String>();

		try {
			for (int i = 0; i < totalRegister; i++)
			{
				//Analyze each register
				JSONObject dataItemObject = (JSONObject) dataChartJSONArray.get(i);
				@SuppressWarnings("rawtypes")
				Iterator keys = dataItemObject.keys();
				Vector<String> yValues = new Vector<String>();
				while (keys.hasNext())
				{
					String itKey = (String) keys.next();
					String itValue = (String) dataItemObject.get(itKey);
					Date date = Services.Strings.getDate(itValue);
					if ((date!=null) && (mChartsAttribute.contentEquals(itKey)))
						xValueAux.add(itValue);
					else {
						//if (!mArrayTitles.contains(itKey))
							//mArrayTitles.add(itKey);
						yValues.add(itValue);
					}
				}

				String[] strYValues =new String[yValues.size()];
				int k =0;
				for(String str : yValues) {
					strYValues[k++] = str;
				}
				yListValues.add(strYValues);
			}

			//Generate the vector X and Y
			String[] yValuesAux = yListValues.get(0);
			int totalData = yValuesAux.length * yListValues.size();

			//Set mArrayData
			mArrayData = new int[yValuesAux.length];
			for (int i = 0; i < yValuesAux.length; i++) {
				mArrayData[i] =  yListValues.size();
			}

			mXValue = new Vector<String>(totalData);
			mYValue = new Vector<String>(totalData);
			for (int i = 0; i < totalData; i++) {
				if (i < xValueAux.size())
					mXValue.add(i, xValueAux.get(i));
				else
					mXValue.add(i, Strings.EMPTY);
				mYValue.add(i, Strings.EMPTY);
			}
			for (int i = 0; i < yListValues.size(); i++) {
				String[] yValues = yListValues.get(i);
				for (int j = 0; j < yValues.length; j++) {
					mXValue.remove(i + j*(yListValues.size()));
					mXValue.add(i + j*(yListValues.size()), xValueAux.get(i));

					mYValue.remove(i + j*(yListValues.size()));
					mYValue.add(i + j*(yListValues.size()), yValues[j]);
				}
			}

			//Set total data
			//mTotalData = mArrayTitles.size();
			mTotalData = mTitles.length;

			//Titles
			/*mTitles =new String[mTotalData];
	        int i =0;
			for(String str : mArrayTitles)
				mTitles[i++] = str;*/
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void setChartsAttribute(String chartsAttribute) {
		mChartsAttribute  = chartsAttribute;
	}

	public void setChartsNameAttribute(String chartsNameAttribute) {
		mChartsNameAttribute  = chartsNameAttribute;
	}

	public void setSeriesAttribute(String[] seriesAttribute) {
		mTitles  = seriesAttribute;
	}

	public void deserializePie(ViewData data) {
		mTotalData = data.getCount();
		//Titles
		mTitles = new String[mTotalData];
		//Values
		mValues = new String[mTotalData];

		for (int i = 0; i < data.getCount(); i++) {
	        Entity e = data.getEntities().get(i);
			mTitles[i] = e.optStringProperty(mChartsNameAttribute);
	 		mValues[i] = e.optStringProperty(mChartsAttribute);
		}
	}

	public void deserializeTimeLine(EntityList list) {
		mTotalData = list.size();
		mArrayTitles = new Vector<String>(mTotalData);
		mArrayData = new int[mTotalData];

		mXValue = new Vector<String>();
	    mYValue = new Vector<String>();
	    mAnnotationTitle = new Vector<String>();
	    mAnnotationText = new Vector<String>();

		try {
			for (int i = 0; i < mTotalData; i++)
			{
				Entity dataItemObject = list.get(i);
				String dataItemName = dataItemObject.optStringProperty("Name"); //$NON-NLS-1$
				mArrayTitles.add(i, dataItemName);
				EntityList dataItemData = dataItemObject.getLevel("Data"); //$NON-NLS-1$
				if (dataItemData!=null)
				{
					mTotalCountData = dataItemData.size();
					mArrayData[i]= mTotalCountData;
					for (int j = 0; j < dataItemData.size(); j++)
					{
						Entity objData = dataItemData.get(j);
						mXValue.add(objData.optStringProperty("XValue")); //$NON-NLS-1$
						mYValue.add(objData.optStringProperty("YValue")); //$NON-NLS-1$
						mAnnotationTitle.add(objData.optStringProperty("AnnotationTitle")); //$NON-NLS-1$
						mAnnotationText.add(objData.optStringProperty("AnnotationText")); //$NON-NLS-1$
					}
				}
			}

			//Titles
			mTitles =new String[mTotalData];
	        int i =0;
			for(String str : mArrayTitles)
				mTitles[i++] = str;


		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
