package com.artech.common;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.artech.base.services.Services;
import com.artech.controls.common.IViewDisplayImage;

public class ImageHelperHandlers
{
	private static abstract class ForPostOnUiThread implements ImageHelper.Handler
	{
		@Override
		public void receive(final Drawable d)
		{
			Services.Device.runOnUiThread(new Runnable()
			{
				@Override
				public void run() { posted(d); }
			});
		}

		protected abstract void posted(Drawable d);
	}

	public static class ForViewBackground extends ForPostOnUiThread
	{
		private final View mView;

		public ForViewBackground(View view)
		{
			mView = view;
		}

		@SuppressWarnings("deprecation")
		@Override
		protected void posted(Drawable d)
		{
			mView.setBackgroundDrawable(d);
		}
	}

	public static class ForImageView extends ForPostOnUiThread
	{
		private final IViewDisplayImage mView;

		public ForImageView(IViewDisplayImage view)
		{
			mView = view;
		}

		@Override
		protected void posted(Drawable d)
		{
			mView.setImageDrawable(d);
		}
	}

	public static class ForActivityBackground implements ImageHelper.Handler
	{
		private final Activity mActivity;

		public ForActivityBackground(Activity activity)
		{
			mActivity = activity;
		}

		@Override
		public void receive(final Drawable d)
		{
			mActivity.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					mActivity.getWindow().setBackgroundDrawable(d);
				}
			});
		}
	}
}
