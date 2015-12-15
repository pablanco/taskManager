package com.artech.controls;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.artech.base.metadata.enums.Alignment;
import com.artech.base.metadata.enums.ImageScaleType;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.utils.PrimitiveUtils;
import com.artech.controls.common.IViewDisplayImage;
import com.artech.utils.BitmapUtils;
import com.makeramen.roundedimageview.RoundedImageView;

public abstract class GxImageViewBase extends FrameLayout implements IViewDisplayImage, IGxThemeable
{
	private ImageView mImageView;
	private Drawable mDrawable;
	private ThemeClassDefinition mThemeClass;
	private String mImageTag;

	private int mAlignment;
	private ImageScaleType mScaleType;
	private Integer mImageWidth;
	private Integer mImageHeight;
	private boolean mAutogrow;

	private Matrix mMatrix;

	public GxImageViewBase(Context context)
	{
		super(context);
		initialize();
	}

	public GxImageViewBase(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initialize();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public GxImageViewBase(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initialize();
	}

	private void initialize()
	{
		mScaleType = ImageScaleType.FIT;
		mAlignment = Alignment.CENTER;
		mMatrix = new Matrix();
		setWillNotDraw(true);

		mImageView = new ImageView(getContext());

		/* EXPERIMENTAL
		mImageView = new ImageView(getContext())
		{
			@Override
			public void requestLayout()
			{
				// It's NOT necessary to request a full layout unless the imageview's dimensions are affected
				// by the image it displays. That only happens when autogrow = true.
				if (isAutogrow())
					super.requestLayout();
			}
		};
		*/

		addView(mImageView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER));

		onImageParametersChanged();
	}

	protected ImageView getImageView()
	{
		return mImageView;
	}

	@Override
	public void setImageDrawable(Drawable drawable)
	{
		if (mDrawable != drawable)
		{
			mDrawable = drawable;
			onImageParametersChanged();
		}
	}

	@Override
	public void setImageResource(int resId)
	{
		Drawable drawable = getContext().getResources().getDrawable(resId);
		setImageDrawable(drawable);
	}

	@Override
	public void setImageBitmap(Bitmap bmp)
	{
		if (bmp != null)
			setImageDrawable(new BitmapDrawable(getContext().getResources(), bmp));
		else
			setImageDrawable(null);
	}

	@Override
	public String getImageTag()
	{
		return mImageTag;
	}

	@Override
	public void setImageTag(String tag)
	{
		mImageTag = tag;
	}

	/* EXPERIMENTAL
	@Override
	public Rect getImageDisplayRect()
	{
		if (isAutogrow())
			return null;

		int width = mImageView.getWidth();
		if (mImageWidth != null)
			width = mImageWidth;

		int height = mImageView.getHeight();
		if (mImageHeight != null)
			height = mImageHeight;

		if (width > 0 && height > 0)
			return new Rect(0, 0, width, height);
		else
			return null;
	}

	@Override
	public ImageScaleType getImageScaleType()
	{
		return mScaleType;
	}
	*/

	public void setAlignment(int alignment)
	{
		// Add default values if not specified, to make sure comparison works.
		if ((alignment & Alignment.HORIZONTAL_MASK) == 0)
			alignment |= Alignment.CENTER_HORIZONTAL;

		if ((alignment & Alignment.VERTICAL_MASK) == 0)
			alignment |= Alignment.CENTER_VERTICAL;

		if (mAlignment != alignment)
		{
			mAlignment = alignment;

			LayoutParams lp = (FrameLayout.LayoutParams)mImageView.getLayoutParams();
			lp.gravity = alignment;
			mImageView.setLayoutParams(lp);

			// Gravity affects both the parent layout and the imageview itself.
			onImageParametersChanged();
		}
	}

	public void setImageScaleType(ImageScaleType scaleType)
	{
		if (mScaleType != scaleType)
		{
			mScaleType = scaleType;
			onImageParametersChanged();
		}
	}

	public void setImageSize(Integer width, Integer height)
	{
		if (!PrimitiveUtils.areEquals(mImageWidth, width) || !PrimitiveUtils.areEquals(mImageHeight, height))
		{
			mImageWidth = width;
			mImageHeight = height;

			// Image size affects the parent layout, since we need to resize the ImageView.
			LayoutParams lp = (LayoutParams)mImageView.getLayoutParams();
			lp.width = (mImageWidth != null ? mImageWidth : LayoutParams.MATCH_PARENT);
			lp.height = (mImageHeight != null ? mImageHeight : LayoutParams.MATCH_PARENT);
			mImageView.setLayoutParams(lp);

			// It may also affect the ImageView, if we need to calculate a new matrix.
			getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
			{
				@Override
				@SuppressLint("NewApi")
				@SuppressWarnings("deprecation")
				public void onGlobalLayout()
				{
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
						getViewTreeObserver().removeOnGlobalLayoutListener(this);
					else
						getViewTreeObserver().removeGlobalOnLayoutListener(this);

					onImageParametersChanged();
				}
			});
		}
	}

	@Override
	public void setThemeClass(ThemeClassDefinition themeClass)
	{
		mThemeClass = themeClass;
		applyClass(themeClass);
	}

	@Override
	public ThemeClassDefinition getThemeClass()
	{
		return mThemeClass;
	}

	protected void setPropertiesImageSizeScaleRadiusFromThemeClass(ThemeClassDefinition themeClass)
	{
		boolean controlChanged = false;
		if (themeClass.getCornerRadius() != 0)
		{
			if (!(mImageView instanceof RoundedImageView))
			{
				ViewGroup.LayoutParams params = mImageView.getLayoutParams();
				removeView(mImageView);
				mImageView = new RoundedImageView(getContext());
				addView(mImageView, params);
				controlChanged = true;
			}
			RoundedImageView roundedImageView = (RoundedImageView)mImageView;
			roundedImageView.setCornerRadius(themeClass.getCornerRadius());
		}
		else
		{
			if (mImageView instanceof RoundedImageView)
			{
				ViewGroup.LayoutParams params = mImageView.getLayoutParams();
				removeView(mImageView);
				mImageView = new ImageView(getContext());
				addView(mImageView, params);
				controlChanged = true;
			}
		}
		// call to onImageParametersChanged
		setImageScaleType(themeClass.getImageScaleType());
		setImageSize(themeClass.getImageWidth(), themeClass.getImageHeight());

		//force call to onImageParameterChanged if control was changed
		if (controlChanged)
			onImageParametersChanged();
	}

	protected void setAutogrow(boolean autogrow)
	{
		mAutogrow = autogrow;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		super.onLayout(changed, l, t, r, b);

		if (changed)
			onImageParametersChanged();
	}

	private void onImageParametersChanged()
	{
		mImageView.setImageDrawable(mDrawable);

		// Cleanup: remove any previous tiling mode possibly applied to the drawable.
		if (mScaleType != ImageScaleType.TILE && mDrawable instanceof BitmapDrawable)
			((BitmapDrawable)mDrawable).setTileModeXY(null, null);

		if (isAutogrow())
		{
			// Autogrow is a special case: just let the ImageView grow as needed, vertically.
			mImageView.setAdjustViewBounds(true);
		}
		else if (mScaleType == ImageScaleType.FILL)
		{
			mImageView.setScaleType(ScaleType.FIT_XY);
		}
		else if (mScaleType == ImageScaleType.FIT && mAlignment == Alignment.CENTER)
		{
			mImageView.setScaleType(ScaleType.FIT_CENTER);
		}
		else if (mScaleType == ImageScaleType.FIT && mAlignment == (Alignment.LEFT | Alignment.TOP))
		{
			mImageView.setScaleType(ScaleType.FIT_START);
		}
		else if (mScaleType == ImageScaleType.FIT && mAlignment == (Alignment.RIGHT | Alignment.BOTTOM))
		{
			mImageView.setScaleType(ScaleType.FIT_END);
		}
		else if (mScaleType == ImageScaleType.FILL_KEEPING_ASPECT && mAlignment == Alignment.CENTER)
		{
			mImageView.setScaleType(ScaleType.CENTER_CROP);
		}
		else if (mScaleType == ImageScaleType.NO_SCALE && mAlignment == Alignment.CENTER)
		{
			mImageView.setScaleType(ScaleType.CENTER);
		}
		else if (mScaleType == ImageScaleType.TILE)
		{
			// It doesn't make sense to tile a drawable that isn't a bitmap
			// (since it will have no intrinsic dimensions).
			if (mDrawable instanceof BitmapDrawable)
				((BitmapDrawable)mDrawable).setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);

			mImageView.setScaleType(ScaleType.FIT_XY);
		}
		else
		{
			// Needs a matrix.
			calculateMatrix();
		}
	}

	private boolean isAutogrow()
	{
		return (mAutogrow &&
				mImageWidth == null && mImageHeight == null &&
				(mScaleType == ImageScaleType.FILL_KEEPING_ASPECT || mScaleType == ImageScaleType.FIT));
	}

	private void calculateMatrix()
	{
		if (mDrawable == null)
			return; // We don't have a drawable to calculate measurements.

		// Note: No need to consider padding, the inner view never has padding.
		float viewWidth = mImageView.getWidth();
		float viewHeight = mImageView.getHeight();

		// Use custom dimensions if applied.
		if (mImageWidth != null)
			viewWidth = mImageWidth;
		if (mImageHeight != null)
			viewHeight = mImageHeight;

		if (viewWidth == 0 && viewHeight == 0)
			 return; // If the view's bounds aren't known yet, hold off on until they are.

		float drawWidth = mDrawable.getIntrinsicWidth();
		float drawHeight = mDrawable.getIntrinsicHeight();

		if (drawWidth <= 0 || drawHeight <= 0)
		{
			// For a drawable with no intrinsic size (e.g. a solid color), the only scale
			// type that makes sense is FILL.
			mImageView.setScaleType(ScaleType.FIT_XY);
			return;
		}

		// Some cases (such as FILL or TILE) have already been resolved without the need for a matrix.
		// We handle the remaining cases here (basically finding the scale and computing the translation
		// from the desired alignment).
		mMatrix.reset();
		BitmapUtils.computeScalingMatrix(drawWidth, drawHeight, viewWidth, viewHeight, mScaleType, mAlignment, mMatrix);

		mImageView.setScaleType(ScaleType.MATRIX);
		mImageView.setImageMatrix(mMatrix);
	}

	@Override
	public int hashCode() {
		return mImageView.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof GxImageViewBase) {
			GxImageViewBase imageViewBase = (GxImageViewBase) o;
			return mImageView.equals(imageViewBase.getImageView());
		}
		return false;
	}
}
