package com.plusot.common.share;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.plusot.common.Globals;
import com.plusot.common.util.ToastHelper;
import com.plusot.common.util.Watchdog;
import com.plusot.javacommon.util.TimeUtil;

import java.io.File;
import java.util.ArrayList;

public class LogMail {	
	private static final String CLASSTAG = LogMail.class.getSimpleName();
	private static final String PLUSOTMAIL = "report@plusot.com";

	private final Listener listener;
	private final String mailContents;
	private final String sending;
	private final String no_mail;
	private final Context context;
	private final String address;

	public interface Listener {
		public void onMailComplete();
	}

	public LogMail(final Context context, final String mailContents, final String sending, final String no_mail, final Listener listener) {
		this.listener = listener;
		this.context = context;
		this.mailContents = mailContents;
		this.sending = sending;
		this.no_mail = no_mail;
		this.address = PLUSOTMAIL;

		send();
	}
	
	
	public LogMail(final Context context, final String address, final String mailContents, final String sending, final String no_mail, final Listener listener) {
		this.listener = listener;
		this.context = context;
		this.mailContents = mailContents;
		this.sending = sending;
		this.no_mail = no_mail;
		this.address = address;

		send();
	}

	private class Result {
        final boolean succes;
		public Result(boolean succes){
            this.succes = succes;
		}
	}

	private class SendMailTask extends AsyncTask<Void, Void, Result> {		
		@Override
		protected Result doInBackground(Void... params) {
			int id = Watchdog.addProcessS(CLASSTAG + ".SendMailTask");
			String[] files = LogFiles.createFileList(true);
			for (String file : files) {
				LLog.d(Globals.TAG, CLASSTAG + ".listFiles: " + file);
			}
			AccountManager manager = AccountManager.get(Globals.appContext); 
			Account[] accounts = manager.getAccountsByType("com.google"); //manager.getAccounts(); 

            boolean succes = true;
			if (accounts.length > 0) {
				mailMe(
						new String[]{address}, 
						accounts[0].name, 
						Globals.TAG + "_" + TimeUtil.formatTime(System.currentTimeMillis()), 
						mailContents,
						files);
			} else
                succes = false;

			Watchdog.removeProcessS(id, CLASSTAG);
			if (context == null && listener != null) listener.onMailComplete();
			return new Result(succes);
		}

		protected void onPostExecute(Result result) {

            if (!result.succes && context != null) ToastHelper.showToastLong(no_mail);
            if (listener != null && context != null) listener.onMailComplete();
		}
	}

	private void mailMe(String[] emailTo, String emailCC, String subject, String emailText, String[] filePaths) {
		//need to "send multiple" to get more than one attachment
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
		emailIntent.setType("text/plain");
		//emailIntent.setType("application/zip"); message/rfc822
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, emailTo);
		if (emailCC != null) emailIntent.putExtra(android.content.Intent.EXTRA_CC, new String[]{emailCC});
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		//		ArrayList<String> list = new ArrayList<String>();
		//		list.add(emailText);
		//		Log.i(Globals.TAG, CLASSTAG + " Email text = " + emailText);
		//		emailIntent.putStringArrayListExtra(android.content.Intent.EXTRA_TEXT, list); // emailText);
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, emailText); 
		//has to be an ArrayList
		ArrayList<Uri> uris = new ArrayList<Uri>();
		//convert from paths to Android friendly Parcelable Uri's
		for (String file : filePaths) if (file != null) {
			File fileIn = new File(file);
			Uri u = Uri.fromFile(fileIn);
			uris.add(u);
		}
		emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		try {
			if (context == null) {

				emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				Globals.appContext.startActivity(emailIntent); //Intent.createChooser(emailIntent, "Send mail..."));
			} else
				context.startActivity(emailIntent); 
		} catch (Exception e) {
			Log.e(Globals.TAG, CLASSTAG + " Could not send email ", e);
		}
	}

	private void send() {
		if (context != null) ToastHelper.showToastShort(sending);
		new SendMailTask().execute();
	}
}
