package com.plusot.senselib.util;

import java.io.IOException;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.shell.ShellExec;



public class BatteryInfo extends Thread implements ShellExec.ShellCallback {
	private static final String CLASSTAG = BatteryInfo.class.getSimpleName();
	private ShellExec exec;
	
	public BatteryInfo () {
		
		start();
	}
	
	@Override
	public void run() {
		exec = new ShellExec(); //Globals.appContext);
		try {
			exec.exec(new String[] {"dumpsys", "batteryinfo"}, -1, this);
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".constructor: IOExeption", e);
		} catch (InterruptedException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".constructor: InterruptedException", e);
		}
	}

	@Override
	public void shellOut(String shellLine) {
		LLog.d(Globals.TAG, CLASSTAG + ".shellOut: " + shellLine);	
	}

	@Override
	public void processComplete(int tag, String commandLine, int exitValue) {
		LLog.d(Globals.TAG, CLASSTAG + ".processComplete for: " + commandLine + " with exit value " + exitValue);
	}

}
