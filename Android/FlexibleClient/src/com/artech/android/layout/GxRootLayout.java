package com.artech.android.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.metadata.layout.TableDefinition;
import com.artech.ui.Coordinator;

/**
 * Root of a Panel layout.
 * @author Matias
 *
 */
public class GxRootLayout extends GxLayout
{
	private LayoutDefinition mLayout;
	private boolean mIsExpandComplete;

	public GxRootLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public GxRootLayout(Context context, LayoutDefinition layout, Coordinator coordinator)
	{
		super(context, layout.getTable(), coordinator);
	}

	@Override
	public void setLayout(Coordinator coordinator, TableDefinition layout)
	{
		if (!layout.isMainTable())
			throw new IllegalArgumentException("GxRootLayout should only be used for the root table of a layout.");

		super.setLayout(coordinator, layout);
		mLayout = layout.getLayout();
	}

	public void afterExpandLayout()
	{
		if (mIsExpandComplete)
			throw new IllegalStateException("GxRootLayout.afterExpandLayout() should not be called twice.");

		mIsExpandComplete = true;

		/*
		// See if this layout needs a FAB, and add it if so.
		ActionGroupActionDefinition fabAction = mLayout.getActionBar().getPromotedAction();
		if (fabAction != null)
			addFloatingActionButton(fabAction);
		*/
	}

	public View getFirstChild()
	{
		return this.getChildAt(0);
	}

	/*
	private void addFloatingActionButton(final ActionGroupActionDefinition fabAction)
	{
		FrameLayout fabWrapper = new FrameLayout(getContext());
		addView(fabWrapper, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 0, 0));

		FloatingActionButton fab = new FloatingActionButton(getContext());
		fab.setVisibility(fabAction.isVisible() ? VISIBLE : GONE);

		FrameLayout.LayoutParams lpFab = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lpFab.gravity = Gravity.BOTTOM | Gravity.RIGHT;
		lpFab.rightMargin = lpFab.bottomMargin = getFloatingActionButtonMargin();

		fabWrapper.addView(fab, lpFab);
		fab.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Coordinator coordinator = getCoordinator();
				if (coordinator != null)
					coordinator.runAction(fabAction.getEvent(), new Anchor(v));
			}
		});
	}

	private static int getFloatingActionButtonMargin()
	{
		// Metrics from http://www.google.com/design/spec/components/buttons.html#buttons-floating-action-button
		// The floating action button should be placed 16dp min from the edge on mobile and 24dp min on tablet/desktop.
		int dips = (Services.Device.getScreenSmallestWidth() >= PlatformDefinition.SMALLEST_WIDTH_DP_TABLET ? 24 : 16);
		return Services.Device.dipsToPixels(dips);
	}
	*/
}
