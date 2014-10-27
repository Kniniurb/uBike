package com.plusot.senselib.twitter;

import android.app.Activity;
import android.os.Bundle;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;

public class TweetActivity extends Activity {
	private static String CLASSTAG = TweetActivity.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LLog.i(Globals.TAG, CLASSTAG + ".onCreate");
		new Tweet(this, getIntent());
		finish();
	}
}
