package com.artech.controls.magazineviewer;

import java.util.ArrayList;
import java.util.Random;

import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.utils.ThemeUtils;

public class FlipperOptions
{
	private ArrayList<Integer> _layout;
	private int _itemsPerPage;
	private FlipperLayoutType _layoutType;
	private boolean mShowFooter;
	private String _headerText;
	private int _defaultItemsPerPage;
	private int mRowsPerColumn = -1;

	private Integer mFooterBackgroundColor = null;
	private Integer mFooterUnselectedColor = null;
	private Integer mFooterSelectedColor = null;

	enum FlipperLayoutType
	{
		Specific,
		Random
	}

	public int getItemsPerPage() {
		if (_itemsPerPage > 0)
			return _itemsPerPage;
		// if a specific layout is specified so count how many items are in each page
		if (_layout != null) {
			_itemsPerPage = 0;
			for (Integer rows : _layout)
				_itemsPerPage += rows;
			if (_itemsPerPage == 0)
				_itemsPerPage = 1;
		} else
			_itemsPerPage = _defaultItemsPerPage;

		return _itemsPerPage;
	}

	public void setItemsPerPage(int itemsPerPage) {
		_defaultItemsPerPage = itemsPerPage;
	}

	public FlipperLayoutType getLayoutType() {
		return _layoutType;
	}

	public void setLayoutType(FlipperLayoutType layoutType) {
		_layoutType = layoutType;
	}

	public ArrayList<Integer> getLayout() {
		if (_layoutType == FlipperLayoutType.Specific)
			return _layout;
		else {
			int totalItemsPerPage = getItemsPerPage();
			int cantItems = 0;
			_layout = new ArrayList<Integer>();
			while (cantItems < totalItemsPerPage) {
				int inPageItems = getRandomNumber(1, totalItemsPerPage - cantItems);
				_layout.add(inPageItems);
				cantItems += inPageItems;
			}
			return _layout;
		}
	}

	/**
	 * Returns a random integer from the interval [min, max].
	 * 
	 * @return an integer betweetn min and max inclusive.
	 */
	private int getRandomNumber(int min, int max) {
		Random rnd = new Random();
		return rnd.nextInt(max - min + 1) + min;
	}

	public void setLayout(ArrayList<Integer> layout) {
		_layout = layout;
	}

	public boolean isShowFooter() {
		return mShowFooter;
	}

	public void setShowFooter(boolean showFooter) {
		mShowFooter = showFooter;
	}

	public String getHeaderText() {
		return _headerText;
	}

	public void setHeaderText(String headerText) {
		_headerText = headerText;
	}

	public void setFooterThemeClass(ThemeClassDefinition themeClass)
	{
		setFooterBackgroundColor(ThemeUtils.getColorId(themeClass.optStringProperty("SDPageControllerBackgroundColor")));
		setFooterSelectedColor(ThemeUtils.getColorId(themeClass.optStringProperty("SDPageIndicatorSelectedColor")));
		setFooterUnselectedColor(ThemeUtils.getColorId(themeClass.optStringProperty("SDPageIndicatorUnselectedColor")));
	}

	public Integer getFooterBackgroundColor()
	{
		return mFooterBackgroundColor;
	}

	public void setFooterBackgroundColor(Integer color)
	{
		mFooterBackgroundColor = color;
	}

	public Integer getFooterSelectedColor()
	{
		return mFooterSelectedColor;
	}

	public void setFooterSelectedColor(Integer color)
	{
		mFooterSelectedColor = color;
	}

	public Integer getFooterUnselectedColor()
	{
		return mFooterUnselectedColor;
	}

	public void setFooterUnselectedColor(Integer color)
	{
		mFooterUnselectedColor = color;
	}

	public int getRowsPerColumn() {
		return mRowsPerColumn;
	}

	public void setRowsPerColumn(int rows) {
		mRowsPerColumn = rows;
	}

	public int getFooterHeight()
	{
		if (mShowFooter)
			return Services.Device.dipsToPixels(26); // 10*2 (padding), plus 3*2 (circle with 3dp radius).
		else
			return 0;
	}
}
