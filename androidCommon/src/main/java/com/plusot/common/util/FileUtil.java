package com.plusot.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;

public class FileUtil {
	private static final String CLASSTAG = FileUtil.class.getSimpleName();
	private static final int BUFFER = 2048; 


	public static boolean concat(String start, String end, File infile, File outfile) {

		FileChannel inChan = null, outChan = null;
		PrintWriter fw = null;
		try {
			fw = new PrintWriter(new BufferedWriter(new FileWriter(outfile, false)));
			fw.write(start);
			fw.flush();
		} catch (FileNotFoundException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".concatFiles, start: File Not Found", e);
			return false;
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".concatFiles, start: IO Exception", e);
			return false;
		} finally {
			if (fw != null) fw.close();
		}

		FileOutputStream outStream = null;
		RandomAccessFile inStream = null;
		try {
			// create the channels appropriate for appending:
			outStream = new FileOutputStream(outfile, true);
			outChan = outStream.getChannel();
			inStream = new RandomAccessFile(infile, "r");
			inChan = inStream.getChannel();

			long startSize = outfile.length();
			long inFileSize = infile.length();
			long bytesWritten = 0;

			//set the position where we should start appending the data:
			outChan.position(startSize);
			long startByte = outChan.position();

			while(bytesWritten < inFileSize){ 
				bytesWritten += outChan.transferFrom(inChan, startByte, inFileSize);
				startByte = bytesWritten + 1;
			}


		}  catch (FileNotFoundException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".concatFiles, work: File Not Found", e);
			return false;
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".concatFiles, work: IO Exception", e);
			return false;
		} finally {
			try {
				if (inChan != null) inStream.close();
				if (outChan != null) outStream.close();
			} catch (IOException e) {

			}
		}
		try {
			fw = new PrintWriter(new BufferedWriter(new FileWriter(outfile, true)));
			fw.write(end);
			fw.flush();
			fw.close();
			return true;
		} catch (FileNotFoundException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".concatFiles, end: File Not Found", e);
			return false;
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".concatFiles, end: IO Exception", e);
			return false;
		} finally {
			if (fw != null) fw.close();
		}
	}

	public static boolean toGZip(String start, String end, File infile, File outfile) {
		GZIPOutputStream outStream = null;
		RandomAccessFile inStream = null;
		if (!infile.exists()) return false;
		try {
			// create the channels appropriate for appending:
			outStream = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(outfile)));
			if (start != null) outStream.write(start.getBytes());
			inStream = new RandomAccessFile(infile, "r");

			byte[] bytes = new byte[BUFFER];
			int bytesRead;

			while((bytesRead = inStream.read(bytes)) != -1) {
				outStream.write(bytes, 0, bytesRead);
			}
			if (end != null) outStream.write(end.getBytes());
		}  catch (FileNotFoundException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".concatFilesToGZip, work: File Not Found", e);
			return false;
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".concatFilesToGZip, work: IO Exception", e);
			return false;
		} finally {
			try {
				if (inStream != null) inStream.close();
				if (outStream != null) outStream.close();
			} catch (IOException e) {
				LLog.e(Globals.TAG, CLASSTAG + ".concatFilesToGZip, work: IO Exception while closing files", e);
				return false;
			}
		}
		return true;
	}

//	public static boolean toGZip(File infile, File outfile) {
//		return toGZip(null, null, infile, outfile);
//	}


	public static void toZip(String[] files, String zipFile) { 
		try  { 
			BufferedInputStream origin = null; 
			FileOutputStream dest = new FileOutputStream(zipFile); 

			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest)); 

			byte data[] = new byte[BUFFER]; 

			for(String file : files) { 
				LLog.v(Globals.TAG, CLASSTAG + ".toZip adding: " + file); 
				FileInputStream fi = new FileInputStream(file); 
				origin = new BufferedInputStream(fi, BUFFER); 
				ZipEntry entry = new ZipEntry(file.substring(file.lastIndexOf("/") + 1)); 
				out.putNextEntry(entry); 
				int count; 
				while ((count = origin.read(data, 0, BUFFER)) != -1) { 
					out.write(data, 0, count); 
					Thread.yield();
				} 
				origin.close(); 
				Thread.sleep(5);
			} 

			out.close(); 
		} catch(Exception e) { 
			LLog.e(Globals.TAG, CLASSTAG + ".toZip exception: " + e.getMessage());
		} 
	}  
}