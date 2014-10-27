package com.plusot.senselib.mail;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.ToastHelper;
import com.plusot.common.util.Watchdog;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.R;
import com.plusot.senselib.http.HttpSender;
import com.plusot.senselib.settings.FileType;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.store.ZipFiles;
import com.plusot.senselib.util.SenseUserInfo;
import com.plusot.senselib.values.Value;

import java.io.File;
import java.util.ArrayList;

public class SendMail {	
	private static final String CLASSTAG = SendMail.class.getSimpleName();

	private final Context context;
	private Bitmapper bits = null;
	private final String summary;
	private final String session;
	private final boolean mayCreateZip;
	private final boolean force;
	private final Listener listener;

	public interface Listener {
		public void onMailComplete();
	}

	public SendMail(final Context context, final Bitmapable owner, final String summary, final String session, final boolean mayCreateZip, final Listener listener, final boolean force) {
		this.context = context;
		this.summary = summary;
		this.session = session;
		this.mayCreateZip = mayCreateZip;
		this.listener = listener;
		this.force = force;
		
		if (Value.fileTypes.contains(FileType.SCR)) {
			bits = new Bitmapper(context, owner);
			bits.make();
		}
		send();
	}

	private class Result {
		final int id;
		final String[] list;
		public Result(int id, String[] list){
			this.id = id;
			this.list = list;
		}
	}

	private class SendMailTask extends AsyncTask<Void, Void, Result> {		
		@Override
		protected Result doInBackground(Void... params) {
			int id = Watchdog.addProcessS(CLASSTAG + ".SendMailTask");
			String[] files = null;
			if (force) {
				files = ZipFiles.createZipFile(bits);
			} else {
				File zipped = ZipFiles.getZipFile(session);
				if (zipped != null) LLog.d(Globals.TAG, CLASSTAG + ".send zip file = " + zipped.getAbsolutePath());
				if (mayCreateZip && zipped != null && System.currentTimeMillis() - zipped.lastModified() < 300000) {
					files = new String[] {zipped.getAbsolutePath()};	
				} else if (mayCreateZip || zipped == null) {
					//bits.makeScreenshots();			
					//bits.makeBitmaps();
					//files.addAll(SenseApp.getFiles(sessionTime));
					files = ZipFiles.createZipFile(bits); //, true));
				} else
					files = new String[] {zipped.getAbsolutePath()};
			} 
			for (String file : files) {
				LLog.d(Globals.TAG, CLASSTAG + ".listFiles: " + file);
			}
			return new Result(id, files);
		}

		protected void onPostExecute(Result result) {
			AccountManager manager = AccountManager.get(context); 
			Account[] accounts = manager.getAccountsByType("com.google"); //manager.getAccounts(); 
			if (accounts == null || accounts.length == 0) {
				accounts = manager.getAccounts();
			}

			if (accounts != null && accounts.length > 0 && result.list != null && result.list.length > 0) {
				String link = PreferenceKey.SHARE_URL.getString() + "?device=" + SenseUserInfo.getDeviceId();
				int session = HttpSender.getSession();
				if (session >= 0) link += "&session=" + session;
				String content = context.getString(R.string.email_text, link);
//				if (SenseGlobals.isLite) content = context.getString(R.string.email_text_lite, link);
				mailMe(
						context, 
						accounts[0].name, 
						null, 
						Globals.TAG + "_" + TimeUtil.formatTime(System.currentTimeMillis()), 
						content + "\r\n\r\n" + 
								summary + "\r\n\r\n" + 
								context.getString(R.string.email_signature), 
								result.list);
			} else
				ToastHelper.showToastLong(R.string.no_mail_to_send);
			Watchdog.removeProcessS(result.id, CLASSTAG);
			listener.onMailComplete();
		}
	}

	private void mailMe(Context context, String emailTo, String emailCC, String subject, String emailText, String[] filePaths) {
		//need to "send multiple" to get more than one attachment
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
		emailIntent.setType("text/plain");
		//emailIntent.setType("application/zip"); message/rfc822
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, 
				new String[]{emailTo});
		if (emailCC != null)
			emailIntent.putExtra(android.content.Intent.EXTRA_CC, 
					new String[]{emailCC});
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
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
		context.startActivity(emailIntent); //Intent.createChooser(emailIntent, "Send mail..."));
	}

	private void send() {
		ToastHelper.showToastShort(R.string.mail_sending);
		new SendMailTask().execute();
	}
}
