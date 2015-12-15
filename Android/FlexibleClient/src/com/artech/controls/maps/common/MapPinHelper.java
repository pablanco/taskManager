package com.artech.controls.maps.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.artech.application.MyApplication;
import com.artech.base.model.Entity;
import com.artech.base.utils.Strings;
import com.artech.common.ImageHelper;
import com.artech.common.StorageHelper;
import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.utils.BitmapUtils;
import com.artech.utils.Cast;
import com.fedorvlasov.lazylist.ImageLoader;

public class MapPinHelper
{
	public static class ResourceOrBitmap
	{
		public final Integer resourceId;
		public final Bitmap bitmap;

		public ResourceOrBitmap(int resourceId)
		{
			this.resourceId = resourceId;
			this.bitmap = null;
		}

		public ResourceOrBitmap(Bitmap bitmap)
		{
			this.bitmap = bitmap;
			this.resourceId = null;
		}

		boolean isNull()
		{
			return (resourceId == null && bitmap == null);
		}
	}

	private final Context mContext;
	private final GxMapViewDefinition mMapDefinition;
	private ImageLoader mImageLoader;

	public MapPinHelper(Context context, GxMapViewDefinition mapDefinition, ImageLoader imageLoader)
	{
		mContext = context;
		mMapDefinition = mapDefinition;
		mImageLoader = imageLoader;
	}

	public @NonNull ResourceOrBitmap getPinImage(Entity item)
	{
		ResourceOrBitmap pin = loadPinResourceOrBitmap(item);
		return applyPinImageClass(pin);
	}

	private @NonNull ResourceOrBitmap loadPinResourceOrBitmap(Entity item)
	{
		// Try for item's custom  pin image.
		if (Strings.hasValue(mMapDefinition.getPinImageExpression()))
		{
			String imageValue = Cast.as(String.class, item.getProperty(mMapDefinition.getPinImageExpression()));
			if (Strings.hasValue(imageValue))
			{
				// 1) From resources.
				Integer resourceId = ImageHelper.getDataImageResourceId(imageValue);
				if (resourceId != null)
					return new ResourceOrBitmap(resourceId);

				// 2) From a bitmap file (either local or in the server).
				String imageUrl;
				if (!StorageHelper.isLocalFile(imageValue))
					imageUrl = MyApplication.getApp().UriMaker.MakeImagePath(imageValue);
				else
					imageUrl = imageValue;

				Bitmap pinImageBitmap = mImageLoader.getBitmap(imageUrl);
				if (pinImageBitmap != null)
					return new ResourceOrBitmap(pinImageBitmap);
			}
		}

		// Try for generic pin item image.
		int pinImageResourceId = mMapDefinition.getPinImageResourceId();
		if (pinImageResourceId != 0)
			return new ResourceOrBitmap(pinImageResourceId);

		// No pin, default will be used.
		return new ResourceOrBitmap(null);
	}

	private @NonNull ResourceOrBitmap applyPinImageClass(@NonNull ResourceOrBitmap pin)
	{
		if (pin.isNull())
			return pin;

		GxMapViewDefinition.PinImageProperties properties = mMapDefinition.getPinImageProperties();
		if (properties.width != 0 && properties.height != 0 && properties.scaleType != null)
		{
			// We need a bitmap to resize, either the bitmap we already had or read it from resources with its id.
			Bitmap originalBitmap = pin.bitmap;
			if (originalBitmap == null)
				originalBitmap = BitmapUtils.createFromDrawable(mContext.getResources().getDrawable(pin.resourceId));

			if (originalBitmap != null)
			{
				Bitmap newBitmap = BitmapUtils.createScaledBitmap(mContext.getResources(), originalBitmap, properties.width, properties.height, properties.scaleType);
				if (newBitmap != null)
					pin = new ResourceOrBitmap(newBitmap);
			}
		}

		return pin;
	}
}
