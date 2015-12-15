package com.artech.android.api;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;

import com.artech.base.services.Services;
import com.artech.base.utils.ResultRunnable;
import com.artech.externalapi.ExternalApi;
import com.artech.externalapi.ExternalApiResult;

@SuppressWarnings("deprecation")
@SuppressLint("NewApi")
public class Clipboard extends ExternalApi {
	private final static String METHOD_GET_TEXT = "getText";
	private final static String METHOD_SET_TEXT = "setText";
	
	private static android.text.ClipboardManager sClipboard;

	@Override
	public @NonNull ExternalApiResult execute(String method, List<Object> parameters)
	{
		if (sClipboard == null)
		{
			sClipboard = Services.Device.invokeOnUiThread(new ResultRunnable<android.text.ClipboardManager>() {
	
				@Override
				public android.text.ClipboardManager run() {
					return getSystemClipboard();
				}
				
			});
		}

		if (METHOD_GET_TEXT.equalsIgnoreCase(method) && parameters.size() == 0)
		{
			return ExternalApiResult.success(getClipboardText());
		}
		else if (METHOD_SET_TEXT.equalsIgnoreCase(method) && parameters.size() == 1)
		{
			String text = (String) parameters.get(0);
			setClipboardText(text);
			return ExternalApiResult.SUCCESS_CONTINUE;
		}
		else
			return ExternalApiResult.failureUnknownMethod(this, method);
	}
	
	private android.text.ClipboardManager getSystemClipboard() {
		Context context = getActivity().getApplicationContext();
		
		android.text.ClipboardManager clipboard;
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
			clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		} else {
			clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		}
		
		return clipboard;
	}
	
	private String getClipboardText() {
		String text = "";
		
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
			if (sClipboard.hasText()) {
				text = (String) sClipboard.getText();
			}
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) sClipboard;
			if (clipboard.hasPrimaryClip()) {
				android.content.ClipData.Item dataClipItem = clipboard.getPrimaryClip().getItemAt(0);
				if (dataClipItem.getText() != null) {
					text = dataClipItem.getText().toString();
				}
			}
		}
		
		return text;
	}
	
	private void setClipboardText(String text) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
			sClipboard.setText(text);
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) sClipboard;
			android.content.ClipData clipData = android.content.ClipData.newPlainText("PlainText", text);
			clipboard.setPrimaryClip(clipData);
		}
	}

}
