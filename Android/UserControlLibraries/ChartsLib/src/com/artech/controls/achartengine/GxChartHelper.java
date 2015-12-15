package com.artech.controls.achartengine;

import java.util.Date;
import java.util.List;

import org.achartengine.GraphicalActivity;
import org.achartengine.chart.PieChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.chart.TimeChart;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.*;
import org.achartengine.renderer.XYMultipleSeriesRenderer.XAlign;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint.Align;

import com.artech.base.utils.Strings;

public class GxChartHelper {

	/**
	 * Builds an XY multiple series renderer.
	 *
	 * @param colors the series rendering colors
	 * @param styles the series point styles
	 * @return the XY multiple series renderers
	 */
	public static XYMultipleSeriesRenderer buildRenderer(int[] colors, int[] colorsLegend, PointStyle[] styles, boolean deviceMovilSmall) {
	    XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
	    setRenderer(renderer, colors, colorsLegend, styles, deviceMovilSmall);
	    return renderer;
	}

	public static void setRenderer(XYMultipleSeriesRenderer renderer, int[] colors, int[] colorsLegend, PointStyle[] styles, boolean deviceMovilSmall) {
	    renderer.setAxisTitleTextSize(16);
	    renderer.setChartTitleTextSize(20);
	    renderer.setLabelsTextSize(15);
	    if (deviceMovilSmall)
	    	renderer.setLegendTextSize(10);
	    else
	    	renderer.setLegendTextSize(15);
	    renderer.setPointSize(5f);
    	renderer.setMargins(new int[] { 15, 30, 0, 30 });

	    int length = colors.length;
	    for (int i = 0; i < length; i++) {
	      XYSeriesRenderer r = new XYSeriesRenderer();
	      r.setColor(colorsLegend[i]);
	      r.setLegendColor(colorsLegend[i]);
	      r.setPointStyle(styles[i]);
	      renderer.addSeriesRenderer(r);
	    }
	}

	/**
	   * Sets a few of the series renderer settings.
	   *
	   * @param renderer the renderer to set the properties to
	   * @param title the chart title
	   * @param xTitle the title for the X axis
	   * @param yTitle the title for the Y axis
	   * @param xMin the minimum value on the X axis
	   * @param xMax the maximum value on the X axis
	   * @param yMin the minimum value on the Y axis
	   * @param yMax the maximum value on the Y axis
	   * @param axesColor the axes color
	   * @param labelsColor the labels color
	   */
	public static void setChartSettings(XYMultipleSeriesRenderer renderer, String title, String xTitle,
	      String yTitle, double xMin, double xMax, double yMin, double yMax, int axesColor,
	      int labelsColor, XAlign xAlign, Align yAlign) {
	    renderer.setChartTitle(title);
	    renderer.setXTitle(xTitle);
	    renderer.setYTitle(yTitle);
	    renderer.setXAxisMin(xMin);
	    renderer.setXAxisMax(xMax);
	    renderer.setYAxisMin(yMin);
	    renderer.setYAxisMax(yMax);
	    renderer.setAxesColor(axesColor);
	    renderer.setLabelsColor(labelsColor);
	    renderer.setXAxisAlign(xAlign, 0);
	    renderer.setYAxisAlign(yAlign, 0);
	}

	/**
	 * Builds an XY multiple time dataset using the provided values.
	 *
	 * @param titles the series titles
	 * @param xValues the values for the X axis
	 * @param yValues the values for the Y axis
	 * @return the XY multiple time dataset
	 */
	public static XYMultipleSeriesDataset buildDateDataset(String[] titles, List<Date[]> xValues,
	      List<double[]> yValues) {
	    XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
	    if (titles!=null && xValues.size()>0 && yValues.size()>0) {
		    int length = titles.length;
		    for (int i = 0; i < length; i++) {
		      TimeSeries series = new TimeSeries(titles[i]);
		      Date[] xV = xValues.get(i);
		      double[] yV = yValues.get(i);
		      int seriesLength = xV.length;
		      for (int k = 0; k < seriesLength; k++) {
		        series.add(xV[k], yV[k]);
		      }
		      dataset.addSeries(series);
		    }
	    }
	    return dataset;
	}

	/**
	 * Builds a category renderer to use the provided colors.
	 *
	 * @param colors the colors
	 * @return the category renderer
	 */
	public static DefaultRenderer buildCategoryRenderer(int[] colors) {
	    DefaultRenderer renderer = new DefaultRenderer();
	    renderer.setLabelsTextSize(15);
	    renderer.setLegendTextSize(15);
	    renderer.setMargins(new int[] { 0, 15, 0, 15 });
	    for (int color : colors) {
	      SimpleSeriesRenderer r = new SimpleSeriesRenderer();
	      r.setColor(color);
	      r.setLegendColor(color);
	      renderer.addSeriesRenderer(r);
	    }
	    return renderer;
	}

	/**
	 * Builds a category series using the provided values.
	 *
	 * @param titles the series titles
	 * @param values the values
	 * @return the category series
	 */
	public static CategorySeries buildCategoryDataset(String title, String[] category, String[] categoryLegend, String[] values) {
	    CategorySeries series = new CategorySeries(title);
	    int k = 0;
	    for (String value : values) {
	      series.add(category[k], categoryLegend[k], Double.valueOf(value));
	      k++;
	    }

	    return series;
	}

	/**
	 * Creates a time chart intent that can be used to start the graphical view
	 * activity.
	 *
	 * @param context the context
	 * @param dataset the multiple series dataset (cannot be null)
	 * @param renderer the multiple series renderer (cannot be null)
	 * @param format the date format pattern to be used for displaying the X axis
	 *          date labels. If null, a default appropriate format will be used
	 * @param activityTitle the graphical chart activity title
	 * @return a time chart intent
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset and the renderer don't include the same number of
	 *           series
	 */
	public static final Intent getTimeChartIntent(Context context, XYMultipleSeriesDataset dataset,
	    XYMultipleSeriesRenderer renderer, String format, String activityTitle) {
		Intent intent = new Intent(context, GraphicalActivity.class);
		TimeChart chart = getTimeChart(dataset, renderer, format);
		checkParameters(dataset, renderer);
		intent.putExtra(Strings.EMPTY, chart);
		intent.putExtra(activityTitle, activityTitle);
		return intent;
	}

	/**
	 * Creates a time chart intent that can be used to start the graphical view
	 * activity.
	 *
	 * @param dataset the multiple series dataset (cannot be null)
	 * @param renderer the multiple series renderer (cannot be null)
	 * @param format the date format pattern to be used for displaying the X axis
	 *          date labels. If null, a default appropriate format will be used.
	 * @return a TimeChart
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset and the renderer don't include the same number of
	 *           series
	 */
	public static final TimeChart getTimeChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer, String format) {
	    checkParameters(dataset, renderer);
	    TimeChart chart = new TimeChart(dataset, renderer);
	    chart.setDateFormat(format);
	    return chart;
	}

	/**
	 * Creates a pie chart intent that can be used to start the graphical view
	 * activity.
	 *
	 * @param dataset the category series dataset (cannot be null)
	 * @param renderer the series renderer (cannot be null)
	 * @return a PieChart
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset number of items is different than the number of
	 *           series renderers
	 */
	public static final PieChart getPieChart(CategorySeries dataset, DefaultRenderer renderer) {
	    checkParameters(dataset, renderer);
	    PieChart chart = new PieChart(dataset, renderer);
	    return chart;
	}

	/**
	 * Checks the validity of the dataset and renderer parameters.
	 *
	 * @param dataset the multiple series dataset (cannot be null)
	 * @param renderer the multiple series renderer (cannot be null)
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset and the renderer don't include the same number of
	 *           series
	 */
	private static void checkParameters(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
		if (dataset.getSeriesCount()!=0) {
		    if (dataset == null || renderer == null
		        || dataset.getSeriesCount() != renderer.getSeriesRendererCount()) {
		      throw new IllegalArgumentException(
		          "Dataset and renderer should be not null and should have the same number of series"); //$NON-NLS-1$
		    }
		}
	}

	/**
	 * Checks the validity of the dataset and renderer parameters.
	 *
	 * @param dataset the category series dataset (cannot be null)
	 * @param renderer the series renderer (cannot be null)
	 * @throws IllegalArgumentException if dataset is null or renderer is null or
	 *           if the dataset number of items is different than the number of
	 *           series renderers
	 */
	private static void checkParameters(CategorySeries dataset, DefaultRenderer renderer) {
	    if (dataset == null || renderer == null
	        || dataset.getItemCount() != renderer.getSeriesRendererCount()) {
	      throw new IllegalArgumentException(
	          "Dataset and renderer should be not null and the dataset number of items should be equal to the number of series renderers"); //$NON-NLS-1$
	    }
	}

}
