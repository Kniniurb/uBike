/*******************************************************************************
lime'kick
 * Copyright (c) 2012 Plusot Biketech
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Bruinink - initial API and implementation and/or initial documentation
 * Based on GraphView by:
 *     Arno den Hond - http://android.arnodenhond.com/
 * 
 *******************************************************************************/

package com.plusot.senselib.widget;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.SparseArray;

import com.plusot.javacommon.util.Scaler;
import com.plusot.senselib.util.Util;

import java.util.Locale;

/**
 * Graph creates a scaled line or xy graph with x and y axis labels. 
 * @author Peter Bruinink
 *
 */
public class BaseGraph {
	//private static final String CLASSTAG = BaseGraph.class.getSimpleName();
	public static final int DEFAULT_ALPHA = 0x55;
	
	public enum LabelStyle {
		INT,
		DECIMAL,
		TIME;
	}
	
	public enum AngleLocation {
		NONE,
		MID,
		LOWERLEFT;
		boolean isNone() {
			return this.equals(NONE);
		}
	}

	public static String[] labels = null;

	private final char xyz[] = {'0','x','y','z','a','b','c', '7'};
	private float zones[] = null;

	private final int LINE_COLOR = 0xFF000000;
	private static final int HORIZONTAL_DIVISIONS = 4;
	private static final int VERTICAL_DIVISIONS = 4;
	private float textSize = 12;

	private final static int[] SHADER_COLOR1 = {0xFF660000, 0xFFFF0000};
	private final static int[] SHADER_COLOR2 = {0xFF006600, 0xFF00FF00};
	private final static int[] SHADER_COLOR3 = {0xFF000066, 0xFF0000FF};
	private final static int[] SHADER_COLOR4 = {0xFF666600, 0xFFBBAA00};
	private final static int[] SHADER_COLOR5 = {0xFF006666, 0xFF00FFFF};
	private final static int[] SHADER_COLOR6 = {0xFF660066, 0xFFFF00FF};
	private static int[][] SHADER_COLORS = {SHADER_COLOR1, SHADER_COLOR2, SHADER_COLOR3, SHADER_COLOR4, SHADER_COLOR5, SHADER_COLOR6}; 

	private final static int MAX_SHADERS = 6;
	//private int[][] shaderColors = new int[MAX_SHADERS][2];

	protected Paint paint;
	private XY[] values = null;
	private long[] xs = null;
	private String title = null;
	private float borderTop = 5;
	private float borderLeft = 25;
	private float borderRight = 5;
	private float borderBottom = 20;
	private int alpha = DEFAULT_ALPHA;
	//private float scaleY = 1.0f;
	private LabelStyle labelStyleX = LabelStyle.TIME;
	private LabelStyle labelStyleY = LabelStyle.DECIMAL;
	private long maxX = 0;
	private long minX = 0;
	private float maxY = 0;
	private float minY = 0;
	private final int xLabelInterval;
	private final int yLabelInterval;
	private final boolean hasLegenda;
	private float lastValue = 0;
	private AngleLocation angleLocation = AngleLocation.NONE;
	private Scaler scaler = null;

	
	//private int[] tags = new int[MAX_SHADERS]; 

	public BaseGraph(int xLabelInterval, int yLabelInterval, boolean hasLegenda) {
		this.xLabelInterval = xLabelInterval;
		this.yLabelInterval = yLabelInterval;
		this.hasLegenda = hasLegenda;
		paint = new Paint();
		paint.setAntiAlias(true);

	}
	
	private float scale(float value) {
		if (scaler == null) return value;
		return scaler.onScale(value).floatValue();
	}
	
	public void setScaler(Scaler scaler) {
		this.scaler = scaler;
	}
	
	public Scaler getScaler() {
		return scaler;
	}
	
	public static void setShaderColor(int index, int color1, int color2) {
		if (index >= MAX_SHADERS) return;
		SHADER_COLORS[index][0] = color1; //GraphUtil.dimColor(color, 0.7f);
		SHADER_COLORS[index][1] = color2;
	}
	
	public static void setShaderColor(int index, int color) {
		if (index >= MAX_SHADERS) return;
		SHADER_COLORS[index][0] = GraphUtil.dimColor(color, 0.7f);
		SHADER_COLORS[index][1] = color;
	}

	/*public void setLabelScaleY(float scaleY) {
		this.scaleY = scaleY;
	}*/

	public void setValues(XY[] values, float lastValue, AngleLocation showAngle, float[] zones) {
		if (values == null) return;
		this.values = values;
		xs = new long[values.length];
		this.lastValue = lastValue;
		this.angleLocation = showAngle;
		this.zones = zones;
	}

	public void setTitle(String title) {
		this.title = title;
		if (title != null) {
			if (borderTop < 15) borderTop = 25;	
		} else {
			if (borderTop > 10) borderTop = 5;
		}	
	}

	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}

	@SuppressLint("DefaultLocale")
	public void draw(Canvas canvas, float width, float height) {
		calcMinMax();

		float horstart = borderLeft;
		float diffX = maxX - minX;
		float diffY = maxY - minY;
		float graphheight = height - (borderTop + borderBottom);
		float graphwidth = width - (borderLeft + borderRight);

		textSize = 12 * horstart / 25;

		//paint = new Paint();

		paint.setTextAlign(Align.RIGHT);
		paint.setShader(null);
		paint.setStrokeWidth(1);
		paint.setAlpha(0xFF);
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setColor(LINE_COLOR);
		paint.setAlpha(3 * alpha / 10);


		for (int i = 0; i <= VERTICAL_DIVISIONS; i++) {
			float y = graphheight - ((graphheight / VERTICAL_DIVISIONS) * i) + borderTop;
			canvas.drawLine(horstart, y, horstart + graphwidth, y, paint);
			String text = "";
			switch (labelStyleY) {
			case INT: text = GraphUtil.format(scale(minY + diffY / VERTICAL_DIVISIONS * i), 0); break;
			case TIME: text = GraphUtil.formatTime((long)(scale(diffY / VERTICAL_DIVISIONS * i))); break;
			default: text = GraphUtil.format(scale(minY + diffY / VERTICAL_DIVISIONS * i)); break;
			}
			if (text.length() <= 3) 
				paint.setTextSize(textSize);
			else
				paint.setTextSize(textSize * 3 / text.length());

			if (i % yLabelInterval == 0) canvas.drawText(text, horstart - 4, y, paint);
		}


		paint.setTextSize(textSize);
		paint.setTextAlign(Align.LEFT);
		for (int i = 0; i <= HORIZONTAL_DIVISIONS; i++) {
			float x = ((graphwidth / HORIZONTAL_DIVISIONS) * i) + horstart;
			canvas.drawLine(x, graphheight + borderTop, x, borderTop, paint);
			paint.setTextAlign(Align.CENTER);
			if (i == HORIZONTAL_DIVISIONS)
				paint.setTextAlign(Align.RIGHT);
			if (i == 0)
				paint.setTextAlign(Align.LEFT);
			String text = "";
			switch (labelStyleX) {
			case INT: text = GraphUtil.format(diffX / HORIZONTAL_DIVISIONS * i, 0); break;
			case TIME: text = GraphUtil.formatTime((long)(diffX / HORIZONTAL_DIVISIONS * i)); break;
			default: text = GraphUtil.format(diffX / HORIZONTAL_DIVISIONS * i); break;
			}
			if (xLabelInterval == 0) {
				if (i == 0 || i == HORIZONTAL_DIVISIONS) canvas.drawText(text, x, height - borderBottom + textSize * 1.2f, paint);
			} else if (i % xLabelInterval == 0) {
				canvas.drawText(text, x, height - borderBottom + textSize * 1.2f, paint);
			}
		}
		paint.setAlpha(alpha);

		if (title != null) {
			paint.setTextAlign(Align.CENTER);
			paint.setTextSize(textSize * 1.3f);
			canvas.drawText(title, (graphwidth / 2) + horstart, borderTop - 4, paint);
		}	


		if (values == null || values.length == 0 || (maxY == minY)) return;

		Shader shaders[] = new Shader[MAX_SHADERS];
		for (int i = 0; i < MAX_SHADERS; i++)
			shaders[i] = new LinearGradient(horstart, borderTop + graphheight, horstart, borderTop, SHADER_COLORS[i % MAX_SHADERS], null, Shader.TileMode.MIRROR);
		if (zones != null && zones.length >= 2) {
			float prevY = 0;
			for (int i = 0; i < zones.length; i++) {
				switch (i) {
				case 1:	paint.setColor(0xFF0000); break;
				case 2:	paint.setColor(0xFFFF00); break;
				case 3:	paint.setColor(0x00FF00); break;
				case 4:	paint.setColor(0x00FFFF); break;
				case 5:	paint.setColor(0x0000FF); break;
				case 6:	paint.setColor(0x990000); break;
				default: paint.setColor(0x999900); break;
				}
				paint.setAlpha(0x27);
			
				paint.setStyle(Paint.Style.FILL_AND_STROKE);
				float y = zones[i];
				y -= minY;
				y /= diffY;
				y *= graphheight;
				if (y <= 0) y = 1;
				if (y >= graphheight) y = graphheight - 1;
				if (i > 0) canvas.drawRect(horstart + 1, graphheight + borderTop - prevY, horstart + graphwidth - 1, graphheight + borderTop - y, paint);
				prevY = y;
			}
			
		}

		paint.setShader(shaders[0]);
		paint.setStrokeWidth(2);

		SparseArray<Path> paths = new SparseArray<Path>();
		SparseArray<Float> lastX = new SparseArray<Float>();

		int iCount = 0;
		for (XY xy: values) if (iCount < xs.length) {
			float x = (float)(xs[iCount++] - minX); //(float)( xy.getX() - minX);
			x /= diffX;
			x *= graphwidth;
			float[] ys = xy.getY();
            //int[] tags = xy.getTags();
			if (ys != null) for (int i = 0; i < Math.min(ys.length, MAX_SHADERS); i++) {
				if (paths.get(xy.getTag(i)) == null) {

					Path path = new Path();
					if (minY < 0) {
						float y = -minY;
						y /= diffY;
						if (y > 1) 
							y = graphheight;
						else
							y *= graphheight;
						path.moveTo(horstart + x, graphheight + borderTop - y);
					} else {	
						path.moveTo(horstart + x, graphheight + borderTop);
					}
					paths.put(xy.getTag(i), path);
				}
				float y = ys[i];
				y -= minY;
				y /= diffY;
				y *= graphheight;
				if (i < MAX_SHADERS) {
                    paths.get(xy.getTag(i)).lineTo(horstart + x, graphheight + borderTop - y);
					//LLog.d(Globals.TAG, CLASSTAG + ".draw:" + x + ", " + y + " (" +  graphheight + "), tag = " + xy.getTag(i)); 
				}
				lastX.put(xy.getTag(i), x); 
			}
		}
		int index = 0;

		paint.setTextAlign(Align.LEFT);
		paint.setTextSize(textSize * 0.8f);


		for (int i = 0; i < paths.size(); i++) { //..keySet()) {
			int tag = paths.keyAt(i);
			Path path = paths.get(tag);
			Float x = lastX.get(tag);
			if (path == null || x == null) continue;
			if (minY < 0) {
				float y = -minY;
				y /= diffY;
				if (y > 1) 
					y = graphheight;
				else
					y *= graphheight;
				path.lineTo(horstart + x, graphheight + borderTop - y);
			} else {	
				path.lineTo(horstart + x, graphheight + borderTop);
			}
			path.close();

			paint.setStrokeWidth(2);
			//paint.setAlpha(0x66); //.setColor(0xFF88AAFF);
			paint.setAlpha(alpha); //.setColor(0xFF88AAFF);
			paint.setStyle(Paint.Style.FILL);
			paint.setShader(shaders[index]);
			canvas.drawPath(path,  paint);
			paint.setAlpha(Math.min(0xFF, alpha * 2)); //.setColor(0xFF88AAFF);
			paint.setStyle(Paint.Style.STROKE);
			canvas.drawPath(path,  paint);

			if (hasLegenda && labels != null && paths.size() > 1) {
				x = horstart + 8 + index * (graphwidth - 8) / paths.size();

				float y = height - 4;
				paint.setStrokeWidth(1);
				paint.setTextSize(textSize);
				String info = "" + tag;
				if (tag < 0 && tag >= -6) {
					info = "" + xyz[-tag];
					x = horstart + (-tag - 1) * graphwidth / 6;
				} else {
					info = Util.arrayGetByBits(tag, labels, ", ");
				}
				if (info != null) canvas.drawText(info.toLowerCase(Locale.US), x, y, paint);
			}
			index++;
		}
		if (!angleLocation.isNone()) {
			float midX = width / 2;
			float midY = height / 2;
			float radius = Math.min(graphheight, graphwidth) / 2.0f;
			if (angleLocation.equals(AngleLocation.LOWERLEFT)) {
				radius = Math.min(graphheight, graphwidth) / 2.8f;
				midX = 1.05f * radius;
				midY = height - 1.05f * radius;
			}
			paint.setStrokeWidth(2);
			paint.setStyle(Paint.Style.FILL_AND_STROKE);
			paint.setShader(null);
			paint.setColor(0xFFFFFFFF);
			paint.setAlpha(0x88);
			canvas.drawCircle(midX, midY, radius * 1.1f, paint);

			canvas.save();
			paint.setAlpha(0xCC);

			for (int i = 0; i < 36; i++) {
				if (i % 9 == 0) {
					paint.setStrokeWidth(4);
					canvas.drawLine(midX, midY + radius * 0.8f, midX, midY + radius * 1.1f, paint);
				} else if (i % 3 == 0) {
					paint.setStrokeWidth(2);

					canvas.drawLine(midX, midY + radius * 0.9f, midX, midY + radius * 1.1f, paint);
				} else {
					paint.setStrokeWidth(1);
					canvas.drawLine(midX, midY + radius, midX, midY + radius * 1.1f, paint);

				}
				canvas.rotate(10.0f, midX, midY);
			}
			paint.setStrokeWidth(2);


			canvas.rotate((float) lastValue - 90, midX, midY);

			Path path = new Path();
			paint.setAlpha(0xAA);
			path.moveTo(radius + midX, midY);
			path.lineTo(midX, 0.4f * radius + midY);
			path.lineTo(midX, -0.4f * radius + midY);
			path.close();
			paint.setColor(0xFFFF0000);
			paint.setStyle(Paint.Style.FILL_AND_STROKE);
			canvas.drawPath(path, paint);

			paint.setColor(LINE_COLOR);
			paint.setStyle(Paint.Style.STROKE);
			canvas.drawPath(path, paint);

			paint.setColor(0xFF556688);

			path.reset(); 
			path.moveTo(-radius + midX, -0.1f * radius + midY);
			path.lineTo(-radius + midX, 0.1f * radius + midY);
			path.lineTo(midX, 0.1f * radius + midY);
			path.lineTo(midX, -0.1f * radius + midY);
			path.close();
			paint.setStyle(Paint.Style.FILL_AND_STROKE);
			canvas.drawPath(path, paint);
			paint.setColor(LINE_COLOR);
			paint.setStyle(Paint.Style.STROKE);
			canvas.drawPath(path, paint);
			paint.setStyle(Paint.Style.FILL_AND_STROKE);
			paint.setColor(0xFF999999);
			canvas.drawCircle(midX, midY, 0.05F * radius, paint);

			canvas.restore();
		}

	}

	public void setShowAngle(AngleLocation value) {
		angleLocation = value;
	}


	private void calcMinMax() {
		maxX = Long.MIN_VALUE;
		minX = Long.MAX_VALUE;
		maxY = - Float.MAX_VALUE;
		minY = Float.MAX_VALUE;
		int iCount = 0;
		long xOffset = 0;
		if (values == null || values.length == 0) {
			maxX = 10000;
			minX = 0;
			maxY = 100;
			minY = 0;
		} else for (XY xy: values) {
			long x = xy.getX();
			if (iCount > 0) {
				if (x - xOffset - xs[iCount - 1] > 300000) {
					xs[iCount] = xs[iCount - 1] + 60000;
					xOffset = x - xs[iCount];
				} else {
					xs[iCount] = x - xOffset;
				}
				iCount++;
			} else
				xs[iCount++] = x; 
			if (x - xOffset > maxX) maxX = x - xOffset;
			if (x - xOffset < minX) minX = x - xOffset;
			float[] ys = xy.getY();
			if (ys != null) for (int i = 0; i < ys.length; i++) {
				if (ys[i] > maxY) maxY = ys[i];
				if (ys[i] < minY) minY = ys[i];
			}
		}
	}

	public void setBorderTop(float borderTop) {
		this.borderTop = borderTop;
	}

	public void setBorderLeft(float borderLeft) {
		this.borderLeft = borderLeft;
	}

	public void setBorderRight(float borderRight) {
		this.borderRight = borderRight;
	}

	public void setBorderBottom(float borderBottom) {
		this.borderBottom = borderBottom;
	}

}
