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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Graph creates a scaled line or xy graph with x and y axis labels. 
 * @author Peter Bruinink
 *
 */
public class Graph extends BaseGraph {

	private final float height;
	private final float width;
	private Canvas canvas;
	private Bitmap bmp;
	
	//private int[] tags = new int[MAX_SHADERS]; 

	public Graph(int width, int height) {
		super(1, 1, true);
		bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		bmp.setDensity(1);
		this.width = bmp.getWidth();
		this.height = bmp.getHeight();
		this.canvas= new Canvas(bmp);
		this.setBorderBottom(40);
		
	}
	
	public Bitmap getBitmap() {
		return bmp;
	}

	public void draw() {
		paint.setColor(Color.WHITE);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		canvas.drawRect(0, 0, width, height, paint);
		
		super.draw(canvas, width, height);
	}	
}
