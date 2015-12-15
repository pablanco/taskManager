package com.artech.controls.magazineviewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.artech.controls.R;
import com.artech.android.layout.GridContext;
import com.artech.base.controls.IGxControlNotifyEvents;
import com.artech.base.controls.IGxControlPreserveState;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.layout.ControlInfo;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.layout.Size;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.controllers.ViewData;
import com.artech.controls.IGridView;
import com.artech.controls.IGxThemeable;
import com.artech.controls.grids.GridAdapter;
import com.artech.controls.grids.GridHelper;
import com.artech.controls.grids.IGridSite;
import com.artech.controls.viewpager.GxCirclePageIndicator;
import com.artech.ui.Coordinator;
import com.artech.utils.ThemeUtils;
import com.artech.controls.magazineviewer.FlipperOptions.FlipperLayoutType;

public class GxMagazineViewer extends android.widget.LinearLayout implements IGridView, IGxThemeable, IGridSite, IGxControlRuntime, IGxControlNotifyEvents, IGxControlPreserveState
{
	private static final String SPECIFIC = "specific"; //$NON-NLS-1$
	private GridDefinition mDefinition;
	private FlipperAdapter mAdapter;
	private Coordinator mCoordinator;
	protected FlipperOptions mFlipperOptions;
	private GridHelper mHelper;
	private GridAdapter mGridAdapter;
	private Size mSize;
	private GxCirclePageIndicator mCirclePageIndicator;
	private ViewPager mViewPager;

	private boolean mHasMoreData = false;
	private boolean mRecalculatePageOnRotation = false;
	private int mCurrentFirstItem = 0;
	private int mCurrentPagePortrait = 0;
	private int mCurrentPageLandscape = 0;
	private boolean mSetProgrammatically = false;
	private ThemeClassDefinition mThemeClass;
	private GridContext mContext;

	private static final int REQUEST_THRESHOLD = 2;
	private static final String EVENT_PAGE_CHANGED = "PageChanged";
	private static final String PROPERTY_CURRENT_PAGE = "CurrentPage";
	private static final String PROPERTY_SELECTED_INDEX = "SelectedIndex";
	private static final String METHOD_ENSURE_VISIBLE = "EnsureVisible";
	private static final String METHOD_SELECT = "Select";
	private static final String STATE_CURRENT_PAGE_LANDSCAPE = "CurrentPageLandscape";
	private static final String STATE_CURRENT_PAGE_PORTRAIT = "CurrentPagePortrait";
	private static final String STATE_CURRENT_FIRST_ITEM = "CurrentFirstItem";
	private static final String STATE_RECALCULATE_PAGE = "RecalculatePage";

	public GxMagazineViewer(Context context, Coordinator coordinator, LayoutItemDefinition def) {
		super(context);
		mContext = (GridContext) context;
		mCoordinator = coordinator;
		setLayoutDefinition(def);
	}

	public GxMagazineViewer(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private void initView() {
		setOrientation(LinearLayout.VERTICAL);
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		setLayoutParams(layoutParams);

		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.simple_circles, this);

		mGridAdapter = new GridAdapter(getContext(), mHelper, mDefinition);
		mHelper.setCoordinator(mCoordinator);
		FlipDataSource flipDataSource = new FlipDataSource(getContext(), mFlipperOptions, mGridAdapter, mHelper);
		
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mAdapter = new FlipperAdapter(flipDataSource, mSize);
		mGridAdapter.registerDataSetObserver(new DataSetObserver() {

			@Override
			public void onChanged() {
				super.onChanged();
				mAdapter.notifyDataSetChanged();
			}
			
		});
		
		mViewPager.setAdapter(mAdapter);

		mCirclePageIndicator = (GxCirclePageIndicator) findViewById(R.id.indicator);
		mCirclePageIndicator.setViewPager(mViewPager);
		mCirclePageIndicator.setOnPageChangeListener(new PageChangeListener());

		mCirclePageIndicator.setOptions(mFlipperOptions);
	}

	// Get current page depending on current orientation
	private int getCurrentPage() {
		int currentPage;

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			currentPage = mCurrentPagePortrait;
		} else {
			currentPage = mCurrentPageLandscape;
		}

		return currentPage;
	}

	// Set current page depending on current orientation
	private void setCurrentPage(int currentPage) {
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			mCurrentPagePortrait = currentPage;
		} else {
			mCurrentPageLandscape = currentPage;
		}
	}
	

	private class PageChangeListener extends ViewPager.SimpleOnPageChangeListener {

		@Override
		public void onPageSelected(int position) {
			// If the page was changed by the user, mark that the current page should be recalculated in case the activity is re-created.
			if (!mSetProgrammatically) {
				// Update values
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
					mCurrentPagePortrait = position;
				} else {
					mCurrentPageLandscape = position;
				}
				mCurrentFirstItem = position * mFlipperOptions.getItemsPerPage();

				mRecalculatePageOnRotation = true;
				mCoordinator.runControlEvent(GxMagazineViewer.this, EVENT_PAGE_CHANGED);
				mGridAdapter.selectIndex(mCurrentFirstItem, mContext, mHelper, true);
			} else {
				mSetProgrammatically = false;
			}

			requestMoreDataIfNeeded();
		}
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
	public void addListener(GridEventsListener listener)
	{
		mHelper = new GridHelper(this, mDefinition, false);
		mHelper.setListener(listener);
	}

	protected void setControlInfo(ControlInfo info)
	{
		mFlipperOptions = new FlipperOptions();
		ArrayList<Integer> layout = new ArrayList<>();

		try
		{
			String rowsPerColumn = info.optStringProperty("@SDMagazineViewerRowsPerColumn"); //$NON-NLS-1$
			String[] cols = Services.Strings.split(rowsPerColumn, ' ');
			for (String col : cols)
				layout.add(Integer.parseInt(col));
		}
		catch (NumberFormatException e)
		{
			Services.Log.warning("Invalid SDMagazineViewerRowsPerColumn", e);
		}

		mFlipperOptions.setLayout(layout);
		mFlipperOptions.setItemsPerPage(info.optIntProperty("@SDMagazineViewerItemsPerPage")); //$NON-NLS-1$
		mFlipperOptions.setHeaderText(info.optStringProperty("@SDMagazineViewerHeaderText")); //$NON-NLS-1$
		mFlipperOptions.setShowFooter(info.optBooleanProperty("@SDMagazineViewerShowFooter")); //$NON-NLS-1$

		String pageLayoutType = info.optStringProperty("@SDMagazineViewerPageLayout"); //$NON-NLS-1$
		if (pageLayoutType.compareToIgnoreCase(SPECIFIC) == 0)
			mFlipperOptions.setLayoutType(FlipperLayoutType.Specific);
		else
			mFlipperOptions.setLayoutType(FlipperLayoutType.Random);

		ThemeClassDefinition indicatorClass = PlatformHelper.getThemeClass(info.optStringProperty("@SDMagazineViewerPageControllerClass"));
		if (indicatorClass != null)
			mFlipperOptions.setFooterThemeClass(indicatorClass);
		else
			mFlipperOptions.setFooterBackgroundColor(ThemeUtils.getColorId(info.optStringProperty("@SDMagazineViewerMagazinePageControllerBackColor")));
	}

	@Override
	public void setLayoutParams(ViewGroup.LayoutParams params) {
		mHelper.adjustMargins(params);
		super.setLayoutParams(params);
	}

	@Override
	public void update(ViewData data) {
		mGridAdapter.setData(data);
		mAdapter.notifyDataSetChanged();
		mHasMoreData = data.isMoreAvailable();

		boolean validPageInAdapter = mAdapter.getCount() > 0 && getCurrentPage() >= 0 && getCurrentPage() < mAdapter.getCount();
		if (validPageInAdapter) {
			// Update ViewPager to currentPage if it's not the same as the current one.
			if (getCurrentPage() != mViewPager.getCurrentItem()) {
				mSetProgrammatically = true;
				mViewPager.setCurrentItem(getCurrentPage(), false);
			}
		}

		if (!requestMoreDataIfNeeded()) {
			// Update the indicator when we finish getting all the data.
			mCirclePageIndicator.notifyDataSetChanged();
		}
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass) {
		mThemeClass = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass() {
		return mThemeClass;
	}

	private void setLayoutDefinition(LayoutItemDefinition layoutItemDefinition) {
		mDefinition = (GridDefinition) layoutItemDefinition;
		if (mDefinition != null)
			setControlInfo(mDefinition.getControlInfo());
	}

	@Override
	public void setAbsoluteSize(Size size)
	{
		// Remove footer size from item size.
		mSize = new Size(size.getWidth(), size.getHeight() - mFlipperOptions.getFooterHeight());
		initView();
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass) {
		mHelper.setThemeClass(themeClass);
	}

	@Override
	public void setProperty(String name, Object value) {

		if (PROPERTY_CURRENT_PAGE.equalsIgnoreCase(name) && value != null) {
			Integer page = Services.Strings.tryParseInt(String.valueOf(value));

			if (page != null && mAdapter != null) {
				if (page < 1) {
					page = 1;
				}

				int itemCount = mAdapter.getCount();
				if (itemCount > 0) {
					if (page > itemCount) {
						page = itemCount;
					}
					mViewPager.setCurrentItem(page - 1, false);
				}

				setCurrentPage(page - 1);
			}
		} else if (PROPERTY_SELECTED_INDEX.equalsIgnoreCase(name)) {
			Integer selectedIndex = Services.Strings.tryParseInt(String.valueOf(value));
			mGridAdapter.selectIndex(selectedIndex - 1, mContext, mHelper, true);
		}
	}

	@Override
	public Object getProperty(String name) {
		String result = null;

		if (PROPERTY_CURRENT_PAGE.equalsIgnoreCase(name)) {
			result = String.valueOf(getCurrentPage() + 1);
		} else if (PROPERTY_SELECTED_INDEX.equalsIgnoreCase(name)) {
			result = String.valueOf(mGridAdapter.getSelectedIndex() + 1);
		}

		return result;
	}

	@Override
	public void runMethod(String name, List<Object> parameters)
	{
		if (METHOD_ENSURE_VISIBLE.equalsIgnoreCase(name) && parameters.size() == 1) {
			// Index number is expected as an integer value (>= 1).
			Integer index = Services.Strings.tryParseInt(String.valueOf(parameters.get(0)));

			if (index != null && index > 0) {
				ensureVisible(index);
			}
		} else if (METHOD_SELECT.equalsIgnoreCase(name) && parameters.size() == 1) {
			// Index number is expected as an integer value (>= 1).
			Integer index = Services.Strings.tryParseInt(String.valueOf(parameters.get(0)));

			ensureVisible(index);
			mGridAdapter.selectIndex(index - 1, mContext , mHelper, false);
		}
		
	}

	// Displays the page containing the item with such index.
	private void ensureVisible(int index) {
		int pageNumber = index / mFlipperOptions.getItemsPerPage();
		if (index % mFlipperOptions.getItemsPerPage() != 0) {
			pageNumber++;
		}
		mSetProgrammatically = true;
		mViewPager.setCurrentItem(pageNumber - 1, false);
		setCurrentPage(pageNumber - 1);
	}

	@Override
	public void notifyEvent(EventType type)
	{
		if (type == EventType.ACTIVITY_DESTROYED && mViewPager != null)
			mViewPager.setAdapter(null); // Destroy all views.
	}
	
	@Override
	public String getControlId() {
		return mDefinition.getName();
	}

	@Override
	public void saveState(Map<String, Object> state) {
		state.put(STATE_RECALCULATE_PAGE, mRecalculatePageOnRotation);
		state.put(STATE_CURRENT_FIRST_ITEM, mCurrentFirstItem);
		state.put(STATE_CURRENT_PAGE_PORTRAIT, mCurrentPagePortrait);
		state.put(STATE_CURRENT_PAGE_LANDSCAPE, mCurrentPageLandscape);
	}

	@Override
	public void restoreState(Map<String, Object> state) {
		mRecalculatePageOnRotation = (Boolean) state.get(STATE_RECALCULATE_PAGE);
		mCurrentFirstItem = (Integer) state.get(STATE_CURRENT_FIRST_ITEM);
		mCurrentPagePortrait = (Integer) state.get(STATE_CURRENT_PAGE_PORTRAIT);
		mCurrentPageLandscape = (Integer) state.get(STATE_CURRENT_PAGE_LANDSCAPE);
		
		if (mRecalculatePageOnRotation) {
			// Calculate the new currentPage according to the previous currentFirstItem
			setCurrentPage(mCurrentFirstItem / mFlipperOptions.getItemsPerPage());

			// Calculate the new currentFirstItem of the new page
			mCurrentFirstItem = getCurrentPage() * mFlipperOptions.getItemsPerPage();

			mRecalculatePageOnRotation = false;
			
			if (getCurrentPage() != mViewPager.getCurrentItem()) {
				mViewPager.setCurrentItem(getCurrentPage(), false);
			}
		}
	}
}
