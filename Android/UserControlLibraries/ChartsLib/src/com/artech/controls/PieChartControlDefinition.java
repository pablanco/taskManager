package com.artech.controls;

import android.content.Context;

import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.utils.Strings;

public class PieChartControlDefinition {

	private ControlPropertiesDefinition mProps;
	private String mChartsValueAttribute = Strings.EMPTY;
	private String mChartsNameAttribute = Strings.EMPTY;
	private boolean mShowInPercentage = false;


	public PieChartControlDefinition(Context context, LayoutItemDefinition def)
	{
		mProps = new ControlPropertiesDefinition(def);
  		mChartsValueAttribute = mProps.readDataExpression("@SDChartsValueAttribute", "@SDChartsValueField"); //$NON-NLS-1$
  		if (mChartsValueAttribute != null && mChartsValueAttribute.equalsIgnoreCase("(none)"))
  			mChartsValueAttribute = "";
  		mChartsNameAttribute = mProps.readDataExpression("@SDChartsCategoryAttribute", "@SDChartsCategoryField"); //$NON-NLS-1$
 		if (mChartsNameAttribute != null && mChartsNameAttribute.equalsIgnoreCase("(none)"))
 			mChartsNameAttribute = "";

  		mShowInPercentage = def.getControlInfo().optBooleanProperty("@SDChartsShowinPercentage"); //$NON-NLS-1$
	}


	public String getChartsValueAttribute() {
		return mChartsValueAttribute;
	}


	public void setChartsValueAttribute(String mChartsValueAttribute) {
		this.mChartsValueAttribute = mChartsValueAttribute;
	}


	public String getChartsNameAttribute() {
		return mChartsNameAttribute;
	}


	public void setChartsNameAttribute(String mChartsNameAttribute) {
		this.mChartsNameAttribute = mChartsNameAttribute;
	}


	public boolean isShowInPercentage() {
		return mShowInPercentage;
	}


	public void setShowInPercentage(boolean mShowInPercentage) {
		this.mShowInPercentage = mShowInPercentage;
	}

}
