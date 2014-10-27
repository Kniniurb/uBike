package com.plusot.senselib.widget;

import android.util.SparseArray;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.javacommon.util.StringUtil;
import com.plusot.javacommon.util.TimeUtil;

import java.util.List;

public class XY {
    private static final String CLASSTAG = XY.class.getSimpleName();
    private long x;
	private int[] tags = null;
	private float[] y = null;

	public XY(final long x) {
		this.x = x;
	}
	
//	public XY() {
//		this.x = 0;
//	}
	
	public XY(final long x, int[] tags, float[] y) {
		this.x = x;
		this.tags = tags;
		this.y = y;
	}

	public void setY(int tag, float y) {
        if (y == Float.NaN) {
            LLog.d(Globals.TAG, CLASSTAG + ".setY NaN");
        }
		if (tags == null || this.y == null) {
			initY(tag, y);
			return;
		}
		for (int i = 0; i < this.y.length; i++) {
			if (tags[i] == tag) {
				this.y[i] = y;
				return;
			}
		}
		initY(tag, y);
	}

	public void setY(float[] y) {
		this.y = y;
		
		tags = new int[y.length];
		for (int i = 0; i < tags.length; i++) tags[i] = - i - 1;
	}

	public void timesY(int tag, float times) {
		if (tags == null || this.y == null) return;
		for (int i = 0; i < this.y.length; i++) {
			if (tags[i] == tag) {
				this.y[i] *= times;
				return;
			}
		}
	}

	private void initY(int tag, float y) {
		if (tags == null || this.y == null) {
			tags = new int[1]; 
			this.y = new float[1];
		} else {
			float[] orgY = this.y;
			this.y = new float[this.y.length + 1];
			System.arraycopy(orgY, 0, this.y, 0, orgY.length);
			int[] orgTags = this.tags;
			this.tags = new int[orgTags.length + 1];
			System.arraycopy(orgTags, 0, this.tags, 0, orgTags.length);
		}
		this.y[this.y.length - 1] = y;
		tags[tags.length - 1] = tag;
	}

	public void addY(int tag, float y) {
		if (tags == null || this.y == null) {
			initY(tag, y);
			return;
		}
		for (int i = 0; i < this.y.length; i++) {
			if (tags[i] == tag) {
				this.y[i] += y;
				return;
			}
		}
		initY(tag, y);
	}

	public void meanY(int tag, float y) {
		if (tags == null || this.y == null) {
			initY(tag, y);
			return;
		}
		for (int i = 0; i < this.y.length; i++) {
			if (tags[i] == tag) {
				this.y[i] += y;
				this.y[i] /= 2f;
				return;
			}
		}
		initY(tag, y);
	}

	public long getX() {
		return x;
	}

	public float[] getY() {
//        if (y == null || y.length == 0) return new float[]{0};
//        float[] newY = new float[y.length];
//        for (int i = 0; i < y.length; i++) {
//            if (y[i] != Float.NaN)
//                newY[i] = y[i];
//            else
//                newY[i] = 0;
//        }
//		return newY;
        return y;
	}

	public int[] getTags() {
		return tags;
	}
	
	public int getTag(int i) {
		if (tags == null || i >= tags.length) return i + 1;
		return tags[i];
	}

	public void mean(XY other) {
		if (other == null) return;
		x /= 2;
		x += other.x / 2;
		if (other.y != null && other.tags != null) for (int i = 0; i < other.y.length; i++) {
			meanY(other.tags[i], other.y[i]);	
		}
	}

	public static XY mean(List<XY> list) {
		if (list == null || list.size() < 1) return null;
		if (list.size() == 1) return list.get(0);
		SparseArray<Float> prods = new SparseArray<Float>();
		SparseArray<Float> lastY = new SparseArray<Float>();
		SparseArray<Long> totalX = new SparseArray<Long>();
		SparseArray<Long> prevX = new SparseArray<Long>();
		long xFirst = -1;
		long xLast = -1;
		for (XY xy: list) {
			if (xFirst == -1) xFirst = xy.x;
			xLast = xy.x;
				
			for (int i = 0; i < xy.tags.length; i++) {
				if (prevX.get(xy.tags[i]) == null) {					
					lastY.put(xy.tags[i], xy.y[i]);
				} else {
					long deltaX = xy.x - prevX.get(xy.tags[i]);
                    if (deltaX <= 0) continue;
				
					Float prod = prods.get(xy.tags[i]);
					if (prod == null) prod = 0f;
					Float prevY = lastY.get(xy.tags[i]);
					if (prevY != null) prods.put(xy.tags[i], prod + (xy.y[i] + prevY) * deltaX / 2);
					lastY.put(xy.tags[i], xy.y[i]);
					Long total = totalX.get(xy.tags[i]);
					if (total == null) total = (long) 0;
					totalX.put(xy.tags[i], total + deltaX);
 				}
				prevX.put(xy.tags[i], xy.x);
			}
		}
		XY avg = new XY((xLast + xFirst) / 2);
		for (int index = 0; index < lastY.size(); index++) {  //     Integer tag: lastY.keySet()) {
			int tag = lastY.keyAt(index);
			Float prod = prods.get(tag);
			if (prod == null)
				avg.setY(tag, lastY.get(tag));
			else
				avg.setY(tag, prods.get(tag) / totalX.get(tag));
		}
		return avg;

	}
	
	@Override
	public String toString() {
		return "" + x + ',' + StringUtil.toString(tags, ";") + ',' + StringUtil.toString(y, ";");
	}
	
	public String toHRString() {
		return "" + TimeUtil.formatTime(x) + ',' + StringUtil.toString(tags, ";") + ',' + StringUtil.toString(y, ";");
	}
	
	public static XY fromString(String string) {
		String[] strings = string.split(",");
		if (strings.length < 3) return null;
		long x = StringUtil.toLong(strings[0], null);
		int[] tags = StringUtil.toInts(strings[1].split(";"), null);
		float[] ys = StringUtil.toFloats(strings[2].split(";"), null);
		
		return new XY(x, tags, ys);
		
	}
}
