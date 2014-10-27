/*******************************************************************************
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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.plusot.common.Globals;
import com.plusot.javacommon.util.Scaler;
import com.plusot.senselib.R;

/**
 * XYGraphView creates a scaled line or xy graph with x and y axis labels. 
 * @author Peter Bruinink
 *
 */
public class XYGraphView extends View {
	private BaseGraph graph; 


	//private int[] tags = new int[MAX_SHADERS]; 

	public XYGraphView(Context context) {
		super(context);
		init();
	}

	public XYGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
		setAttributes(context, attrs);
	}

	public XYGraphView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
		setAttributes(context, attrs);
	}


	private void init() {
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//		    setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//		}
		graph = new BaseGraph(4, 2, Globals.testing.isTest());
		if (Globals.testing.isTest() == true) {
			BaseGraph.setShaderColor(0, 
					this.getContext().getResources().getColor(R.color.graph1_color));
			BaseGraph.setShaderColor(1, 
					this.getContext().getResources().getColor(R.color.graph2_color));
			BaseGraph.setShaderColor(2, 
					this.getContext().getResources().getColor(R.color.graph3_color));
			BaseGraph.setShaderColor(3, 
					this.getContext().getResources().getColor(R.color.graph4_color));
			BaseGraph.setShaderColor(4, 
					this.getContext().getResources().getColor(R.color.graph5_color));
			BaseGraph.setShaderColor(5, 
					this.getContext().getResources().getColor(R.color.graph6_color));
		} else {
			BaseGraph.setShaderColor(0, 

					this.getContext().getResources().getColor(R.color.base_color));
			BaseGraph.setShaderColor(1, 
					this.getContext().getResources().getColor(R.color.lighter_basecolor));
			BaseGraph.setShaderColor(2, 
					this.getContext().getResources().getColor(R.color.darker_basecolor));
			BaseGraph.setShaderColor(3, 
					this.getContext().getResources().getColor(R.color.darkest_basecolor));
			BaseGraph.setShaderColor(4, 
					this.getContext().getResources().getColor(R.color.darker_color));
			BaseGraph.setShaderColor(5, 
					this.getContext().getResources().getColor(R.color.lightest_basecolor));
		}
		this.setBackgroundColor(0x66FFFFFF);
	}

	private void setAttributes(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GraphView);
		setBackgroundColor(a.getColor(R.styleable.GraphView_background, 0x33FFFFFF));
		graph.setBorderBottom(a.getDimensionPixelSize(R.styleable.GraphView_borderBottom, 20));
		graph.setBorderTop(a.getDimensionPixelSize(R.styleable.GraphView_borderTop, 5));
		graph.setBorderLeft(a.getDimensionPixelSize(R.styleable.GraphView_borderLeft, 20));
		graph.setBorderRight(a.getDimensionPixelSize(R.styleable.GraphView_borderRight, 5));
		graph.setAlpha(a.getInt(R.styleable.GraphView_alpha, BaseGraph.DEFAULT_ALPHA));
		a.recycle();	
	}

	public void setValues(XY[] values, float lastValue, BaseGraph.AngleLocation angleLocation, float[] zones) {
		graph.setValues(values, lastValue, angleLocation, zones);
		invalidate();
	}

	public void setTitle(String title) {
		graph.setTitle(title);
		invalidate();
	}

	public void setScaler(Scaler scaler) {
		graph.setScaler(scaler);
	}

	public Scaler getScaler() {
		return graph.getScaler();
	}


	@Override
	protected void onDraw(Canvas canvas) {
		graph.draw(canvas, getWidth(), getHeight());
	}


	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		//mChanged = true;
	}

}
