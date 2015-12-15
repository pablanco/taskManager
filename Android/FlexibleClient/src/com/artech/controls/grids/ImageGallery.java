package com.artech.controls.grids;

import java.io.File;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.net.Uri;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.artech.R;
import com.artech.actions.ICustomMenuManager;
import com.artech.android.layout.GridContext;
import com.artech.android.media.utils.FileUtils;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.metadata.theme.ThemeClassDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.services.Services;
import com.artech.base.utils.MathUtils;
import com.artech.base.utils.PlatformHelper;
import com.artech.base.utils.Strings;
import com.artech.common.ImageHelper;
import com.artech.compatibility.SherlockHelper;
import com.artech.controllers.ViewData;
import com.artech.controls.GxTextView;
import com.artech.controls.IGridView;
import com.artech.controls.IGxThemeable;
import com.artech.controls.ImageViewDisplayImageWrapper;
import com.artech.usercontrols.SDImageGalleryDefinition;
import com.artech.utils.Cast;

@SuppressWarnings("deprecation")
public class ImageGallery extends LinearLayout implements IGridView, ICustomMenuManager, IGxThemeable
{
	/***
	 * Gallery and ImageView
	 */
	private Gallery mGallery;
	private ImageView mImageView;
	private GxTextView mTitleTextView;
	private GxTextView mSubTitleTextView;

	// Current images in the grid view.
    private ArrayList<String> mCurrentImages = new ArrayList<String>();
    private ArrayList<String> mCurrentTitleImages = new ArrayList<String>();
    private ArrayList<String> mCurrentSubTitleImages = new ArrayList<String>();
	private int mCurrentImage = -1;

    private ImageGalleryAdapter mAdapter;
    private SDImageGalleryDefinition mGalleryDefinition;

    private GridContext mContext;
    private ShareActionProvider mShareActionProvider;

	private final GridHelper mHelper;
	private GridEventsListener mListener;
	private ThemeClassDefinition mThemeClass;

	// For flip.
	private final GestureDetector mGestureDetector;
    private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;

	public ImageGallery(Context context, LayoutItemDefinition definition)
	{
		super(context);
		GridDefinition gridDefinition = (GridDefinition)definition;

		mContext = (GridContext) context;
		mGalleryDefinition = new SDImageGalleryDefinition(getContext(), gridDefinition);
		mGestureDetector = new GestureDetector(context, new GestureListener());

		createView(context);

		if (mGalleryDefinition.hasShareAction())
		{
			Context actionBarContext = SherlockHelper.getActionBarThemedContext(Cast.as(Activity.class, mContext.getBaseContext()));
			if (actionBarContext != null)
				mShareActionProvider = new ShareActionProvider(actionBarContext);
		}

		mHelper = new GridHelper(this, gridDefinition);
	}

	private void createView(Context context)
	{
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(com.artech.R.layout.gximagegallerylayout, this, true);

        mGallery = (Gallery)findViewById(R.id.GalleryImageGallery);
        mGallery.setCallbackDuringFling(false);

        mTitleTextView = (GxTextView)findViewById(R.id.titleTextViewImageGallery);
        mSubTitleTextView = (GxTextView)findViewById(R.id.subTitleTextViewImageGallery);

        mImageView = (ImageView)findViewById(R.id.ImageViewImageGallery);

        mImageView.setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(final View view, final MotionEvent event)
            {
            	mGestureDetector.onTouchEvent(event);
                return true;
            }
        });

        mImageView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mImageView.setPadding(15, 5, 15, 5);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        setThemeImageGallery();
	}

	private void setThemeImageGallery()
	{
		ThemeClassDefinition themeTitle = PlatformHelper.getThemeClass("Attribute.Title"); //$NON-NLS-1$
		ThemeClassDefinition themeSubtitle = PlatformHelper.getThemeClass("Attribute.Subtitle"); //$NON-NLS-1$

		if ((themeTitle != null) && (mTitleTextView != null))
			mTitleTextView.setThemeClass(themeTitle);

		if ((themeSubtitle != null) && (mSubTitleTextView != null))
			mSubTitleTextView.setThemeClass(themeSubtitle);
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

	@Override
	public void applyClass(ThemeClassDefinition themeClass)
	{
		mHelper.setThemeClass(themeClass);
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void addListener(GridEventsListener listener)
	{
		mListener = listener;
		mHelper.setListener(listener);
		mGallery.setOnItemClickListener(mOnItemClickList);
        mGallery.setOnItemSelectedListener(mOnItemSelectedListener);
	}

	@Override
	public void update(ViewData data)
	{
		// There is no image in the definition?
	 	if (!Services.Strings.hasValue(mGalleryDefinition.getThumbnailAttribute()))
    		return;

    	String thumbnailField = mGalleryDefinition.getThumbnailAttribute();
    	String titleField = mGalleryDefinition.getTitleAttribute();
    	String subtitleField = mGalleryDefinition.getSubtitleAttribute();

    	mCurrentImages.clear();
    	mCurrentTitleImages.clear();
    	mCurrentSubTitleImages.clear();

        for (Object entity : data.getEntities())
        {
            Entity e = (Entity) entity;
            mCurrentImages.add(e.optStringProperty(thumbnailField));

            String strTitleImages = Strings.EMPTY;
            if (Services.Strings.hasValue(titleField))
            	strTitleImages = e.optStringProperty(titleField);

            String strSubTitleImages = Strings.EMPTY;
            if (Services.Strings.hasValue(subtitleField))
            	strSubTitleImages = e.optStringProperty(subtitleField);

            mCurrentTitleImages.add(strTitleImages);
            mCurrentSubTitleImages.add(strSubTitleImages);
        }

	 	if (mAdapter == null)
		{
			mAdapter = new ImageGalleryAdapter(mContext.getBaseContext());
			mGallery.setAdapter(mAdapter);
		}

	 	mAdapter.setData(data.getEntities());
		mAdapter.notifyDataSetChanged();

		mCurrentImage = MathUtils.constrain(mCurrentImage, 0, mCurrentImages.size() - 1);
		setCurrentImage(mCurrentImage, true);
	}

	//OnItemSelected Listener
	private OnItemSelectedListener mOnItemSelectedListener = new OnItemSelectedListener()
	{
		private static final int VISIBLE_THRESHOLD = 5;

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
		{
			if (position >= mCurrentImages.size() - VISIBLE_THRESHOLD)
				mListener.requestMoreData();

			setCurrentImage(position, false);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0)
		{
			if (mCurrentImages.size()>0)
				setCurrentImage(0, false);
		}
	};

	//OnItemClick Listener
	private OnItemClickListener mOnItemClickList = new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			setCurrentImage(position, false);
		}
	};

	private void setCurrentImage(int index, boolean forceRefresh)
	{
		if (mCurrentImage != index || forceRefresh)
		{
			mCurrentImage = index;
			if (!MathUtils.isConstrained(mCurrentImage, 0, mCurrentImages.size() - 1))
			{
				mCurrentImage = -1;
				mImageView.setImageDrawable(null);
				mTitleTextView.setText(Strings.EMPTY);
				mSubTitleTextView.setText(Strings.EMPTY);
				return;
			}

			String imageIdentifier = mCurrentImages.get(index);
			String imageTitle = mCurrentTitleImages.get(index);
			String imageSubtitle = mCurrentSubTitleImages.get(index);

			// Update image view.
			ImageHelper.showDataImage(mContext.getImageLoader(), ImageViewDisplayImageWrapper.to(mImageView), imageIdentifier);

			mTitleTextView.setText(imageTitle);
			mSubTitleTextView.setText(imageSubtitle);

			// Prepare share intent, if enabled.
			if (mShareActionProvider != null)
				mShareActionProvider.setShareIntent(getShareIntent(imageIdentifier, imageTitle));
		}
	}

    public class ImageGalleryAdapter extends BaseAdapter
    {
        private Context context;
        private int imageBackground;

        private EntityList mData;

        public ImageGalleryAdapter(Context c)
        {
            context = c;
            TypedArray ta = context.obtainStyledAttributes(R.styleable.ImageGallery);
			imageBackground = ta.getResourceId(R.styleable.ImageGallery_android_galleryItemBackground, 1);
			ta.recycle();
        }

        @Override
		public int getCount() {
            return mCurrentImages.size();
        }

        @Override
		public Object getItem(int position) {
            return mCurrentImages.get(position);
        }

        @Override
		public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
            {
            	ImageView image = new ImageView(context);

            	Rect thumbnailSize = mGalleryDefinition.getThumbnailSize();

            	int right = thumbnailSize.right;
            	int bottom = thumbnailSize.bottom;

            	image.setLayoutParams(new Gallery.LayoutParams(right,bottom));
            	image.setScaleType(ImageView.ScaleType.FIT_XY);
            	image.setPadding(5, 5, 5, 5);
            	image.setBackgroundResource(imageBackground);
            	try
            	{
            		String imageUri = mCurrentImages.get(position);
           			ImageHelper.showDataImage(mContext.getImageLoader(), ImageViewDisplayImageWrapper.to(image), imageUri);
            		image.setAnimation(null);
            	}
            	catch(Exception e) { }

            	return image;
            }
            else
                return convertView;
        }

        public void setData(EntityList data)
    	{
    		mData = data;
    	}

        public Entity getEntity(int position)
    	{
        	if (mData!=null)
        	{
        		if (mData.size()>position)
        		{
        			return mData.get(position);
        		}
        	}
        	return null;
    	}
    }

	private class GestureListener extends SimpleOnGestureListener
	{
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
        	try
        	{
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
					return false;

				// right to left swipe
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
				{
					if (mCurrentImage < mCurrentImages.size() - 1)
						mGallery.setSelection(mCurrentImage + 1);
					else
						Toast.makeText(mContext, R.string.GXM_NoNext, Toast.LENGTH_LONG).show();
				}
				else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
				{
					if (mCurrentImage > 0)
						mGallery.setSelection(mCurrentImage - 1);
					else
						Toast.makeText(mContext, R.string.GXM_NoPrevious, Toast.LENGTH_LONG).show();
				}
			}
        	catch (Exception e) { }

			return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e)
        {
        	if (mListener == null)
				return false;

        	if (!MathUtils.isConstrained(mCurrentImage, 0, mCurrentImages.size() - 1))
        		return false;

        	Entity myEntity = mAdapter.getEntity(mCurrentImage);
        	if (myEntity!=null)
        		return mHelper.runDefaultAction(myEntity);
        	return false;
        }
    }

	private Intent getShareIntent(String imageIdentifier, String imageTitle)
	{
		File localFile = ImageHelper.getCachedImageFile(imageIdentifier);
		if (localFile != null && localFile.exists())
		{
			/* Removed because it creates duplicate entries in gallery. Google+ doesn't work, Facebook/Twitter/Gmail does.
			// Some sharing actions only accept images from a content provider; not from a file.
			// Therefore add it to gallery first.
			String imageName = (Services.Strings.hasValue(imageTitle) ? imageTitle : localFile.getName());
			String imageUri = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), localFile.getAbsolutePath(), imageName, null);
			*/

			IntentBuilder builder = IntentBuilder.from(mHelper.getActivity());
			builder.setText(imageTitle);
			builder.setStream(Uri.fromFile(localFile));
			builder.setType(FileUtils.getMimeType(localFile));

			return builder.getIntent();
		}
		else
			return null;
	}

	@SuppressLint("AlwaysShowAction")
	@Override
	public void onCustomCreateOptionsMenu(Menu menu)
	{
		// Add the "Share" action if specified.
		if (mShareActionProvider != null)
		{
			MenuItem shareItem = menu.add("<share>");
			MenuItemCompat.setShowAsAction(shareItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			MenuItemCompat.setActionProvider(shareItem, mShareActionProvider);
		}
	}
}