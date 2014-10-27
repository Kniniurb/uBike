package com.plusot.common.util;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import android.annotation.SuppressLint;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.javacommon.util.TimeUtil;

public class FileExplorer {
	private static final String CLASSTAG = FileExplorer.class.getSimpleName();
	
	public static final int FITLOG_TAG = 3;
	public static final int ACTIVITY_TAG = 1;


	private final String path;
	private final String spec;
	private final String ext;
	private final int tag;

	public FileExplorer(final String path, final String spec, final String ext, final int tag) {
		this.path = path;
		this.spec = spec;
		this.ext = ext;
		this.tag = tag;
	}

	public String[] getYears() {
		File folder = new File(path);
		String[] files = folder.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				if (filename.contains(spec + Globals.FILE_DETAIL_DELIM) && filename.contains(ext)) return true;
				return false;
			}
		});

		Set<String> years= new TreeSet<String>();
		if (files != null) for (String file : files) {
			file = file.replace(ext, "");
			String parts[] = file.split(Globals.FILE_DETAIL_DELIM);
			if (parts.length < 1) continue;
			years.add(parts[parts.length - 1].substring(0, 4));
		}
		return years.toArray(new String[0]);
	}

	public Integer[] getMonths(final String year) {
		File folder = new File(path);
		String[] files = folder.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				if (filename.contains(spec + Globals.FILE_DETAIL_DELIM + year) && filename.contains(ext)) return true;
				return false;
			}
		});

		Set<Integer> months= new TreeSet<Integer>();
		if (files != null) for (String file : files) {
			file = file.replace(ext, "");
			String parts[] = file.split(Globals.FILE_DETAIL_DELIM);
			if (parts.length < 1) continue;
			String intPart = null;
			try {
				intPart = parts[parts.length - 1].substring(4, 6);
				int month = Integer.valueOf(intPart);
				months.add(month);
			} catch (NumberFormatException e) {
				LLog.e(Globals.TAG, CLASSTAG + ".getMonths: Could not convert: " + intPart);
			}

		}
		return months.toArray(new Integer[0]);
	}

	public static String[] getMonths(final String year, final Integer[] months) {
		DateFormatSymbols sym = new DateFormatSymbols(Locale.getDefault());
		String[] monthNames = sym.getMonths();
		String[] monthStrs = new String[months.length];
		int i = 0;
		for (int month : months) {
			monthStrs[i++] = monthNames[(month - 1) % 12] + " " + year; //DateUtils.getMonthString(month - 1, DateUtils.LENGTH_LONG) + " " + year;
		}
		return monthStrs;
	}

	public Integer[] getDays(final String year, final int month) {
		File folder = new File(path);
		String[] files = folder.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				if (filename.contains(spec + Globals.FILE_DETAIL_DELIM + year + String.format("%02d", month)) && filename.contains(ext)) return true;
				return false;
			}
		});

		Set<Integer> days= new TreeSet<Integer>();
		if (files != null) for (String file : files) {
			file = file.replace(ext, "");
			String parts[] = file.split(Globals.FILE_DETAIL_DELIM);
			if (parts.length < 1) continue;
			String intPart = null;
			try {
				intPart = parts[parts.length - 1].substring(6, 8);
				int day = Integer.valueOf(intPart);
				days.add(day);
			} catch (NumberFormatException e) {
				LLog.d(Globals.TAG, CLASSTAG + ".getMonths: Could not convert: " + intPart);
			}
		}
		return days.toArray(new Integer[0]);
	}

	public String[] getTimes(final String year, final int month, final int day) {
		File folder = new File(path);
		String[] files = folder.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				if (filename.contains(spec + Globals.FILE_DETAIL_DELIM + year + String.format("%02d", month) + String.format("%02d", day)) && filename.contains(ext)) return true;
				return false;
			}
		});

		Set<String> times = new TreeSet<String>();
		for (String file : files) {
			file = file.replace(ext, "");
			String parts[] = file.split(Globals.FILE_DETAIL_DELIM);
			if (parts.length < 1) continue;
			String timePart = null;

			timePart = parts[parts.length - 1].substring(9, 15);
			timePart = timePart.substring(0, 2) + ":" + timePart.substring(2, 4) + ":" + timePart.substring(4, 6);
			times.add(timePart);

		}
		return times.toArray(new String[0]);
	}

	public static String[] getDays(final String year, final String month, final Integer[] days) {
		String[] dayStrs = new String[days.length];
		int i = 0;
		for (int day : days) {
			dayStrs[i++] = day + " " + month;
		}
		return dayStrs;
	}

	public String[] getFileList(final String dateSpec) {
		File folder = new File(path);

		String[] fileList = folder.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				if (dateSpec != null) {
					if (filename.contains(spec + Globals.FILE_DETAIL_DELIM + dateSpec) && filename.contains(ext)) return true;
				} else {
					if (filename.contains(spec + Globals.FILE_DETAIL_DELIM) && filename.contains(ext)) return true;	
				}
				return false;
			}
		});

		if (fileList == null || fileList.length == 0) return null;

		if (dateSpec == null || dateSpec.equals("")) {
			Arrays.sort(fileList, java.text.Collator.getInstance());

			final String maxString = fileList[fileList.length - 1].substring(spec.length() + 1, spec.length() + 9);
			fileList = folder.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					if (filename.contains(spec + Globals.FILE_DETAIL_DELIM + maxString) && filename.contains(ext)) return true;
					return false;
				}
			});
		}
		return fileList;
	}

	public String[] getFileList(long time) {
		return getFileList(TimeUtil.formatTime(time, "yyyyMMdd"));
	}

	public String[] getFileList(int year, int month, int day) {
		return getFileList(String.format("%04d%02d%02d", year, month, day));
	}

	public String getFileList(String year, int month, int day, String time) {
		String[] list = getFileList(year + String.format("%02d%02d", month, day) + "-" + time.substring(0,2) + time.substring(3,5) + time.substring(6,8));
		if (list == null || list.length == 0) return null;
		return list[0];
	}


	public String[] getFileList(String year, int month, int day) {
		return getFileList(year + String.format("%02d%02d", month, day));
	}

	public int getTag() {
		return tag;
	}
	
	public String[] getFileListWithPath(long date) {
		String[] list =  getFileList(TimeUtil.formatDateShort(date));
		if (list == null || list.length == 0) return null;
		String[] files = new String[list.length];
		int i = 0;
		for (String file : list) {
			files[i++] = path + file;
		}
		return files;
	}

	public static String[] getFileList(final String path, final String spec, final String[] exts, final boolean desc) {
		File folder = new File(path);

		String[] fileList = folder.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {

				for (String ext: exts) {
					if (filename.contains(spec) && filename.contains(ext)) return true;	
				}
				return false;
			}
		});

		if (fileList == null || fileList.length == 0) return null;

		Arrays.sort(fileList, java.text.Collator.getInstance());
		if (desc) {
			int l = fileList.length;
			for (int i = 0; i < l / 2; i++) {
				String temp = fileList[i];
				fileList[i] = fileList[l - i - 1];
				fileList[l - i- 1] = temp;
			}
		}
		return fileList;
	}

	public static class FileList {
		public final String[] items;
		public final Map<String, String> lookup;
		
		public FileList(final String[] items, final Map<String, String> lookup) {
			this.items = items;
			this.lookup = lookup;
		}
		
	}
	@SuppressLint("DefaultLocale")
	public static FileList getFileList(final String[] paths, final String[] exts, final boolean desc) {
		//List<String> list = new ArrayList<String>();
		Map<String, String> map = new HashMap<String, String>();
		for (String path: paths) {
			File folder = new File(path);

			String[] fileList = folder.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {

					for (String ext: exts) {
						if (filename.toLowerCase(Locale.US).contains(ext.toLowerCase(Locale.US))) return true;	
					}
					return false;
				}
			});
			if (fileList != null && fileList.length > 0) for (String item: fileList) {
				map.put(item, path + item);
				//list.addAll(Arrays.asList(fileList));
			}
		}
		String[] fileList = map.keySet().toArray(new String[0]); //list.toArray(new String[0]);
		
		if (fileList == null || fileList.length == 0) return null;

		Arrays.sort(fileList, java.text.Collator.getInstance());
		if (desc) {
			int l = fileList.length;
			for (int i = 0; i < l / 2; i++) {
				String temp = fileList[i];
				fileList[i] = fileList[l - i - 1];
				fileList[l - i- 1] = temp;
			}
		}
		return new FileList(fileList, map);
	}

}
