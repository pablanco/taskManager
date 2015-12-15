package com.artech.utils;

import java.util.HashMap;
import java.util.Hashtable;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager.TaskDescription;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.artech.R;
import com.artech.android.ResourceManager;
import com.artech.application.MyApplication;
import com.artech.base.metadata.layout.TableDefinition;
import com.artech.base.metadata.loader.MetadataLoader;
import com.artech.base.metadata.theme.BackgroundImageMode;
import com.artech.base.metadata.theme.ThemeApplicationBarClassDefinition;
import com.artech.base.metadata.theme.ThemeApplicationClassDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.ReflectionHelper;
import com.artech.base.utils.Strings;
import com.artech.common.ImageHelper;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.controls.GxGradientDrawable;

public class ThemeUtils
{

	public static float elevationValue = 0;

	public static int getColorId(String colorString, int defaultColor)
	{
		Integer colorId = getColorId(colorString);
		if (colorId == null)
			colorId = defaultColor;

		return colorId;
	}

	private static Hashtable<String, Integer> sColors = new Hashtable<>();

	public static Integer getColorId(String colorString)
	{
		if (Strings.hasValue(colorString))
		{
			if (sColors.containsKey(colorString))
				return sColors.get(colorString);

			try
			{
				//gx format is #rrggbbaa , parse color in android is #aarrggbb , so convert it before parse.
				String colorNewFormatString = colorString.trim();
				if (colorNewFormatString.startsWith("#") && colorNewFormatString.length() >= 9) //$NON-NLS-1$
					colorNewFormatString = "#" + colorNewFormatString.substring(7, 9) + colorNewFormatString.substring(1, 7);  //$NON-NLS-1$

				int colorInt = Color.parseColor(colorNewFormatString);
				sColors.put(colorString, colorInt);
				return colorInt;
			}
			catch (Exception ex)
			{
				Services.Log.warning("errorColor " + "error parser " + colorString + ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return null;
	}

	private static HashMap<String, Typeface> sTypefaceCache = new HashMap<>();

	private static Typeface getFontFamily(String name)
	{
		if (Strings.hasValue(name))
		{
			// Cache Typeface objects. For performance reasons, and also to avoid a memory leak
			// in some versions (http://code.google.com/p/android/issues/detail?id=9904).
			if (sTypefaceCache.containsKey(name))
				return sTypefaceCache.get(name);

			Typeface typeface = resolveTypeface(name);
			sTypefaceCache.put(name, typeface);
			return typeface;
		}
		else
			return null;
	}

	private static Typeface resolveTypeface(String fontFamily)
	{
		// Support custom font family, ttf files works in all android versions.
		// only try to get if font family contains ".", avoid runtime exception.
		if (fontFamily.contains("."))
		{
			String familyPath = "fonts/" + fontFamily; //$NON-NLS-1$
			try
			{
				Typeface tf = Typeface.createFromAsset(MyApplication.getAppContext().getAssets(), familyPath);
				if (tf != null)
					return tf;
			}
			catch (RuntimeException ex)
			{
				Services.Log.Error("getFontFamily " + fontFamily + " " + ex.getMessage()); //$NON-NLS-1$
			}
		}

		Typeface defaultFont = Typeface.create((String)null, Typeface.NORMAL);

		String[] fontNames = Services.Strings.split(fontFamily, ',');
		for (String fontName : fontNames)
		{
			Typeface myFont = Typeface.create(fontName, Typeface.NORMAL);
			if (!defaultFont.equals(myFont))
				return myFont;
		}

		return defaultFont;
	}

	public static void setFontProperties(TextView textView, ThemeClassDefinition themeClass)
	{
		setFontProperties(textView, themeClass, true);
	}

	public static void setFontProperties(TextView textView, ThemeClassDefinition themeClass, boolean setDefaultThemeColor)
	{
		if (themeClass == null)
			return;

		ThemeDefaults.beforeSetThemeProperties(textView);

		// Text Color
		Integer colorId = ThemeUtils.getColorId(themeClass.getColor());
		Integer highlightedColorId = ThemeUtils.getColorId(themeClass.getHighlightedColor());
		if (colorId != null || highlightedColorId != null)
			setTextColor(textView, colorId, highlightedColorId);
		else if (setDefaultThemeColor)
			ThemeDefaults.resetTextColor(textView);

		// Font Size
		Integer fontSizef = themeClass.getFont().getFontSize();
		if (fontSizef != null)
			textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizef);
		else
			ThemeDefaults.resetTextSize(textView);

		//	Font
		Typeface myfont = ThemeUtils.getFontFamily(themeClass.getFont().getFontFamily());
		Integer fontStyle = themeClass.getFont().getFontStyle();
		if (fontStyle == null)
			fontStyle = Typeface.NORMAL;

		if (myfont != null)
			textView.setTypeface(myfont, fontStyle);
		else if (fontStyle != Typeface.NORMAL)
			textView.setTypeface(textView.getTypeface(), fontStyle);
		else
			ThemeDefaults.resetTypeface(textView);
	}

	private static void setTextColor(TextView tv, Integer color, Integer highlightedColor)
	{
		if (highlightedColor != null)
		{
			if (color == null)
				color = ThemeDefaults.getDefaultTextColor(tv);

			ColorStateList colors = new ColorStateList(
				new int[][]	{
					new int [] { android.R.attr.state_pressed },
					new int [] { android.R.attr.state_selected },
					new int[0],
				},
				new int[] {
					highlightedColor,
					highlightedColor,
					color,
				});

			tv.setTextColor(colors);
		}
		else if (color != null)
		{
			tv.setTextColor(color);
		}
	}

	/**
	 * Sets the background properties (image and color) plus border to a specific control.
	 * @param view The view to be themed.
	 * @param themeClass The theme class to apply.
	 * @param options Options (such as how to handle background drawable's size).
	 */
	public static void setBackgroundBorderProperties(View view, ThemeClassDefinition themeClass, BackgroundOptions options)
	{
		if (themeClass == null)
			return;

		ThemeDefaults.beforeSetThemeProperties(view);

		Drawable background = createBackgroundDrawableFromClass(themeClass, options);
		background = applyHighlightingFromClass(background, view, themeClass, options);

		if (background != null)
			CompatibilityHelper.setBackground(view, background);
		else
			ThemeDefaults.resetBackground(view);

		// "Elevation" is considered part of the border/background, since it involves the shadow.
		setElevationProperties(view, themeClass);
	}

	private static Drawable applyHighlightingFromClass(Drawable normalBackground, View view, ThemeClassDefinition themeClass, BackgroundOptions backgroundOptions)
	{
		// In LOLLIPOP map highlighted background color to ripple in touchable controls.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
			!Strings.hasValue(themeClass.getHighlightedBackgroundImage()) &&
			backgroundOptions != null && backgroundOptions.isActionableControl())
		{
			Integer rippleColor = getColorId(themeClass.getHighlightedBackgroundColor());
			if (rippleColor == null)
				rippleColor = getAndroidThemeColorId(view.getContext(), R.attr.colorControlHighlight);

			if (rippleColor != null)
			{
				ColorStateList rippleColorStateList = new ColorStateList(new int[][] { new int[] {} }, new int[] { rippleColor });

				Drawable rippleMask;
				if (normalBackground != null && themeClass.hasBorder())
				{
					GradientDrawable gradientMask = new GradientDrawable();
					gradientMask.setCornerRadius(themeClass.getCornerRadius());
					gradientMask.setStroke(themeClass.getBorderWidth() * 2, Color.TRANSPARENT);

					gradientMask.setColor(Color.WHITE);

					rippleMask = gradientMask;
				}
				else
					rippleMask = new ColorDrawable(Color.WHITE); // Color is irrelevant, must only be a solid drawable that fills a rectangle.

				return new RippleDrawable(rippleColorStateList, normalBackground, rippleMask);
			}
		}

		Drawable highlightedBackground = null;

		if (Strings.hasValue(themeClass.getHighlightedBackgroundColor()) || Strings.hasValue(themeClass.getHighlightedBackgroundImage()))
			highlightedBackground = createBackgroundDrawableFromClass(themeClass, BackgroundOptions.copy(backgroundOptions).setIsHighlighted(true));

		if (highlightedBackground != null)
		{
			if (normalBackground == null)
				normalBackground = ThemeDefaults.getDefaultBackground(view);

			StateListDrawable stateBackground = new StateListDrawable();
			stateBackground.addState(new int[] { android.R.attr.state_selected }, highlightedBackground);
			stateBackground.addState(new int[] { android.R.attr.state_pressed }, highlightedBackground);
			stateBackground.addState(new int[] { }, normalBackground);

			return stateBackground;
		}
		else if (normalBackground != null)
		{
			return normalBackground;
		}
		else
			return null;
	}

	@SuppressLint("NewApi")
	private static void setElevationProperties(View view, ThemeClassDefinition themeClass)
	{
		Integer elevation = themeClass.getElevation();
		if (elevation != null)
		{
			ViewCompat.setElevation(view, elevation);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			{
				// For buttons, the StateListAnimator overrides the specified elevation. So clear it.
				// See http://stackoverflow.com/questions/27080338/android-5-0-androidelevation-works-for-view-but-not-button
				if (view instanceof Button)
					view.setStateListAnimator(null);
			}
		}
		else
			ThemeDefaults.resetElevation(view);
	}

	/**
	 * Sets the background properties (image and color) for the whole activity window.
	 */
	public static void setBackground(Activity activity, ThemeApplicationClassDefinition themeClass)
	{
		Drawable background = createBackgroundDrawableFromClass(themeClass, BackgroundOptions.DEFAULT);
		if (background != null)
			activity.getWindow().setBackgroundDrawable(background);
	}

	/**
	 * Sets the background properties (image, color, and elevation) for the ActionBar.
	 */
	@SuppressLint("NewApi")
	public static void setActionBarBackground(Activity activity, ActionBar actionBar, ThemeApplicationBarClassDefinition themeClass,
											  boolean animateBackgroundChange, ThemeApplicationBarClassDefinition previousClass)
	{
		Drawable background = createBackgroundDrawableFromClass(themeClass, BackgroundOptions.DEFAULT);

		Integer ThemeId = ResourceManager.getThemeType(activity);

		if (background == null)
		{
			background = getActionBarDefaultColor(activity, ThemeId);
		}

		if (animateBackgroundChange)
		{
			Drawable oldBackground = createBackgroundDrawableFromClass(previousClass, BackgroundOptions.DEFAULT);
			if (oldBackground == null)
			{
				oldBackground = getActionBarDefaultColor(activity, ThemeId);
			}

			TransitionDrawable changeBackground = new TransitionDrawable(new Drawable[]{oldBackground, background});
			changeBackground.setCrossFadeEnabled(true);

			actionBar.setBackgroundDrawable(changeBackground);
			changeBackground.startTransition(activity.getResources().getInteger(android.R.integer.config_mediumAnimTime));
		}
		else
		{
			if (background!=null)
				actionBar.setBackgroundDrawable(background);
		}

		Integer elevation = themeClass.getElevation();
		setActionBarElevation(activity, actionBar, elevation);

		// Task color must match the primary color (i.e. the application bar color).
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			Integer primaryColor = ThemeUtils.getColorId(themeClass.getBackgroundColor());
			if (primaryColor != null)
			{
				// task description color cannot be transparent
				int taskDescriptionColor  = Color.rgb(Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor));
				TaskDescription taskDescription = new TaskDescription(null, null, taskDescriptionColor);
				activity.setTaskDescription(taskDescription);
			}
		}
	}

	private static Drawable getActionBarDefaultColor(Activity activity, Integer ThemeId)
	{
		int colorPrimary = R.attr.colorPrimary;
		int actionBarColor = ThemeUtils.getAndroidThemeColorId(activity, colorPrimary);
		Drawable background = new ColorDrawable(actionBarColor);

		return background;
	}

	public static void resetElevation(Activity activity, ActionBar actionBar, boolean restoreOldValue)
	{
		if (activity!=null && actionBar!=null)
		{
			if (restoreOldValue && elevationValue > 0)
			{
				setActionBarElevation(activity, actionBar, Math.round(elevationValue));
			} else
			{
				float elevation = actionBar.getElevation();
				setActionBarElevation(activity, actionBar, 0);
				if (elevation > 0)
				{
					elevationValue = elevation;
				}
			}
		}
	}

	private static void setActionBarElevation(Activity activity, ActionBar actionBar, Integer elevation)
	{
		if (elevation != null)
		{
			actionBar.setElevation(elevation);

			// setElevation() is ignored for pre-5.0. The standard way to remove the shadow would be to override
			// windowContentOverlay in the theme. Here we remove the shadow programmatically (using reflection, yuck).
			if (elevation == 0 && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
			{
				try
				{
					View content = activity.findViewById(android.R.id.content);
					ReflectionHelper.setField(content, "mForeground", null);
					View overlay = activity.findViewById(R.id.decor_content_parent);
					ReflectionHelper.setField(overlay, "mWindowContentOverlay", null);
				}
				catch (Exception e)
				{
					// Reflection failed, too bad.
					e.printStackTrace();
				}
			}
		}
	}


	@SuppressLint({ "NewApi", "InlinedApi" })
	public static void setStatusBarColor(Activity activity, ThemeApplicationBarClassDefinition themeClass,
										 boolean animateBackgroundChange, ThemeApplicationBarClassDefinition previousThemeClass)
	{
		if (CompatibilityHelper.isStatusBarOverlayingAvailable())
		{

			Integer statusBarColor = getColorId(themeClass.getStatusBarColor());
			if (statusBarColor == null)
			{
				// Reset to original colorPrimaryDark.
				statusBarColor = getStatusBarDefaultColor(activity);
			}

			if (statusBarColor != null)
			{
				if (animateBackgroundChange)
				{
					Integer oldStatusBarColor = null;
					if (previousThemeClass==null)
						Services.Log.debug("no previous theme class");
					else
						oldStatusBarColor = getColorId(previousThemeClass.getStatusBarColor());

					if (oldStatusBarColor == null)
					{
						// Reset to original colorPrimaryDark.
						oldStatusBarColor = getStatusBarDefaultColor(activity);
					}

					if (oldStatusBarColor != null)
					{
						final ValueAnimator valueAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
								oldStatusBarColor,
								statusBarColor);

						//final GradientDrawable background = (GradientDrawable) view.getBackground();
						final Window myWindow = activity.getWindow();
						final DrawerLayout myDrawerLayout = (DrawerLayout)activity.findViewById(R.id.drawer_layout);
						final FrameLayout myFrameStatusBarLayout = (FrameLayout)activity.findViewById(R.id.statusBarDummyTop);

						valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
						{
							@Override
							public void onAnimationUpdate(final ValueAnimator animator)
							{
								//when use slide
								if (myDrawerLayout!=null)
								{
									myDrawerLayout.setStatusBarBackgroundColor((Integer) animator.getAnimatedValue());
									myFrameStatusBarLayout.setBackgroundColor((Integer) animator.getAnimatedValue());
									myWindow.setStatusBarColor(Color.TRANSPARENT);
								}
								else
								{
									// if not slide set status bar normally.
									myWindow.setStatusBarColor((Integer) animator.getAnimatedValue());
								}
							}

						});
						valueAnimator.setDuration(activity.getResources().getInteger(android.R.integer.config_mediumAnimTime));
						valueAnimator.start();
						return;
					}
				}
				setStatusBarColor(activity, statusBarColor);

			}
		}
	}

	@SuppressLint({ "NewApi", "InlinedApi" })
	private static void setStatusBarColor(Activity activity, Integer statusBarColor)
	{
		if (CompatibilityHelper.isStatusBarOverlayingAvailable())
		{
			//when use slide
			DrawerLayout myDrawerLayout = (DrawerLayout)activity.findViewById(R.id.drawer_layout);
			FrameLayout myFrameStatusBarLayout = (FrameLayout)activity.findViewById(R.id.statusBarDummyTop);
			if (myDrawerLayout!=null)
			{
				myDrawerLayout.setStatusBarBackgroundColor(statusBarColor);
				myFrameStatusBarLayout.setBackgroundColor(statusBarColor);
				activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
			}
			else
			{
				// if not slide set status bar normally.
				activity.getWindow().setStatusBarColor(statusBarColor);
			}
		}
	}

	private static Integer getStatusBarDefaultColor(Activity activity)
	{
		int colorPrimary = R.attr.colorPrimaryDark;
		return ThemeUtils.getAndroidThemeColorId(activity, colorPrimary);
	}

	public static void setTitleFontProperties(Activity activity, ThemeClassDefinition appBarsClass)
	{
		Toolbar actionBarToolbar = (Toolbar)activity.findViewById(R.id.action_bar);
		if (actionBarToolbar != null)
		{
			try
			{
				// We can set a color with standard methods, but setTextAppearance() only admits resources.
				// Get access to the internal TextView and customize it directly.
				// This is hacky stuff, and may break later. It's working with appcompat_v7 r22.
				TextView titleView = (TextView)ReflectionHelper.getField(actionBarToolbar, "mTitleTextView");
				if (titleView != null)
					setFontProperties(titleView, appBarsClass);
			}
			catch (Exception e)
			{
				Services.Log.warning("Exception trying to reflect title view", e);
			}
		}
	}

	public static void setAppBarIconImage(ActionBar bar, ThemeApplicationBarClassDefinition appBarsClass)
	{
		Drawable iconImage = createDrawableImageFromClass(appBarsClass, appBarsClass.getIcon());
		if (iconImage != null)
			bar.setIcon(iconImage);
	}

	public static void setAppBarTitleImage(Activity activity, ActionBar bar, ThemeApplicationBarClassDefinition appBarsClass)
	{
		Drawable titleImage = createDrawableImageFromClass(appBarsClass, appBarsClass.getTitleImage());
		if (titleImage != null)
		{
			ImageView imageView = new ImageView(activity);
			imageView.setImageDrawable(titleImage);
			imageView.setAdjustViewBounds(true);

			ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
			params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
			bar.setCustomView(imageView, params);

			// Show image, hide title
			bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
		}
		else
		{
			bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
		}
	}

	public static Drawable createBackgroundDrawableFromClass(ThemeClassDefinition themeClass, BackgroundOptions options)
	{
		if (themeClass == null)
			return null;

		// Background and Border.
		GxGradientDrawable gradient = null;
		if (themeClass.hasBackgroundColor(options.getIsHighlighted()) || themeClass.hasBorder())
		{
			gradient = new GxGradientDrawable();
			gradient.setThemeClass(themeClass, options.getIsHighlighted());
		}

		String image = themeClass.getBackgroundImage();
		if (options.getIsHighlighted() && Strings.hasValue(themeClass.getHighlightedBackgroundImage()))
			image = themeClass.getHighlightedBackgroundImage();

		if (image.length() > 0)
		{
			Drawable back = ImageHelper.getStaticImage(image);
			if (back != null)
			{
				if (gradient == null)
				{
					BitmapDrawable bitmap = Cast.as(BitmapDrawable.class, back);
					if (bitmap != null)
					{
						// Always create a new gradient for each new background image.
						gradient = createGradientFromBitmap(bitmap, themeClass.getBackgroundImageMode(), options.getUseBitmapSize());
						return gradient;
					}
					return back;
				}
				else
				{
					// Set the gradient's size from the bitmap if specified.
					if (options.getUseBitmapSize() && back instanceof BitmapDrawable)
					{
						BitmapDrawable backBitmap = (BitmapDrawable)back;
						gradient.setSize(backBitmap.getIntrinsicWidth(), backBitmap.getIntrinsicHeight());
					}

					gradient.setBackground(back);
				}
			}
		}
		return gradient;
	}

	private static Drawable createDrawableImageFromClass(ThemeClassDefinition themeClass, String imageName)
	{
		if (imageName.length() > 0)
		{
			Drawable back = ImageHelper.getStaticImage(imageName);
			if (back != null)
			{
				BitmapDrawable bitmap = Cast.as(BitmapDrawable.class, back);
				if (bitmap != null)
				{
					// Always create a new gradient for each new background image. Initialize it with the image size.
					return createGradientFromBitmap(bitmap, themeClass.getBackgroundImageMode(), true);
				}
				else
					return back;
			}
		}

		return null;
	}

	private static GxGradientDrawable createGradientFromBitmap(BitmapDrawable bitmap, BackgroundImageMode imageMode, boolean useBitmapSize)
	{
		GxGradientDrawable gradient;
		gradient = new GxGradientDrawable();

		if (useBitmapSize)
			gradient.setSize(bitmap.getIntrinsicWidth(), bitmap.getIntrinsicHeight());

		// Set the image as background.
		gradient.setColor(MyApplication.getInstance().getResources().getColor(android.R.color.transparent));
		gradient.setBackground(bitmap);
		gradient.setBackgroundImageMode(imageMode);
		return gradient;
	}

	public static void setBackground(TableDefinition tableDef, ViewGroup rootView, ThemeClassDefinition themeClass)
	{
		if (Strings.hasValue(tableDef.getBackground()))
		{
			//String key = themeClass + MetadataLoader.getObjectName(tableDef.getBackground());
			setBackground(tableDef.getBackground(), rootView, themeClass);
		}
	}

	private static void setBackground(String background, ViewGroup rootView, ThemeClassDefinition themeClass)
	{
		Drawable back = ImageHelper.getStaticImage(MetadataLoader.getObjectName(background));
		if (back != null)
		{
			if (themeClass != null)
			{
				GxGradientDrawable draw = new GxGradientDrawable();
				draw.setThemeClass(themeClass, false);
				draw.setBackground(back);
				back = draw;
			}

			CompatibilityHelper.setBackground(rootView, back);
		}
	}

	public static String getAndroidThemeColor(Context context, int attr) {
		TypedValue tv = new TypedValue();
		context.getTheme().resolveAttribute(attr, tv, true);

	    //System.out.println("tv.string=" + tv.string);
	    //System.out.println("tv.coerced=" + tv.coerceToString());
	    String color = "#ffffff"; //$NON-NLS-1$
	    try
	    {
	    	int colorId = context.getResources().getColor(tv.resourceId);
	    	//System.out.println("colorResourceId=" + colorId);
	    	color = "#"+Integer.toHexString(colorId).substring(2); //$NON-NLS-1$
	    }
	    catch(Resources.NotFoundException ex)
	    {
	    	if (tv.coerceToString().toString().length()==9)
	    	color = "#"+ tv.coerceToString().toString().substring(3); //$NON-NLS-1$
	    }
		return color;
	}

	public static Integer getAndroidThemeColorId(Context context, int attr) {
		TypedValue tv = new TypedValue();
		context.getTheme().resolveAttribute(attr, tv, true);
		try
		{
			return context.getResources().getColor(tv.resourceId);
		}
		catch(Resources.NotFoundException ex)
		{
		}
		return null;
	}

}
