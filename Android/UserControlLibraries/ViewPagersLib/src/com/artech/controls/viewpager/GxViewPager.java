package com.artech.controls.viewpager;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.artech.controls.R;
import com.artech.base.controls.IGxControlPreserveState;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.layout.Size;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.MathUtils;
import com.artech.base.utils.PlatformHelper;
import com.artech.controllers.ViewData;
import com.artech.controls.IGridView;
import com.artech.controls.IGxThemeable;
import com.artech.controls.grids.GridHelper;
import com.artech.controls.grids.IGridSite;
import com.artech.controls.magazineviewer.FlipperOptions;
import com.artech.ui.Coordinator;
import com.artech.utils.ThemeUtils;

public class GxViewPager extends android.widget.LinearLayout
	implements IGridView, IGxThemeable, IGridSite, IGxControlRuntime, IGxControlPreserveState
{
	private static final String STATE_CURRENT_PAGE = "CurrentPage";
	private GridDefinition mDefinition;
	private GxViewPagerAdapter mAdapter;
	private Coordinator mCoordinator;

	private GridHelper mHelper;
    private Size mSize;
	private ViewPager mViewPager;
	private GxCirclePageIndicator mCirclePageIndicator;

	private boolean mHasMoreData = false;
	private int mCurrentPage = 0;
	protected FlipperOptions mFlipperOptions  = new FlipperOptions();
	private ThemeClassDefinition mThemeClass;

	private static final int REQUEST_THRESHOLD = 2;

	public GxViewPager(Context context, Coordinator coordinator, LayoutItemDefinition def)
	{
		super(context);
		mCoordinator = coordinator;
		setLayoutDefinition(def);
	}

	public GxViewPager(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	private void initView()
	{
		mHelper.setBounds(mSize.getWidth(), mSize.getHeight());

		setOrientation(LinearLayout.VERTICAL);
		LayoutParams layoutparms = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		setLayoutParams(layoutparms);

		LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.simple_circles, this);

		mViewPager = (ViewPager) findViewById(R.id.pager);

		mCirclePageIndicator = (GxCirclePageIndicator) findViewById(R.id.indicator);
		mCirclePageIndicator.setOnPageChangeListener(new PageChangeListener());

		// Configure visual properties.
		mCirclePageIndicator.setOptions(mFlipperOptions);
	}

	public int getCurrentPage()
	{
		return mCurrentPage;
	}

	public void setCurrentPage(int currentPage)
	{
		mCurrentPage = currentPage;
	}

	private class PageChangeListener extends ViewPager.SimpleOnPageChangeListener
	{

		@Override
		public void onPageSelected(int position)
		{
			mCurrentPage = position;
			mAdapter.setCurrentItem(position);
			mCoordinator.runControlEvent(GxViewPager.this, EVENT_PAGE_CHANGED);
			requestMoreDataIfNeeded();
		}
	}

	@Override
	public void addListener(GridEventsListener listener)
	{
		mHelper = new GridHelper(this, mDefinition);
		mHelper.setListener(listener);
	}

	@Override
	public void setLayoutParams(ViewGroup.LayoutParams params)
	{
		mHelper.adjustMargins(params);
		super.setLayoutParams(params);
	}

	private boolean requestMoreDataIfNeeded() {
		// Get more data when near the end of the current set.
		boolean getMoreData = mHasMoreData && (getCurrentPage() + REQUEST_THRESHOLD >= mAdapter.getCount());
		if (getMoreData) {
			mHelper.requestMoreData();
		}

		return getMoreData;
	}

	@Override
	public void update(ViewData data)
	{
		prepareAdapter();
		mAdapter.setData(data);
		mHasMoreData = data.isMoreAvailable();

		boolean validPageInAdapter = mAdapter.getCount() > 0 && mCurrentPage >= 0 && mCurrentPage < mAdapter.getCount();

		// Update ViewPager to currentPage if we have the page and it's not the same as the current one.
		if (validPageInAdapter && mCurrentPage != mViewPager.getCurrentItem()) {
			mViewPager.setCurrentItem(mCurrentPage, false);
			mCirclePageIndicator.notifyDataSetChanged();
		}

		if (!requestMoreDataIfNeeded()) {
			// Update the indicator when we finish getting all the data.
			mCirclePageIndicator.notifyDataSetChanged();
		}
	}

	private void prepareAdapter()
	{
		if (mAdapter == null)
		{
			mAdapter = new GxViewPagerAdapter(getContext(), this, mHelper);
			mViewPager.setAdapter(mAdapter);
			mCirclePageIndicator.setViewPager(mViewPager);
		}
	}

	public void onItemClick(View view)
	{
		Entity entity = mAdapter.getEntity(mCurrentPage);
		mHelper.runDefaultAction(entity);
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		mThemeClass = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass() {
		return mThemeClass;
	}

	protected void setControlInfo(ControlInfo info)
	{
		mFlipperOptions = new FlipperOptions();
		mFlipperOptions.setShowFooter(info.optBooleanProperty("@SDPagedGridShowPageController")); //$NON-NLS-1$

		ThemeClassDefinition indicatorClass = PlatformHelper.getThemeClass(info.optStringProperty("@SDPagedGridPageControllerClass"));
		if (indicatorClass != null)
			mFlipperOptions.setFooterThemeClass(indicatorClass);
		else
			mFlipperOptions.setFooterBackgroundColor(ThemeUtils.getColorId(info.optStringProperty("@SDPagedGridPageControllerBackColor")));
	}

	@Override
	public void setAbsoluteSize(Size size)
	{
		mSize = new Size(size.getWidth(), size.getHeight() - mFlipperOptions.getFooterHeight());
		initView();
	}

	private void setLayoutDefinition(LayoutItemDefinition layoutItemDefinition)
	{
		mDefinition = (GridDefinition) layoutItemDefinition;
		if (mDefinition != null)
			setControlInfo(mDefinition.getControlInfo());
	}

	private static final String PROPERTY_CURRENT_PAGE = "CurrentPage";
	private static final String EVENT_PAGE_CHANGED = "PageChanged";

	@Override
	public void setProperty(String name, Object value)
	{
		if (PROPERTY_CURRENT_PAGE.equalsIgnoreCase(name) && value != null)
		{
			Integer page = Services.Strings.tryParseInt(String.valueOf(value));
			if (mAdapter != null && page != null)
			{
				int itemNumber = page - 1;
				itemNumber = MathUtils.constrain(itemNumber, 0, mAdapter.getCount() - 1);
				mViewPager.setCurrentItem(itemNumber);
			}
		}
	}

	@Override
	public Object getProperty(String name)
	{
		if (PROPERTY_CURRENT_PAGE.equalsIgnoreCase(name))
		{
			return String.valueOf(mCurrentPage + 1);
		}
		else
			return null;
	}

	@Override
	public void runMethod(String methodName, List<Object> parameters)
	{
		// No methods supported.
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass) {
		mHelper.setThemeClass(themeClass);
	}

	@Override
	public String getControlId() {
		return mDefinition.getName();
	}

	@Override
	public void saveState(Map<String, Object> state) {
		state.put(STATE_CURRENT_PAGE, getCurrentPage());
	}

	@Override
	public void restoreState(Map<String, Object> state) {
		int currentPage = (Integer) state.get(STATE_CURRENT_PAGE);
		setCurrentPage(currentPage);
		mViewPager.setCurrentItem(mCurrentPage, false);
	}
}
