package com.plusot.common.share;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import com.plusot.common.Globals;
import com.plusot.common.util.FileExplorer;
import com.plusot.common.util.FileUtil;
import com.plusot.javacommon.util.TimeUtil;

public class LogFiles {

	private static final String CLASSTAG = LogFiles.class.getSimpleName();
	private static final String ZIPEXT = ".gz";
	private static Set<String> files = new HashSet<String>();
	private static long filesMade = 0;
	public static final long NEWZIP_INTERVAL = 300000;
	//public static SparseArray<String> zippedFiles
	
	public static String timePart() {
		return TimeUtil.formatTime(System.currentTimeMillis(), "yyyyMMdd-HHmmss");
	}

	public static String systemLogToFile() {
		Process proc = null;
		try {
			//Util.showToastLong(context, R.string.saving);
			File file = new File(Globals.getLogPath()); 
			if (!file.isDirectory()) file.mkdirs();
			String fileName = Globals.getLogPath() + Globals.LOG_SPEC + Globals.FILE_DETAIL_DELIM + timePart() + Globals.LOG_EXT;
			file = new File(fileName); 
			file.createNewFile(); 
			String[] cmd = {"logcat", "-v", "time", "-d", Globals.TAG + ":D", "*:E"}; //, "-d"}; //, "-f " + file.getAbsolutePath()};
			proc = Runtime.getRuntime().exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader (proc.getInputStream()), 1024);
			String line;
			PrintWriter os = new PrintWriter(new BufferedWriter(new FileWriter(file)));

			while ((line = reader.readLine()) != null) {
				os.println(line);
				Thread.yield();	
			}
			os.close();
			reader.close();
			//ToastHelper.showToastLong(R.string.savelog_succes);
			return fileName;
		} catch (IOException e) {
			//Util.showToastLong(context, context.getString(R.string.savelog_fail) + ": " + e.getMessage());
		} finally {
//			if (proc != null){
//				try {
//					proc.destroy();
//				} catch (Exception e) {
//					LLog.e(Globals.TAG, CLASSTAG + ".systemLogToFile: Could not destroy process " + e.getMessage());
//				}
//				proc = null;
//
//			}
		}
		return null;
	}

	public static String[] createFileList(boolean zipIt) {
		String[] result = null;
		synchronized(files) {
			long now = System.currentTimeMillis();
			if (now - filesMade > NEWZIP_INTERVAL) {
				filesMade = System.currentTimeMillis();
				files.clear();
				LLog.d(Globals.TAG, CLASSTAG + ".createZipFile: Creating new file list");
				
				systemLogToFile();
				FileExplorer exp = new FileExplorer(Globals.getLogPath(), Globals.LOG_SPEC, Globals.LOG_EXT, 0);
				String[] logs = exp.getFileList(now); 
				if (logs != null) for (String file : logs) {
					files.add(Globals.getLogPath() + file);
				}
				
			}
			if (SimpleLog.hasInstances()) {
				files.addAll(SimpleLog.getFileNames(null));
				SimpleLog.closeInstances();
			}
			//			catch (InterruptedException e) {
			//			LLog.d(Globals.TAG, CLASSTAG + ".createZipFile interrupted");
			//		}
			result = files.toArray(new String[0]);
		} 
		if (zipIt) {
			String zipFile = getZipFileName(timePart(), false);		
			FileUtil.toZip(result, zipFile);
			LLog.d(Globals.TAG, CLASSTAG + ".zipFile: Zipped new files to " + zipFile);
			result = new String[]{ zipFile };
		}
		return result;	
	}
	
	private static String getZipFileName(String session, boolean check) {
		if (session == null) {
			LLog.d(Globals.TAG, CLASSTAG + ".getZipFileName: No Session time");
			return null;
		}
		String zipFile = Globals.getDataPath() + Globals.TAG + Globals.FILE_DETAIL_DELIM + "report" + Globals.FILE_DETAIL_DELIM + session + ZIPEXT;
		LLog.d(Globals.TAG, CLASSTAG + ".getZipFileName: " + zipFile);
		if (!check) return zipFile;
		if(new File(zipFile).exists()) return zipFile;
		return null;
	}
	
	public static File getZipFile(String session) {
		if (session == null) {
			LLog.d(Globals.TAG, CLASSTAG + ".getZipFileName: No Session time");
			return null;
		}
		String zipFile = Globals.getDataPath() + Globals.TAG + Globals.FILE_DETAIL_DELIM + "report" + Globals.FILE_DETAIL_DELIM + session + ZIPEXT;
		LLog.d(Globals.TAG, CLASSTAG + ".getZipFile: " + zipFile);
		File file = new File(zipFile);
		if(file.exists()) return file;
		return null;
	}


	
}

