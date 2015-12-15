package com.artech.android.downloader;

import java.io.FileNotFoundException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;

public class FileDownloader {
	private Context mContext;
	private FileDownloaderListener mListener;
	private DownloadManager mDownloadManager;
	private long mDownloadId;
	
	public FileDownloader(Context context, FileDownloaderListener listener, Uri uri) {
		mContext = context;
		mListener = listener;
		
		DownloadManager.Request downloadRequest = new DownloadManager.Request(uri)
			.setVisibleInDownloadsUi(false);
		
		mDownloadManager = new DownloadManager(mContext.getContentResolver(), mContext.getApplicationInfo().packageName);
		mDownloadId = mDownloadManager.enqueue(downloadRequest);
		
		IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		mContext.registerReceiver(mReceiver, intentFilter);
	}
	
	public static FileDownloader downloadMediaFile(Context context, FileDownloaderListener listener, Uri uri) {
		return new FileDownloader(context, listener, uri);
	}
	
	/**
	 *  Returns the progress in a [0-100] integer, representing the percentage.
	 */
	public int getDownloadProgress() {
		DownloadManager.Query query = new DownloadManager.Query();
		query.setFilterById(mDownloadId);
		
		Cursor cursor = mDownloadManager.query(query);
		
		if (!cursor.moveToFirst()) {
			return -1;
		}
		
		int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
		if (DownloadManager.STATUS_RUNNING != cursor.getInt(statusIndex)) {
			return -2;
		}
		
		int bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
		int bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
		long bytesDownloaded = cursor.getLong(bytesDownloadedIndex);
		long bytesTotal = cursor.getLong(bytesTotalIndex);
		
		return (int) ((bytesDownloaded * 100l) / bytesTotal);
	}
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
				long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
				
				if (id != mDownloadId) {
					return;
				}
				
				DownloadManager.Query query = new DownloadManager.Query();
				query.setFilterById(id);
				
				Cursor cursor = mDownloadManager.query(query);
				
				if (!cursor.moveToFirst()) {
					return;
				}
				
				int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
				if (DownloadManager.STATUS_SUCCESSFUL != cursor.getInt(statusIndex)) {
					return;
				}
				
				int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
				String downloadUri = cursor.getString(uriIndex);
				
				try {
					mDownloadManager.openDownloadedFile(id);
				} catch (FileNotFoundException e) {
					return;
				}
				
				int titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
				String fileName = cursor.getString(titleIndex);
				
				mListener.OnFileDownloaded(Uri.parse(downloadUri), fileName);
				mContext.unregisterReceiver(mReceiver);
			}
		}
	};
	
	public interface FileDownloaderListener {
		void OnFileDownloaded(Uri fileUri, String fileName);
	}
}
