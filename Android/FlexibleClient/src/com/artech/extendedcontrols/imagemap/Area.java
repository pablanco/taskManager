package com.artech.extendedcontrols.imagemap;

import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.AbsoluteLayout;

import com.artech.controls.grids.GridAdapter;
import com.artech.controls.grids.GridItemLayout;

/**
 *  Area is abstract Base for tappable map areas
 *   descendants provide hit test and focal point
 */
@SuppressWarnings("deprecation")
abstract class Area {
	private int _id;
	private String _name;
	private HashMap<String,String> _values;
	private Bitmap _decoration = null;
	private int size=0;
	private GridItemLayout _layout = null;
	private GridAdapter mAdapter = null;

	public boolean resize = false;

	public Area(int id, String name) {
		_id = id;
		if (name != null) {
			_name = name;
		}
	}

	public int getId() {
		return _id;
	}

	public String getName() {
		return _name;
	}

	// all xml values for the area are passed to the object
	// the default impl just puts them into a hashmap for
	// retrieval later
	public void addValue(String key, String value) {
		if (_values == null) {
			_values = new HashMap<String,String>();
		}
		_values.put(key, value);
	}

	public String getValue(String key) {
		String value=null;
		if (_values!=null) {
			value=_values.get(key);
		}
		return value;
	}

	// a method for setting a simple decorator for the area
	public void setBitmap(Bitmap b) {
		_decoration = b;
	}

	public GridItemLayout getLayout() {
		return _layout;
	}

	public void setLayout(GridItemLayout layout) {
		_layout = layout;
	}
	// an onDraw is set up to provide an extensible way to
	// decorate an area.  When drawing remember to take the
	// scaling and translation into account


	public void onDraw(Canvas canvas,float mResizeFactorX ,float mResizeFactorY ,int mScrollLeft,int mScrollTop,float prop) {
		//if (_decoration != null) {


			float x = (getOriginX() * mResizeFactorX/prop) + mScrollLeft - 17;
			float y = (getOriginY() * mResizeFactorY/prop) + mScrollTop - 17;
			//System.out.println("--> x: " + x  +" y: " +y );
			mAdapter.setBounds(size, size);
			_layout.setVisibility(View.VISIBLE);
			_layout.setLayoutParams(new AbsoluteLayout.LayoutParams(size, size, (int)x, (int)y));


		//}
	}

	abstract boolean isInArea(float x, float y);
	abstract float getOriginX();
	abstract float getOriginY();

	int getSize() {
		return size;
	}

	void setSize(int size) {
		this.size = size;
	}

	public GridItemLayout get_layout() {
		return _layout;
	}

	public void set_layout(GridItemLayout _layout) {
		this._layout = _layout;
	}

	public GridAdapter getmAdapter() {
		return mAdapter;
	}

	public void setmAdapter(GridAdapter mAdapter) {
		this.mAdapter = mAdapter;
	}

}