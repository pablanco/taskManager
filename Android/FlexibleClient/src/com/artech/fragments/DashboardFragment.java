package com.artech.fragments;

import java.util.Collections;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.artech.R;
import com.artech.actions.ActionExecution;
import com.artech.actions.ActionFactory;
import com.artech.actions.ActionParameters;
import com.artech.actions.CompositeAction;
import com.artech.actions.CompositeAction.IEventListener;
import com.artech.actions.UIContext;
import com.artech.adapters.DashBoardAdapter;
import com.artech.android.layout.GxTheme;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.DashboardMetadata;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.common.ImageHelper;
import com.artech.controllers.IDataViewController;
import com.artech.controls.GxLinearLayout;
import com.artech.controls.ImageViewDisplayImageWrapper;

public class DashboardFragment extends BaseFragment implements IDataView
{
	private DashboardMetadata mDefinition;
	private Connectivity mConnectivity;
	private DashBoardAdapter mAdapter;
	private Entity mData;
	private boolean mClientStartExecuted;

	private View mContentView;
	private AdapterView<ListAdapter> mDashboardView;
	private boolean mIsActive;

	public void initialize(DashboardMetadata definition, Connectivity connectivity)
	{
		mDefinition = definition;
		mData = new Entity(StructureDefinition.EMPTY);
		mData.setExtraMembers(mDefinition.getVariables());

		mConnectivity = Connectivity.getConnectivitySupport(connectivity, definition.getConnectivitySupport());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (mDefinition == null)
			return null;

		mContentView = inflater.inflate(R.layout.maindashboard, container, false);
		mAdapter = new DashBoardAdapter(getUIContext(), mData);
		mAdapter.setDefinition(mDefinition);

		// Hide both controls here, show the correct one in showDashboardOptions().
		GridView grid = (GridView)mContentView.findViewById(R.id.DashBoardGridView);
		ListView list = (ListView)mContentView.findViewById(R.id.DashBoardListView);
		grid.setVisibility(View.GONE);
		list.setVisibility(View.GONE);

		if (mDefinition.getControl().equalsIgnoreCase(DashboardMetadata.CONTROL_LIST))
			mDashboardView = list;
		else
			mDashboardView = grid;

		applyStyle();
		startDashboard();
		return mContentView;
	}

	private void startDashboard()
	{
		if (!mClientStartExecuted)
		{
			ActionDefinition clientStartDefinition = mDefinition.getClientStart();
			if (clientStartDefinition != null)
			{
				// Run ClientStart, show dashboard afterwards.
				CompositeAction clientStart = ActionFactory.getAction(getUIContext(), clientStartDefinition, new ActionParameters(mData));
				clientStart.setEventListener(mClientStartEventListener);
				new ActionExecution(clientStart).executeAction();
			}
			else
			{
				mClientStartExecuted = true;
				showDashboardOptions();
			}
		}
		else
			showDashboardOptions();
	}

	private final IEventListener mClientStartEventListener = new IEventListener()
	{
		@Override
		public void onEndEvent(CompositeAction event, boolean successful)
		{
			mClientStartExecuted = true;
			showDashboardOptions();
		}
	};

	private void showDashboardOptions()
	{
		mDashboardView.setVisibility(View.VISIBLE);
		mDashboardView.setAdapter(mAdapter);
		mDashboardView.setOnItemClickListener(mAdapter);
	}

	private void applyStyle()
	{
		// Set dashboard background and header images.
		GxLinearLayout root = (GxLinearLayout)mContentView.findViewById(R.id.DashBoardMainLinearLayout);
		ImageHelper.displayBackground(root, mDefinition.getBackgroundImage());
		ImageView header = (ImageView)mContentView.findViewById(R.id.DashBoardHeaderImage);
		ImageHelper.displayImage(ImageViewDisplayImageWrapper.to(header), mDefinition.getHeaderImage());

		// Apply dashboard theme class.
		ThemeClassDefinition gridThemeClass = mDefinition.getThemeClassForGrid();
		if (gridThemeClass != null)
			GxTheme.applyStyle(root, gridThemeClass);
	}

	@Override
	public IViewDefinition getDefinition()
	{
		return mDefinition;
	}

	@Override
	public short getMode()
	{
		return DisplayModes.VIEW;
	}

	@Override
	public LayoutDefinition getLayout()
	{
		return null;
	}

	@Override
	public IDataViewController getController()
	{
		// TODO Not needed for now, but should return one.
		return null;
	}

	@Override
	public UIContext getUIContext()
	{
		return new UIContext(getActivity(), this, mContentView, mConnectivity);
	}

	@Override
	public Entity getContextEntity()
	{
		return mData;
	}

	@Override
	public boolean isActive()
	{
		return mIsActive;
	}

	@Override
	public void setActive(boolean value)
	{
		mIsActive = value;
	}

	@Override
	public boolean isDataReady()
	{
		return mClientStartExecuted;
	}

	@Override
	public void refreshData(boolean keepPosition)
	{
		// Dashboards have no refresh event (as of now) so this does nothing.
	}

	@Override
	public void saveFragmentState(LayoutFragmentState state)
	{
		if (mClientStartExecuted)
			state.setData(mData);
	}

	@Override
	public void restoreFragmentState(LayoutFragmentState state)
	{
		Entity data = state.getData();
		if (data != null && !data.isEmpty())
		{
			mClientStartExecuted = true;
			mData = data;
		}
	}

	@Override
	public List<BaseFragment> getChildFragments()
	{
		return Collections.emptyList();
	}

	@Override
	public List<View> getControlViews()
	{
		// It has no controls.
		return Collections.emptyList();
	}
}
