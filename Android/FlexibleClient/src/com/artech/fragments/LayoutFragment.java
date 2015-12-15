package com.artech.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.artech.R;
import com.artech.actions.ActionExecution;
import com.artech.actions.ActionParametersHelper;
import com.artech.actions.CompositeAction;
import com.artech.actions.DynamicCallAction;
import com.artech.actions.ICustomMenuManager;
import com.artech.actions.UIContext;
import com.artech.activities.ActivityHelper;
import com.artech.activities.GenexusActivity;
import com.artech.activities.IGxActivity;
import com.artech.activities.IntentParameters;
import com.artech.adapters.AdaptersHelper;
import com.artech.android.ViewHierarchyVisitor;
import com.artech.android.api.EventDispatcher;
import com.artech.android.layout.GridsLayoutVisitor;
import com.artech.android.layout.GxLayout.LayoutParams;
import com.artech.android.layout.GxRootLayout;
import com.artech.android.layout.OrientationLock;
import com.artech.app.ComponentId;
import com.artech.app.ComponentParameters;
import com.artech.app.ComponentUISettings;
import com.artech.base.controls.IGxControlPreserveState;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.DynamicCallDefinition;
import com.artech.base.metadata.IDataSourceDefinition;
import com.artech.base.metadata.IDataViewDefinition;
import com.artech.base.metadata.IViewDefinition;
import com.artech.base.metadata.ObjectParameterDefinition;
import com.artech.base.metadata.SectionDefinition;
import com.artech.base.metadata.StructureDefinition;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.metadata.enums.LayoutModes;
import com.artech.base.metadata.enums.RequestCodes;
import com.artech.base.metadata.layout.ComponentDefinition;
import com.artech.base.metadata.layout.LayoutDefinition;
import com.artech.base.metadata.layout.Size;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.ListUtils;
import com.artech.base.utils.Strings;
import com.artech.common.DataRequest;
import com.artech.common.IntentHelper;
import com.artech.common.SecurityHelper;
import com.artech.compatibility.SherlockHelper;
import com.artech.controllers.IDataSourceBoundView;
import com.artech.controllers.IDataSourceController;
import com.artech.controllers.IDataViewController;
import com.artech.controllers.ViewData;
import com.artech.controls.GxLinearLayout;
import com.artech.controls.LoadingIndicatorView;
import com.artech.utils.Cast;
import com.artech.utils.ThemeUtils;

public class LayoutFragment extends BaseFragment implements IDataView, IDataSourceBoundView, ICustomMenuManager
{
	// Definition & plumbing
	private IDataViewHost mHost;
	private IDataView mParent;
	private LayoutDefinition mLayout;
	private IDataViewController mController;
	private IDataViewDefinition mDefinition;
	private short mMode;
	private boolean mCanHaveScroll = true;
	private boolean mHaveScroll = false;

	private ScrollView mScrollView = null;
	private View mRootCellView = null;

	// Layout helpers and controls.
	private LayoutFragmentAdapter mAdapter;
	private LoadingIndicatorView mLoadingIndicator;

	private GxLinearLayout mContentViewMargin;
	private GxRootLayout mContentView;
	private View mContentRoot;
	private boolean mUseMarginView = false;

	// Status
	private boolean mIsActive;
	private boolean mIsViewCreated;
	private boolean mIsLayoutExpanded;
	private Entity mCurrentEntity;
	private boolean mHasDataArrived;
	private ViewData mPendingUpdate;
	private LayoutFragmentState mPendingRestoreLayoutState;
	private final SecurityHelper.Token mSecurityToken;
	private Connectivity mConnectivity;
	private boolean mDialogDismissed;

	// Events Handling
	public final static String GENEXUS_EVENTS = "GxEvents";
	private IntentFilter mEventsFilter = new IntentFilter(GENEXUS_EVENTS);

	public LayoutFragment()
	{
		// TODO: For now, cannot enable setRetainInstance (fragment needs to be recreated).
		// setRetainInstance(true);
		mSecurityToken = new SecurityHelper.Token();
	}

	public void initialize(Connectivity connectivity, IDataViewHost host, IDataView parent, IDataViewController controller)
	{
		mConnectivity = Connectivity.getConnectivitySupport(connectivity, controller.getDefinition().getConnectivitySupport());
		mHost = host;
		mParent = parent;
		mController = controller;
		mDefinition = controller.getDefinition();
		mMode = controller.getComponentParams().Mode;

		if (mDefinition != null)
		{
			mLayout = mDefinition.getLayoutForMode(mMode);
			if (mDefinition instanceof SectionDefinition)
			{
				//just for calculate fk. should call to match relation directly.
				((SectionDefinition)mDefinition).getVisibleItems(LayoutDefinition.TYPE_VIEW);
			}
		}
	}

	// Our handler for received Intents. This will be called whenever an Intent
	// with an action from GeneXus is broadcasted.
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null) {
				String actionName = intent.getExtras().getString(EventDispatcher.ACTION_NAME);
				 for (ActionDefinition adef : mDefinition.getActions()) {
					if (adef.getName().equalsIgnoreCase(actionName))	{
						Entity e = getContextEntity();
						int i = 0;
						for (ActionParameter par : adef.getEventParameters()) {
							e.setProperty(par.getValue(), intent.getExtras().getString(String.valueOf(i)));
							i++;
						}
						runAction(adef, null);
					 }
				}
			}
		}
	};

    @Override
	public void onPause()
	{
		// Unregister to receive events from GeneXus
		LocalBroadcastManager.getInstance(getUIContext()).unregisterReceiver(mMessageReceiver);
		super.onPause();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		// Register to receive events from GeneXus
		LocalBroadcastManager.getInstance(getUIContext()).registerReceiver(mMessageReceiver, mEventsFilter);
	}

	@Override
	public @NonNull Dialog onCreateDialog(Bundle savedInstanceState)
    {
    	// Use a custom Dialog subclass to intercept onBackPressed()
        return new LayoutFragmentDialog(this);
    }

	@Override
	public void onStart()
	{
		super.onStart();
		if (mController != null)
			mController.onFragmentStart(this);
	}

	@Override
	public IViewDefinition getDefinition()
	{
		return mDefinition;
	}

	@Override
	public short getMode()
	{
		return mMode;
	}

	protected short getLayoutMode()
	{
		return LayoutModes.VIEW;
	}

	@Override
	public IDataViewController getController()
	{
		return mController;
	}

	@Override
	public UIContext getUIContext()
	{
		return new UIContext(getActivity(), this, getContentView(), mConnectivity);
	}

	IGxActivity getGxActivity()
	{
		return Cast.as(IGxActivity.class, getActivity());
	}

	protected LayoutFragmentAdapter getAdapter()
	{
		return mAdapter;
	}

	private static final String CONTROL_STATE = "ControlState::"; //$NON-NLS-1$

	@Override
	public void saveFragmentState(LayoutFragmentState state)
	{
		for (IGxControlPreserveState control : ViewHierarchyVisitor.getViews(IGxControlPreserveState.class, getContentView()))
		{
			Map<String, Object> controlState = new HashMap<>();
			control.saveState(controlState);

			if (controlState.size() != 0)
				state.setProperty(CONTROL_STATE + control.getControlId(), controlState);
		}
	}

	@Override
	public void restoreFragmentState(LayoutFragmentState state)
	{
		if (state == null)
			return;

		if (mIsLayoutExpanded)
		{
			// Restore layout now.
			restoreLayoutState(state);
		}
		else
		{
			// Keep state in local variable so we can restore layout properties when layout is expanded.
			mPendingRestoreLayoutState = state;
		}
	}

	private void restoreLayoutState(LayoutFragmentState state)
	{
		for (IGxControlPreserveState control : ViewHierarchyVisitor.getViews(IGxControlPreserveState.class, getContentView()))
		{
			@SuppressWarnings("unchecked")
			Map<String, Object> controlState = (Map<String, Object>) state.getProperty(CONTROL_STATE + control.getControlId());
			if (controlState != null)
				control.restoreState(controlState);
		}
	}

	public void setCanHaveScroll(boolean value)
	{
		mCanHaveScroll = value;
	}

	@Override
	public boolean isActive()
	{
		if (mParent != null && !mParent.isActive())
			return false;

		return mIsActive;
	}

	@Override
	public void setActive(boolean value)
	{
		if (mIsActive != value)
		{
			mIsActive = value;
			setHasOptionsMenu(value);
		}
	}

	@Override
	public boolean isDataReady()
	{
		return mHasDataArrived;
	}

	@Override
	public void refreshData(boolean keepPosition)
	{
		if (mController != null)
			mController.getParent().onRefresh(mController, keepPosition, true);
	}

	@Override
	public LayoutDefinition getLayout()
	{
		return mLayout;
	}

	@Override
	public Entity getContextEntity()
	{
		if (mCurrentEntity != null)
		{
			if (mAdapter != null)
				mAdapter.controlstoData(mCurrentEntity);
		}
		else
		{
			StructureDefinition structure = StructureDefinition.EMPTY;
			if (mDefinition != null && mDefinition.getMainDataSource() != null)
				structure = mDefinition.getMainDataSource().getStructure();

			mCurrentEntity = new Entity(structure);
			if (mDefinition != null)
				mCurrentEntity.setExtraMembers(mDefinition.getVariables());

		}
		return mCurrentEntity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (mController == null)
			return new View(inflater.getContext()); // This is necessary to prevent a crash with the child FragmentManager

		// Expand the layout
		int layoutResourceId = R.layout.layoutfragment;
		mHaveScroll = false;
		if (mLayout != null)
		{
			GridsLayoutVisitor visitor = new GridsLayoutVisitor();
			mLayout.getTable().accept(visitor);
			if (mCanHaveScroll && !visitor.hasAnyScrollable())
			{
				layoutResourceId = R.layout.layoutfragmentscroll;
				mHaveScroll = true;
			}
		}

		View layoutHolder =  inflater.inflate(layoutResourceId, container, false);
      	mContentView = (GxRootLayout) layoutHolder.findViewById(R.id.layoutContent);
      	mLoadingIndicator = (LoadingIndicatorView)layoutHolder.findViewById(R.id.loadingIndicatorView);

    	mAdapter = new LayoutFragmentAdapter(this, getGxActivity(), mLayout, getLayoutMode(), mMode);

		// add scroll listener
		if (mHaveScroll && mLayout.getEnableHeaderRowPattern())
		{
			mScrollView =  (ScrollView)layoutHolder.findViewById(R.id.scrollViewLayoutContentScroll);
			mScrollView.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener);

		}

    	//change the Content to GxLinearLayout if necessary
      	if (mAdapter.getContentHasMargin())
      	{
      		ViewGroup parent = (ViewGroup) mContentView.getParent();
      		parent.removeView(mContentView);
      		mContentViewMargin = new GxLinearLayout(getActivity());
      		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

      		parent.addView(mContentViewMargin, params);
      		ViewGroup.LayoutParams paramMargin = mContentViewMargin.getLayoutParams();
      		if (paramMargin instanceof LinearLayout.LayoutParams)
      			((LinearLayout.LayoutParams) paramMargin).weight = 1;

      		// Place the original GxRootLayout inside this wrapper.
      		mContentViewMargin.addView(mContentView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

      		mUseMarginView = true;
      	}

      	mContentRoot = layoutHolder.findViewById(R.id.scrollViewLayoutContentScroll);
      	if (mContentRoot == null)
      		mContentRoot = getContentView();

		changeContentVisible(View.GONE);
      	drawLayoutSetControllers();

      	mIsViewCreated = true;
      	if (mPendingUpdate != null)
      	{
      		update(mPendingUpdate);
      		mPendingUpdate = null;
      	}

		return layoutHolder;
	}


	private void changeContentVisible(int visible)
	{
		mContentRoot.setVisibility(visible);
	}

	private View getContentView()
	{
		if (mUseMarginView)
			return mContentViewMargin;
		else
			return mContentView;
	}

	private void drawLayoutSetControllers()
	{
		if (mIsLayoutExpanded)
			return;

		Size desiredSize = getDesiredSize();
		if (desiredSize == null)
			desiredSize = AdaptersHelper.getWindowSize(getActivity(), mHost.getMainLayout());

		mAdapter.expandLayout(mContentView, desiredSize);

		// Fire data loading.
		for (IDataSourceBoundView boundView : mAdapter.getDataSourceBoundViews())
			mController.attachDataController(boundView);

		mIsLayoutExpanded = true;

		if (mPendingRestoreLayoutState != null)
		{
			restoreLayoutState(mPendingRestoreLayoutState);
			mPendingRestoreLayoutState = null;
		}
	}

	@Override
	public void setController(IDataSourceController controller)
	{
		// No need to keep track of controller for this view.
		// mMainDataController = controller;
	}

	@Override
	public String getDataSourceId()
	{
		IDataSourceDefinition dataSource = getDataSource();
		if (dataSource != null)
			return dataSource.getName();
		else if (mDefinition != null)
			return mDefinition.getName();
		else
			return Strings.EMPTY;
	}

	@Override
	public IDataSourceDefinition getDataSource()
	{
		return (mLayout != null ? mLayout.getDataSource() : null);
	}

	@Override
	public String getDataSourceMember()
	{
		return null; // Whole layout is not associated to a member.
	}

	@Override
	public int getDataSourceRowsPerPage()
	{
		return 0; // Data source does not have paging.
	}

	// IDataBoundView implementation.

	@Override
	public void update(ViewData data)
	{
		if (data.getDataUnchanged())
		{
			// should hide loading indicator if exists.
			return;
		}

		if (!mIsViewCreated)
		{
			// In case it's called before the view has been created.
			mPendingUpdate = data;
			return;
		}

		// Redirect to login on authentication error.
		if (SecurityHelper.handleSecurityError(getUIContext(), data.getStatusCode(), data.getStatusMessage(), mSecurityToken) != SecurityHelper.Handled.NOT_HANDLED)
			return;

		// Show error if we have an error message.
		if (Services.Strings.hasValue(data.getStatusMessage()))
			mLoadingIndicator.setText(data.getStatusMessage());

		if (data.getStatusCode() == DataRequest.ERROR_SECURITY_AUTHORIZATION)
			changeContentVisible(View.GONE); // Hide any previous data when authorization fails (e.g. refresh after changing permissions).

		if (data.getSingleEntity() != null)
		{
			mCurrentEntity = data.getSingleEntity();
			mHasDataArrived = true;

			// Handle Calls on Start Event
			if (redirect(getUIContext(), mCurrentEntity))
				return;

			beforeDrawData(data);
			mAdapter.drawData(data);

			createComponents();

			// Since content changed visibility, refresh menu options.
			SherlockHelper.invalidateOptionsMenu(getActivity());
		}
	}

	public static boolean redirect(UIContext context, Entity data)
	{
		List<DynamicCallDefinition> calls = DynamicCallDefinition.from(data);
		if (calls.size() > 0)
		{
			CompositeAction actions = new CompositeAction(context, null, null);
			for (DynamicCallDefinition call : calls)
			{
				DynamicCallAction action = DynamicCallAction.redirect(context, data, call.getCallString());
				actions.addAction(action);
			}

			ActionExecution exec = new ActionExecution(actions);
			exec.executeAction(); // Will also finish() the current activity.
			return true;

		}
		else
			return false;
	}

	private void beforeDrawData(ViewData data)
	{
		mLoadingIndicator.setVisibility(View.GONE);
		changeContentVisible(View.VISIBLE);
	}

	private void createComponents()
	{
		// For each ComponentContainer that has a design-time component, create it.
		for (ComponentContainer container : mAdapter.getComponentContainers())
		{
			if (container.getStatus() == ComponentContainer.INACTIVE)
			{
				container.setParentFragment(this);

				if (!container.hasTabParent())
					container.setStatus(ComponentContainer.TOACTIVATED);
			}
		}

		attachContentContainers();
	}

	public void attachContentContainers()
	{
		// Attach Content containers a.k.a. inline sections.
		for (ComponentContainer container : mAdapter.getComponentContainers())
		{
			if (container.getStatus() == ComponentContainer.TOACTIVATED)
			{
				// Activate or create fragment
				FragmentManager fragmentManager = getChildFragmentManager();
				FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
				if (container.getFragment() == null)
				{
					//Create fragment.
					container.setId(container.getContentControlId());

					//Create only if ContentContainer exist in activity.
					//View contentView = this.getActivity().findViewById(content.getContentControlId());
					boolean isConected = !container.hasTabParentDisconected();

					if (isConected)
					{
						BaseFragment innerFragment = createInnerComponent(container);
						if (innerFragment != null)
						{
							if ((innerFragment instanceof LayoutFragment) && (mHaveScroll || container.hasTabParentWithScroll()))
								((LayoutFragment)innerFragment).setCanHaveScroll(false);

							fragmentTransaction.add(container.getId(), innerFragment);
							container.setFragment(innerFragment);
						}
					}
					else
					{
						Services.Log.warning("Not activating ComponentContainer beacuse is not visible id: " + container.getContentControlId()); //$NON-NLS-1$
					}
				}

				if (container.getFragment() != null)
				{
					fragmentTransaction.commit();
					container.setActive(true);
				}
			}

			if (container.getStatus() == ComponentContainer.TOINACTIVATED)
				container.setActive(false);
		}
	}

	private BaseFragment createInnerComponent(ComponentContainer container)
	{
		if (container.getFragment() != null)
			throw new IllegalStateException("ComponentContainer already has a Fragment.");

		ComponentDefinition definition = container.getDefinition();
		if (definition != null && definition.getObject() != null)
		{
			// Build component according to its container's definition.
			ComponentId innerId = new ComponentId(mController.getId(), definition.getName());
			ComponentParameters innerParams = new ComponentParameters(definition.getObject(), mMode, getInnerDataViewParameters(definition), mController.getComponentParams().NamedParameters);
			ComponentUISettings innerSettings = ComponentUISettings.childOf(this, container.getComponentSize());

			return mHost.createComponent(innerId, innerParams, innerSettings);
		}
		else
			return null;
	}

	public void removeInnerComponent(ComponentContainer container)
	{
		if (container.getFragment() == null)
			throw new IllegalStateException("ComponentContainer doesn't have a Fragment.");

		// Detach and destroy the inner fragment.
		getChildFragmentManager().beginTransaction().remove(container.getFragment()).commit();
		mHost.destroyComponent(container.getFragment());
		container.setFragment(null);
	}

	public List<String> getInnerDataViewParameters(ComponentDefinition definition)
	{
		// Default is that inner DV parameters are the same as outer DV parameters.
		List<String> parameters = mController.getComponentParams().Parameters;
		if (definition.getParameters().size() != 0 && mCurrentEntity != null)
			parameters = ActionParametersHelper.getParametersForDataView(definition.getParameters(), mCurrentEntity);

		return parameters;
	}

	@Override
	public boolean needsMoreData()
	{
		return !mHasDataArrived;
	}

	@Override
	public List<BaseFragment> getChildFragments()
	{
		ArrayList<BaseFragment> list = new ArrayList<>();
		if (mAdapter != null)
		{
			for (ComponentContainer container : mAdapter.getComponentContainers())
			{
				BaseFragment child = container.getFragment();
				if (child != null)
				{
					list.add(child);
					list.addAll(child.getChildFragments());
				}
			}
		}

		return list;
	}

	@Override
	public void onCustomCreateOptionsMenu(Menu menu)
	{
		if (mAdapter != null)
		{
			for (ICustomMenuManager customMenuManager : mAdapter.getCustomMenuManagers())
				customMenuManager.onCustomCreateOptionsMenu(menu);
		}
	}

	public void setReturnResult(Intent data)
	{
		// The list to be placed in an Intent must have parcelable data, so we convert it to String.
		List<String> serializableParameters = ListUtils.toStringList(getOutputParameters());
		IntentHelper.putList(data, IntentParameters.Parameters, serializableParameters);
	}

	private List<Object> getOutputParameters()
	{
		Entity data = getContextEntity();
		List<Object> output = new ArrayList<Object>();

		if (data != null)
		{
			for (ObjectParameterDefinition parameter : getDefinition().getParameters())
				output.add(data.getProperty(parameter.getName()));
		}

		return output;
	}

	public void returnOK()
	{
		Intent result = new Intent();
		((LayoutFragment)getTargetFragment()).setReturnResult(result);

		LayoutFragmentActivity activity = Cast.as(LayoutFragmentActivity.class, getActivity());
		if (activity != null)
			activity.onActivityResult(RequestCodes.ACTION, Activity.RESULT_OK, result);

		destroyDialog();
	}

	public void returnCancel()
	{
		LayoutFragmentActivity activity = Cast.as(GenexusActivity.class, getActivity());
	    if (activity != null)
	    	activity.onActivityResult(RequestCodes.ACTION, Activity.RESULT_CANCELED, null);

	    destroyDialog();
	}

	private void destroyDialog()
	{
		Services.Device.invokeOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				if (getActivity() != null)
				{
				    GenexusActivity activity = Cast.as(GenexusActivity.class, getActivity());
				    if (activity != null)
				    	activity.destroyComponent(LayoutFragment.this);

					mDialogDismissed = true;
				    dismiss();

					OrientationLock.unlock(getActivity(), OrientationLock.REASON_SHOW_POPUP);
				}
			}
		});
	}

	@Override
	public void onDismiss(DialogInterface dialog)
	{
		super.onDismiss(dialog);

		if (!mDialogDismissed)
			returnCancel();
	}

	@Override
	public void onDestroy()
	{
		if (mAdapter != null)
			mAdapter.onDestroyFragment();

		super.onDestroy();
	}

	@Override
	public List<View> getControlViews() {
		return mAdapter.getCoordinator().getControlViews();
	}

	// EnableHeaderRowPattern
	private final ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener()
	{
		private boolean mIsTransparent = true;

		@Override
		public void onScrollChanged()
		{
			if (getActivity()==null || mLayout ==null)
				return;

			//change the theme when scroll down and up
			if (mRootCellView==null && mContentView!=null)
			{
				mRootCellView = mContentView.getFirstChild();
			}
			int bottomY = 0;
			boolean isVisible= false;
			if (mRootCellView!=null)
			{
				// get the first row of main content.
				Rect rect = new Rect();
				isVisible = mRootCellView.getGlobalVisibleRect(rect);
				bottomY = rect.bottom;


				//change action bar theme
				int statusActionBarHeight = AdaptersHelper.getStatusAndActionBarHeight(getActivity(), mLayout);
				if (bottomY < statusActionBarHeight && mIsTransparent && isVisible)
				{
					// use default theme
					ActivityHelper.setActionBarTheme(getActivity(), mLayout, true);
					mIsTransparent = false;
					// restore elevation.
					ActionBar bar = SherlockHelper.getActionBar(getActivity());
					ThemeUtils.resetElevation(getActivity(), bar, true);
				}
				else if (bottomY > statusActionBarHeight && !mIsTransparent && isVisible)
				{
					// use header row theme
					ActivityHelper.setActionBarThemeClass(getActivity(), mLayout.getHeaderRowApplicationBarClass(), true);
					mIsTransparent = true;
					// reset elevation
					ActionBar bar = SherlockHelper.getActionBar(getActivity());
					ThemeUtils.resetElevation(getActivity(), bar, false);
				}
			}
		}

	};


}
