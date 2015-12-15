package com.artech.controls.common;

import android.os.AsyncTask;
import android.widget.TextView;

import com.artech.base.services.IValuesFormatter;

public class TextViewFormatter
{
	private final TextView mTextView;
	private final IValuesFormatter mValuesFormatter;

	public TextViewFormatter(TextView textView, IValuesFormatter valuesFormatter)
	{
		mTextView = textView;
		mValuesFormatter = valuesFormatter;
	}

	public void setText(String value)
	{
		if (mValuesFormatter != null)
		{
			if (mValuesFormatter.needsAsync())
				new BackgroundTask().execute(value);
			else
				mTextView.setText(mValuesFormatter.format(value));
 		}
		else
			mTextView.setText(value);
	}

	private class BackgroundTask extends AsyncTask<String, Void, CharSequence>
	{
		@Override
		protected CharSequence doInBackground(String... params)
		{
			if (params.length == 1)
				return mValuesFormatter.format(params[0]);
			else
				return null;
		}

		@Override
		protected void onPostExecute(CharSequence result)
		{
			if (result != null)
				mTextView.setText(result);
		}
	}
}
