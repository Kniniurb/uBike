package com.plusot.senselib.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.senselib.R;
import com.plusot.wheelgadget.OnWheelChangedListener;
import com.plusot.wheelgadget.WheelView;
import com.plusot.wheelgadget.adapters.NumericWheelAdapter;

public class MaxHeartrateDialogPreference extends DialogPreference {
	private static final String CLASSTAG = MaxHeartrateDialogPreference.class.getSimpleName();
	

	private WheelView wheel;
	private TextView tv;
	private OnCloseListener listener = null;
	
	public interface OnCloseListener {
		public void onPositiveResult(int level);
	}

	public MaxHeartrateDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
	}
	
	public void setOnCloseListener(OnCloseListener listener) {
		this.listener = listener;
	}
	
	@Override 
	protected View onCreateDialogView() {
		LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View view = layoutInflater.inflate(R.layout.maxheartrate_dialog, null);
        wheel = (WheelView) view.findViewById(R.id.wheel_heartrate);
        tv = (TextView) view.findViewById(R.id.text_heartrate);
        
        OnWheelChangedListener listener = new OnWheelChangedListener() {
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                update();
            }
        };
        wheel.setViewAdapter(new NumericWheelAdapter(getContext(), 110, 220));
        wheel.addChangingListener(listener);
        return view;
	}
	
	private int update() {
		int value = 110 + wheel.getCurrentItem();	 
		tv.setText(this.getDialogMessage() + ": " + value + " bpm");
		return value;
	}
	
	@Override
	protected void onBindDialogView(View view) {
	    super.onBindDialogView(view);
	    
	    SharedPreferences sharedPreferences = getSharedPreferences();
	    int value = sharedPreferences.getInt(this.getKey(), 0);
	    LLog.d(Globals.TAG, CLASSTAG + ".onBindDialogView: Max heart rate = " + value);
	    
	    value = Math.abs(value);
	    wheel.setCurrentItem(value - 110);
	    
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
	    super.onDialogClosed(positiveResult);

	    if (positiveResult) {
	        int value = update();        
	        Editor editor = getEditor();
	        editor.putInt(this.getKey(), value);
	        LLog.d(Globals.TAG, CLASSTAG + ".onDialogClosed: Max heart rate = " + value);
	        editor.commit();
	        if (listener != null) listener.onPositiveResult(value);
	    }
	}
	
}	
