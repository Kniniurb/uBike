package com.plusot.senselib.values;

import android.app.Activity;
import android.view.View;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.Watchdog;
import com.plusot.senselib.R;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.util.Util;
import com.plusot.senselib.widget.FitLabelView;
import com.plusot.senselib.widget.XY;
import com.plusot.senselib.widget.XYGraphView;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class ValueView implements Value.ValueListener /*, MapListener */, Watchdog.Watchable {
	private static final String CLASSTAG = ValueView.class.getSimpleName();
	public static final int MAPVIEW_ID = 8000;

	private final int viewId;
	private Set<Value> values = new HashSet<Value>();
	private Value currentValue = null;
	//	private int zoomLevel = 14;
	//	private MyMapView map = null;
	//	private boolean hasMap = false;
	private Listener listener = null;

	public interface Listener {
		Activity getActivity();
	}

	public ValueView(final Listener listener, final int viewId) {
		this.viewId = viewId;
		this.listener = listener;
		Watchdog watchdog = Watchdog.getInstance();
		if (watchdog != null) watchdog.add(this, PreferenceKey.AUTOSWITCH.getInt());
		//hasMap = prefs.hasMap();	
	}

	public void resume() {
		//LLog.d(Globals.TAG, CLASSTAG + ".ValueView.resume called for: " + viewId);
		Watchdog watchdog = Watchdog.getInstance();
		if (watchdog != null) watchdog.add(this, PreferenceKey.AUTOSWITCH.getInt());
		//hasMap = prefs.hasMap();			

	}

	public Set<Value> getValues() {
		return values;
	}
	
	public EnumSet<ValueType> getValueTypes() {
		EnumSet<ValueType> valueTypes = EnumSet.noneOf(ValueType.class);
		for (Value value: values) {
			valueTypes.add(value.getValueType());
		}
		return valueTypes;
	}

	public Value getCurrentValue() {
		return currentValue;
	}

	public boolean hasValues() {
		return values.size() > 0;
	}

	public boolean hasValue(Value value) {
		return values.contains(value);
	}

	public void addValue(Value value) {
		if (value == null) return;
		//close();
		values.add(value);
		value.addListener(this);
		currentValue = value;

		//checkMap(value);
	}

	public void setValue(Value value) {
		if (value == null) return;
		for (Value tempValue : values.toArray(new Value[0])) close(tempValue);
		values.add(value);
		value.addListener(this);
		currentValue = value;
	}

	private Activity getActivity() {
		if (listener == null) return null;
		if (!Globals.runMode.isRun()) return null;
		return listener.getActivity();
	}

	@Override
	public void onValueChanged(Value value, boolean isLive) { //, Value.ChangeFlag flag) {
		if (!values.contains(value)) return;

		if (value != currentValue) {
			return;
		} 

		Activity activity = getActivity();
		if (activity == null) {
			return;
		}

		View view = Util.findViewById(getActivity(), viewId);
		if (view == null) return;

		int alpha = - 1;
		XYGraphView graph = (XYGraphView) view.findViewById(R.id.graph);;
		if (graph != null) {
			switch (value.getValueType()) {
			case LOCATION:
			case TIME:
				graph.setVisibility(View.INVISIBLE);
				break;
			default:
				if (graph.getScaler() == null) graph.setScaler(value.getUnit().getScaler());
				//setLabelScaleY(value.getUnit().getMultiplierAsFloat());
                XY[] xy = value.getQuickView().toArray(new XY[0]);
//                if (value.getValueType().equals(ValueType.SPEED)) {
//                    LLog.d(Globals.TAG, CLASSTAG + " Speed XY = " + xy.length + " ");
//                }
				graph.setValues(xy, value.getValueType().presentAsAngle(value.getValueAsFloat()), value.getValueType().presentAsAngle(), value.getZones());
				if (graph.getVisibility() == View.INVISIBLE) graph.setVisibility(View.VISIBLE);
			}
		}
		if (SenseGlobals.activity.equals(SenseGlobals.ActivityMode.REPLAY)) 
			alpha = 0xA0;
		else if (SenseGlobals.activity.equals(SenseGlobals.ActivityMode.STOP))
			alpha = 0x60;

		String unit = value.getUnitLabel(activity);
		if (unit.length() > 0) unit = " " + unit;
		Util.setText(view, R.id.caption, 
				/*value.getPresentation().getLabel(activity, " ") + */
				value.getValueType(activity)  + unit); //, alpha);

		if (value.getValueType().presentByDefault() == Presentation.NONE)
			Util.setText(view, R.id.value, "", alpha); 
		else
			Util.setText(view, R.id.value, value.print(), alpha); 

		if (value.getValueType().presentAsAvg() == Presentation.NONE)
			Util.setText(view, R.id.avg, "", alpha); 
		else
			Util.setText(view, R.id.avg, value.print(value.getValueType().presentAsAvg()), alpha); 

		if (value.getValueType().presentAsDeltaDown() == Presentation.NONE)
			Util.setText(view, R.id.deltadown, "", alpha); 
		else
			Util.setText(view, R.id.deltadown, value.print(value.getValueType().presentAsDeltaDown()), alpha); 

		if (value.getValueType().presentAsDeltaUp() == Presentation.NONE)
			Util.setText(view, R.id.deltaup, "", alpha); 
		else
			Util.setText(view, R.id.deltaup, value.print(value.getValueType().presentAsDeltaUp()), alpha); 

		switch (value.getValueType().presentAsMax()) {
		case NONE:
			Util.setText(view, R.id.max, "", alpha);
			break;
		case VALUE:
			FitLabelView maxView = (FitLabelView) view.findViewById(R.id.max);
			maxView.setTextColor(Globals.appContext.getResources().getColor(R.color.value_color));
			//maxView.setAlpha(alpha);
			maxView.setText(value.print(value.getValueType().presentAsMax())); 
			break;
		case MAX:
			maxView = (FitLabelView) view.findViewById(R.id.max);
			maxView.setTextColor(Globals.appContext.getResources().getColor(R.color.maxvalue_color));
			//maxView.setAlpha(alpha);
			maxView.setText(value.print(value.getValueType().presentAsMax())); 
			break;
		default:
			view.setTag(value.getValueType().getLabel(Globals.appContext));
			break;
		}
	}

	public void close(Value value) {	
		if (value == null) return;
		values.remove(value);
		value.removeListener(this);
		LLog.d(Globals.TAG, CLASSTAG + ": Removing Value: " + value.getValueType().getLabel(Globals.appContext));
		currentValue = null;
		if (!values.isEmpty()) {
			Value temp[] =  values.toArray(new Value[0]);
			if (temp.length > 0) currentValue = temp[0];
		}
	}
	
	@Override
	public void onWatchdogCheck(long count) {
		if (values.size() > 1) {
			Value newValue = Util.next(values, currentValue);
			while (!newValue.hasRecentRegistrations() && newValue != currentValue) {
				newValue = Util.next(values, newValue);
			}
			if (newValue != currentValue) {
				currentValue = newValue;
				onValueChanged(currentValue, false); //, ChangeFlag.ON_NEXTFIELD);			
			}
		}		
	} 

	public Value nextValue() {
		if (values.size() <= 1) return null;
		currentValue = Util.next(values, currentValue);
		onValueChanged(currentValue, false); //, ChangeFlag.ON_NEXTFIELD);
		return currentValue;
	}

	public Value prevValue() {
		if (values.size() <= 1) return null;
		currentValue = Util.prev(values, currentValue);
		onValueChanged(currentValue, false); //, ChangeFlag.ON_NEXTFIELD);
		return currentValue;
	}

	@Override
	public void onWatchdogClose() {
		// TODO Auto-generated method stub

	}

}


