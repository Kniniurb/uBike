/*******************************************************************************
 * Copyright (c) 2012 Plusot Biketech
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Bruinink - initial API and implementation and/or initial documentation
 * 
 *******************************************************************************/
package com.plusot.senselib.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.preference.Preference;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.plusot.senselib.R;

public class IntegerPreference extends Preference {


	public static int maximum    = 300;
	//public static int interval   = 1;

	private int value = 50;
	private EditText monitorBox;


	public IntegerPreference(Context context) {
		super(context);
	}

	public IntegerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public IntegerPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected View onCreateView(ViewGroup parent){

		LinearLayout layout = new LinearLayout(getContext());
		layout.setPadding(15, 5, 10, 5);
		layout.setOrientation(LinearLayout.HORIZONTAL);

		LinearLayout titleLayout = new LinearLayout(getContext());
		titleLayout.setOrientation(LinearLayout.VERTICAL);

		LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		titleParams.gravity = Gravity.LEFT;
		titleParams.weight  = 1.0f;
		titleLayout.setLayoutParams(titleParams);
		
		TextView view = new TextView(getContext());
		view.setText(getTitle());
		view.setTextSize(22);
		view.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
		view.setGravity(Gravity.LEFT);
		view.setLayoutParams(titleParams);

		titleLayout.addView(view);

		view = new TextView(getContext());
		view.setText(this.getSummary());
		view.setTextSize(16);
		view.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
		view.setGravity(Gravity.LEFT);
		view.setLayoutParams(titleParams);
		
		titleLayout.addView(view);

		LinearLayout.LayoutParams monitorParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		monitorParams.gravity = Gravity.CENTER;
		monitorParams.topMargin = 0;
		monitorParams.leftMargin = 0;
		monitorParams.rightMargin = 0;
		monitorParams.bottomMargin = 0;

		Button downButton = new Button(getContext());
		downButton.setText("- ");
		downButton.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
		//downButton.setPadding(5, 0, 5, 0);
		
		downButton.setTextSize(18);
		downButton.setLayoutParams(monitorParams);
		downButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (value > 0) value--;
				changeValue();
			}		
		});
		
		Button upButton = new Button(getContext());
		upButton.setText("+");
		upButton.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
		//upButton.setPadding(5, 0, 5, 0);
		
		upButton.setTextSize(18);
		upButton.setLayoutParams(monitorParams);
		upButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (value < maximum) value++;
				changeValue();
			}		
		});
		
		monitorBox = new EditText(getContext());
		//monitorBox.setBackgroundResource(R.drawable.square_border);
		monitorBox.setTextSize(18);
		monitorBox.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
		monitorBox.setPadding(4, 0, 4, 0);
		monitorBox.setLayoutParams(monitorParams);
		monitorBox.setInputType(InputType.TYPE_CLASS_NUMBER);
		monitorBox.setText(value + "");
		monitorBox.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				value = Integer.valueOf(monitorBox.getText().toString());
				if(!callChangeListener(value)){
					return; 
				}
				updatePreference(value);
				notifyChanged();
			}});
		
		LinearLayout setterLayout = new LinearLayout(getContext());
		setterLayout.setBackgroundResource(R.drawable.square_border);
		setterLayout.addView(downButton);
		setterLayout.addView(monitorBox);
		setterLayout.addView(upButton);
		setterLayout.setPadding(0, 2, 0, 0);
		LinearLayout.LayoutParams setterParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		setterLayout.setLayoutParams(setterParams);

		layout.addView(titleLayout);
		layout.addView(setterLayout);
		//layout.addView(monitorBox);
		//layout.addView(upButton);
		layout.setId(android.R.id.widget_frame);

		return layout; 
	}

	public void changeValue() {
		if(!callChangeListener(value)){
			return; 
		}
		monitorBox.setText(value+"");
		updatePreference(value);
		notifyChanged();
	}

	@Override 
	protected Object onGetDefaultValue(TypedArray ta,int index){

		int dValue = (int)ta.getInt(index,50);

		return validateValue(dValue);
	}


	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

		int temp = restoreValue ? getPersistedInt(207) : (Integer)defaultValue;

		if(!restoreValue)
			persistInt(temp);

		value = temp;
	}


	private int validateValue(int value){
		if(value > maximum)
			value = maximum;
		else if(value < 0)
			value = 0;
		return value;  
	}


	private void updatePreference(int newValue){
		SharedPreferences.Editor editor = getEditor();
		editor.putInt(getKey(), newValue);
		editor.commit();
	}

}

