package com.fedorvlasov.lazylist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;

import com.artech.activities.IGxBaseActivity;
import com.artech.application.MyApplication;
import com.artech.base.services.Services;
import com.artech.common.StandardImages;
import com.artech.common.StorageHelper;
import com.artech.controls.common.IViewDisplayImage;
import com.artech.utils.Cast;

public class ImageLoader
{
	private static final String LOG_TAG = "ImageLoader";

	private static final LruCache<String, Bitmap> sCache = initCache();
	private static ConcurrentHashMap<String, SoftReference<Drawable>> cacheDrawable = new ConcurrentHashMap<String, SoftReference<Drawable>>();

	private static final File cacheDir = StorageHelper.getImagesDirectory();
	private final PhotosQueue photosQueue = new PhotosQueue();

	private ArrayList<PhotosLoader> mLoaderThreads;
	private static final int NUMBER_OF_THREADS = 2;

	public ImageLoader(Context context)
	{
		// Make the background thread low priority. This way it will not affect the UI performance
		mLoaderThreads = new ArrayList<PhotosLoader>();
		for (int i = 0; i < NUMBER_OF_THREADS; i++)
		{
			PhotosLoader loader = new PhotosLoader();
			loader.setPriority(Thread.NORM_PRIORITY - 1);
			mLoaderThreads.add(loader);
		}
	}

	private static LruCache<String, Bitmap> initCache()
	{
	    final int maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024);
	    final int cacheSize = maxMemory / 8;

		return new LruCache<String, Bitmap>(cacheSize)
		{
			@Override
			protected int sizeOf(String key, Bitmap value)
			{
				if (value != null)
					return (value.getRowBytes() * value.getHeight()) / 1024;
				else
					return 0;
			}
		};
	}

	public void DisplayImage(String url, IViewDisplayImage imageView, boolean showLoading)
	{
		Bitmap cachedBitmap = getFromMemoryCache(url);
		if (cachedBitmap != null)
		{
			StandardImages.stopLoading(imageView);
			imageView.setImageBitmap(cachedBitmap);
		}
		else
		{
			// Loading is shown here.
			// Not necessary if the image is cached, since it will be displayed immediately.
			if (showLoading)
				StandardImages.startLoading(imageView);

			queuePhoto(url, imageView);
		}
	}

	public void cancelDisplay(IViewDisplayImage imageView)
	{
		synchronized (photosQueue.photosToLoad)
		{
			photosQueue.clean(imageView);
		}
	}

	private void queuePhoto(String url, IViewDisplayImage imageView)
	{
		// This ImageView may be used for other images before. So there may be some old tasks in the queue. We need to discard them.
		PhotoToLoad p = new PhotoToLoad(url, imageView);
		synchronized (photosQueue.photosToLoad)
		{
			photosQueue.clean(imageView);
			photosQueue.photosToLoad.add(p);
			photosQueue.photosToLoad.notifyAll();
		}

		// start thread if it's not started yet
		for (PhotosLoader loader : mLoaderThreads)
		{
			if (loader.getState() == Thread.State.NEW)
				loader.start();
		}
	}

	public Bitmap getBitmap(String imageUrl)
	{
		return getBitmap(imageUrl, false);
	}

	public Bitmap getBitmap(String imageUrl, boolean getFullImage)
	{
		if (StorageHelper.isLocalFile(imageUrl))
		{
			// Read local file from filesystem.
			File localFile = new File(imageUrl);
			return decodeFile(localFile, 0, getFullImage, true);
		}
		else
		{
			File cacheFile = getCachedImageFile(imageUrl);

			// Try to get from SD card file cache (already downloaded?)
			Bitmap b = decodeFile(cacheFile, 0, getFullImage, true);
			if (b != null)
			{
				putInMemoryCache(imageUrl, b);
				return b;
			}

			try
			{
				// Load from remote server.
				InputStream is = new URL(imageUrl).openStream();
				OutputStream os = new FileOutputStream(cacheFile);
				IOUtils.copy(is, os);
				os.close();
				return decodeFile(cacheFile, 0, getFullImage, true);
			}
			catch (Exception ex)
			{
				Services.Log.Error(LOG_TAG, "Exception during getBitmap()", ex); //$NON-NLS-1$
				return null;
			}
		}
	}

	public static File getImage(String imagePath)
	{
		if (StorageHelper.isLocalFile(imagePath))
			return new File(imagePath);

	 	String imageFullPath = MyApplication.getInstance().getUriMaker().MakeImagePath(imagePath);
	    File file = getCachedImageFile(imageFullPath);

	    if (file == null || !file.exists())
	    {
	    	try
	    	{
			    String filename = Services.Serializer.makeFileName(imageFullPath);
			    file = new File(cacheDir, filename);
	    		InputStream is = new URL(imageFullPath).openStream();
				OutputStream os = new FileOutputStream(file);
				IOUtils.copy(is, os);
				os.close();
	    	}
			catch (Exception ex)
			{
				Services.Log.Error(LOG_TAG, "Exception during getImage()", ex); //$NON-NLS-1$
				return null;
			}
	    }

		return file;
	}

	public static File getCachedImageFile(String imageRemoteUri)
	{
		String filename = Services.Serializer.makeFileName(imageRemoteUri);
		File cacheFile = new File(cacheDir, filename);
		return cacheFile;
	}

	public static Bitmap decodeFile(File file, int scaleFactor, boolean getFullImage, boolean applyExifRotation)
	{
		Bitmap bitmap = internalDecodeFile(file, scaleFactor, getFullImage);
		if (bitmap != null && applyExifRotation)
			bitmap = internalApplyExifRotation(file, bitmap);

		return bitmap;
	}

	/**
	 * Decodes a bitmap from the File, possibly resizing it to avoid excessive memory consumption.
	 * Does not apply a rotation according to the file's EXIF orientation.
	 */
	private static Bitmap internalDecodeFile(File f, int scaleFactor, boolean getFullImage)
	{
		if (f == null || !f.exists())
			return null;

		try
		{
			if (getFullImage)
			{
				scaleFactor = 1;
				BitmapFactory.Options o2 = getScaleOptions(scaleFactor);
				return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
			}

			//decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f),null,o);

			if (scaleFactor == 0)
				scaleFactor = getScaleForBitmap(o, false);

			BitmapFactory.Options o2 = getScaleOptions(scaleFactor);
			Bitmap scaledBitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, o2);

			int dstWidth = o.outWidth / scaleFactor;
			int dstHeight = o.outHeight / scaleFactor;
			if (dstWidth < o2.outWidth || dstHeight < o2.outHeight)
			{
				Services.Log.debug("Using createScaledBitmap to resize to scale " + scaleFactor);
				Bitmap rescaledBitmap = Bitmap.createScaledBitmap(scaledBitmap, dstWidth, dstHeight, true);
				scaledBitmap.recycle();
				scaledBitmap = rescaledBitmap;
			}

			return scaledBitmap;

		}
		catch (IOException e)
		{
			Services.Log.Error(LOG_TAG, String.format("IOException decoding file '%s'.", f.getName()), e); //$NON-NLS-1$
		}
		catch (OutOfMemoryError e)
		{
			clearMemoryCache();
			Services.Log.Error(LOG_TAG, String.format("Out of memory decoding file '%s'.", f.getName()), e); //$NON-NLS-1$
		}

		return null;
	}

	private static Bitmap internalApplyExifRotation(File srcFile, Bitmap bitmap)
	{
		if (srcFile == null || !srcFile.exists() || bitmap == null)
			throw new IllegalArgumentException("Invalid parameters for internalApplyExifRotation().");

		try
		{
			ExifInterface fileExif = new ExifInterface(srcFile.getAbsolutePath());
			int exifOrientation = fileExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			if (exifOrientation != ExifInterface.ORIENTATION_NORMAL)
			{
				// We only consider the most common cases.
				Matrix matrix = new Matrix();
				if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90)
					matrix.setRotate(90);
				else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180)
					matrix.setRotate(180);
				else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270)
					matrix.setRotate(270);

				if (!matrix.isIdentity())
				{
					Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

					// Discard the old bitmap, and set up to return the rotated one instead.
					bitmap.recycle();
					bitmap = rotatedBitmap;
				}
			}
		}
		catch (Exception e)
		{
			// Probably an OutOfMemoryError, or possibly IOException while reading the EXIF metadata.
			Services.Log.warning(LOG_TAG, "Exception trying to apply EXIF orientation; returning original bitmap", e);
		}

		return bitmap;
	}

	//decodes image and scales it to reduce memory consumption
	// used in upload to use the exact size
	public static Bitmap decodeFileExactSize(File f, int desireSizeInPixels)
	{
		if (f == null || !f.exists())
			return null;

		try
		{
			//decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f),null,o);

			int maxSize = Math.max(o.outWidth, o.outHeight);
			int scaleFactor =  (int)Math.floor(maxSize/desireSizeInPixels);
			boolean shouldResizeExact = true;
			if (scaleFactor<1)
			{
				scaleFactor = 1;
				shouldResizeExact = false;
			}

			BitmapFactory.Options o2 = getScaleOptions(scaleFactor);
			Bitmap scaledpicture = BitmapFactory.decodeStream(new FileInputStream(f), null, o2);

			int dstWidth;
			int dstHeight;
			if (o.outWidth>o.outHeight)
			{
				dstWidth = desireSizeInPixels;
				double factor = (double)o.outWidth/desireSizeInPixels;
				dstHeight = (int) Math.floor((o.outHeight / factor));
			}
			else
			{
				dstHeight = desireSizeInPixels;
				double factor = (double)o.outHeight/desireSizeInPixels;
				dstWidth = (int) Math.floor((o.outWidth / factor));
			}

			//return the exact size
			if (shouldResizeExact && o2.outWidth!=desireSizeInPixels && o2.outHeight!=desireSizeInPixels)
			{
				Services.Log.debug("createScaledBitmap to resize exact size " + desireSizeInPixels);
				Bitmap scaledPictureFinal = Bitmap.createScaledBitmap(scaledpicture, dstWidth, dstHeight, true);
				scaledpicture.recycle();
				return scaledPictureFinal;
			}
			else
				return scaledpicture;

		}
		catch (IOException e)
		{
			Services.Log.Error(LOG_TAG, String.format("IOException decoding file '%s'.", f.getName()), e); //$NON-NLS-1$
		}
		catch (OutOfMemoryError e)
		{
			clearMemoryCache();
			Services.Log.Error(LOG_TAG, String.format("Out of memory decoding file '%s'.", f.getName()), e); //$NON-NLS-1$
		}

		return null;
	}

	@SuppressWarnings("deprecation")
	private static BitmapFactory.Options getScaleOptions(int scale)
	{
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize= scale;
		o2.inPurgeable = true;
		o2.inInputShareable = true;
		o2.inTempStorage = new byte[16*1024];
		return o2;
	}

	private static int getScaleForBitmap(BitmapFactory.Options o, boolean toUpload)
	{
		int requiredSize = 500;

		// Default required size for display is same as screen's smallest width in pixels
		// (reasonable for image displayed with "Fit").
		if (!toUpload)
			requiredSize = Services.Device.dipsToPixels(Services.Device.getScreenSmallestWidth());

		int width = o.outWidth, height = o.outHeight;
		int width_tmp = o.outWidth, height_tmp = o.outHeight;
		int scale = 1;

		// fit to square of REQUIRED_SIZE by REQUIRED_SIZE (does not return a power of 2).
		while (width_tmp > requiredSize || height_tmp > requiredSize)
		{
			scale++;
			width_tmp = width/scale;
			height_tmp = height/scale;
		}

		return scale;
	}

	public static Bitmap decodeByteArray(byte[] array, int size)
	{
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(array, 0, size, o);

		int scale = getScaleForBitmap(o, true);

		BitmapFactory.Options o2 = getScaleOptions(scale);
		return BitmapFactory.decodeByteArray(array, 0, size, o2);
	}

	//Task for the queue
	private class PhotoToLoad
	{
		public String url;
		public IViewDisplayImage imageView;

		public PhotoToLoad(String u, IViewDisplayImage i)
		{
			url = u;
			imageView = i;
		}
	}

	public void stopThread()
	{
		for (PhotosLoader loader : mLoaderThreads)
			loader.interrupt();
	}

	//stores list of photos to download
	private class PhotosQueue
	{
		private final ArrayList<PhotoToLoad> photosToLoad=new ArrayList<PhotoToLoad>();

		//removes all instances of this ImageView
		public void clean(IViewDisplayImage image)
		{
			int removed = 0;
			for (int j = photosToLoad.size() - 1 ;j >= 0; j--)
			{
				if (photosToLoad.get(j) != null && photosToLoad.get(j).imageView == image)
				{
					photosToLoad.remove(j);
					removed++;
				}
			}

			if (removed != 0)
				Services.Log.debug(LOG_TAG, String.format("Cancel() cleared %s pending operations.", removed));
		}
	}

	private class PhotosLoader extends Thread
	{
		@Override
		public void run()
		{
			try
			{
				while(true)
				{
					// Thread waits until there are any images to load in the queue
					PhotoToLoad photoToLoad;
					synchronized (photosQueue.photosToLoad)
					{
						while (photosQueue.photosToLoad.size() == 0)
							photosQueue.photosToLoad.wait();

						photoToLoad = photosQueue.photosToLoad.get(0);
						photosQueue.photosToLoad.remove(0);
					}

					// Check if this ImageView has been reused since the request was queued.
					if (!isLoadValidForImageView(photoToLoad))
						continue;

					Bitmap bmp = getBitmap(photoToLoad.url);
					if (bmp != null)
						putInMemoryCache(photoToLoad.url, bmp);

					// Check if this ImageView has been reused while the image was downloading from the server.
					if (!isLoadValidForImageView(photoToLoad))
						continue;

					// Display the image.
					postImageLoad(photoToLoad.imageView, new BitmapDisplayer(bmp, photoToLoad));

					if (Thread.interrupted())
						break;
				}
			}
			catch (InterruptedException e)
			{
				//allow thread to exit
			}
		}

		private void postImageLoad(IViewDisplayImage view, BitmapDisplayer displayer)
		{
			Services.Device.runOnUiThread(displayer);
		}
	}

	private boolean isLoadValidForImageView(PhotoToLoad photoToLoad)
	{
		// Check that a request for image load is still valid (basically, that the ImageView hasn't been
		// reused for other data).
		String imageViewTag = photoToLoad.imageView.getImageTag();
		return (imageViewTag != null && imageViewTag.equalsIgnoreCase(photoToLoad.url));
	}

	//Used to display bitmap in the UI thread
	private class BitmapDisplayer implements Runnable
	{
		private final Bitmap mBtmap;
		private final PhotoToLoad mPhotoToLoad;
		public BitmapDisplayer(Bitmap b, PhotoToLoad photoToLoad)
		{
			mBtmap = b;
			mPhotoToLoad = photoToLoad;
		}

		@Override
		public void run()
		{
			// Last check, in UI thread.
			// Previous checks were done in background and situation may have changed
			// while this code was waiting to run.
			if (!isLoadValidForImageView(mPhotoToLoad))
				return;

			IViewDisplayImage imageView = mPhotoToLoad.imageView;
			StandardImages.stopLoading(imageView);

			if (mBtmap != null)
				imageView.setImageBitmap(mBtmap);
			else
				StandardImages.showPlaceholderImage(imageView, false);
		}
	}

	public static void clearCache()
	{
		// Clear memory cache
		clearMemoryCache();

		// Clear SD cache
		File[] files = cacheDir.listFiles();
		if (files != null)
		{
			for(File f : files)
				FileUtils.deleteQuietly(f);
		}
	}

	public static Drawable getDrawable(String url, String source2, boolean loadAsHdpi)
	{
		try
		{
			Drawable drawable = getCachedDrawable(url);
			if (drawable != null)
				return drawable;

			InputStream is = new URL(url).openStream();
			try
			{
				// To ensure get the correct target density
				// http://developer.android.com/reference/android/graphics/drawable/BitmapDrawable.html#BitmapDrawable(java.io.InputStream)
				// load image as were hdpi, set the correct density to the bitmap
				// http://stackoverflow.com/questions/8837810/android-load-drawable-programatically-and-resize-it
				// Could fail in some htc phones

				if (loadAsHdpi)
				{
					// 	set options to resize the image
					Options opts = new BitmapFactory.Options();
					opts.inDensity = DisplayMetrics.DENSITY_HIGH;
					drawable = Drawable.createFromResourceStream(MyApplication.getAppContext().getResources(), null, is, source2, opts);
				}
				else
				{
					// Load image with the same density as the device (in case we want to use "no scale").
					drawable = Drawable.createFromStream(is, source2);
					if (drawable instanceof BitmapDrawable)
						((BitmapDrawable)drawable).setTargetDensity(MyApplication.getAppContext().getResources().getDisplayMetrics());
				}

			}
			catch (NullPointerException ex)
			{
				//if fails try the old method , for htc olds phones
				drawable = Drawable.createFromStream(is, source2);
			}
			finally
			{
				is.close();
			}
			cacheDrawable.put(url, new SoftReference<Drawable>(drawable));
			return drawable;

		}
		catch (Exception ex)
		{
			Services.Log.Error(LOG_TAG, "Exception during getDrawable()", ex); //$NON-NLS-1$
			return null;
		}
		catch (OutOfMemoryError e)
		{
			clearMemoryCache();
			Services.Log.Error(LOG_TAG, String.format("Out of memory reading '%s'.", url), e); //$NON-NLS-1$
			return null;
		}
	}

	public static Drawable getCachedDrawable(String imageIdentifier)
	{
		final SoftReference<Drawable> ref = cacheDrawable.get(imageIdentifier);
		if (ref != null)
		{
			final Drawable draw = ref.get();
			if (draw == null)
				cacheDrawable.remove(imageIdentifier);

			return draw;
		}

		return null;
	}

	private static void putInMemoryCache(String url, Bitmap bitmap)
	{
		synchronized (sCache)
		{
			sCache.put(url, bitmap);
		}
	}

	private static Bitmap getFromMemoryCache(String url)
	{
		synchronized (sCache)
		{
			return sCache.get(url);
		}
	}

	public static void clearMemoryCache()
	{
		sCache.evictAll();
		cacheDrawable.clear();
	}

	public static ImageLoader fromContext(Context context)
	{
		IGxBaseActivity baseActivity = Cast.as(IGxBaseActivity.class, context);
		if (baseActivity != null)
			return baseActivity.getImageLoader();
		else
			throw new IllegalArgumentException(String.format("Context '%s' does not have an ImageLoader.", context));
	}

}
