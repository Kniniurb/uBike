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
package com.plusot.senselib.widget;

import java.util.List;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Display;
import android.view.WindowManager;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.ToastHelper;
import com.plusot.senselib.R;
import com.plusot.senselib.osmdroid.ClickableOverlay;
import com.plusot.senselib.osmdroid.MyPathOverlay;
import com.plusot.senselib.settings.PreferenceKey;

public class MyMapView extends MapView {
	private static String CLASSTAG = MyMapView.class.getSimpleName();
	private MyPathOverlay pathOverlay;
	private MyPathOverlay gpxOverlay = null;
	private ClickableOverlay clickable;


	public static final OnlineTileSourceBase MAPQUEST = new XYTileSource("MapQuest", //PreferenceKey.TILESOURCE.getString(),
			ResourceProxy.string.mapquest_osm, 0, 18, 256, ".png", new String[] {
			"http://otile1.mqcdn.com/tiles/1.0.0/osm/",
			"http://otile2.mqcdn.com/tiles/1.0.0/osm/",
			"http://otile3.mqcdn.com/tiles/1.0.0/osm/",
			"http://otile4.mqcdn.com/tiles/1.0.0/osm/" 
	});
	public static final OnlineTileSourceBase MAPNIK = new XYTileSource("Mapnik",
			ResourceProxy.string.mapnik, 0, 18, 256, ".png", new String[] {
			"http://a.tile.openstreetmap.org/",
			"http://b.tile.openstreetmap.org/",
			"http://c.tile.openstreetmap.org/" 
	});

	public static final OnlineTileSourceBase UMAPS = new XYTileSource("4uMaps",
			ResourceProxy.string.mapquest_osm, 0, 17, 256, ".png", new String[] {
			"http://www.4umaps.eu/" 
	});

	public static final OnlineTileSourceBase CYCLEMAP = new XYTileSource("CycleMap",
			ResourceProxy.string.cyclemap, 0, 17, 256, ".png", new String[] {
			"http://a.tile.opencyclemap.org/cycle/",
			"http://b.tile.opencyclemap.org/cycle/",
			"http://c.tile.opencyclemap.org/cycle/" 
	});
	//	public static final OnlineTileSourceBase WANDER = new XYTileSource("WanderReitKarte",
	//			ResourceProxy.string.topo, 0, 17, 256, ".png", new String[] {
	//			"http://www.wanderreitkarte.de/topo/" 
	//	});
	public static final OnlineTileSourceBase OUTDOORS = new XYTileSource("Outdoors",
			ResourceProxy.string.cyclemap, 0, 17, 256, ".png", new String[] {
			"http://a.tile.thunderforest.com/outdoors/",
			"http://b.tile.thunderforest.com/outdoors/",
			"http://c.tile.thunderforest.com/outdoors/", 
	});
	public static final OnlineTileSourceBase LANDSCAPE = new XYTileSource("Landscape",
			ResourceProxy.string.cyclemap, 0, 17, 256, ".png", new String[] {
			"http://a.tile.thunderforest.com/landscape/",
			"http://b.tile.thunderforest.com/landscape/",
			"http://c.tile.thunderforest.com/landscape/", 
	});



	//	public static final OnlineTileSourceBase BASE = new XYTileSource("Base",
	//			ResourceProxy.string.base, 4, 17, 256, ".png",
	//			new String[] { "http://topo.openstreetmap.de/base/" });
	//
	//	public static final OnlineTileSourceBase TOPO = new XYTileSource("Topo",
	//			ResourceProxy.string.topo, 4, 17, 256, ".png",
	//			new String[] { "http://topo.openstreetmap.de/topo/" });
	//
	//	public static final OnlineTileSourceBase HILLS = new XYTileSource("Hills",
	//			ResourceProxy.string.hills, 8, 17, 256, ".png",
	//			new String[] { "http://topo.geofabrik.de/hills/" });
	//
	//	public static final OnlineTileSourceBase CLOUDMADESTANDARDTILES = new CloudmadeTileSource(
	//			"CloudMadeStandardTiles", ResourceProxy.string.cloudmade_standard, 0, 18, 256, ".png",
	//			new String[] { "http://a.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s",
	//					"http://b.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s",
	//					"http://c.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s" });
	//
	//	// FYI - This tile source has a tileSize of "6"
	//	public static final OnlineTileSourceBase CLOUDMADESMALLTILES = new CloudmadeTileSource(
	//			"CloudMadeSmallTiles", ResourceProxy.string.cloudmade_small, 0, 21, 64, ".png",
	//			new String[] { "http://a.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s",
	//					"http://b.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s",
	//					"http://c.tile.cloudmade.com/%s/%d/%d/%d/%d/%d%s?token=%s" });


	public static final OnlineTileSourceBase[] TILE_SOURCES = new OnlineTileSourceBase[]{CYCLEMAP, MAPQUEST, MAPNIK, UMAPS, OUTDOORS, LANDSCAPE}; //, BASE, TOPO, HILLS, CLOUDMADESTANDARDTILES, CLOUDMADESMALLTILES};
	public static final String[] TILE_SOURCE_NAMES = new String[]{"Cycle Map", "Mapquest", "Mapnik", "4uMaps", "OutDoors", "Landscape"}; // , "Base", "Topo", "Hills", "Cloudmade Standard", "Cloudmade Small"};
	public static final String[] TILE_SOURCE_VALUES = new String[]{"0", "1", "2", "3", "4", "5"}; 


	public MyMapView(Context context) {
		super(context, 256, new DefaultResourceProxyImpl(context), new MapTileProviderBasic(context, TILE_SOURCES[PreferenceKey.TILESOURCE.getInt() % TILE_SOURCES.length]));
		init();
	}

	public MyMapView(Context context, AttributeSet attrs) {
		super(context, 256, new DefaultResourceProxyImpl(context), new MapTileProviderBasic(context, TILE_SOURCES[PreferenceKey.TILESOURCE.getInt() % TILE_SOURCES.length]), null, attrs);
		init();
	}

	private int prevZoom = 0 ;

	@Override
	public int getMaxZoomLevel() {
		int zoom = this.getZoomLevel(true);
		if (zoom != prevZoom) {
			LLog.d(Globals.TAG, CLASSTAG + ".getMaxZoomLevel zoom = " + zoom);
			ToastHelper.showToastShort(Globals.appContext.getString(R.string.zoom_level, zoom));
		}
		prevZoom = zoom;
		return super.getMaxZoomLevel();
	}	

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void init() {
		LocationManager locationMgr = (LocationManager) this.getContext().getSystemService(Context.LOCATION_SERVICE);
		Location loc = locationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		setClickable(true);
		setLongClickable(false);
		setUseDataConnection(true);
		setMultiTouchControls(true);
		setBuiltInZoomControls(true);

		getController().setZoom(14);
		GeoPoint pt = new GeoPoint(51.917168, 5.830994);
		if (loc != null) pt = new GeoPoint(loc.getLatitude(), loc.getLongitude());

		getController().setCenter(pt);

		pathOverlay = new MyPathOverlay(0xFFFF0000, this.getContext()); //, mapView, resourceProxy);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			WindowManager wm = (WindowManager) Globals.appContext.getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			pathOverlay.setStrokeWidth(size.x / 130 + 1);
		} else
			pathOverlay.setStrokeWidth(3f);
		pathOverlay.setCrossSize(5f);

		getOverlays().add(pathOverlay);
	}

	public void setOnTouchListener(ClickableOverlay.Listener listener) {
		if (clickable != null) getOverlays().remove(clickable);
		clickable = new ClickableOverlay(this.getContext(), listener);
		getOverlays().add(clickable);

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	public void addGpx(List<Point> points) {
		if (points == null) return;
		if (gpxOverlay == null) {
			gpxOverlay = new MyPathOverlay(0x990000FF, this.getContext()); //, mapView, resourceProxy);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
				WindowManager wm = (WindowManager) Globals.appContext.getSystemService(Context.WINDOW_SERVICE);
				Display display = wm.getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				gpxOverlay.setStrokeWidth(size.x / 150 + 1);
			} else 
				gpxOverlay.setStrokeWidth(3f);
			gpxOverlay.setCrossSize(0);
			getOverlays().add(gpxOverlay);
		}
		gpxOverlay.setPoints(points);
		if (points.size() > 0) getController().setCenter(new GeoPoint(points.get(0).x, points.get(0).y));
	}

	public void setZoom(int zoom) {
		getController().setZoom(zoom);
	}

	public void setPath(List<XY> list) {
		pathOverlay.clearPath();
		if (list == null || list.size() == 0) return;
		GeoPoint pt = null;

		for (XY xy: list) {		
			if (xy.getY().length >= 2) {
				if (xy.getTag(0) == -1)
					pt = new GeoPoint(xy.getY()[0], xy.getY()[1]);
				else
					pt = new GeoPoint(xy.getY()[1], xy.getY()[0]);
				pathOverlay.addPoint(pt);
			}

		}
		if (pt != null) {
			getController().setCenter(pt);


		}
	}

	public void pause() {

	}

	public void resume() {
		pathOverlay.isEnabled();
		if (gpxOverlay != null) gpxOverlay.isEnabled();
	}

}