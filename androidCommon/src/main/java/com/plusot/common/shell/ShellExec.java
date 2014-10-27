package com.plusot.common.shell;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;

public class ShellExec {
	private static final String CLASSTAG = ShellExec.class.getSimpleName();
	//private final File fileBinDir;
	//private final Context context;

	public interface ShellCallback {
		public void shellOut (String shellLine);
		public void processComplete(int tag, String commandLine, int exitValue);
	}

	public ShellExec()  {
		//context = _context;
		//fileBinDir = context.getDir("bin",0);
	}

	public int exec(String[] cmds, int tag, ShellCallback sc) throws IOException, InterruptedException {		

		ProcessBuilder pb = new ProcessBuilder(cmds);
		//pb.directory(fileBinDir);

		StringBuffer cmdlog = new StringBuffer();

		for (String cmd : cmds) {
			if (cmdlog.length() > 0) cmdlog.append(' ');
			cmdlog.append(cmd);			
		}

		//LLog.v(CommonGlobals.TAG, CLASSTAG + " " + cmdlog.toString());

		//	pb.redirectErrorStream(true);
		Process process = pb.start();  
		
		// any error message?
		StreamGobbler errorGobbler = new 
				StreamGobbler(process.getErrorStream(), "ERROR", sc);            

		// any output?
		StreamGobbler outputGobbler = new 
				StreamGobbler(process.getInputStream(), "OUTPUT", sc);

		// kick them off
		errorGobbler.start();
		outputGobbler.start();

		int exitVal = process.waitFor();

		sc.processComplete(tag, cmdlog.toString(), exitVal);

		return exitVal;
	}

	class StreamGobbler extends Thread {
		InputStream is;
		String type;
		ShellCallback sc;

		StreamGobbler(InputStream is, String type, ShellCallback sc) {
			this.is = is;
			this.type = type;
			this.sc = sc;
			setName("StreamGobler" + getId());
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line=null;
				while ( (line = br.readLine()) != null)
					if (sc != null)
						sc.shellOut(line);

			} catch (IOException ioe) {
				LLog.e(Globals.TAG, CLASSTAG + " error reading shell slog",ioe);
			}
		}
	}
}
