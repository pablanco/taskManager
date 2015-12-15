package com.artech.controls;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.achartengine.chart.PointStyle;
import org.achartengine.chart.TimeChart;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer.XAlign;
import org.achartengine.renderer.XYSeriesRenderer;

import android.graphics.Color;
import android.graphics.Paint.Align;

import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.controls.achartengine.GxChartHelper;
import com.artech.controls.achartengine.GxChartSpecification;

public class GxTimeLineControl {

	private final GxChartSpecification mChartSpec;
	private List<Date[]> mDates = new ArrayList<Date[]>();
	private List<double[]> mValues = new ArrayList<double[]>();
	private double mMaxDouble = Double.MIN_VALUE;
	private double mMinDouble = Double.MAX_VALUE;
	private double mMargin = 0;
	private float mDisplayWidth;
	private float mDisplayHeight;
	private final String mTitle;
	private int[] mTotalColors;
    private int[] mTotalColorsLegend;
    private XYMultipleSeriesRenderer mRenderer;
	private final XAlign mXAlign;
    private final Align mYAlign;
    private int mTotalCountData = 0;


	public GxTimeLineControl(GxChartSpecification chartSpec, String title, XAlign xAlign, Align yAlign)
	{
		mChartSpec = chartSpec;
		mTitle = title;
		mXAlign = xAlign;
	    mYAlign = yAlign;
	}

	public void setColors(int[] totalColors, int[] totalColorsLegend) {
		mTotalColors = totalColors;
		mTotalColorsLegend = totalColorsLegend;
	}

	public void setSizeDisplay(float displayWidth, float displayHeight) {
		mDisplayWidth = displayWidth;
		mDisplayHeight = displayHeight;
	}

	public void obtainInfo()
	{
		//Obtain Dates and values
		double previusMaxDouble = Double.MIN_VALUE;
		double previusMinDouble = Double.MAX_VALUE;
		mTotalCountData = 0;
		if (mChartSpec != null && mChartSpec.mArrayData != null)
		{
			for (int i = 0; i < mChartSpec.mArrayData.length; i++)
			{
				int mCurrentTotalCountData = mChartSpec.mArrayData[i];
				mDates.add(new Date[mCurrentTotalCountData]);
				mValues.add(new double[mCurrentTotalCountData]);
		        for (int j = 0; j < mCurrentTotalCountData; j++)
		        {
					String xValueDate = mChartSpec.mXValue.get(mTotalCountData + j);
					String yValue = mChartSpec.mYValue.get(mTotalCountData + j);
					
					if (Services.Strings.isDateFormatValid(xValueDate, "yyyy-MM-dd'T'HH:mm:ss")) { //$NON-NLS-1$
						mDates.get(i)[j] = Services.Strings.getDateTime(xValueDate);
					} else if (Services.Strings.isDateFormatValid(xValueDate, "yyyy-MM-dd")) { //$NON-NLS-1$
						mDates.get(i)[j] = Services.Strings.getDate(xValueDate);
					} else {
						mDates.get(i)[j] = new Date(0);
					}

					double doubleYValue = (yValue == null || !Strings.hasValue(yValue)) ? 0.0 : Double.parseDouble(yValue);
					mValues.get(i)[j] = doubleYValue;

					if (doubleYValue > mMaxDouble)
					{
						previusMaxDouble = mMaxDouble;
						mMaxDouble = doubleYValue;
					} else {
						if (doubleYValue > previusMaxDouble)
						{
							previusMaxDouble = doubleYValue;
						}
					}
					if (doubleYValue < mMinDouble)
					{
						previusMinDouble = mMinDouble;
						mMinDouble = doubleYValue;
					} else {
						if (doubleYValue < previusMinDouble)
						{
							previusMinDouble = doubleYValue;
						}
					}
				}
		        mTotalCountData = mTotalCountData + mCurrentTotalCountData;
			}

			//Order Dates
			orderDates();

			//Obtain the margin to the view in coordinate Y
			if ((previusMinDouble - mMinDouble) < (mMaxDouble - previusMaxDouble))
			{
				mMargin = previusMinDouble - mMinDouble;
				/*if (mMargin == 0)
					mMargin = mMaxDouble - previusMaxDouble;*/
			} else {
				mMargin = mMaxDouble - previusMaxDouble;
				/*if (mMargin == 0)
					mMargin = previusMinDouble - mMinDouble;*/
			}

			mTotalCountData = mChartSpec.mTotalCountData;
		}
    }

    public int getTotalCountData() {
		return mTotalCountData;
	}

	private void orderDates()
    {
    	List<Date[]> mDatesOrder = new ArrayList<Date[]>();
    	List<double[]> mValuesOrder = new ArrayList<double[]>();

    	Date[] serieDate;
    	double[] serieValue;
    	Date auxDate;
    	double auxValue;

    	for(int i = 0; i < mDates.size(); i++)
    	{
    		serieDate = mDates.get(i);
    		serieValue = mValues.get(i);
    		if (serieDate.length > 0)
			{
		    	for(int j = 1; j < serieDate.length; j++)
		    	{
		    		int k;
		    		auxDate = serieDate[j];
		    		auxValue = serieValue[j];
		    		for(k = j-1; k >= 0; k--)
		    		{
		    			if(auxDate.after(serieDate[k]))
		    			{
		    				serieDate[k+1] = auxDate;
		    				serieValue[k+1] = auxValue;
		    				break;
		    			}
		    			else
		    			{
		    				serieDate[k+1] = serieDate[k];
		    				serieValue[k+1] = serieValue[k];
		    			}
		    		}
		    		if(k == -1)
		    		{
		    			serieDate[0] = auxDate;
		    			serieValue[0] = auxValue;
		    		}
		    	}
			}
	    	mDatesOrder.add(serieDate);
	    	mValuesOrder.add(serieValue);
    	}
    	mDates = mDatesOrder;
    	mValues = mValuesOrder;
    }

	public XYMultipleSeriesRenderer getRenderer() {
		return mRenderer;
	}

	public void setRenderer(XYMultipleSeriesRenderer mRenderer) {
		this.mRenderer = mRenderer;
	}

	public TimeChart generateTimeLineChart(long longDateInit, long longDateEnd) {

		if (mChartSpec.mTotalData==0)
			return null;
    	int xLabels = (int)(mDisplayWidth /100);
    	if (mDisplayWidth < 400)
    		xLabels = (int)(mDisplayWidth /50);

    	int yLabels = (int)(mDisplayHeight /100);
    	if (mDisplayHeight < 600)
    		yLabels = (int)(mDisplayHeight /50);

    	int length = mChartSpec.mTotalData;

        int[] colors = new int[length];
        int[] colorsLegend = new int[length];
        PointStyle[] styles = new PointStyle[length];
        for (int i = 0; i < length; i++)
        {
        	colors[i] = mTotalColors[i % mTotalColors.length];
        	colorsLegend[i] = mTotalColorsLegend[i % mTotalColorsLegend.length];
        	styles[i] = PointStyle.POINT;
        }

        boolean deviceMovilSmall = false;
        if ((mDisplayWidth < 400) && (mDisplayHeight < 600))
        	deviceMovilSmall = true;
    	mRenderer = GxChartHelper.buildRenderer(colors, colorsLegend, styles, deviceMovilSmall);
        GxChartHelper.setChartSettings(mRenderer, mTitle, Strings.EMPTY, Strings.EMPTY, longDateInit, longDateEnd, mMinDouble - mMargin, mMaxDouble + mMargin, Color.GRAY, Color.LTGRAY, mXAlign, mYAlign);
        mRenderer.setXLabels(xLabels);
        mRenderer.setYLabels(yLabels);
        if ((mDisplayWidth < 400) || (mDisplayHeight < 600))
        	mRenderer.setLabelsTextSize((int)(mDisplayWidth /30));
        mRenderer.setShowGrid(true);
        mRenderer.setPanEnabled(false, false);
        mRenderer.setZoomEnabled(false, false);
        length = mRenderer.getSeriesRendererCount();
        for (int i = 0; i < length; i++) {
          XYSeriesRenderer seriesRenderer = (XYSeriesRenderer) mRenderer.getSeriesRendererAt(i);
          seriesRenderer.setFillBelowLine(true);
          seriesRenderer.setFillBelowLineColor(colors[i]);
          seriesRenderer.setLineWidth(2.5f);
        }
        try {
			//return GxChartHelper.getTimeChart(GxChartHelper.buildDateDataset(mChartSpec.mTitles, mDates, mValues), mRenderer, null);     //$NON-NLS-1$
        	//return GxChartHelper.getTimeChart(GxChartHelper.buildDateDataset(mChartSpec.mTitles, mDates, mValues), mRenderer, "MM/dd/yyyy HH:mm");     //$NON-NLS-1$
			return GxChartHelper.getTimeChart(GxChartHelper.buildDateDataset(mChartSpec.mTitles, mDates, mValues), mRenderer, "MM/dd/yyyy");     //$NON-NLS-1$
        } catch (IllegalArgumentException ex) {
        	return null;
        }
    }

	public Date ObtainMinDate()
    {
		return ObtainDate(true);
    }

    public Date ObtainMaxDate()
    {
    	return ObtainDate(false);
    }

    private Date ObtainDate(boolean isMinDate)
    {
    	Date best = null;
    	for (int i = 0; i < mDates.size(); i++)
    	{
    		Date[] array = mDates.get(i);
    		for (int j = 0; j < array.length; j++)
    		{
    			Date item = array[j];
    			if (item != null)
    			{
    				if (best == null || isMinDate && item.before(best) || !isMinDate && item.after(best))
    					best = item;
    			}
    		}
    	}

    	if (best == null)
    		best = new Date();

    	return best;
    }
}
