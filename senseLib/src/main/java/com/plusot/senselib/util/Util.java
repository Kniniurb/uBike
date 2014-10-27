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
package com.plusot.senselib.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.widget.TextView;

import com.plusot.senselib.widget.FitLabelView;

public class Util {

	public static void setText(View v, int resource, String text) {
		if (v == null) {
			//LLog.e(CLASSTAG, "Invalid activity");
			return;
		}
		v = v.findViewById(resource);;
		if (v == null) return;
		if (v instanceof TextView) {
			((TextView) v).setText(text);
		} else if (v instanceof FitLabelView) {
			((FitLabelView) v).setText(text);
		}
	}

	public static void setText(View v, int resource, String text, int alpha) {
		if (v == null) {
			//LLog.e(CLASSTAG, "Invalid activity");
			return;
		}
		v = v.findViewById(resource);;
		if (v == null) return;
		if (v instanceof TextView) {
			((TextView) v).setText(text);
		} else if (v instanceof FitLabelView) {
			((FitLabelView) v).setText(text);
			((FitLabelView) v).setAlpha(alpha);
		}
	}

	public static void setText(Dialog view, int resource, String text) {
		if (view == null) {
			//LLog.e(CLASSTAG, "Invalid activity");
			return;
		}
		View v = view.findViewById(resource);
		if (v == null) return;
		if (v instanceof TextView) {
			((TextView) v).setText(text);
			//LLog.d(Globals.TAG, CLASSTAG + ".setText: " + text);
		} else if (v instanceof FitLabelView) {
			((FitLabelView) v).setText(text);
		}
	}

	public static View findViewBySibbling(View view, int id) {
		if (view.getParent() instanceof View) {
			View parentView = ((View) view.getParent());
			if (parentView != null) {
				return parentView.findViewById(id);
			}
		}
		return null;
	}

	public static View findViewById(Activity activity, int resource) {
		if (activity == null) return null;
		View v = activity.findViewById(resource);

		return v;
	}

	public static boolean[] toBooleanArray(Boolean[] array) {
		if (array == null) return null;
		boolean booleans[] = new boolean[array.length];
		int i = 0;
		for (boolean bool : array) {
			booleans[i++] = bool;
		}
		return booleans;
	}

	/*public static boolean[] toBooleanArray(Collection<String[]> collection) {
		if (collection == null) return null;
		boolean booleans[] = new boolean[collection.size()];
		int i = 0;
		for (String[] strings : collection) {
			booleans[i++] = strings.length > 0;
		}
		return booleans;
	}*/

	public static boolean[] toBooleanArray(Collection<Boolean> collection) {
		if (collection == null) return null;
		boolean booleans[] = new boolean[collection.size()];
		int i = 0;
		for (Boolean bool : collection) {
			booleans[i++] = bool;
		}
		return booleans;
	}

	public static <T> T next(Set<T> set, T current) {
		Iterator<T> iterator = set.iterator();
		T first = null;
		while (iterator.hasNext()) {
			T t = iterator.next();
			if (first == null) first = t;
			if (current == null) return t;
			if (t == current) current = null;
		}
		return first;
	}

	public static <T> T prev(Set<T> set, T current) {
		Iterator<T> iterator = set.iterator();
		T prev = null;
		T t = null;
		while (iterator.hasNext()) {
			t = iterator.next();
			if (t == current) break;
			prev = t;
		}
		if (prev == null) {
			while (iterator.hasNext()) {
				t = iterator.next();
			};
			return t;
		}
		return prev;
	}

	public static String arrayGetByBits(int bits, String[] array, String delim) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 32; i++) {
			int test = 1 << i;
			if  ((bits & test) == test) {
				if (sb.length() > 0) sb.append(delim);
				sb.append(array[i]);
			}
		}
		return sb.toString();
	}
	
	public static int parseInt(String intStr) {
		char[] chars = intStr.toCharArray();
		int result = 0;
		for (char ch : chars) {
			result *= 10;
			result += (ch - 48); 
		}
		return result;
	}
	
	public static double parseDouble(String intStr) {
		char[] chars = intStr.toCharArray();
		boolean countDecimals = false;
		double result = 0;
		int dec = 1;
		for (char ch : chars) {
			if (ch == '.' || ch == ',')
				countDecimals = true;
			else {
				if (countDecimals) dec *= 10;
				result *= 10;
				result += (ch - 48); 
			}
		}
		return result / dec;
	}


}