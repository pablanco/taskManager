package com.artech.controls.maps.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.artech.android.ResourceManager;
import com.artech.base.services.Services;
import com.artech.compatibility.CompatibilityHelper;

public class MapItemViewHelper
{
	private final ViewGroup mMapView;
	private RelativeLayout mItemViewContainer;
	private final int mBackgroundColor;

	private final static int MARKER_INFO_WINDOW_OPACITY = 184; // of 256
	private final static int MARKER_INFO_WINDOW_CORNER_RADIUS = 8; // px
	public final static double MARKER_INFO_WINDOW_OFF_CENTER_FACTOR = 0.15; // 15%
	private final static int MARKER_INFO_WINDOW_BOTTOM_MARGIN = 50; // dips

	public MapItemViewHelper(ViewGroup mapView)
	{
		mMapView = mapView;

		int backgroundColorResource = ResourceManager.getResource(mapView.getContext(), android.R.color.background_dark, android.R.color.background_light);
		mBackgroundColor = mapView.getContext().getResources().getColor(backgroundColorResource);
	}

	private Context getContext()
	{
		return mMapView.getContext();
	}

	public void displayItem(View itemView)
	{
		if (mItemViewContainer == null)
		{
			mItemViewContainer = new RelativeLayout(getContext());
			mItemViewContainer.setBackgroundColor(Color.TRANSPARENT);
			mMapView.addView(mItemViewContainer, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}
		else
			mItemViewContainer.removeAllViews();

		// Use a semi-transparent background for the item.
		LinearLayout itemViewHolder = new LinearLayout(getContext());

		GradientDrawable itemViewBackground = new GradientDrawable();
		itemViewBackground.setAlpha(MARKER_INFO_WINDOW_OPACITY);
		itemViewBackground.setColor(mBackgroundColor);
		itemViewBackground.setCornerRadius(MARKER_INFO_WINDOW_CORNER_RADIUS);
		CompatibilityHelper.setBackground(itemViewHolder, itemViewBackground);

		LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		llp.setMargins(5, 5, 5, 5);
		itemViewHolder.addView(itemView, llp);

		RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		rlp.addRule(RelativeLayout.CENTER_HORIZONTAL, -1);
		rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, -1);
		rlp.bottomMargin = (int)(mMapView.getHeight() * (0.5 - MARKER_INFO_WINDOW_OFF_CENTER_FACTOR)) + Services.Device.dipsToPixels(MARKER_INFO_WINDOW_BOTTOM_MARGIN);

		mItemViewContainer.addView(itemViewHolder, rlp);
	}

	public void removeCurrentItem()
	{
		// Remove item view when the map is scrolled.
		if (mItemViewContainer != null && mItemViewContainer.getChildCount() != 0)
			mItemViewContainer.removeAllViews();
	}
}
