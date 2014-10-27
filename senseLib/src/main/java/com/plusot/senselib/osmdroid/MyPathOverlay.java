/*******************************************************************************
 * Copyright (c) 2012 Plusot Biketech
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Bruinink - initial API and implementation and/or initial documentation
 * Based on PathOverlay by:
 *     Viesturs Zarins
 * 
 *******************************************************************************/
package com.plusot.senselib.osmdroid;

import java.util.ArrayList;
import java.util.List;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;

import com.plusot.senselib.R;
import com.plusot.senselib.SenseGlobals;

/**
 * 
 * @author Viesturs Zarins
 * 
 *         This class draws a path line in given color.
 */
public class MyPathOverlay extends Overlay {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	/**
	 * Stores points, converted to the map projection.
	 */
	private List<Point> mPoints;

	/**
	 * Number of points that have precomputed values.
	 */
	private int mPointsPrecomputed;

	/**
	 * Paint settings.
	 */
	protected Paint mPaint = new Paint();

	private final Path mPath = new Path();

	private final Point mTempPoint1 = new Point();
	private final Point mTempPoint2 = new Point();

	// bounding rectangle for the current line segment.
	private final Rect mLineBounds = new Rect();
	private float crossSize = 4;
	private String[] texts = null;
	private float strokeWidth = 2.0f;
	private int color = 0xFFFF0000;
	protected final Bitmap PERSON_ICON;
	protected final PointF PERSON_HOTSPOT;


	// ===========================================================
	// Constructors
	// ===========================================================

	public MyPathOverlay(final int color, final Context ctx) {
		this(color, ctx, new DefaultResourceProxyImpl(ctx));
	}

	public MyPathOverlay(final int color, final Context ctx, final ResourceProxy pResourceProxy) {
		super(pResourceProxy);
		this.color = color;
		this.mPaint.setColor(color);
		this.mPaint.setStrokeWidth(strokeWidth);
		this.mPaint.setStyle(Paint.Style.STROKE);

		this.clearPath();
		if (SenseGlobals.isBikeApp) {
			PERSON_ICON = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.bicycle_green); //mResourceProxy.getBitmap(ResourceProxy.bitmap.person);
			PERSON_HOTSPOT = new PointF(2.0f * mScale + 0.5f, 44.0f * mScale + 0.5f);

		} else {
			PERSON_ICON = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.person); //mResourceProxy.getBitmap(ResourceProxy.bitmap.person);
			PERSON_HOTSPOT = new PointF(24.0f * mScale + 0.5f, 39.0f * mScale + 0.5f);
		}
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void setColor(final int color) {
		this.mPaint.setColor(color);
		this.color = color;
	}

	public void setAlpha(final int a) {
		this.mPaint.setAlpha(a);
	}

	public Paint getPaint() {
		return mPaint;
	}

	public void setStrokeWidth(float strokeWidth) {
		this.strokeWidth = strokeWidth;
		mPaint.setStrokeWidth(strokeWidth);
	}

	public void setCrossSize(float crossSize) {
		this.crossSize = crossSize;
	}

	public void setPaint(Paint pPaint) {
		if (pPaint == null)
			throw new IllegalArgumentException("pPaint argument cannot be null");
		mPaint = pPaint;
	}

	public void clearPath() {
		this.mPoints = new ArrayList<Point>();
		this.mPointsPrecomputed = 0;
	}

	public void addPoint(final GeoPoint pt) {
		this.addPoint(pt.getLatitudeE6(), pt.getLongitudeE6());
	}

	public void setPoints(final List<Point> pts) {
		this.mPoints = pts;
	}

	public void addPoint(final int latitudeE6, final int longitudeE6) {
		this.mPoints.add(new Point(latitudeE6, longitudeE6));
	}

	public int getNumberOfPoints() {
		return this.mPoints.size();
	}

	public void setTexts(final String[] texts) {
		this.texts = texts;
	}

	/**
	 * This method draws the line. Note - highly optimized to handle long paths, proceed with care.
	 * Should be fine up to 10K points.
	 */
	@Override
	protected void draw(final Canvas canvas, final MapView mapView, final boolean shadow) {

		if (shadow) {
			return;
		}


		if (this.mPoints.size() < 2) {
			// nothing to paint
			return;
		}

		final Projection pj = mapView.getProjection();

		// precompute new points to the intermediate projection.
		final int size = this.mPoints.size();

		while (this.mPointsPrecomputed < size) {
			final Point pt = this.mPoints.get(this.mPointsPrecomputed);
			pj.toProjectedPixels(pt.x, pt.y, pt);

			this.mPointsPrecomputed++;
		}

		Point screenPoint0 = null; // points on screen
		Point screenPoint1 = null;
		Point projectedPoint0; // points from the points list
		Point projectedPoint1;

		// clipping rectangle in the intermediate projection, to avoid performing projection.
		BoundingBoxE6 boundingBox = pj.getBoundingBox();
		Point topLeft = pj.toProjectedPixels(boundingBox.getLatNorthE6(),
				boundingBox.getLonWestE6(), null);
		Point bottomRight = pj.toProjectedPixels(boundingBox.getLatSouthE6(),
				boundingBox.getLonEastE6(), null);
		final Rect clipBounds = new Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);


		mPath.rewind();
		projectedPoint0 = this.mPoints.get(size - 1);
		mLineBounds.set(projectedPoint0.x, projectedPoint0.y, projectedPoint0.x, projectedPoint0.y);

		for (int i = size - 2; i >= 0; i--) {
			// compute next points
			projectedPoint1 = this.mPoints.get(i);
			mLineBounds.union(projectedPoint1.x, projectedPoint1.y);

			if (!Rect.intersects(clipBounds, mLineBounds)) {
				// skip this line, move to next point
				projectedPoint0 = projectedPoint1;
				screenPoint0 = null;
				continue;
			}

			// the starting point may be not calculated, because previous segment was out of clip
			// bounds
			if (screenPoint0 == null) {
				screenPoint0 = pj.toPixelsFromProjected(projectedPoint0, this.mTempPoint1);
				if (i == size - 2) {
					this.mPaint.setStrokeWidth(1);
					this.mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
					this.mPaint.setTextSize(20);
					this.mPaint.setColor(0x99000000);
					float offset = 22;
					if (texts != null) for (int j = 0; j < texts.length; j++) if (texts[j] != null) {
						canvas.drawText(texts[j], screenPoint0.x, screenPoint0.y + offset, this.mPaint);
						offset+= 23;
					}
					this.mPaint.setStrokeWidth(strokeWidth);
					this.mPaint.setStyle(Paint.Style.STROKE);
					this.mPaint.setColor(color);

					if (crossSize > 0 ) {
						mPath.moveTo(screenPoint0.x - crossSize, screenPoint0.y - crossSize);
						mPath.lineTo(screenPoint0.x + crossSize, screenPoint0.y + crossSize);
						mPath.moveTo(screenPoint0.x - crossSize, screenPoint0.y + crossSize);
						mPath.lineTo(screenPoint0.x + crossSize, screenPoint0.y - crossSize);
						canvas.drawBitmap(PERSON_ICON, screenPoint0.x -PERSON_HOTSPOT.x, screenPoint0.y -PERSON_HOTSPOT.y, mPaint);
					} else {
						canvas.drawCircle(screenPoint0.x, screenPoint0.y, strokeWidth * 3, mPaint);
					}
				}
				mPath.moveTo(screenPoint0.x, screenPoint0.y);
			}

			screenPoint1 = pj.toPixelsFromProjected(projectedPoint1, this.mTempPoint2);

			// skip this point, too close to previous point
			if (Math.abs(screenPoint1.x - screenPoint0.x)
					+ Math.abs(screenPoint1.y - screenPoint0.y) <= 1) {
				continue;
			}

			mPath.lineTo(screenPoint1.x, screenPoint1.y);

			// update starting point to next position
			projectedPoint0 = projectedPoint1;
			screenPoint0.x = screenPoint1.x;
			screenPoint0.y = screenPoint1.y;
			mLineBounds.set(projectedPoint0.x, projectedPoint0.y, projectedPoint0.x, projectedPoint0.y);
		}

		canvas.drawPath(mPath, this.mPaint);


	}
}
