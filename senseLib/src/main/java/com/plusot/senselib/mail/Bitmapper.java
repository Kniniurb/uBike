package com.plusot.senselib.mail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.store.ZipFiles;
import com.plusot.senselib.values.Value;
import com.plusot.senselib.values.ValueType;
import com.plusot.senselib.widget.Graph;
import com.plusot.senselib.widget.XY;

public class Bitmapper {
	private static final String CLASSTAG = Bitmapper.class.getSimpleName();
	private final Context context;
	private final Bitmapable request;
	private static List<String> files = new ArrayList<String>();
	private static long filesMade = 0;

	public Bitmapper(Context context, Bitmapable request) {
		this.context = context;
		this.request = request;
	}

	private String addScreenShot(View view) {
		view.setDrawingCacheEnabled(true);
		Bitmap bmp = view.getDrawingCache();
		try {
			String fileName = "Screenshot";
			if (view.getTag() instanceof String) 
				fileName = (String) view.getTag();
			fileName += "_" + TimeUtil.formatTime(System.currentTimeMillis(), "yyyyMMdd-HHmmss") + ".png";
			fileName = SenseGlobals.getScreenshotPath() + fileName;
			FileOutputStream fo = new FileOutputStream(new File(fileName));
			bmp.compress(Bitmap.CompressFormat.PNG, 90, fo);
			fo.close();
			//ToastHelper.showToastShort(fileName + " " + getString(R.string.saved));
			return fileName;
		} catch (FileNotFoundException e) {
			LLog.e(Globals.TAG, CLASSTAG + ": Could not open file for screenshot.", e);
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ": Could not close file for screenshot.", e);
		}
		return null;
	}

	private String addBitmap(Value value) {
		try {
			Graph graph = new Graph(600, 450);
			graph.setScaler(value.getUnit().getScaler());
			graph.setValues(value.getQuickView().toArray(new XY[0]), value.getValueType().presentAsAngle(value.getValueAsFloat()), value.getValueType().presentAsAngle(), value.getZones());
			String unit = value.getUnitLabel(context);
			if (unit.length() > 0) unit = " " + unit;
			graph.setTitle(value.getValueType(context)  + unit);
			graph.draw();
			//graph.setTitle(title)
			String fileName = value.getValueType(context);
			fileName += "_" + TimeUtil.formatTime(System.currentTimeMillis(), "yyyyMMdd-HHmmss") + ".png";
			fileName = SenseGlobals.getScreenshotPath() + fileName;
			FileOutputStream fo = new FileOutputStream(new File(fileName));
			if (!graph.getBitmap().compress(Bitmap.CompressFormat.PNG, 90, fo))
				//LLog.d(Globals.TAG, CLASSTAG + ".addBitmap: " + fileName + " written succesfully");
				//else
				LLog.d(Globals.TAG, CLASSTAG + ".addBitmap: " + fileName + " not written");

			fo.close();
			Thread.yield();
			//ToastHelper.showToastShort(fileName + " " + getString(R.string.saved));
			return fileName;
		} catch (FileNotFoundException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".addBitmap: Could not open file for screenshot.", e);
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".addBitmap: Could not close file for screenshot.", e);
		}
		return null;
	}

	public void clear() {
		synchronized(files) {files.clear();}
	}

	public List<String> getFiles() {
		synchronized(files) {
			return new ArrayList<String>(files);
		}
	}

	private void makeScreenshots() {
		File file = new File(SenseGlobals.getScreenshotPath()); 
		if (!file.isDirectory()) file.mkdirs();

		if (request != null) try {
			View[] views = request.onRequestViews();
			if (views != null && views.length > 0) for (View view : views) {
				String fileName = addScreenShot(view);
				if (fileName != null) synchronized(files) {files.add(fileName); }
			}
		} catch (Exception e) {
		}
	}

	private void makeBitmaps() {
		for (ValueType valueType : ValueType.values()) {
			if (!valueType.sendAsBitmap(PreferenceKey.XTRAVALUES.isTrue())) continue;
			Value value = Value.getValue(valueType, false);
			if (value == null) continue;
			if (!value.hasRegistrations()) continue;
			String fileName = addBitmap(value);
			if (fileName != null) synchronized(files) {files.add(fileName);}
		}
	}
	
	public void make() {
		if (System.currentTimeMillis() - filesMade > ZipFiles.NEWZIP_INTERVAL) {
			clear();
			makeScreenshots();
			makeBitmaps();
			filesMade = System.currentTimeMillis();
		}
	}

}
