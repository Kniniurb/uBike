package com.plusot.senselib.activities;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;

public class LongCheckBoxPreference extends CheckBoxPreference implements View.OnLongClickListener {
	private View.OnLongClickListener listener = null;
	
	public LongCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public LongCheckBoxPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public LongCheckBoxPreference(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public void setOnLongClickListener(View.OnLongClickListener listener) {
		this.listener = listener;
	}
	
	@Override
	public boolean onLongClick(View v) {
		if (listener == null) return false;
		return listener.onLongClick(v);
	}

}
