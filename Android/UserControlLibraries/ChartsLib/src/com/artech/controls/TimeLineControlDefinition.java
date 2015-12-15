package com.artech.controls;

import java.util.Calendar;
import java.util.Vector;

import org.achartengine.renderer.XYMultipleSeriesRenderer.XAlign;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;

import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;

public class TimeLineControlDefinition {



	//Charts ControlInfo
	private String mXAxisPosition = Strings.EMPTY;
    private String mYAxisPosition = Strings.EMPTY;
    private String mChartsTimeAttribute = Strings.EMPTY;
    private String[] mSeriesAttributeCollection;
    private String[] mSeriesLabelCollection;
    private String[] mSeriesAttributeFieldSpecifierCollection;
	private boolean mIsAnnotatedTimeLineSDT;
    private XAlign mXAlign = XAlign.TOP;
    private Align mYAlign = Align.LEFT;

    //TimeLine period
    private static String DATE = "d"; //$NON-NLS-1$
    private static String WEEK = "w"; //$NON-NLS-1$
    private static String MONTH = "m"; //$NON-NLS-1$
    private static String YEAR = "y"; //$NON-NLS-1$

    //Charts colors
    private int[] mTotalColors = new int[] { Color.argb(112, 0, 255, 0), Color.argb(72, 255, 0, 0), Color.argb(72, 0, 0, 255), Color.argb(72, 255, 255, 0), Color.argb(128, 255, 165, 0), Color.argb(72, 0, 255, 255), Color.argb(128, 255, 0, 255), Color.argb(128, 0, 128, 0), Color.argb(72, 255, 255, 255), Color.argb(72, 128, 0, 128) };

    //Charts TimePeriod
    private CharSequence[] mTimePeriodCharSequence = { "1d", "5d", "1m", "3m", "6m", "1y", "Max"};  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

    public static class TimePeriod {
        public String description;
        public int date;
        public int value;
    }
    private Vector<TimePeriod> mTimePeriodCollection;



	private ControlPropertiesDefinition mProps;

	public TimeLineControlDefinition(Context context, LayoutItemDefinition def)
	{
		mProps = new ControlPropertiesDefinition(def);
		ControlInfo info = def.getControlInfo();
		obtainTimePeriodCollection(info);
    	String[] tempSerie = obtainSeriesCollection(info.optStringProperty("@SDChartsSeriesAttributeCollection")); //$NON-NLS-1$
    	mSeriesAttributeFieldSpecifierCollection = obtainSeriesCollection(info.optStringProperty("@SDChartsSeriesFieldCollection")); //$NON-NLS-1$

    	if (mSeriesAttributeFieldSpecifierCollection.length > 0) {
    	  	mSeriesAttributeCollection = new String[mSeriesAttributeFieldSpecifierCollection.length];

    		for (int i = 0; i < mSeriesAttributeFieldSpecifierCollection.length ; i++)
	    	{
	    		String att = tempSerie[0];
	    		String field = mSeriesAttributeFieldSpecifierCollection[i];
	    		if (Services.Strings.hasValue(field)) {
	    			mSeriesAttributeCollection[i] = mProps.getDataExpression(att, field);
	    		}
	    		else
	    			mSeriesAttributeCollection[i] = att;
	    	}
	    }
    	else
    		mSeriesAttributeCollection = tempSerie;

    	mSeriesLabelCollection = obtainSeriesCollection(info.optStringProperty("@SDChartsSeriesLabelCollection")); //$NON-NLS-1$
    	mXAxisPosition = info.optStringProperty("@SDChartsXAxisPosition"); //$NON-NLS-1$
	    mYAxisPosition = info.optStringProperty("@SDChartsYAxisPosition"); //$NON-NLS-1$
	    if (!Services.Strings.hasValue(info.optStringProperty("@SDChartsTimeField")) && Services.Strings.hasValue("@SDChartsTimeAttribute"))
	    	mIsAnnotatedTimeLineSDT = true;

	    mChartsTimeAttribute = mProps.readDataExpression("@SDChartsTimeAttribute", "@SDChartsTimeField"); //$NON-NLS-1$
	    if (mChartsTimeAttribute == null || mChartsTimeAttribute.equalsIgnoreCase(NONE))
	    	mChartsTimeAttribute = "";
	}

	private final static String NONE = "(none)"; //$NON-NLS-1$

	private String[] obtainSeriesCollection(String strSeries) {
		try {
			strSeries = strSeries.replace(NONE, "");
			if (strSeries.length() !=0)
				return strSeries.replace(Strings.SPACE, Strings.EMPTY).split(Strings.COMMA);
		}
		catch (Exception e) { }
		return new String[0];
	}


	private void obtainTimePeriodCollection(ControlInfo info) {
		String strTimePeriod = info.optStringProperty("@SDChartsTimePeriodCollection"); //$NON-NLS-1$

		//Add period Max
		if (!strTimePeriod.equalsIgnoreCase("max")) //$NON-NLS-1$
			strTimePeriod = strTimePeriod.concat(",Max"); //$NON-NLS-1$

		if (strTimePeriod.length() !=0)
			mTimePeriodCharSequence = strTimePeriod.replace(Strings.SPACE, Strings.EMPTY).split(Strings.COMMA);

		if (mXAxisPosition.equalsIgnoreCase("Bottom")) //$NON-NLS-1$
			mXAlign = XAlign.BOTTOM;
		if (mYAxisPosition.equalsIgnoreCase("Right")) //$NON-NLS-1$
			mYAlign = Align.RIGHT;

		mTimePeriodCollection = new Vector<TimePeriod>();
		try {
		    for (int i = 0; i < mTimePeriodCharSequence.length; i++)
		    {
		    	TimePeriod period = obtainPeriodTime(i);
		    	mTimePeriodCollection.add(period);
		    }
		}
		catch (Exception e) { }
	}


	private TimePeriod obtainPeriodTime(int position)
	{
		TimePeriod period = new TimePeriod();
    	period.description = mTimePeriodCharSequence[position].toString();
    	if (!period.description.equalsIgnoreCase("Max")) //$NON-NLS-1$
    	{
	    	String strDate = period.description.substring(period.description.length()-1, period.description.length());
	    	if (strDate.equalsIgnoreCase(DATE))
	    		period.date = Calendar.DATE;
    		if (strDate.equalsIgnoreCase(WEEK))
    			period.date = Calendar.WEEK_OF_YEAR;
			if (strDate.equalsIgnoreCase(MONTH))
				period.date = Calendar.MONTH;
			if (strDate.equalsIgnoreCase(YEAR))
				period.date = Calendar.YEAR;
	    	period.value = Integer.parseInt(period.description.substring(0, period.description.length()-1));
    	}
    	return period;
	}

	public TimePeriod getPeriod(int position) {
		if (position >= 0 && position < mTimePeriodCollection.size())
			return mTimePeriodCollection.get(position);
		return null;
	}

	public XAlign getXAlign() {
		return mXAlign;
	}

	public Align getYAlign() {
		return mYAlign;
	}

	public int[] getTotalColors() {
		return mTotalColors;
	}

	public String getTimeAttribute() {
		return mChartsTimeAttribute;
	}

	public String[] getSeriesLabelCollection() {
		return mSeriesLabelCollection;
	}

	public String[] getSeriesAttributeCollection() {
		return mSeriesAttributeCollection;
	}

	public Vector<TimePeriod> getTimePeriodCollection() {
		return mTimePeriodCollection;
	}

	public CharSequence[] getTimePeriodCharSequence() {
		return mTimePeriodCharSequence;
	}

	public void setTimeAttribute(String name) {
		mChartsTimeAttribute = name;
	}

	public void setSeriesAttributeCollection(String[] array) {
		mSeriesAttributeCollection = array;
	}


	public boolean IsAnnotatedTimeLineSDT() {
		return mIsAnnotatedTimeLineSDT;
	}


	public void setIsAnnotatedTimeLineSDT(boolean mIsAnnotatedTimeLineSDT) {
		this.mIsAnnotatedTimeLineSDT = mIsAnnotatedTimeLineSDT;
	}

}
