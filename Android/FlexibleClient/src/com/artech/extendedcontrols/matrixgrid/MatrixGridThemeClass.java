package com.artech.extendedcontrols.matrixgrid;

import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.metadata.theme.ThemeDefinition;

public class MatrixGridThemeClass {

	private ThemeClassDefinition thisClass;
	private ThemeDefinition mTheme;
	
	public MatrixGridThemeClass(ThemeClassDefinition cls) {
		thisClass = cls;
		mTheme = cls.getTheme();
	}
	
	// Matrix Grid
	public ThemeClassDefinition getCellClass()
	{
		if (getCellClassReference().length()>0)
		{
			return mTheme.getClass(getCellClassReference());
		}
		return null;
	}

	private String getCellClassReference()
	{
		return thisClass.optStringProperty("CellTableClassReference"); //$NON-NLS-1$
	}
	public ThemeClassDefinition getXAxisLabelClass()
	{
		if (getXAxisLabelClassReference().length()>0)
		{
			return mTheme.getClass(getXAxisLabelClassReference());
		}
		return null;
	}

	private String getXAxisLabelClassReference()
	{
		return thisClass.optStringProperty("XAxisLabelClassReference"); //$NON-NLS-1$
	}

	public ThemeClassDefinition getYAxisTitleLabelClass()
	{
		if (getYAxisTitleLabelClassReference().length()>0)
		{
			return mTheme.getClass(getYAxisTitleLabelClassReference());
		}
		return null;
	}

	private String getYAxisTitleLabelClassReference()
	{
		return thisClass.optStringProperty("YAxisTitleLabelClassReference"); //$NON-NLS-1$
	}
	
	public ThemeClassDefinition getYAxisDescriptionLabelClass()
	{
		if (getYAxisDescriptionLabelClassReference().length()>0)
		{
			return mTheme.getClass(getYAxisDescriptionLabelClassReference());
		}
		return null;
	}

	private String getYAxisDescriptionLabelClassReference()
	{
		return thisClass.optStringProperty("YAxisDescriptionLabelClassReference"); //$NON-NLS-1$
	}

	public ThemeClassDefinition getYAxisTableClass() {
		if (getYAxisTableClassReference().length() > 0)
			return mTheme.getClass(getYAxisTableClassReference());
		return null;
	}

	
	private String getRowTableClassReferenceEven() {
		return thisClass.optStringProperty("RowTableClassReferenceEven"); //$NON-NLS-1$
	}

	public ThemeClassDefinition getRowTableClassReferenceEvenClass() {
		if (getRowTableClassReferenceEven().length() > 0)
			return mTheme.getClass(getRowTableClassReferenceEven());
		return null;
	}

	private String getRowTableClassReferenceOdd() {
		return thisClass.optStringProperty("RowTableClassReferenceOdd"); //$NON-NLS-1$
	}

	public ThemeClassDefinition getRowTableClassReferenceOddClass() {
		if (getRowTableClassReferenceOdd().length() > 0)
			return mTheme.getClass(getRowTableClassReferenceOdd());
		return null;
	}
	
	
	private String getXAxisTableClassReference() {
		return thisClass.optStringProperty("XAxisTableClassReference"); //$NON-NLS-1$
	}

	public ThemeClassDefinition getXAxisTableClass() {
		if (getYAxisTableClassReference().length() > 0)
			return mTheme.getClass(getXAxisTableClassReference());
		return null;
	}

	private String getYAxisTableClassReference() {
		return thisClass.optStringProperty("YAxisTableClassReference"); //$NON-NLS-1$
	}

	public ThemeClassDefinition getSelectedRowClass() {
		String cls = thisClass.optStringProperty("SelectedRowTableClassReference");
		if (cls.length() > 0)
			return mTheme.getClass(cls);
		return null;
	}

	public ThemeClassDefinition getSelectedCellClass() {
		String cls = thisClass.optStringProperty("SelectedCellTableClassReference");
		if (cls.length() > 0)
			return mTheme.getClass(cls);
		return null;
	}
	


}
