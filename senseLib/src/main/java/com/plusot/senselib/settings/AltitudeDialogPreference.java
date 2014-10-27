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
import com.plusot.wheelgadget.adapters.ArrayWheelAdapter;
import com.plusot.wheelgadget.adapters.NumericWheelAdapter;

public class AltitudeDialogPreference extends DialogPreference {
	//private static final String androidns="http://schemas.android.com/apk/res/android";
	private static final String CLASSTAG = AltitudeDialogPreference.class.getSimpleName();
	

	private WheelView altMinPlus;
	private WheelView alt100;
	private WheelView alt10;
	private WheelView alt1;
	private TextView tv;
	private OnCloseListener listener = null;
	private int ref = 0;
	
	public interface OnCloseListener {
		public void onPositiveResult(int ref, int level);
	}

	public AltitudeDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
	}
	
	public void setOnCloseListener(OnCloseListener listener) {
		this.listener = listener;
	}
	
	public void setRef(int value) {
		this.ref = value;
	}
	
	@Override 
	protected View onCreateDialogView() {
		LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View view = layoutInflater.inflate(R.layout.altitude_dialog, null);
        altMinPlus = (WheelView) view.findViewById(R.id.alt_min_plus);
        alt100 = (WheelView) view.findViewById(R.id.alt100);
        alt10 = (WheelView) view.findViewById(R.id.alt10);
        alt1 = (WheelView) view.findViewById(R.id.alt1);
        tv = (TextView) view.findViewById(R.id.altitude_offset);
        
        OnWheelChangedListener listener = new OnWheelChangedListener() {
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                update();
            }
        };
        altMinPlus.setViewAdapter(new ArrayWheelAdapter<String>(getContext(), new String[] {"-", "+"}));
        alt100.setViewAdapter(new NumericWheelAdapter(getContext(), 0, 9));
        alt10.setViewAdapter(new NumericWheelAdapter(getContext(), 0, 9));
        alt1.setViewAdapter(new NumericWheelAdapter(getContext(), 0, 9));
        
        altMinPlus.addChangingListener(listener);
        alt100.addChangingListener(listener);
        alt10.addChangingListener(listener);
        alt1.addChangingListener(listener);
        
        return view;
	}
	
	private int update() {
		int value = 
        		alt100.getCurrentItem() * 100 +
        		alt10.getCurrentItem() * 10 +
        		alt1.getCurrentItem();	  
		
		if (altMinPlus.getCurrentItem() == 0) value *= -1;
		tv.setText(this.getDialogMessage() + ": " + value + "m");
		return value - ref;
	}
	
	@Override
	protected void onBindDialogView(View view) {
	    super.onBindDialogView(view);
	    
	    SharedPreferences sharedPreferences = getSharedPreferences();
	    int value = sharedPreferences.getInt(this.getKey(), 0);
	    LLog.d(Globals.TAG, CLASSTAG + ".onBindDialogView: Offset = " + value);
	    value += ref;

	    if (value < 0) altMinPlus.setCurrentItem(0); else altMinPlus.setCurrentItem(1);
	    value = Math.abs(value);
	    value %= 1000;
	    alt100.setCurrentItem(value / 100);
	    value %= 100;
	    alt10.setCurrentItem(value / 10);
	    value %= 10;
	    alt1.setCurrentItem(value);  
	    
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
	    super.onDialogClosed(positiveResult);

	    if (positiveResult) {
	        int value = update();        
	        Editor editor = getEditor();
	        editor.putInt(this.getKey(), value);
	        LLog.d(Globals.TAG, CLASSTAG + ".onDialogClosed: Offset = " + value);
	        editor.commit();
	        if (listener != null) listener.onPositiveResult(ref, value);
	    }
	}
	
}	
