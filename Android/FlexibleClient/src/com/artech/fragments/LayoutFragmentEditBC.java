package com.artech.fragments;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.artech.R;
import com.artech.actions.ActionExecution;
import com.artech.actions.WorkWithAction;
import com.artech.activities.IntentParameters;
import com.artech.android.api.ProgressIndicatorApi;
import com.artech.android.media.utils.FileUtils;
import com.artech.application.MyApplication;
import com.artech.base.application.IBusinessComponent;
import com.artech.base.application.OutputResult;
import com.artech.base.metadata.DataItem;
import com.artech.base.metadata.enums.DisplayModes;
import com.artech.base.metadata.enums.LayoutModes;
import com.artech.base.model.Entity;
import com.artech.base.providers.IApplicationServer;
import com.artech.base.services.Services;
import com.artech.base.utils.Strings;
import com.artech.common.IProgressListener;
import com.artech.common.IntentHelper;
import com.artech.common.ServiceHelper;
import com.artech.common.TrnHelper;
import com.artech.compatibility.CompatibilityHelper;
import com.artech.controllers.DataSourceControllerBC;
import com.artech.controllers.IDataSourceController;
import com.artech.controls.GxImageViewData;
import com.artech.controls.IGxEdit;

public class LayoutFragmentEditBC extends LayoutFragment
{
	private DataSourceControllerBC mController;
	private ProgressDialog mProgressDialog;
	
	@Override
	public void setController(IDataSourceController controller)
	{
		if (!(controller instanceof DataSourceControllerBC))
			throw new IllegalArgumentException("Controller for LayoutFragmentEditBC is not of the correct type.");
		
		mController = (DataSourceControllerBC)controller;
	}

	@Override
	protected short getLayoutMode()
	{
		return LayoutModes.EDIT;
	}
	
	public void runSaveAction(boolean withImplicitReturn)
	{
		SaveActionTask task = new SaveActionTask(withImplicitReturn);
		CompatibilityHelper.executeAsyncTask(task);
	}

	private class SaveActionTask extends AsyncTask<Entity, Message, OutputResult> implements IProgressListener
	{
		private boolean mIsReturnImplicit;
		private LinkedHashMap<String, Uri> mMediaToUpload;
		private int mProgressStep;

		public SaveActionTask(boolean isReturnImplicit)
		{
			mIsReturnImplicit = isReturnImplicit;
			mMediaToUpload = calculateMediaToUpload(mController.getBCEntity());
		}

		private LinkedHashMap<String, Uri> calculateMediaToUpload(Entity entity)
		{
			LinkedHashMap<String, Uri> mediaToUpload = new LinkedHashMap<String, Uri>();
			for (DataItem dataItem : entity.getLevel().Items)
			{
    			if (dataItem.isMediaOrBlob())
    			{
    				String value = entity.optStringProperty(dataItem.getName());
    				if (Strings.hasValue(value))
    				{
    					Uri mediaUri = Uri.parse(value);
    					String scheme = mediaUri.getScheme();
    					if (ContentResolver.SCHEME_FILE.equals(scheme) || ContentResolver.SCHEME_CONTENT.equals(scheme))
    					    mediaToUpload.put(dataItem.getName(), mediaUri);
    				}
    			}
			}
			
			return mediaToUpload;
		}
		
		@SuppressLint("NewApi")
		@Override
		protected void onPreExecute()
		{
			// Show default progress indicator only if there isn't another one running.
			if (ProgressIndicatorApi.isShowing(getActivity()))
				return;

			int textId = R.string.GXM_Saving;
			if (mController.getMode() == DisplayModes.DELETE)
				textId = R.string.GXM_Deleting;

			mProgressDialog = new ProgressDialog(getActivity());
			mProgressDialog.setTitle(textId);
			if (CompatibilityHelper.isHoneycomb())
				mProgressDialog.setProgressNumberFormat(null);
			
			if (mMediaToUpload.size() != 0)
			{
				mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			}
			else
			{
				mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mProgressDialog.setIndeterminate(true);
			}

			mProgressDialog.setMessage(getResources().getText(R.string.GXM_Saving));
			mProgressDialog.show();
		}

		@Override
		protected OutputResult doInBackground(Entity... params)
		{
			return saveBCAndMedia(mController.getBCEntity());
		}
		
		private OutputResult saveBCAndMedia(Entity entity)
		{
			if (entity == null)
				return null;

			short mode = mController.getMode();
			IBusinessComponent bc = mController.getBusinessComponent();
			
			if (mode == DisplayModes.INSERT || mode == DisplayModes.EDIT)
			{
				if (!uploadMedia())
					return OutputResult.error("Media upload failed."); // TODO: Return exact error from saveMultimedia()

				notifyProgressMessage(Services.Strings.getResource(R.string.GXM_Saving));
				return bc.save(entity);
			}
			else if (mode == DisplayModes.DELETE)
			{
				return bc.delete();
			}
			else
				throw new IllegalArgumentException(String.format("Unknown mode: %s", mode)); //$NON-NLS-1$
		}

		private boolean uploadMedia()
		{
			int mediaFilesUploaded = 0;
			int mediaFilesTotal = mMediaToUpload.size();
			
			IApplicationServer server = mController.getServer();
			Context context = MyApplication.getAppContext();
			Entity entity = mController.getBCEntity();
			
			for (Map.Entry<String, Uri> mediaItem : mMediaToUpload.entrySet())
			{
				String mediaAttribute = mediaItem.getKey();
				Uri mediaUri = mediaItem.getValue();
				
				IGxEdit view = getAdapter().getEdit(mediaAttribute);
				GxImageViewData imageView = TrnHelper.getGxImage((View) view);
				notifyProgressMessage(getResources().getText(R.string.GXM_Uploading).toString());

				String mediaMimeType = FileUtils.getMimeType(context, mediaUri);
				String mediaExtension = FileUtils.getExtension(context, mediaUri);

				InputStream data;
				try
				{
					data = context.getContentResolver().openInputStream(mediaUri);
				}
				catch (FileNotFoundException e)
				{
					break;
				}

				boolean mediaFileUploaded;
				if (mediaMimeType.startsWith("image") && imageView != null)
				{
					int maxUploadSizeMode = imageView.getMaximumUploadSizeMode();
					mediaFileUploaded = ServiceHelper.resizeAndUploadImage(context, server, mediaUri, mediaAttribute, maxUploadSizeMode, entity, this);
				}
				else
				{
					long dataLength = FileUtils.getContentLength(context, mediaUri);
					mediaFileUploaded = ServiceHelper.uploadFile(server, data, dataLength, mediaExtension, mediaMimeType, entity, mediaAttribute, this);
				}
				
				if (mediaFileUploaded)
					mediaFilesUploaded++;
			}
			
			return (mediaFilesUploaded == mediaFilesTotal); // All uploaded sucessfully?
		}

		@Override
		public void setCount(long length)
		{
			notifyProgressTotal(length);
		}

		private void notifyProgressTotal(long length)
		{
			Message msg = Message.obtain();
			Bundle b = new Bundle();
			b.putInt("Total", (int)length); //$NON-NLS-1$
			msg.setData(b);
			onProgress(msg);
		}
		
		@Override
		public void step()
		{
			mProgressStep++;
			notifyProgressStep();
		}

		private void notifyProgressStep()
		{
			Message msg = Message.obtain();
			Bundle b = new Bundle();
			b.putInt("Step", mProgressStep); //$NON-NLS-1$
			msg.setData(b);
			onProgress(msg);
		}
		
		private void notifyProgressMessage(String data)
		{
			Message msg = Message.obtain();
			Bundle b = new Bundle();
			b.putString("Message", data); //$NON-NLS-1$
			msg.setData(b);
			onProgress(msg);
		}
		
		
		@Override
		protected void onProgressUpdate(Message... progress)
		{
			if (mProgressDialog != null && mProgressDialog.isShowing() && progress != null && progress.length > 0)
			{
				Message p = progress[0];
				String message = p.getData().getString("Message"); //$NON-NLS-1$
				if (message != null)
					mProgressDialog.setTitle(message);

				int total = p.getData().getInt("Total"); //$NON-NLS-1$
				if (total > 0)
				{
					mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					mProgressDialog.setMax(total);
				}

				int step = p.getData().getInt("Step"); //$NON-NLS-1$
				if (step > 0)
					mProgressDialog.setProgress(step);
			}
		}

		public void onProgress(Message s)
		{
			publishProgress(s);
		}

		@Override
		protected void onPostExecute(final OutputResult result)
		{
			if (result == null)
				return;

			// Display the result of insert/update/delete.
			afterSaveAction(result, mIsReturnImplicit);
		}
	}
	
	private void afterSaveAction(OutputResult result, final boolean isReturnImplicit)
	{
		if (getActivity() == null)
			return;
		
		if (mProgressDialog != null)
			mProgressDialog.dismiss();

		if (result.isOk())
		{
			// Show warnings (if any) then continue (possibly finishing activity).
			String warningText = result.getWarningText();
			if (Services.Strings.hasValue(warningText))
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(warningText);
				builder.setPositiveButton(R.string.GXM_button_ok, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						endSaveAction(isReturnImplicit);
					}
				});

				builder.show();
			}
			else
			{
				endSaveAction(isReturnImplicit);
			}
		}
		else
		{
			// Show errors.
			String errorText = result.getErrorText();
			showErrorDialog(errorText, false);

			// finish save event on fail
			ActionExecution.cancelCurrent();
		}
	}
	
	private void endSaveAction(boolean isReturnImplicit)
	{
		MyApplication.getInstance().showMessage(getString(DisplayModes.getSuccessMessageResource(mController.getMode())));

		if (isReturnImplicit)
		{
			((LayoutFragmentActivity)getActivity()).setReturnResult();
			getActivity().finish();
		}
		else
			ActionExecution.continueCurrent(getActivity(), false);
	}

	@Override
	public void setReturnResult(Intent data)
	{
		// Parm rule is not used in edit. Only set result with updated entity.
		// super.setReturnResult(data);

		data.putExtra(IntentParameters.Mode, mController.getMode());
		IntentHelper.putObject(data, WorkWithAction.UPDATED_ENTITY_IN_INTENT, Entity.class, mController.getBCEntity());
	}

	private void showErrorDialog(String message, final boolean finishAfterwards)
	{
		if (getActivity() == null)
			return;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.GXM_errtitle);
		builder.setCancelable(false);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.GXM_button_ok, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int id)
			{
				if (finishAfterwards)
					getActivity().finish();
			}
		});

		try
		{
			builder.show();
		}
		catch (Exception e)
		{
			Services.Log.Error("Exception during showErrorDialog(). Possibly the activity is already closed.", e);
		}
	}
}
