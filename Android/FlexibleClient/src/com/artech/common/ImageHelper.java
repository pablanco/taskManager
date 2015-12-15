package com.artech.common;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.artech.application.MyApplication;
import com.artech.base.metadata.enums.ImageUploadModes;
import com.artech.base.metadata.settings.UploadSizeDefinition;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.controls.common.IViewDisplayImage;
import com.fedorvlasov.lazylist.ImageLoader;

public class ImageHelper
{
	private final static ExecutorService sExecutor = Executors.newCachedThreadPool();

	public static Drawable getStaticImage(String imageName)
	{
		return getStaticImage(imageName, false);
	}

	public static Drawable getStaticImage(String imageName, boolean loadAsHdpi)
	{
		return getStaticImage(imageName, false, loadAsHdpi);
	}

	public static Drawable getStaticImage(String imageName, boolean waitForImage, boolean loadAsHdpi)
	{
		return getStaticImage(imageName, waitForImage, null, loadAsHdpi);
	}

	/**
	 * Gets the specified "static" image, given it image name. Tries to load it from
	 * the embedded resources, and if that fails, from the cache of downloaded images.
	 *
	 * If that also fails, then if the waitForServer parameter is true, the method
	 * blocks until the server returns the image. Otherwise a thread is launched to
	 * request the image from the server, though this call returns null.
	 */
	private static Drawable getStaticImage(final String imageName, boolean waitForImage, final Handler onReceive, final boolean loadAsHdpi)
	{
		if (!Services.Strings.hasValue(imageName))
			return null;

		// 1) Try to get from resources.
		Drawable drawable = getImageFromResources(imageName);
		if (drawable != null && !Services.Application.isLiveEditingEnabled())
			return drawable;

		// 2) Try to get from cache.
		drawable = getImageFromCache(imageName);
		if (drawable != null)
			return drawable;

		if (waitForImage)
		{
			// 3.a) Download the image and return it.
			drawable = getImageFromServer(imageName, loadAsHdpi);
		}
		else
		{
			// 3.b) Launch a thread to get the image.
			sExecutor.execute(new Runnable()
			{
				@Override
				public void run()
				{
					Drawable drawable = getImageFromServer(imageName, loadAsHdpi);
					if (onReceive != null)
						onReceive.receive(drawable);
				}
			});
		}

		return drawable;
	}

	private static Drawable getImageFromResources(String imageName)
	{
		int resourceId = Services.Resources.getImageResourceId(imageName);
		if (resourceId != 0)
		{
			try
			{
				return MyApplication.getAppContext().getResources().getDrawable(resourceId);
			}
			catch (OutOfMemoryError e)
			{
				ImageLoader.clearMemoryCache();
				Services.Log.Error(String.format("Out of memory loading resource '%s'.", imageName), e);

				// Return a stub drawable instead of null; otherwise it will try to download the image from the server.
				// We ALREADY have that image, we just couldn't load (ran out of memory).
				return new ColorDrawable(Color.TRANSPARENT);
			}
		}
		else
			return null;
	}

	private static Drawable getImageFromCache(String imageName)
	{
		// Cache key is image URL.
		String imageUrl = Services.Resources.getImageUri(imageName);
		if (imageUrl == null)
			return null;

		return ImageLoader.getCachedDrawable(imageUrl);
	}

	private static Drawable getImageFromServer(String imageName, boolean loadAsHdpi)
	{
		// Load Image with image loader.
		String imageUrl = Services.Resources.getImageUri(imageName);
		if (imageUrl == null)
			return null;

		return ImageLoader.getDrawable(imageUrl, imageUrl, loadAsHdpi);
	}

	public static void displayImage(IViewDisplayImage view, String imageName)
	{
		if (view != null && Services.Strings.hasValue(imageName))
		{
			Drawable drawable = getStaticImage(imageName, false, new ImageHelperHandlers.ForImageView(view), false);
			if (drawable != null)
				view.setImageDrawable(drawable);
		}
	}

	public static void displayBackground(View view, String imageName)
	{
		if (view != null && Services.Strings.hasValue(imageName))
		{
			Drawable drawable = getStaticImage(imageName, false, new ImageHelperHandlers.ForViewBackground(view), false);
			if (drawable != null)
				CompatibilityHelper.setBackground(view, drawable);
		}
	}

	public static void showStaticImage(ImageLoader loader, IViewDisplayImage icon, String imageName)
	{
		if (Services.Strings.hasValue(imageName))
		{
			// 1) Try to get from resources.
			Drawable drawable = getImageFromResources(imageName);
			if (drawable != null)
			{
				icon.setImageDrawable(drawable);
				return;
			}

			// 2) Try to get from cache.
			drawable = getImageFromCache(imageName);
			if (drawable != null)
			{
				icon.setImageDrawable(drawable);
				return;
			}

			// 3) Try to get with image loader.
			String imageUrl = Services.Resources.getImageUri(imageName);
			icon.setImageTag(imageUrl);
			loader.DisplayImage(imageUrl, icon, false);
		}
		else
		{
			StandardImages.showPlaceholderImage(icon, true);
		}
	}

	public static void showDataImage(ImageLoader loader, IViewDisplayImage imageView, String imageUri)
	{
		showDataImage(loader, imageView, imageUri, false);
	}

	public static void showDataImage(ImageLoader loader, IViewDisplayImage imageView, String imageUri, boolean placeholderRequired)
	{
		if (Strings.hasValue(imageUri))
		{
			// Try to load from resources first, in case it is embedded.
			if (showDataImageFromResource(imageView, imageUri))
				return;

			// It's an image file, either remote or in the local filesystem. Load Image with image loader.
			String imageFullPath;
			boolean showLoading = true;
			if (StorageHelper.isLocalFile(imageUri))
			{
				imageFullPath = imageUri;
				showLoading = false;
			}
			else
				imageFullPath = MyApplication.getApp().UriMaker.MakeImagePath(imageUri);

			imageView.setImageTag(imageFullPath);

			// Enqueue the request to load.
			loader.DisplayImage(imageFullPath, imageView, showLoading);
		}
		else
		{
			// Set image identifier to null, to clear it in list when reusing views.
			imageView.setImageTag(null);

			StandardImages.stopLoading(imageView);
			StandardImages.showPlaceholderImage(imageView, placeholderRequired);
		}
	}

	public static boolean showDataImageFromResource(IViewDisplayImage icon, String imageUri)
	{
		Integer resourceId = getDataImageResourceId(imageUri);
		if (resourceId != null)
		{
			icon.setImageResource(resourceId);
			return true;
		}
		else
			return false;
	}

	/**
	 * If the data image is actually embedded in the app, return its resource id.
	 * This can happen, for example, if the image is loaded with &var.FromImage(KB_image).
	 * @return The resource id if found, otherwise null.
	 */
	public static Integer getDataImageResourceId(String imageUri)
	{
		String imageResourceLC = Strings.toLowerCase(imageUri);
		if (!(imageResourceLC.startsWith("http://") || imageResourceLC.startsWith("https://")) && imageResourceLC.contains("resources"))
		{
			String lastSegment = imageUri.replace('\\', '/');
			int pos = lastSegment.lastIndexOf('/') + 1;
			if (pos > 1)
			{
				lastSegment = lastSegment.substring(pos);
				pos = lastSegment.lastIndexOf(".");
				if (pos > 1)
				{
					String resourceName = lastSegment.substring(0, pos);
					int resourceId = Services.Resources.getImageResourceId(resourceName);

					if (resourceId > 0)
						return resourceId;
				}
			}
		}

		return null;
	}

	public static void showLocalImage(ImageView imageView, String selectedImagePath, boolean getFullImage)
	{
		if (selectedImagePath != null && selectedImagePath.length() > 0)
		{
			Bitmap scaledpicture = ImageLoader.decodeFile(new File(selectedImagePath), 0, getFullImage, true);
			if (scaledpicture!=null)
				imageView.setImageBitmap(scaledpicture);
		}
	}

	public static Bitmap getScaledBitmapExactSize(String imagePath, int desireSizeInPixels)
	{
		try
		{
			File f = new File(imagePath);
			return ImageLoader.decodeFileExactSize(f, desireSizeInPixels);
		}
		catch (OutOfMemoryError e)
		{
			ImageLoader.clearMemoryCache();
			Services.Log.error(e);
		}

		return null;
	}

	public static void clearCache()
	{
		ImageLoader.clearCache();
	}

	public static Drawable getDrawableValue(String imagePath)
	{
		if (imagePath.length() > 0)
		{
			// Load Image with image loader.
			String imageFullPath = MyApplication.getApp().UriMaker.MakeImagePath(imagePath);
			return ImageLoader.getDrawable(imageFullPath, imageFullPath, true);
		}

		return null;
	}

	public interface Handler
	{
		void receive(final Drawable d);
	}

	public static File getCachedImageFile(String imageIdentifier)
	{
		if (!Services.Strings.hasValue(imageIdentifier))
			return null;

		String imageRemoteUri = MyApplication.getApp().UriMaker.MakeImagePath(imageIdentifier);
		return ImageLoader.getCachedImageFile(imageRemoteUri);
	}

	public static byte[] resizeImageIfNecessary(String imagePath, UploadSizeDefinition uploadDef) throws IOException
	{
		if (uploadDef.UploadMode==ImageUploadModes.ACTUALSIZE)
		{
			//do not resize, return actual file bytes.
			 return FileUtils.readFileToByteArray(new File(imagePath));
		}
		else if (uploadDef.SizeMode==UploadSizeDefinition.SIZEINPX)
		{
			// resize in px
			return resizeImageIfNecessaryExactSize(imagePath, uploadDef.SizeLimit);
		}
		else
		{
			// resize in kb
			return resizeImageIfNecessarySizeInKB(imagePath, uploadDef.SizeLimit);
		}
	}

	private static byte[] resizeImageIfNecessarySizeInKB(String imagePath, double desireSizeInKB) throws IOException
	{
		File imageFile = new File(imagePath);
		double oriSize2 = imageFile.length() / 1024;
		double ratio = oriSize2 / desireSizeInKB;
		int scale = ImageUploadModes.getScaleRatioFromCoeficient(ratio);

		if (scale == 1)
		{
			//return actual file, the image already has the desire size.
			return FileUtils.readFileToByteArray(new File(imagePath));
		}

		byte[] fileByteArray = resizeToScale(imagePath, scale);
		if (fileByteArray != null)
		{
			double newsize = fileByteArray.length/1024;

			// 2do change if necessary
			if ((desireSizeInKB * 1.1) < newsize)
			{
				//retry with mayor scale, how to calculate?
				scale = scale + 1;
				fileByteArray = resizeToScale(imagePath, scale);
			}
			else if ((desireSizeInKB * 0.9) > newsize)
			{
				byte[] firstResult = fileByteArray;
				//retry with minor scale, how to calculate?
				scale = scale - 1;
				fileByteArray = resizeToScale(imagePath, scale);
				double retrysize =  fileByteArray.length/1024;
				if ( (Math.abs(retrysize-desireSizeInKB) > Math.abs(newsize-desireSizeInKB))
						|| ((desireSizeInKB * 1.1) < retrysize) )
				{
					//too less scale keep first result
					fileByteArray = firstResult;
				}
			}
		}

		return fileByteArray;
	}

	private static byte[] getByteArrayFromImageWithQuality(Bitmap bmp) throws IOException
	{
		//this method return byte[] from bitmap //NORMAL quality
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (bmp != null)
		{
			bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
			return baos.toByteArray();
		}
		return null;
	}

	public static byte[] resizeImageIfNecessaryExactSize(String imagePath, int desireSizeInPixels) throws IOException
	{
		byte[] fileByteArray = null;
		Bitmap scaledpictureI = getScaledBitmapExactSize(imagePath, desireSizeInPixels);
		if (scaledpictureI != null)
		{
			fileByteArray = getByteArrayFromImageWithQuality(scaledpictureI);
			scaledpictureI.recycle();
		}
		return fileByteArray;
	}

	private static byte[] resizeToScale(String filePath, int scale) throws IOException
	{
		// In this case do NOT pre-rotate according to EXIF orientation, we want to keep the bitmap as-is.
		// This supposes that this method is only called in the context of uploading images to the server!
		byte[] fileByteArray = null;
		Bitmap scaledpictureI = ImageLoader.decodeFile(new File(filePath), scale, false, false);
		if (scaledpictureI!=null)
		{
			fileByteArray = getByteArrayFromImageWithQuality(scaledpictureI);
			scaledpictureI.recycle();
		}
		return fileByteArray;
	}
}
