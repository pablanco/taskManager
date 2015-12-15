package com.artech.controls;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.layout.LayoutActionDefinition;
import com.artech.base.metadata.theme.LayoutBoxMeasures;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.UIActionHelper;
import com.artech.controls.utils.TextViewUtils;
import com.artech.ui.Coordinator;
import com.artech.utils.BackgroundOptions;
import com.artech.utils.ThemeUtils;

public class GxButton extends LinearLayout implements IGxActionControl, IGxThemeable, IGxLocalizable
{
	private Coordinator mCoordinator;
	private LayoutActionDefinition mLayoutAction;

	private Entity mEntity; // Optional, only for "in grid" buttons.

	// Child control(s).
	private View mControl;

	private Button mButton;

	public GxButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mButton = new AppCompatButton(context);
	}

	public GxButton(Context context, Coordinator coordinator, LayoutActionDefinition layoutAction)
	{
		super(context);
		mCoordinator = coordinator;

		// Get action definition from layout parent and set button properties from there.
		mLayoutAction = layoutAction;

		mControl = createControl(context, mLayoutAction);
		mControl.setFocusable(false); // Needed for click in ListView to work!
		mControl.setOnClickListener(mOnClickListener);

		// Respect size if specified.
		int width = (mLayoutAction.getWidth() != 0 ? mLayoutAction.getWidth() : LinearLayout.LayoutParams.MATCH_PARENT);
		int height = (mLayoutAction.getHeight() != 0 ? mLayoutAction.getHeight() : LinearLayout.LayoutParams.MATCH_PARENT);

		mControl.setLayoutParams(new LinearLayout.LayoutParams(width, height));
		setGravity(mLayoutAction.CellGravity);

		addView(mControl);
	}

	private static View createControl(Context context, LayoutActionDefinition layoutAction)
	{
		String caption = layoutAction.getCaption();

		// If image and no caption, use image. If there is caption, use a button (with or without image).
		if (Services.Strings.hasValue(layoutAction.getImage()) && !Services.Strings.hasValue(caption))
		{
			Drawable actionImage = UIActionHelper.getActionImage(context, layoutAction);
			if (actionImage != null)
			{
				ImageView imageView = new ImageView(context);
				imageView.setImageDrawable(actionImage);
				return imageView;
			}
			else
			{
				// If the image is not there, and there is no caption either, make up a caption from
				// the name. Otherwise a blank will be displayed, although the action is still clickable!
				caption = layoutAction.getEventName();
			}
		}

		Button button = new AppCompatButton(context);
		TextViewUtils.setText(button, caption, layoutAction);
		UIActionHelper.setActionButtonImage(context, layoutAction, layoutAction.getImagePosition(), button);
		return button;
	}

	private final OnClickListener mOnClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			// On child control click, fire composite click.
			// Apparently button touches are not reported to coordinator, do so.
			boolean handled = (mCoordinator != null && mCoordinator.runControlEvent(GxButton.this, GxTouchEvents.TAP));

			if (!handled)
				performClick();
		}
	};
	private ThemeClassDefinition mClassDefinition;

	@Override
	public ActionDefinition getAction()
	{
		return mLayoutAction.getEvent();
	}

	@Override
	public Entity getEntity()
	{
		return mEntity;
	}

	@Override
	public void setEntity(Entity entity)
	{
		mEntity = entity;
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		if (mControl != null)
		{
			mClassDefinition = themeClass;
			applyClass(themeClass);
		}
	}

	@Override
	public ThemeClassDefinition getThemeClass() {
		return mClassDefinition;
	}


	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		if (mControl != null)
			mControl.setEnabled(enabled);
	}

	public void setAttributes(int caption, int width, int height)
	{
		mButton.setText(caption);
		mButton.setOnClickListener(mOnClickListener);
		mButton.setLayoutParams(new LinearLayout.LayoutParams(width, height));

		mControl = mButton;
		addView(mButton);
	}

	public String getCaption()
	{
		if (mControl != null && mControl instanceof Button)
			return ((Button)mControl).getText().toString();
		else
			return Strings.EMPTY;
	}

	public void setCaption(String caption)
	{
		if (mControl != null)
		{
			// Set caption property
			if (mControl instanceof Button)
				TextViewUtils.setText((Button) mControl, caption, mLayoutAction);
		}
	}

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		if (mControl == null)
			return;

		// Set font properties
		if (mControl instanceof Button)
			ThemeUtils.setFontProperties((Button)mControl, themeClass);

		//set padding
		if (themeClass != null)
		{
			LayoutBoxMeasures padding = themeClass.getPadding();
			if (padding != null)
				mControl.setPadding(padding.left, padding.top, padding.right, padding.bottom);
		}

		// Set background and border properties
		ThemeUtils.setBackgroundBorderProperties(mControl, themeClass, BackgroundOptions.defaultFor(mLayoutAction));

	}

	@Override
	public void onTranslationChanged() {
		setCaption(mLayoutAction.getCaption());
	}

	public View getInnerControl()
	{
		return mControl;
	}
}
