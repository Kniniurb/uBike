package com.plusot.senselib.mail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.ToastHelper;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.R;
import com.plusot.senselib.store.ZipFiles;

public class SendStravaMail {	
	private static final String CLASSTAG = SendStravaMail.class.getSimpleName();

	private final Context context;

	public SendStravaMail(final Context context) {
		this.context = context;
	}

	public enum MailResult {
		SUCCES,
		NOFILES,
		NOACCOUNT;
	}

	public MailResult send() {
		String[] files = null;
		files = ZipFiles.createFitFiles(); //, true));
		if (files == null || files.length == 0) {
			ToastHelper.showToastLong(R.string.no_mail_to_send);
			return MailResult.NOFILES;
		}
		for (String file : files) {
			LLog.d(Globals.TAG, CLASSTAG + ".listFiles: " + file);
		}

		AccountManager manager = AccountManager.get(context); 
		Account[] accounts = manager.getAccountsByType("com.google"); //manager.getAccounts(); 
		if (accounts == null || accounts.length == 0) {
			accounts = manager.getAccounts();
			if (accounts != null) {
				for (Account account : accounts) {
					LLog.d(Globals.TAG, CLASSTAG + " Account = " + account.name + " (" + account.type + ")" );
				}
			}
		}

		if (accounts != null && accounts.length > 0) {
			ToastHelper.showToastLong(context.getString(R.string.strava_account, accounts[0].name));

			mailMe(
					context, 
					"upload@strava.com", 
					accounts[0].name, 
					Globals.TAG + " " + TimeUtil.formatTime(System.currentTimeMillis()) + " Fit file", 
					"Fit file of " + TimeUtil.formatTime(System.currentTimeMillis()), 
					files);
			return MailResult.SUCCES;
		} else
			ToastHelper.showToastLong(R.string.no_mail_to_send);
		return MailResult.NOACCOUNT;
	}


	private void mailMe(Context context, String emailTo, String emailCC, String subject, String emailText, String[] filePaths) {
		//need to "send multiple" to get more than one attachment
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
		//emailIntent.addCategory(Intent.CA.CATEGORY_SELECTED_ALTERNATIVE);
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
		PackageManager packageManager = context.getPackageManager();
		List<ResolveInfo> activities = packageManager.queryIntentActivities(emailIntent, 0);
		boolean isIntentSafe = activities.size() > 0;
		for (ResolveInfo activity : activities) {
			LLog.d(Globals.TAG, CLASSTAG + " activity to handle mail: " + activity.toString());
		}

		// Start an activity if it's safe
		if (isIntentSafe) {
			Intent chooser = Intent.createChooser(emailIntent, context.getString(R.string.choose_mailer));
			context.startActivity(chooser); //Intent.createChooser(emailIntent, "Send mail..."));
		}
	}
}
