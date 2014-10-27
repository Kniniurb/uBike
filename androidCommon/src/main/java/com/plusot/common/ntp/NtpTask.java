package com.plusot.common.ntp;

import java.io.IOException;

import android.os.AsyncTask;

import com.plusot.common.Globals;
import com.plusot.common.ntp.NtpLookup.NtpResult;
import com.plusot.common.share.LLog;

public class NtpTask {
	private static final String CLASSTAG = NtpTask.class.getSimpleName();
	
	
	public interface Listener {
		void onInfo(String string);
		void onClockOffset(double clockOffset);
		void onFail(String error);
	}
	
	public static void lookup(final Listener listener) {
		new NtpTask().doLookup(listener);
	}
	
	private void doLookup(final Listener listener) {
		new NtpTaskWorker().execute(new Input(listener));
	}

	private class Input {
		Listener listener;
		public Input(final Listener listener) {
			this.listener = listener;
		}
	}

	private class Result {
		final NtpResult ntpResult;
		final Listener listener;
		final String error;
		
		public Result(final Listener listener, final NtpResult ntpResult, final String error) {
			this.listener = listener;
			this.ntpResult = ntpResult;
			this.error = error;
		}

	}

	private class NtpTaskWorker extends AsyncTask<Input, Void, Result> {		
		@Override
		protected Result doInBackground(Input... params) {
			if (params == null || params.length < 1) return null;
			NtpResult ntpResult;
			try {
				ntpResult = NtpLookup.getInfo();
			} catch (IOException e) {
				LLog.e(Globals.TAG, CLASSTAG + ".NtpTaskWorker.doInBackground: Exception in NtpLookup.getInfo()", e);
				return new Result(params[0].listener, null, e.getMessage());
			}
			return new Result(params[0].listener, ntpResult, null);
		}

		protected void onPostExecute(Result result) {
			if (result != null && result.listener != null) {
				if (result.ntpResult != null) {
					result.listener.onClockOffset(result.ntpResult.clockOffset);
					result.listener.onInfo(result.ntpResult.info);		
				} else 
					result.listener.onFail(result.error);
			}
		}
	}

}
