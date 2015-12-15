package com.artech.controls.achartengine;


import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class GxChatsGridAdapter extends BaseAdapter {

	private Context mContext;
	private CharSequence[] mTextsIds; 
	private boolean arr[];

    public GxChatsGridAdapter(Context context, CharSequence[] textsIds) {
        mContext = context;
        mTextsIds = textsIds;
        arr = new boolean[mTextsIds.length]; 
        setNothingSelect();
    }

	@Override
	public int getCount() {
		return mTextsIds.length;
	}

	@Override
	public Object getItem(int arg0) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView textView;
        if (convertView == null) {
        	textView = new TextView(mContext);
        } else {
        	textView = (TextView) convertView;
        }
        textView.setText(mTextsIds[position]);
        if (arr[position]==true)
        	textView.setTextColor(Color.BLUE);
        return textView;
	}

	public void setCurrentSelect(int position)
	{
		if (position >= 0 && position < arr.length)
			arr[position]=true;
	}
	
	public void setNothingSelect()
	{
		for (int i= 0; i< arr.length; i++)
        	arr[i]=false;
	}
	
}
