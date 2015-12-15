package com.artech.controls;

import org.achartengine.chart.PieChart;
import org.achartengine.renderer.DefaultRenderer;

import com.artech.controls.achartengine.GxChartHelper;
import com.artech.controls.achartengine.GxChartSpecification;

public class GxPieControl
{

	private String[] mValues;
	private String[] mValuesPercentage;

	private GxChartSpecification mChartSpec;
	private String mTitle;
	private boolean mShowInPercentage;
	private int[] mTotalColors;

	public GxPieControl(GxChartSpecification chartSpec, String title, boolean showInPercentage)
	{
		mChartSpec = chartSpec;
		mTitle = title;
		mShowInPercentage = showInPercentage;
	}

	public void setColors(int[] totalColors)
	{
		mTotalColors = totalColors;
	}

	public PieChart generatePieChart()
	{
		try
		{
			if (mChartSpec.mTitles == null)
				return null;
			mValues = mChartSpec.mValues;
			String[] mLegend = mChartSpec.mTitles;
			mValuesPercentage = new String[mValues.length];

			if (mShowInPercentage)
				obtainPercentage();
			else
				System.arraycopy(mValues, 0, mValuesPercentage, 0, mValues.length);

			int[] colors = new int[mValues.length];
			String[] categoryLegend = new String[mValues.length];
			for (int i = 0; i < mValues.length; i++)
			{
				colors[i] = mTotalColors[i % mTotalColors.length];
				categoryLegend[i] = mLegend[i];
			}

			DefaultRenderer renderer = GxChartHelper.buildCategoryRenderer(colors);
			renderer.setZoomButtonsVisible(false);
			renderer.setZoomEnabled(false);
			renderer.setPanEnabled(false);

			renderer.setChartTitleTextSize(20);
			renderer.setChartTitle(mTitle);
			return GxChartHelper.getPieChart(GxChartHelper.buildCategoryDataset(mTitle, mValuesPercentage, categoryLegend, mValues), renderer);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private void obtainPercentage()
	{
		double totalValues = 0;
		for (String mValue : mValues)
			totalValues = totalValues + Double.valueOf(mValue);

		for (int i = 0; i < mValues.length; i++)
		{
			double porcentage = (Double.valueOf(mValues[i]) * 100) / totalValues;
			int cifras = (int) Math.pow(10, 2);
			porcentage = Math.rint(porcentage * cifras) / cifras;

			mValuesPercentage[i] = String.valueOf(porcentage) + " %"; //$NON-NLS-1$
		}
	}
}
