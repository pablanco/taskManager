package com.artech.controls;

import java.io.File;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import com.artech.R;
import com.artech.android.ResourceManager;
import com.artech.android.media.MediaHelper;
import com.artech.android.media.MediaUtils;
import com.artech.application.MyApplication;
import com.artech.base.controls.IGxControlActivityLauncher;
import com.artech.base.controls.IGxControlPreserveState;
import com.artech.base.metadata.enums.ActionTypes;
import com.artech.base.metadata.enums.ControlTypes;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.services.Services;
import com.artech.common.ImageHelper;
import com.artech.common.StandardImages;
import com.artech.common.StorageHelper;
import com.artech.ui.Coordinator;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

public class GxMediaEditControl extends GxImageViewData implements IGxControlActivityLauncher, Target, IGxControlPreserveState {
    private static final String STATE_OUTPUT_MEDIA_FILE = "OutputMediaFile";
    private final LayoutItemDefinition mDefinition;
    private final Coordinator mCoordinator;

    private Uri mMediaUri;

    private AlertDialog mChooserDialog;
    private File mOutputMediaFile;

    public GxMediaEditControl(Context context, Coordinator coordinator, LayoutItemDefinition layoutItem) {
        super(context, layoutItem);
        mDefinition = layoutItem;
        mCoordinator = coordinator;
        setOnClickListener(mMediaEditClick);
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public void setGx_Value(String value) {
        if (value != null) {
            mMediaUri = Uri.parse(value);
            setBitmapFromUri(mMediaUri);
        }
    }

    @Override
    public String getGx_Value() {
        return (mMediaUri != null) ? mMediaUri.toString() : null;
    }

    private final OnClickListener mMediaEditClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String controlType = mDefinition.getControlType();
            if (controlType.equalsIgnoreCase(ControlTypes.PhotoEditor))
                showPhotoOptions();
            else if (controlType.equalsIgnoreCase(ControlTypes.VideoView))
                showVideoOptions();
            else if (controlType.equalsIgnoreCase(ControlTypes.AudioView))
                showAudioOptions();
        }
    };

    private void showPhotoOptions() {
        String[] menuOptions = new String[]{Services.Strings.getResource(R.string.GXM_TakePhoto),
                Services.Strings.getResource(R.string.GXM_SelectImage)};
        int[] actions = {MediaHelper.TAKE_PICTURE, MediaHelper.PICK_IMAGE};
        createChooserDialog(menuOptions, actions);
    }

    private void showVideoOptions() {
        String[] menuOptions = new String[]{Services.Strings.getResource(R.string.GXM_RecordVideo),
                Services.Strings.getResource(R.string.GXM_SelectVideo)};
        int[] actions = {MediaHelper.CAPTURE_VIDEO, MediaHelper.PICK_VIDEO};
        createChooserDialog(menuOptions, actions);
    }

    private void showAudioOptions() {
        String[] menuOptions = new String[]{Services.Strings.getResource(R.string.GXM_RecordAudio),
                Services.Strings.getResource(R.string.GXM_SelectAudio)};
        int[] actions = {MediaHelper.CAPTURE_AUDIO, MediaHelper.PICK_AUDIO};
        createChooserDialog(menuOptions, actions);
    }

    private void createChooserDialog(final String[] menuOptions, final int[] actions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(R.string.GX_BtnSelect)
                .setItems(menuOptions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which < actions.length) {
                            int action = actions[which];
                            Activity activity = mCoordinator.getUIContext().getActivity();

                            if (mCoordinator.getUIContext().getActivityController() != null)
                                mCoordinator.getUIContext().getActivityController().setCurrentActivityLauncher(GxMediaEditControl.this);

                            switch (action) {
                                case MediaHelper.PICK_IMAGE:
                                    MediaHelper.pickImage(activity);
                                    break;
                                case MediaHelper.PICK_VIDEO:
                                    MediaHelper.pickVideo(activity);
                                    break;
                                case MediaHelper.PICK_AUDIO:
                                    MediaHelper.pickAudio(activity);
                                    break;
                                case MediaHelper.TAKE_PICTURE:
                                    mOutputMediaFile = MediaHelper.takePicture(activity);
                                    break;
                                case MediaHelper.CAPTURE_VIDEO:
                                    mOutputMediaFile = MediaHelper.captureVideo(activity);
                                    break;
                                case MediaHelper.CAPTURE_AUDIO:
                                    mOutputMediaFile = MediaHelper.captureAudio(activity);
                                    break;

                                default:
                                    throw new IllegalArgumentException("Unexpected value for media dialog action: " + action);
                            }
                        }
                    }
                });

        mChooserDialog = builder.show();
    }

    @Override
    public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (MediaUtils.isPickMediaRequest(requestCode)) {
                Uri mediaUri = MediaHelper.getPickedMediaUri(data);
                onMediaChanged(mediaUri);
                return true;
            } else if (mOutputMediaFile != null && MediaUtils.isTakeMediaRequest(requestCode)) {
                Uri mediaUri = MediaHelper.getTakenMediaUri(getContext(), data, mOutputMediaFile);
                onMediaChanged(mediaUri);
                mOutputMediaFile = null;
                return true;
            }
        }
        return false;
    }

    /**
     * Called when the user selects a media.
     *
     * @param mediaUri Uri to the selected media.
     */
    private void onMediaChanged(Uri mediaUri) {
        if (mediaUri != null) {
            Uri oldValue = mMediaUri;
            mMediaUri = mediaUri;
            setBitmapFromUri(mediaUri);
            if (!mediaUri.equals(oldValue)) {
                mCoordinator.onValueChanged(this, true);
            }
        }
    }

    private void loadMediaPreview(String controlType, @NonNull Uri mediaUri) {
        if (TextUtils.isEmpty(mediaUri.toString())) {
            StandardImages.showPlaceholderImage(this, true);
            return;
        }
        RequestCreator requestCreator = null;
        if (ControlTypes.PhotoEditor.equals(controlType)) {
            if (TextUtils.isEmpty(mediaUri.getScheme())) {
                // Try to load from resources first, in case it is embedded.
                if (ImageHelper.showDataImageFromResource(this, mediaUri.toString()))
                    return;
                // It's an image file, either remote or in the local filesystem.
                mediaUri = translateGenexusImageUri(mediaUri);
            }
            requestCreator = Picasso.with(getContext()).load(mediaUri);
        } else if (ControlTypes.VideoView.equals(controlType) || ControlTypes.AudioView.equals(controlType)) {
            String actionType = ControlTypes.VideoView.equals(controlType) ? ActionTypes.ViewVideo : ActionTypes.ViewAudio;
            requestCreator = Picasso.with(getContext()).load(ResourceManager.getContentDrawableFor(getContext(), actionType));
        }
        if (requestCreator != null) {
            requestCreator.resize(0, getHeight()).into(this);
        }
    }

    private Uri translateGenexusImageUri(Uri imageUri) {
        String imagePath = imageUri.toString();
        if (StorageHelper.isLocalFile(imagePath)) {
            imagePath = "file://" + imagePath;
        } else {
            imagePath = MyApplication.getApp().UriMaker.MakeImagePath(imagePath);
        }
        return Uri.parse(imagePath);
    }

    private void setBitmapFromUri(final Uri mediaUri) {
        if (getHeight() > 0) {
            loadMediaPreview(getControlType(), mediaUri);
        } else {
            // Wait until the control's layout has been measured.
            getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                @SuppressWarnings("deprecation")
                public void onGlobalLayout() {
                    GxMediaEditControl.this.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    loadMediaPreview(getControlType(), mediaUri);
                }
            });
        }
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        setLoading(true);
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, LoadedFrom from) {
        setImageBitmap(bitmap);
        setLoading(false);
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        setLoading(false);
    }

    @Override
    protected void onDetachedFromWindow() {
        Picasso.with(getContext()).cancelRequest(this);
        if (mChooserDialog != null) {
            mChooserDialog.dismiss();
            mChooserDialog = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public String getControlId() {
        return mDefinition.getName();
    }

    @Override
    public void saveState(Map<String, Object> state) {
        state.put(STATE_OUTPUT_MEDIA_FILE, mOutputMediaFile);
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        mOutputMediaFile = (File) state.get(STATE_OUTPUT_MEDIA_FILE);
    }
}
