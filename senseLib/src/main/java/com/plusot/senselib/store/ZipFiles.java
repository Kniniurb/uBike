package com.plusot.senselib.store;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.os.AsyncTask;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.share.LogFiles;
import com.plusot.common.share.SimpleLog;
import com.plusot.common.util.FileExplorer;
import com.plusot.common.util.FileUtil;
import com.plusot.common.util.Watchdog;
import com.plusot.senselib.SenseGlobals;
import com.plusot.senselib.mail.Bitmapable;
import com.plusot.senselib.mail.Bitmapper;
import com.plusot.senselib.settings.FileType;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.values.Value;

public class ZipFiles {

	private static final String CLASSTAG = ZipFiles.class.getSimpleName();
	private static final String ZIPEXT = ".gz";
	private static Set<String> files = new HashSet<String>();
	private static long filesMade = 0;
	public static final long NEWZIP_INTERVAL = 300000;
	//public static SparseArray<String> zippedFiles

	private Bitmapper bits = null;

	public ZipFiles(final Context context, final Bitmapable owner) {
		if (Value.fileTypes.contains(FileType.SCR)) {
			bits = new Bitmapper(context, owner);
			bits.make();
		}
	}

	private void stopInstances() {
		DataLog.stopInstance();
		FitLog.stopInstance();
		SRMLog.stopInstance(CLASSTAG);
		GpxLog.stopInstance(CLASSTAG, false);
		PwxLog.stopInstance(CLASSTAG, false);
		SimpleLog.stopInstances();
	}

	private static long fitClosed = 0;

	public static String[] createFitFiles() {
		String[] files = null;
		FitLog fit = FitLog.getInstance();
		if (fit == null) return null;
		if (System.currentTimeMillis() - fitClosed > 300000) {
			fit.close();
			fitClosed = System.currentTimeMillis();
		}
		FileExplorer explorer = new FileExplorer(Globals.getDataPath(), FitLog.SPEC, FitLog.EXT, FileExplorer.FITLOG_TAG);
		files = explorer.getFileListWithPath(System.currentTimeMillis());
//		if (Value.fileTypes.contains(FileType.FIT) && AppPreferences.getInstance() != null) {
//			files = AppPreferences.getInstance().getFitFiles();
//		}
		Thread.yield();
		return files;
	}

	private static String[] createZipFile(Bitmapper bits, boolean closeActivity, boolean closeApplication, boolean zipIt) {
		String session = Value.getSessionString();
		String[] result;
		synchronized(files) {
			if (System.currentTimeMillis() - filesMade > NEWZIP_INTERVAL) {
				filesMade = System.currentTimeMillis();
				files.clear();
				LLog.d(Globals.TAG, CLASSTAG + ".createZipFile: Creating new file list");
				if (bits != null) files.addAll(bits.getFiles());
				//if (files == null) files = new ArrayList<String>();

				if (SenseGlobals.isBikeApp) {
					DataLog data =  DataLog.getInstance();
					String fileName;
					if (data != null && (fileName = DataLog.getFileName(session)) != null) {
						if (data.getRecordsWriten() > 0) data.close();
						if (Value.fileTypes.contains(FileType.CSV) && new File(fileName).exists()) files.add(fileName);
						if (closeApplication) DataLog.stopInstance();
					}
					Thread.yield();
					FitLog fit = FitLog.getInstance();;
					if (fit != null && fit.getRecordsWriten() > 0) {
						if (closeApplication) 
							FitLog.stopInstance();
						else
							fit.close();
						if (Value.fileTypes.contains(FileType.FIT)) {
							files.addAll(PreferenceKey.FITFILELIST.getStringList());
						}
					}
					Thread.yield();
					if (SRMLog.getInstance() != null && (fileName = SRMLog.getFileName(session)) != null) {
						if (closeApplication) 
							SRMLog.stopInstance(CLASSTAG);
						else
							SRMLog.getInstance().close(CLASSTAG);
						if (Value.fileTypes.contains(FileType.SRM) && /*!SenseGlobals.isLite &&*/ new File(fileName).exists()) files.add(fileName);	
					}
					Thread.yield();
					if (GpxLog.getInstance() != null  && (fileName = GpxLog.getFileName(session)) != null) {
						if (closeApplication) 
							GpxLog.stopInstance(CLASSTAG, true);
						else
							GpxLog.getInstance().close(CLASSTAG, true);
						if (Value.fileTypes.contains(FileType.GPX) && new File(fileName).exists()) files.add(fileName);	
					}
					Thread.yield();
					if (PwxLog.getInstance() != null  && (fileName = PwxLog.getFileName(session)) != null) {
						if (closeApplication) 
							PwxLog.stopInstance(CLASSTAG, true); 
						else 
							PwxLog.getInstance().close(CLASSTAG, closeActivity, true);
						if (Value.fileTypes.contains(FileType.PWX) && /*!SenseGlobals.isLite &&*/ new File(fileName).exists()) files.add(fileName);		
					}
					Thread.yield();
					//			case GCSV:
					//				if (GoldenCheetahLog.getInstance() != null && (fileName = GoldenCheetahLog.getInstance().getFilename()) != null) {
					//					if (new File(fileName).exists()) files.add(fileName);
					//				}
				}
				//if (Globals.testing.isTest()) 
					files.add(LogFiles.systemLogToFile());
				//else
				//	LogFiles.systemLogToFile();
			}
			if (SimpleLog.hasInstances()) {
				files.addAll(SimpleLog.getFileNames(session));
				if (closeApplication)
					SimpleLog.stopInstances();
				else
					SimpleLog.closeInstances();
			}
			//			catch (InterruptedException e) {
			//			LLog.d(Globals.TAG, CLASSTAG + ".createZipFile interrupted");
			//		}
			result = files.toArray(new String[0]);
		} 
		if (zipIt) {
			String zipFile = getZipFileName(session, false);		
			FileUtil.toZip(result, zipFile);
			LLog.d(Globals.TAG, CLASSTAG + ".zipFile: Zipped new files to " + zipFile);
			result = new String[]{ zipFile };
		}
		if (closeActivity) doCloseActivity(zipIt);

		return result;	
	}

	private static void doCloseActivity(boolean zipIt) {
		if (zipIt && Globals.testing.isNoTest()) clearSD();
		PreferenceKey.FITFILELIST.set("");
	}

	public static File getZipFile(String session) {
		if (session == null) {
			LLog.d(Globals.TAG, CLASSTAG + ".getZipFileName: No Session time");
			return null;
		}
		String zipFile = Globals.getDataPath() + SenseGlobals.fileSpec + SenseGlobals.DELIM_DETAIL + session + ZIPEXT;
		LLog.d(Globals.TAG, CLASSTAG + ".getZipFile: " + zipFile);
		File file = new File(zipFile);
		if (file != null && file.exists()) return file;
		return null;
	}

	public static boolean needsToCreateZip(String session, long oldness) {
		File file = getZipFile(session);
		if (file == null) return true;
		if (System.currentTimeMillis() - file.lastModified() > oldness) return true;
		return false;
	}

	public static String getZipFileName(String session, boolean check) {
		if (session == null) {
			LLog.d(Globals.TAG, CLASSTAG + ".getZipFileName: No Session time");
			return null;
		}
		String zipFile = Globals.getDataPath() + SenseGlobals.fileSpec + SenseGlobals.DELIM_DETAIL + session + ZIPEXT;
		LLog.d(Globals.TAG, CLASSTAG + ".getZipFileName: " + zipFile);
		if (!check) return zipFile;
		if(new File(zipFile).exists()) return zipFile;
		return null;
	}

	public static String[] createZipFile(Bitmapper bits) {
		return createZipFile(bits, false, false, true);
	}

	private static void delete(final String path, final String spec, final String ext) {
		File folder = new File(path);
		String[] files = folder.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				if ((spec == null || filename.startsWith(spec)) && filename.endsWith(ext)) return true;
				return false;
			}
		});
		if (files != null) for (String file: files) {
			if (!(new File(path + file).delete()))
				//LLog.d(Globals.TAG, CLASSTAG + ".delete: Deleted: " + path + file);
				//else
				LLog.d(Globals.TAG, CLASSTAG + ".delete: Not deleted: " + path + file);
		}
	}

	private static void clearSD() {
		LLog.d(Globals.TAG, CLASSTAG + ".clearSD: Removing unnecessary files");

		delete(Globals.getDataPath(), SenseGlobals.fileSpec, ".gp");
		delete(Globals.getDataPath(), SenseGlobals.fileSpec, ".gpx");
		delete(Globals.getDataPath(), SenseGlobals.fileSpec, ".gpx.zip");
		delete(Globals.getDataPath(), SenseGlobals.fileSpec, ".pw");
		delete(Globals.getDataPath(), SenseGlobals.fileSpec, ".smp");
		delete(Globals.getDataPath(), null, ".json");
		delete(Globals.getDataPath(), "srm", ".txt");
		delete(SenseGlobals.getScreenshotPath(), null, ".png");
		//delete(Globals.LOG_PATH, "log", ".txt");
	}

	private class Result {
		final int id;
		final String[] files;
		public Result(int id, String[] files){
			this.id = id;
			this.files = files;
		}
	}

	private class Input {
		final boolean closeActivity;
		final boolean closeApplication;
		final boolean zipIt;
		public Input(final boolean closeActivity, final boolean closeApplication, final boolean zipIt) {
			this.closeActivity = closeActivity;
			this.closeApplication = closeApplication;
			this.zipIt = zipIt;
		}
	}

	private class ZipFilesTask extends AsyncTask<Input, Void, Result> {		
		@Override
		protected Result doInBackground(Input... params) {
			int id = Watchdog.addProcessS(CLASSTAG + ".ZipFilesTask");
			//files.addAll(SenseApp.getFiles(sessionTime));
			if (params == null || params.length < 1) return null;
			return new Result(id, createZipFile(bits, params[0].closeActivity, params[0].closeApplication, params[0].zipIt));
		}

		protected void onPostExecute(Result result) {
			if (result.files != null) for (String file : result.files) {
				LLog.d(Globals.TAG, CLASSTAG + ".onPostExecute: file: " + file);
			} else
				LLog.d(Globals.TAG, CLASSTAG + ".onPostExecute: nothing in list");
			Watchdog.removeProcessS(result.id, CLASSTAG);
		}
	}

	public void zip(boolean closeActivity, boolean closeApplication, boolean zipIt) {
		String session = Value.getSessionString();
		if (!zipIt || needsToCreateZip(session, NEWZIP_INTERVAL)) {
			new ZipFilesTask().execute(new Input(closeActivity, closeApplication, zipIt));
		} else {
			if (closeApplication) stopInstances();
			if (closeActivity) doCloseActivity(zipIt);
		}
	}
}

