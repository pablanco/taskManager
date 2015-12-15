package com.artech.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;

import com.artech.utils.Cast;

public class ViewHierarchyVisitor
{
	/**
	 * Gets all the Views of type viewType present in the hierarchy starting at root.
	 * Hierarchy traversal is preorder so parents will always appear before children in result.
	 * @param viewType Type of views to locate.
	 * @param root View hierarchy.
	 */
	public static <TView> List<TView> getViews(Class<TView> viewType, View root)
	{
		List<TView> list = new ArrayList<TView>();

		if (viewType != null && root != null)
			visit(viewType, root, list);

		return list;
	}

	private static <TView> void visit(Class<TView> viewType, View view, List<TView> accum)
	{
		if (viewType.isInstance(view))
			accum.add(viewType.cast(view));

		for (View child : getViewWithChildren(view).getCustomViewChildren())
			visit(viewType, child, accum);
	}

	/**
	 * Gets the first View in the parent hierarchy that is of the desired type.
	 * @param viewType Type of view to locate.
	 * @param view Child to get parent from.
	 * @return The first view of the specified type that contains the supplied view directly or indirectly, otherwise null.
	 */
	public static <TView> TView getParent(Class<TView> viewType, View view)
	{
		if (view == null)
			return null;

		if (viewType.isInstance(view))
			return viewType.cast(view);

		View parent = Cast.as(View.class, view.getParent());
		return getParent(viewType, parent);
	}

	/**
	 * Similar to View.findViewWithTag(), but can specify a tag key for calling getTag() on each View.
	 * @param root Root view.
	 * @param key Tag key.
	 * @param tagValue Tag value.
	 * @return The first view found with the specified tag (compared using equals()).
	 */
	public static View findViewWithTag(View root, int key, Object tagValue)
	{
		if (tagValue == null)
			return null;

		Object viewTag = root.getTag(key);
		if (tagValue.equals(viewTag))
			return root;

		// Special case for string tags, use case insensitive matching (necessary for control names in particular).
		if (tagValue instanceof String && viewTag instanceof String && ((String)tagValue).equalsIgnoreCase((String)viewTag))
			return root;

		for (View child : getViewWithChildren(root).getCustomViewChildren())
		{
			View found = findViewWithTag(child, key, tagValue);
			if (found != null)
				return found;
		}

		return null;
	}

	private static ICustomViewChildrenProvider getViewWithChildren(View view)
	{
		if (view instanceof ICustomViewChildrenProvider)
			return (ICustomViewChildrenProvider)view;

		return new BaseViewChildrenProvider(view);
	}

	public interface ICustomViewChildrenProvider
	{
		Collection<View> getCustomViewChildren();
	}

	private static class BaseViewChildrenProvider implements ICustomViewChildrenProvider
	{
		private final View mView;

		BaseViewChildrenProvider(View view)
		{
			mView = view;
		}

		@Override
		public Collection<View> getCustomViewChildren()
		{
			if (mView instanceof ViewGroup)
			{
				ViewGroup parent = (ViewGroup)mView;
				LinkedList<View> children = new LinkedList<View>();
				for (int i = 0; i < parent.getChildCount(); i++)
					children.add(parent.getChildAt(i));

				return Collections.unmodifiableCollection(children);
			}
			else
				return Collections.emptyList();
		}
	}
}
