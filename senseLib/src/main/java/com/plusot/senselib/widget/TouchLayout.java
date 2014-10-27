package com.plusot.senselib.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class TouchLayout extends RelativeLayout {
	
	private Callback callback;
	
	public interface Callback {
		void onTouch(TouchLayout layout, MotionEvent ev);
	}
	
	public void setCallback(Callback callback) {
		this.callback = callback;
	}
	
	public TouchLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public TouchLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public TouchLayout(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}


	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (callback != null) callback.onTouch(this, ev);
		return false; 
	}

}
