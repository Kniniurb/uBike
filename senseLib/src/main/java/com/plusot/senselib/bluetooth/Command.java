package com.plusot.senselib.bluetooth;

import java.util.Date;

public class Command {
	private final String cmd;
	private final int interval;
	private final int startDelay;
	private long lastUsed = 0;
	private final long started = System.currentTimeMillis();

	public Command(final String cmd, final int startDelay, final int interval) {
		this.interval = interval;
		this.startDelay = startDelay;
		this.cmd = cmd;
	}
	
	public boolean maySend() {
		long now = new Date(). getTime();
		if (lastUsed == 0) {
			if (startDelay < 0)
				return false;
			else if (Math.abs(now - started) > startDelay) {
				lastUsed = now;
				return true;
			}
		} else if (interval <= 0 )
			return false;
		else if (Math.abs(now - lastUsed) > interval) {
			lastUsed = now;
			return true;
		}
		return false;
	}

	@Override 
	public String toString() {
		return cmd;
	}
}