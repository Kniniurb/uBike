package com.plusot.senselib.values;

import android.content.Context;

import com.plusot.senselib.R;

public enum Presentation {
	VALUE(0),
	AVERAGE(R.string.average),
	DELTA(R.string.delta),
	MAX(R.string.max),
	MIN(R.string.min),
	DELTAUP(R.string.up),
	DELTADOWN(R.string.down),
	VALUETIME(R.string.valuetime),
	NORMALIZED(R.string.normalized),
	VARIANCE(R.string.variance),
	CHANGE(R.string.change),
	NONE(0);

	final int resId;
	
	private Presentation(int resId) {
		this.resId = resId;	
	}

	public String getLabel(Context context, String append) {
		if (context == null || resId == 0) {
			return "";
		}
		String result = context.getString(resId);
		if (result.length() > 0) result = result + append;
		return result;
	}

	/*public static Presentation next(Presentation toggleState) {
		return Presentation.values()[(toggleState.ordinal() + 1) % Presentation.values().length];
	}*/
}
