package com.artech.actions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import com.artech.activities.ActivityController;
import com.artech.activities.IGxActivity;
import com.artech.android.ViewHierarchyVisitor;
import com.artech.android.layout.LayoutTag;
import com.artech.base.metadata.DataItemHelper;
import com.artech.base.metadata.enums.Connectivity;
import com.artech.base.services.Services;
import com.artech.base.utils.NameMap;
import com.artech.base.utils.Strings;
import com.artech.controllers.IDataSourceBoundView;
import com.artech.controls.ApplicationBarControl;
import com.artech.controls.DataBoundControl;
import com.artech.controls.FormControl;
import com.artech.controls.GxControlViewWrapper;
import com.artech.controls.IGxControl;
import com.artech.controls.IGxEdit;
import com.artech.fragments.IDataView;
import com.artech.ui.Anchor;

public class UIContext extends ContextWrapper
{
	private final Activity mActivity;
	private IDataView mDataView;
	private WeakReference<View> mRootView;
	private UIContext mParent;
	private Connectivity mConnectivitySupport;
	private Anchor mAnchor;
	private NameMap<Object> mTags;

	/**
	 * Constructs an UIContext from an Activity, without specifying a root view.
	 * Actions executed under this context won't be able to update the UI.
	 */
	public static UIContext base(Activity activity, Connectivity connectivitySupport)
	{
		return new UIContext(activity, null, null, connectivitySupport);
	}

	public UIContext(Activity activity, IDataView dataView, View rootView, Connectivity c)
	{
		super(activity);
		mActivity = activity;
		mDataView = dataView;
		setRootView(rootView);
		mConnectivitySupport = c;
		mTags = new NameMap<Object>();
	}

	public UIContext(Context context, Connectivity c)
	{
		super(context);
		mActivity = null;
		mDataView = null;
		mConnectivitySupport = c;
		mTags = new NameMap<Object>();
	}

	protected void setRootView(View rootView)
	{
		mRootView = new WeakReference<View>(rootView);
	}

	private View getRootView()
	{
		return (mRootView != null ? mRootView.get() : null);
	}

	public Activity getActivity()
	{
		return mActivity;
	}

	public ActivityController getActivityController()
	{
		if (mDataView != null && mDataView.getController() != null)
			return mDataView.getController().getParent();
		else
			return null;
	}
	
	public IDataView getDataView()
	{
		return mDataView;
	}

	protected void setParent(UIContext parent)
	{
		mParent = parent;

		if (mDataView == null)
			mDataView = parent.getDataView();
	}

	public UIContext getParent()
	{
		return mParent;
	}

	public <TView> List<TView> getViews(Class<TView> viewType)
	{
		View root = getRootView();
		if (root != null)
			return ViewHierarchyVisitor.getViews(viewType, root);
		else
			return new ArrayList<TView>();
	}

	public IGxControl findControl(String name)
	{
		// "Form" is a special control that maps to the activity itself.
		if (FormControl.CONTROL_NAME.equalsIgnoreCase(name))
			return new FormControl(getActivity(), mDataView);

		if (ApplicationBarControl.CONTROL_NAME.equalsIgnoreCase(name))
			return new ApplicationBarControl(getActivity());

		// Check for controls that map to action bar or action group buttons.
		if (getActivity() instanceof IGxActivity)
		{
			IGxControl control = ((IGxActivity)getActivity()).getController().getControl(mDataView, name);
			if (control != null)
				return control;
		}

		View rootView = getRootView();
		if (rootView != null)
		{
			View view = ViewHierarchyVisitor.findViewWithTag(rootView, LayoutTag.CONTROL_NAME, name);

			// TODO: Change this when our controls implement IGxControl directly.
			if (view instanceof IGxControl)
				return (IGxControl)view;
			else if (view != null)
				return new GxControlViewWrapper(view);
		}

		return null;
	}

	public List<IGxEdit> findControlsBoundTo(String name)
	{
		// Might be more than one if "name" is a structure (e.g. SDT fields on screen).
		ArrayList<IGxEdit> list = new ArrayList<IGxEdit>();

		View rootView = getRootView();
		if (Services.Strings.hasValue(name) && rootView != null)
		{
			// TODO: Remove this for variables/attributes with same name.
			name = DataItemHelper.getNormalizedName(name);

			for (IGxEdit edit : ViewHierarchyVisitor.getViews(DataBoundControl.class, rootView))
			{
				String editTag = edit.getGx_Tag();
				if (Services.Strings.hasValue(editTag))
				{
					// Either an exact match, or a "field superset match" (e.g. name is "&sdt" and tag is "&sdt.name".
					if (editTag.equalsIgnoreCase(name) || editTag.startsWith(name) && editTag.substring(name.length()).startsWith(Strings.DOT))
						list.add(edit);
				}
			}
		}

		return list;
	}

	public List<IDataSourceBoundView> findBoundGrids()
	{
		View rootView = getRootView();
		if (rootView != null)
			return ViewHierarchyVisitor.getViews(IDataSourceBoundView.class, rootView);
		else
			return new ArrayList<IDataSourceBoundView>();
	}

	public Connectivity getConnectivitySupport()
	{
		return mConnectivitySupport;
	}

	public Anchor getAnchor()
	{
		return mAnchor;
	}

	public void setAnchor(Anchor anchor)
	{
		mAnchor = anchor;
	}
	
	public Object getTag(String key)
	{
		return mTags.get(key);
	}
	
	public void setTag(String key, Object tag)
	{
		mTags.put(key, tag);
	}
}
