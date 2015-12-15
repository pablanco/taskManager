package com.artech.common;

import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.artech.R;
import com.artech.android.ResourceManager;
import com.artech.base.metadata.enums.ActionTypes;
import com.artech.base.metadata.theme.ThemeApplicationClassDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.PlatformHelper;
import com.artech.controls.GxImageViewData;
import com.artech.controls.ImageViewDisplayImageWrapper;
import com.artech.controls.common.IViewDisplayImage;

public class StandardImages
{
	public static void startLoading(IViewDisplayImage view)
	{
		// Clear any previous image.
		view.setImageDrawable(null);

		// For data images, show a progress indicator (maybe).
		if (view instanceof GxImageViewData)
		{
			ThemeClassDefinition imageClass = getImageClass(view);
			if (imageClass != null && imageClass.getBooleanProperty("image_loading_indicator", true)) //$NON-NLS-1$
				((GxImageViewData)view).setLoading(true);
		}
	}

	public static void stopLoading(IViewDisplayImage view)
	{
		if (view instanceof GxImageViewData)
			((GxImageViewData)view).setLoading(false);
		else
			view.setImageDrawable(null);
	}

	public static void showPlaceholderImage(IViewDisplayImage view, boolean placeholderRequired)
	{
		int defaultPlaceholder = (placeholderRequired ? R.drawable.stub : -1);
		setImage(view, ThemeApplicationClassDefinition.PLACEHOLDER_IMAGE, defaultPlaceholder);
	}

	public static void setLinkImage(ImageView view)
	{
		setRightImage(view, ThemeApplicationClassDefinition.PROMPT_IMAGE, ResourceManager.getContentDrawableFor(view.getContext(), ActionTypes.Link));
	}

	public static void setPromptImage(ImageView view)
	{
		setRightImage(view, ThemeApplicationClassDefinition.PROMPT_IMAGE, ResourceManager.getContentDrawableFor(view.getContext(), ActionTypes.Prompt));
	}

	public static void setActionImage(ImageView view, String action)
	{
		setRightImage(view, null, ResourceManager.getContentDrawableFor(view.getContext(), action));
	}

	private static void setRightImage(ImageView view, String appImage, int defaultResource)
	{
		view.setScaleType(ScaleType.CENTER);
		setImage(ImageViewDisplayImageWrapper.to(view), appImage, defaultResource);
	}

	private static void setImage(IViewDisplayImage view, String appImage, int defaultResource)
	{
		if (appImage != null)
		{
			ThemeClassDefinition imageClass = getImageClass(view);
			if (imageClass != null)
			{
				String imageName = imageClass.getImage(appImage);
				if (Services.Strings.hasValue(imageName))
				{
					ImageHelper.displayImage(view, imageName);
					return;
				}
			}
		}

		// Set default image, or -1 to clear image.
		if (defaultResource == -1)
			view.setImageDrawable(null);
		else if (defaultResource != 0)
			view.setImageResource(defaultResource);
	}

	private static ThemeClassDefinition getImageClass(IViewDisplayImage view)
	{
		// Image properties are read from the specific class image, if set, or from the application class otherwise.
		ThemeClassDefinition imageClass = view.getThemeClass();

		if (imageClass == null)
			imageClass = PlatformHelper.getApplicationClass();

		return imageClass;
	}
}
