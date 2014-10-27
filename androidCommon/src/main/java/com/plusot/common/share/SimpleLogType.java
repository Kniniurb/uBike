package com.plusot.common.share;

public enum SimpleLogType {
	JSON(null, ",", null, "", ".json", true, true),
	TEXT(null, null, null, "", ".txt", true, false),
	CSV(null, null, null, "", ".csv", true, true),
	HRV(null, null, null, "", ".hrv", true, true),
	LOG(null, null, null, "", ".txt", true, true),
	ARGOS("Minutes,Torq (N-m),Km/h,Watts,Km,Cadence,Hrate,ID,Altitude (m)", null, null, ",", ".csv", true, true);

	private final String behindLines;
	private final String beforeLines;
	private final String betweenLines;
	private final String ext;
	public final String sep;
	private final boolean append;
	private final boolean writeOnSessionOnly;

	private SimpleLogType(final String beforeLines, final String betweenLines, final String behindLines, final String sep, final String ext, final boolean append, final boolean writeOnSessionOnly) {
		this.behindLines = behindLines;
		this.betweenLines = betweenLines;
		this.beforeLines = beforeLines;
		this.sep = sep;
		this.ext = ext;
		this.append = append;
		this.writeOnSessionOnly = writeOnSessionOnly;
	}

	public String getBehindLines() {
		return behindLines;
	}

	public String getBeforeLines() {
		return beforeLines;
	}

	public String getBetweenLines() {
		return betweenLines;
	}

	public String getExt() {
		return ext;
	}
	
	public boolean isAppend() {
		return append;
	}

	public boolean isWriteOnSessionOnly() {
		return writeOnSessionOnly;
	}


}
