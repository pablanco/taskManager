package com.artech.controls.achartengine;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import org.achartengine.chart.AbstractChart;
import org.achartengine.chart.PieChart;
import org.achartengine.chart.TimeChart;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

import com.artech.base.metadata.enums.DataTypes;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.model.EntityList;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.controllers.ViewData;
import com.artech.controls.GxPieControl;
import com.artech.controls.GxTimeLineControl;
import com.artech.controls.PieChartControlDefinition;
import com.artech.controls.TimeLineControlDefinition;
import com.artech.controls.TimeLineControlDefinition.TimePeriod;
import com.artech.utils.Cast;

public class GxChartEngine extends LinearLayout {
	private TimeLineControlDefinition mTimeLineDefinition;
	private PieChartControlDefinition mPieDefinition;
    private String mChartType = Strings.EMPTY;
    private String mTitle = Strings.EMPTY;

	//Range dates to draw
	private long mLongDateInit;
	private long mLongDateEnd;
	private boolean mChangeMargins = false;
    //Display properties
	private float mDisplayWidth;
	private float mDisplayHeight;
    private int[] mTotalColorsLegend = new int[] { Color.argb(191, 0, 255, 0), Color.argb(191, 255, 0, 0), Color.argb(191, 0, 0, 255), Color.argb(191, 255, 255, 0), Color.argb(191, 255, 165, 0), Color.argb(191, 0, 255, 255), Color.argb(191, 255, 0, 255), Color.argb(191, 0, 128, 0), Color.argb(191, 255, 255, 255), Color.argb(191, 128, 0, 128) };


	private Context mContext;

	//View GridView, GraphicalView and SeekBar
	private GridView mGridCharts;
	private GxGraphicalViewControl mCharts;
	private SeekBar mSeekBar;
	//ProgressSeekBar properties
	private long[] mProgressSeekBar = new long[100];
	private long longDateMargin = 0;
	private int mCurrentProgressValue;

	private GxChatsGridAdapter mGridAdapter;
	private GxTimeLineControl mTimeLineControl;

	//Charts type
    private static final String TIMELINE = "timeline"; //$NON-NLS-1$
    private static final String PIE = "pie"; //$NON-NLS-1$

	private boolean isData = true;

	public GxChartEngine(Context context) {
		super(context);
	}
	
	public GxChartEngine(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public GxChartEngine(Context context, LayoutItemDefinition definition)
	{
		super(context);
		CompatibilityHelper.disableHardwareAcceleration(this); // Temporary. Remove this line when achartengine is updated to latest version.

		mContext = context;

		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    if (inflater != null)
	       inflater.inflate(com.artech.controls.achartengine.R.layout.chartscontrol, this, true);

		mCharts = (GxGraphicalViewControl) findViewById(com.artech.controls.achartengine.R.id.chartsControlGraphic);
		mSeekBar = (SeekBar) findViewById(com.artech.controls.achartengine.R.id.seekBarInCharts);
		mGridCharts = (GridView) findViewById(com.artech.controls.achartengine.R.id.gridviewCharts);

		mGridCharts.setOnItemClickListener(new OnItemClickListener() {
	        @Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

	        	mGridAdapter.setNothingSelect();
	        	mGridAdapter.setCurrentSelect(position);
	        	mGridCharts.setAdapter(mGridAdapter);
	    		mGridAdapter.notifyDataSetChanged();

	    		OnClickTimePeriod(mTimeLineDefinition.getPeriod(position));
	        }
	    });
		setControlInfo(definition, definition.getControlInfo());
		setChartsDataValue(definition);
	}

	public int[] getTotalColorsLegend() {
		return mTotalColorsLegend;
	}


	public void setControlInfo(LayoutItemDefinition definition, ControlInfo info) {
		mSeekBar.setMax(99);
		mSeekBar.incrementProgressBy(1);
	    mTitle = info.getTranslatedProperty("@SDChartsTitle"); //$NON-NLS-1$
	    mChartType = info.optStringProperty("@SDChartsChartType"); //$NON-NLS-1$

	    if (isTimeLine())
        {
	    	mTimeLineDefinition = new TimeLineControlDefinition(mContext, definition);
        }
    	if (isPie())
        {
    		mPieDefinition = new PieChartControlDefinition(mContext, definition);
        }
	}

	public long getLongDateInit() {
		return mLongDateInit;
	}

	public long getLongDateEnd() {
		return mLongDateEnd;
	}

	public TimeChart setup(GxChartSpecification chartSpec, GxTimeLineControl timeLineControl) {
		timeLineControl.setColors(mTimeLineDefinition.getTotalColors(), getTotalColorsLegend());
		timeLineControl.setSizeDisplay(mDisplayWidth, mDisplayHeight);
		timeLineControl.obtainInfo();
		int totalCountData = timeLineControl.getTotalCountData();


		int selectPeriod = selectTimePeriod(chartSpec.mXValue, totalCountData);
		TimePeriod period = mTimeLineDefinition.getPeriod(selectPeriod);
		if (period != null)
			setTimePeriod(period);
		if ((mLongDateInit < timeLineControl.ObtainMinDate().getTime()) && (mCurrentProgressValue != 0))
		{
			mLongDateInit = timeLineControl.ObtainMinDate().getTime();
			mLongDateEnd = timeLineControl.ObtainMaxDate().getTime();
		}
		mGridAdapter.setNothingSelect();
		mGridAdapter.setCurrentSelect(selectPeriod);

		return timeLineControl.generateTimeLineChart(mLongDateInit, mLongDateEnd);
	}


    private void OnClickTimePeriod(TimePeriod period)
    {
    	mCurrentProgressValue = 99;
		mSeekBar.setProgress(mCurrentProgressValue);
    	setTimePeriod(period);
    	repaintChart(getLongDateInit(), getLongDateEnd());
    }

	private void repaintChart(long longDateInit, long longDateEnd)
	{
		if (isData) {
			if ((longDateInit < mTimeLineControl.ObtainMinDate().getTime()) && (mCurrentProgressValue != 0))
			{
				mTimeLineControl.getRenderer().setXAxisMin(mTimeLineControl.ObtainMinDate().getTime());
				mTimeLineControl.getRenderer().setXAxisMax(mTimeLineControl.ObtainMaxDate().getTime());
			}
			else
			{
				mTimeLineControl.getRenderer().setXAxisMin(longDateInit);
				mTimeLineControl.getRenderer().setXAxisMax(longDateEnd);
			}

			mCharts.invalidate();
		}
	}

	private void updateProgressSeekBar(long longDateInit, long longDateEnd)
	{
		longDateMargin = longDateEnd - longDateInit;
		long diffLongDate = (mTimeLineControl.ObtainMaxDate().getTime()) - (mTimeLineControl.ObtainMinDate().getTime() + longDateMargin);
		long stepLongDate = diffLongDate / 99;
		long currentStepLongDate = 0;
		for (int i = 0; i < 100; i++)
		{
			mProgressSeekBar[i] = (mTimeLineControl.ObtainMinDate().getTime() + longDateMargin) + currentStepLongDate;
			currentStepLongDate += stepLongDate;
		}
		if (mTimeLineControl.ObtainMinDate().getTime() >= longDateInit)
		{
			mCurrentProgressValue = 50;
			mSeekBar.setProgress(mCurrentProgressValue);
			mSeekBar.setEnabled(false);
			mSeekBar.setVisibility(View.GONE);
		}
		else
		{
			mSeekBar.setEnabled(true);
			mSeekBar.setVisibility(View.VISIBLE);
		}
	}

	public boolean isTimeLine()
	{
		return mChartType.equalsIgnoreCase(TIMELINE) || (mChartType.length() == 0);
	}

	public boolean isPie()
	{
		return mChartType.equalsIgnoreCase(PIE);
	}

	public void update(ViewData data) {
		GxChartSpecification chartSpec = new GxChartSpecification();
		if (isPie()) {
	   		chartSpec.setChartsAttribute(mPieDefinition.getChartsValueAttribute());
    		chartSpec.setChartsNameAttribute(mPieDefinition.getChartsNameAttribute());
    		chartSpec.deserializePie(data);
 		} else if (isTimeLine())
 		{
 	//		if (mTimeLineDefinition.IsAnnotatedTimeLineSDT())
 		//		chartSpec.deserializeTimeLine(data.getEntities().toString());
 		//	else
 				chartSpec.deserializeTimeLine(data, mTimeLineDefinition);
 		}
   		selectCharts(chartSpec);
 	}

	public void setGx_Value(String value) {
		if (value.length() > 0) {
			isData = true;
		} else {
			isData = false;
		}

		mCurrentProgressValue = 99;
		mSeekBar.setProgress(mCurrentProgressValue);
		GxChartSpecification chartSpec = new GxChartSpecification();
		if (isTimeLine())
        {
			chartSpec.setChartsAttribute(mTimeLineDefinition.getTimeAttribute());
			if (mTimeLineDefinition.getSeriesLabelCollection() != null)
				chartSpec.setSeriesAttribute(mTimeLineDefinition.getSeriesLabelCollection());
			else
				chartSpec.setSeriesAttribute(mTimeLineDefinition.getSeriesAttributeCollection());
			chartSpec.deserializeTimeLine(value);
        }
		selectCharts(chartSpec);
	}

	protected void init() {
		// Screen resolution
		DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
		mDisplayWidth = displayMetrics.widthPixels;
		mDisplayHeight = displayMetrics.widthPixels;

		mSeekBar.setMax(99);
		mSeekBar.incrementProgressBy(1);
		mSeekBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

			   @Override
			   public void onProgressChanged(SeekBar seekBar, int progress,
			     boolean fromUser) {

				   if (fromUser)
				   {
					   mCurrentProgressValue = progress;
					   long currentMaxLongDate = mProgressSeekBar[progress];
					   long cuurentMinLongdate = currentMaxLongDate - longDateMargin;
					   repaintChart(cuurentMinLongdate, currentMaxLongDate);
				   }

			   }

			   @Override
			   public void onStartTrackingTouch(SeekBar seekBar) {
			   }

			   @Override
			   public void onStopTrackingTouch(SeekBar seekBar) {
			   }
		});

	}

	public void adapterView() {
		mSeekBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mSeekBar.setPadding(10, 0, 10, 0);
		mChangeMargins = true;
	}

    private void selectCharts(GxChartSpecification chartSpec)
    {
    	AbstractChart chart = null;
    	if (isTimeLine())
        {
    	   	//setAdapter in Gridcharts
    	    mGridAdapter = new GxChatsGridAdapter(mContext, mTimeLineDefinition.getTimePeriodCharSequence());
    		mGridCharts.setAdapter(mGridAdapter);
    		mGridAdapter.notifyDataSetChanged();
    		mGridCharts.setNumColumns(mTimeLineDefinition.getTimePeriodCharSequence().length);

    		chart = executeTimeline(chartSpec);
        }
    	if (isPie())
        {
    		chart = executePie(chartSpec);
        }

        createGraphicalViewChart(chart);
    }

    private TimeChart executeTimeline(GxChartSpecification chartSpec)
    {
    	mSeekBar.setVisibility(View.VISIBLE);
		LinearLayout mLinearLayout = (LinearLayout) findViewById(com.artech.controls.achartengine.R.id.chartsLinearLayout);
		mLinearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mLinearLayout.setPadding(0, 0, 0, (int)(mDisplayHeight * 0.01));
		if (!mChangeMargins)
		{
			mLinearLayout = (LinearLayout) findViewById(com.artech.controls.achartengine.R.id.linearLayoutHeaderCharts);
			mLinearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int)(mDisplayHeight * 0.03)));
		}

    	mTimeLineControl = new GxTimeLineControl(chartSpec, mTitle, mTimeLineDefinition.getXAlign(), mTimeLineDefinition.getYAlign());
    	TimeChart timeChart = setup(chartSpec, mTimeLineControl);

		return timeChart;
    }

    private PieChart executePie(GxChartSpecification chartSpec)
    {
    	mSeekBar.setVisibility(View.GONE);
    	mGridCharts.setVisibility(View.GONE);
		GxPieControl pieControl = new GxPieControl(chartSpec, mTitle, mPieDefinition.isShowInPercentage());
    	pieControl.setColors(mTotalColorsLegend);
		return pieControl.generatePieChart();
	}

    private void createGraphicalViewChart(AbstractChart chart) {
    	if (chart!=null)
    		mCharts.CreateGraphicalView(chart);
	}

    private int selectTimePeriod(Vector<String> xValue, int totalCountData) {
		if (totalCountData > 0 && mTimeLineDefinition.getTimePeriodCollection().size() > 2)
		{
			return obtainSelectPeriod(xValue);
		}
		else
		{
			return mTimeLineDefinition.getTimePeriodCollection().size() - 1;
		}
    }

    private void setTimePeriod(TimePeriod period) {
    	int valuePeriod = period.value;
    	if (valuePeriod!=0)
    	{
	    	valuePeriod = valuePeriod * -1;
			Calendar cal=Calendar.getInstance();
			cal.setTime(mTimeLineControl.ObtainMaxDate());
			cal.add(period.date, valuePeriod);
			Date date = cal.getTime();
			mLongDateInit = date.getTime();
			mLongDateEnd = mTimeLineControl.ObtainMaxDate().getTime();
    	} else {
    		//Click in "Max"
			mLongDateInit = mTimeLineControl.ObtainMinDate().getTime();
			mLongDateEnd = mTimeLineControl.ObtainMaxDate().getTime();
    	}

		updateProgressSeekBar(mLongDateInit, mLongDateEnd);

    }

    private int obtainSelectPeriod(Vector<String> valuesDate)
    {
    	Date minDate = null;
    	Date maxDate = null;
    	for (int i = 0; i < valuesDate.size(); i++)
    	{
    		Date date = Services.Strings.getDate(valuesDate.get(i));
			if (date != null)
			{
				if (maxDate == null || date.after(maxDate))
					maxDate = date;
				if (minDate == null || date.before(minDate))
					minDate = date;
			}
    	}

    	long spanInMs = (maxDate != null && minDate != null ? maxDate.getTime() - minDate.getTime() : 0);
    	double spanInDays = Math.floor(spanInMs / (1000 * 60 * 60 * 24));
    	return selectPeriodAccordingDays(spanInDays);
    }

    private int selectPeriodAccordingDays(double days) {
    	int periodSelect = 0;
    	for (int i = mTimeLineDefinition.getTimePeriodCollection().size() - 1; i >= 0; i--)
    	{
    		TimePeriod period = mTimeLineDefinition.getPeriod(i);
    		if (!period.description.equalsIgnoreCase("Max")) //$NON-NLS-1$
        	{
    			if (period.date == Calendar.DATE && days < period.value) {
    					periodSelect = i;
    			}
        		if (period.date == Calendar.WEEK_OF_YEAR && days < 7*(period.value)) {
    					periodSelect = i;
        		}
    			if (period.date == Calendar.MONTH && days < 30*(period.value)) {
    					periodSelect = i;
    			}
    			if (period.date == Calendar.YEAR && days < 365*(period.value)) {
    					periodSelect = i;
    			}
        	}
    	}
    	//To PesoBook select always item 2 in the period if periodSelect > 2 else periodSelect="1y" (last -1) or "Max" (last)
    	if (periodSelect > 2)
    		periodSelect = 2;
    	else
    	{
    		if (mTimeLineDefinition.getTimePeriodCollection().size()>2 && days > 365)
    		{
    			mCurrentProgressValue = 99;
    			mSeekBar.setProgress(mCurrentProgressValue);
    			periodSelect = mTimeLineDefinition.getTimePeriodCollection().size() - 2;
    		}
    		else
    			periodSelect = mTimeLineDefinition.getTimePeriodCollection().size() - 1;
    	}
    	return periodSelect;
    }


	public void setChartsDataValue(LayoutItemDefinition layoutItemDefinition) {
		if (isTimeLine()) {
			Vector<LayoutItemDefinition> dataItems = new Vector<LayoutItemDefinition>();
			layoutItemDefinition.getLayout().getDataItems(layoutItemDefinition, dataItems);

			ArrayList<String> seriesItems = new ArrayList<String>();
			boolean loadSeries = false;
			if (mTimeLineDefinition.getSeriesAttributeCollection().length == 0)
				loadSeries = true;
			if (loadSeries || !Services.Strings.hasValue(mTimeLineDefinition.getTimeAttribute())) {
				for(int i = 0; i < dataItems.size(); i++)
				{
					LayoutItemDefinition item = dataItems.elementAt(i);
		    		if (item.getDataItem().getType().equalsIgnoreCase(DataTypes.date) || item.getDataItem().getType().equalsIgnoreCase(DataTypes.date))
		    				if (!Services.Strings.hasValue(mTimeLineDefinition.getTimeAttribute()))
		    					mTimeLineDefinition.setTimeAttribute(item.getName());
		    		if (loadSeries) {
		    			if (item.getDataItem().getType().equalsIgnoreCase(DataTypes.numeric))
		    				seriesItems.add(item.getName());
		    		}

				}
				if (loadSeries) {
					String [] values = new String[seriesItems.size()];
					mTimeLineDefinition.setSeriesAttributeCollection(seriesItems.toArray(values));
				}
			}

		}

		if (isPie())
		{
			Vector<LayoutItemDefinition> dataItems = new Vector<LayoutItemDefinition>();
			layoutItemDefinition.getLayout().getDataItems(layoutItemDefinition, dataItems);
			for(int i = 0; i < dataItems.size(); i++)
			{
				if (Services.Strings.hasValue(mPieDefinition.getChartsValueAttribute())
						&& Services.Strings.hasValue(mPieDefinition.getChartsNameAttribute()))
							return;

				LayoutItemDefinition item = dataItems.elementAt(i);
	    		if (!item.getDataItem().getType().equalsIgnoreCase(DataTypes.numeric)
	    				&& !Services.Strings.hasValue(mPieDefinition.getChartsNameAttribute()))
	    		{
	    			if (item.optStringProperty("@attribute").startsWith("&")) //$NON-NLS-1$ //$NON-NLS-2$
	    				mPieDefinition.setChartsNameAttribute(item.getCaption());
	    			else
	    				mPieDefinition.setChartsNameAttribute(item.getName());
	    		}

	    		if (!Services.Strings.hasValue(mPieDefinition.getChartsValueAttribute()))
	    		{
	    			if (item.getDataItem().getType().equalsIgnoreCase(DataTypes.numeric))
	    				mPieDefinition.setChartsValueAttribute(item.getName());
	    		}
			}
		}
	}

	public void setValue(Object value) {
		EntityList list = Cast.as(EntityList.class,value);
		if (list != null)
		{
			mCurrentProgressValue = 99;
			mSeekBar.setProgress(mCurrentProgressValue);
			GxChartSpecification chartSpec = new GxChartSpecification();
			if (isTimeLine())
	        {
				chartSpec.setChartsAttribute(mTimeLineDefinition.getTimeAttribute());
				if (mTimeLineDefinition.getSeriesLabelCollection() != null)
					chartSpec.setSeriesAttribute(mTimeLineDefinition.getSeriesLabelCollection());
				else
					chartSpec.setSeriesAttribute(mTimeLineDefinition.getSeriesAttributeCollection());
				chartSpec.deserializeTimeLine(list);
	        }
			selectCharts(chartSpec);
		}

	}


}
