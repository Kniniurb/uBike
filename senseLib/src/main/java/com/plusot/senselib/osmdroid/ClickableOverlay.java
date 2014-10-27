package com.plusot.senselib.osmdroid;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;

public class ClickableOverlay extends Overlay {
	
	public interface Listener {
		public boolean onTouch(MapView mapView, MotionEvent event);
		public boolean onClick(MapView mapView, MotionEvent event);
		}
	
	private Listener mListener;
	
	public ClickableOverlay(Context context, Listener listener) {
		super(context);
		mListener = listener;
	}
	
	public void setOnLongClickListener(Listener listener) {
		mListener = listener;
	}
	
	@Override
	protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
		// nothing to draw
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, MapView mapView) {
		mListener.onTouch(mapView, event);
		return super.onTouchEvent(event, mapView);
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
		mListener.onClick(mapView, e);
		return super.onSingleTapConfirmed(e, mapView);
	}
	
}
