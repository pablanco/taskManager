package com.artech.controls.achartengine;

import org.achartengine.GraphicalView;
import org.achartengine.chart.AbstractChart;

import android.content.Context;
import android.util.AttributeSet;

public class GxGraphicalViewControl extends GraphicalView {
	
	public GxGraphicalViewControl(Context context) {
		super(context);
	}
	
	public GxGraphicalViewControl(Context context, AbstractChart chart) {
		super(context);
	}
	
	public GxGraphicalViewControl(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void CreateGraphicalView(AbstractChart mChart)
	{
		super.CreateGraphicalView(mChart);
	}
	
}
