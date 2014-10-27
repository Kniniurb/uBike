package com.plusot.common.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.shell.ShellExec;

public class Watchdog extends Thread {
	private static final String CLASSTAG = Watchdog.class.getSimpleName();
	private static final long FORCE_STOP = 60000;
	private static final long NOPROCESS_STOP = 2000;
	private static final Vector<Listener> listeners = new Vector<Listener>();
	private static final Vector<TopListener> topListeners = new Vector<TopListener>();
	private static Watchdog instance = null;
	private boolean mayRun = true;
	private boolean stopInProcess = false;
	private static long iCount = 0;
	private SparseArray<String> processes = new SparseArray<String>();
	private int idSeed = 0;
	private long stopTime = 0;
	private BeforeKill beforeKill = null;
	public static boolean killIt = true;

	private class Listener {
		private long interval = 10; 
		private final Watchable watchable;

		private Listener(final Watchable watchable, final long interval) {
			this.watchable = watchable;
			this.interval = interval;
		}
	}

	public interface Watchable {
		public void onWatchdogCheck(long count);
		public void onWatchdogClose();
	}

	public interface TopListener {
		public void onTopUpdate(ProcessInfo info);
	}

	public interface BeforeKill {
		public void onBeforeKill() ;
	}

	private Watchdog() {
		this.setName("Watchdog_" + getId());
		//mayRun = true;
		start();
	}

	public static Watchdog getInstance() {
		if (instance != null) return instance;
		if (!Globals.runMode.isRun()) return null;
		synchronized(Watchdog.class) {
			if (instance == null) instance = new Watchdog();
		}
		return instance;
	}

	public static void stopInstance() {
		if (instance != null) synchronized(Watchdog.class) {
			if (instance != null) {
				Globals.runMode = Globals.RunMode.FINISHED;
				if (!instance.stopInProcess) instance.goStopping();
				//instance.finish();
			}
			//instance = null;
		}
	}

	public void add(Watchable watchable, long intervalInMilliSeconds) {
		synchronized(listeners) {
			for (Listener listener: listeners) if (listener.watchable == watchable) {
				listener.interval = intervalInMilliSeconds / 100;
				return;
			}
			listeners.add(new Listener(watchable, intervalInMilliSeconds / 100));	
		}
	}

	public void add(TopListener listener) {
		synchronized(topListeners) {	
			topListeners.add(listener);	
		}
	}

	public void remove(TopListener listener) {
		synchronized(topListeners) {	
			topListeners.remove(listener);	
		}
	}

	public void set(Watchable watchable, long intervalInMilliSecond) {
		synchronized(listeners) {
			for (Listener listener: listeners) if (listener.watchable == watchable) {
				listener.interval = intervalInMilliSecond / 100;
				return;
			}
			listeners.add(new Listener(watchable, intervalInMilliSecond / 100));	
		}
	}

	public void remove(Watchable watchable) {
		synchronized(listeners) {
			Listener removable = null;
			for (Listener listener: listeners) {
				if (listener.watchable == watchable) {
					removable = listener;
					break;
				}
			}
			if (removable != null) listeners.remove(removable);
		}
	}

	private static Handler watchHandler = new Handler() {

		public void handleMessage (Message msg) {
			switch (msg.what) {
			case 1:
				synchronized(topListeners) {
					for (TopListener listener : topListeners) {
						listener.onTopUpdate(processInfo);
					}
				}
				break;
			default:
				iCount ++;
				synchronized(listeners) {
					for (Listener listener : listeners) {
						if (iCount % listener.interval == 0) listener.watchable.onWatchdogCheck(iCount);
					}
				}
				break;
			}
		}
	};

	private long topChecked = 0;

	public static class ProcessInfo {
		public int idle;
		public int cpu;
		public int total;
		public int threads;
		public String pid;
		public String vss;
		public String rss;
	}

	private static ProcessInfo processInfo = new ProcessInfo();

	private void checkTop() {
		if (System.currentTimeMillis() - topChecked < 10000) return;
		topChecked = System.currentTimeMillis();
		List<String> cmds = new ArrayList<String>();
		cmds.add("top");
		cmds.add("-d");
		cmds.add("1");
		cmds.add("-n");
		cmds.add("1");
		cmds.add("-m");
		cmds.add("8");
		//		cmds.add("-C");
		try {
			new ShellExec().exec(cmds.toArray(new String[0]), -1, new ShellExec.ShellCallback() {

				@Override
				public void shellOut(String shellLine) {
					//					if (shellLine.length() > 2) LLog.d(Globals.TAG, CLASSTAG + " " + shellLine + " for " + Globals.appContext.getPackageName());	
					if (shellLine.contains(Globals.appContext.getPackageName())) {
						String[] parts = shellLine.split(" ");
						int i = 0;
						for (String part : parts) if (part.length() > 0) {
							switch (i) {
							case 0: processInfo.pid = part; break;
							case 2: 
								if (part.endsWith("%")) part = part.substring(0, part.length() - 1);
								try {
									processInfo.cpu = Integer.parseInt(part);
								} catch (NumberFormatException e ) {	
								}

								break;
							case 4: 
								try {
									processInfo.threads = Integer.parseInt(part);
								} catch (NumberFormatException e ) {	
								}
								break;
							case 5: processInfo.vss = part; break;
							case 6: processInfo.rss = part; break;
							}
							i++;
						}
					} else if (shellLine.contains("Idle") && shellLine.startsWith("User")) {
						String[] parts = shellLine.split(" ");
						int idle = Integer.MIN_VALUE;
						for (int i = 0; i < parts.length; i++) if (parts[i].length() > 0) {
							if (parts[i].equals("Idle")) try {
								idle = Integer.parseInt(parts[i + 1]);
							} catch (NumberFormatException e ) {

							} else if (parts[i].equals("=")) try {
								processInfo.total = Integer.parseInt(parts[i + 1]);
								if (idle != Integer.MIN_VALUE) processInfo.idle = 100 * idle / processInfo.total;
							} catch (NumberFormatException e ) {

							}
						}

					}

				}

				@Override
				public void processComplete(int tag, String commandLine,
						int exitValue) {
					//					LLog.d(Globals.TAG, CLASSTAG + " " + commandLine + " completed with " + exitValue);	
					if (Globals.testing.isVerbose()) LLog.d(Globals.TAG, CLASSTAG + " Idle = " + processInfo.idle + "% App = " + processInfo.cpu + "% VSS = " + processInfo.vss + " RSS = " + processInfo.rss + " Threads = " + processInfo.threads);
					watchHandler.sendEmptyMessage(1);


				}
			});
		} catch (IOException e) {
			LLog.d(Globals.TAG, CLASSTAG + " IOException in calling 'top'",e);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void run() {
		while (mayRun) {
			try {
				sleep(100);
				if (stopInProcess) 
					checkStop();
				else {
					watchHandler.sendEmptyMessage(0);
					if (Globals.testing.isTest()) checkTop();
				}
			} catch (InterruptedException e) {
			}
		}
		instance = null;

		int pid = android.os.Process.myPid();
		if (Globals.runMode == Globals.RunMode.RUN) {
			LLog.d(Globals.TAG, CLASSTAG + ".run: Watchdog stopped for pid = " + pid + ", but process not killed, as it seems to be restarted");
		} else if (killIt) {
			LLog.d(Globals.TAG, CLASSTAG + ".run: kill pid = " + pid);
			Globals.runMode = Globals.RunMode.FINISHED;
			android.os.Process.killProcess(pid);
		} else {
			LLog.d(Globals.TAG, CLASSTAG + ".run: stopped, not killing process");
			Globals.runMode = Globals.RunMode.FINISHED;
		}
	}

	public static void killIt() {
		int pid = android.os.Process.myPid();
		android.os.Process.killProcess(pid);
	}

	private void finish() {
		LLog.d(Globals.TAG, CLASSTAG + ".finish called");
		synchronized(listeners) {
			listeners.clear();
		}
		synchronized(topListeners) {
			topListeners.clear();
		}
		mayRun = false;
		interrupt();
	}

	private void checkStop() {	
		if (Globals.runMode == Globals.RunMode.RUN) {
			stopInProcess = false;
			return;
		}
		synchronized(listeners) {
			if (listeners.size() > 0) for (Listener listener: listeners) listener.watchable.onWatchdogClose();
			listeners.clear();
		}
		if (processes.size() == 0) {
			if (beforeKill != null) {
				stopTime = System.currentTimeMillis();
				LLog.d(Globals.TAG, CLASSTAG + ".checkStop: Before kill executing");
				beforeKill.onBeforeKill();
				beforeKill = null;
			}
			if (System.currentTimeMillis() - stopTime > NOPROCESS_STOP) {
				finish();
			}
		} else if (System.currentTimeMillis() - stopTime > FORCE_STOP) {
			for (int i = 0; i <  processes.size(); i++)  {
				LLog.d(Globals.TAG, CLASSTAG + ".checkStop: process " + i +  " = " + processes.valueAt(i) + " still running.");
			}
			LLog.d(Globals.TAG, CLASSTAG + ".checkStop: Forcing application kill");
			finish();
		}
	}

	private int addProcess(String processName) {
		synchronized(this) {
			LLog.d(Globals.TAG, CLASSTAG + ".addProcess: process " + idSeed +  " = " + processName + "." + idSeed);
			processes.append(idSeed, processName + "." + idSeed);
			return idSeed++;
		}
	}

	private void goStopping() {
		stopInProcess = true;
		stopTime = System.currentTimeMillis();
		for (int i = 0; i <  processes.size(); i++)  {
			LLog.d(Globals.TAG, CLASSTAG + ".goStopping: process " + i +  " = " + processes.valueAt(i));
		}
	}

	public static int addProcessS(String processName) {
		if (getInstance() == null) return 0;
		return instance.addProcess(processName + "." + Thread.currentThread().getName() +  "." + Thread.currentThread().getId());
	}

	private void removeProcess(int id, final String caller) {
		synchronized(this) {
			LLog.d(Globals.TAG, CLASSTAG + ".removeProcess: process " + id +  " = " + processes.get(id) + " by " + caller);

			processes.remove(id);
		}
	}

	public static void removeProcessS(int id, final String caller) {
		if (getInstance() == null) return;
		instance.removeProcess(id, caller);
	}

	public static void addBeforeKillListener(BeforeKill beforeKill) {
		if (getInstance() == null) return;
		instance.beforeKill = beforeKill;
	}


}
