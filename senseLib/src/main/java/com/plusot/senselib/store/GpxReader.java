package com.plusot.senselib.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Point;
import android.os.AsyncTask;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;

public class GpxReader {
	private static final String CLASSTAG = GpxReader.class.getSimpleName();
	//private static GpxReader instance = null;

	private final Listener listener;

	public interface Listener {
		void onPoints(List<Point> list);
		void onFailure(ResultType result);
	}

	public GpxReader(Listener listener, String path) {
		this.listener = listener;
		ReadGpxTask task = new ReadGpxTask();
		task.execute(path);
	}

	private class ProgressItem {
		final int count;
		public ProgressItem(int count) {
			this.count = count;
		}

	}

	public enum ResultType {
		NOLATLONG,
		NOFILE,
		FILEPROBLEM,
		OK;
	}

	private class ResultItem {
		final ResultType resultType;
		public ResultItem(final ResultType resultType) {
			this.resultType = resultType;
		}

	}

	private String findField(String string, final String field) {
		string = string.replace(" ", "");
		int iField = string.indexOf(field + "=");
		if (iField < 0) return null;
		int iSt = string.indexOf("\"", iField);
		if (iSt < 0) return null;
		int iEnd = string.indexOf("\"", iSt + 1);
		if (iEnd < 0) return null;
		return string.substring(iSt + 1, iEnd);
	}

	private class ReadGpxTask extends AsyncTask<String, ProgressItem, ResultItem> {
		private List<Point> points = new ArrayList<Point>();

		private ResultType readLatLon(String line) {
			//"<trkpt lat=\"" + Format.format(lat, 7) + "\" lon=\"" + Format.format(lng, 7) + "\">"
			String value;
			double lat = 0;
			double lng = 0;
			if ((value = findField(line, "lat")) == null) return ResultType.NOLATLONG;
			try {
				lat = Double.parseDouble(value);
			} catch (NumberFormatException e) {
				return ResultType.NOLATLONG;
			}
			if ((value = findField(line, "lon")) == null) return ResultType.NOLATLONG;
			try {
				lng = Double.parseDouble(value);
			} catch (NumberFormatException e) {
				return ResultType.NOLATLONG;
			}
			points.add(new Point((int) (lat * 1E6), (int)(lng * 1E6)));
			return ResultType.OK;
		}

		@Override
		protected ResultItem doInBackground(String... params) {
			if (params == null || params.length < 1) return new ResultItem(ResultType.NOFILE);
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(new File(params[0])));	

				String line = in.readLine();
				int i = 0;
				while (line != null) {
					String[] lines = line.split("/>");
					for (String str : lines) if (str.contains("trkpt")) {
						readLatLon(str);
					}
					line = in.readLine();
					if (i++ % 100 == 0) this.publishProgress(new ProgressItem(i));
				}

			} catch (IOException e) {
				LLog.e(Globals.TAG, CLASSTAG + ".ReadGpxTask.doInBackground: Failure in reading line", e);
				return new ResultItem(ResultType.FILEPROBLEM);
			} finally {
				if (in != null) try {
					in.close();
				} catch (IOException e) {
					LLog.e(Globals.TAG, CLASSTAG + ".ReadGpxTask.doInBackground: Could not close stream.");
					return new ResultItem(ResultType.FILEPROBLEM);
				}
			}

			return new ResultItem(ResultType.OK);
		}
		
		@Override
		protected void onProgressUpdate(ProgressItem... progress) {
			if (progress.length > 0) LLog.d(Globals.TAG, CLASSTAG + ".ReadGpxTask.onProgressUpdate "  + progress[0].count);
		}

		@Override
		protected void onPostExecute(ResultItem result) {
			LLog.d(Globals.TAG, CLASSTAG + ".ReadGpxTask.onPostExecute = task done");
			if (result.resultType.equals(ResultType.OK)) {
				listener.onPoints(points);

			} else {
				listener.onFailure(result.resultType);
			}
		}

	}


}
