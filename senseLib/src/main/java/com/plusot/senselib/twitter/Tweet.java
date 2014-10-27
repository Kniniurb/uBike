package com.plusot.senselib.twitter;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import com.plusot.common.Globals;
import com.plusot.common.settings.PreferenceHelper;
import com.plusot.common.share.LLog;
import com.plusot.common.util.ToastHelper;
import com.plusot.senselib.R;

public class Tweet {
	private static String CLASSTAG = Tweet.class.getSimpleName();
	private static RequestToken requestToken = null;
	private static Twitter twitter = null;
	private static String msg = null;
	private final Activity activity;
	
	public Tweet(final Activity activity, final Intent intent) {
		this.activity = activity;
		if (intent == null) return;
		Uri uri = intent.getData();
		if (uri != null && uri.toString().startsWith(TwitterConst.CALLBACK_URL)) {
			String verifier = uri.getQueryParameter(TwitterConst.IEXTRA_OAUTH_VERIFIER);
			LLog.i(Globals.TAG, CLASSTAG + ".constructor: Verifier = " + verifier); 
			new OAuthTokenTask().execute(verifier);
		}	
	}
	
	private boolean isConnected() {
		String str = PreferenceHelper.get(TwitterConst.PREF_KEY_TOKEN, (String)null);
		if (str == null || str.equals("")) {
			LLog.d(Globals.TAG,  CLASSTAG + ".isConnected = false");
			return false;
		}
		LLog.d(Globals.TAG,  CLASSTAG + ".isConnected = true (" + str + ")");
		
		return true;
	}
	
	private void askOAuth() {
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		configurationBuilder.setOAuthConsumerKey(TwitterConst.CONSUMER_KEY);
		configurationBuilder.setOAuthConsumerSecret(TwitterConst.CONSUMER_SECRET);
		Configuration configuration = configurationBuilder.build();
		twitter = new TwitterFactory(configuration).getInstance();

		ToastHelper.showToastLong(Globals.appContext.getString(R.string.twitter_auth_request, Globals.TAG));
		new OAuthTask().execute();
	}
	
	//public Tweet
	private class OAuthTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPostExecute(Void result) {
			if (requestToken != null) activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
			super.onPostExecute(result);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			try {
				requestToken = twitter.getOAuthRequestToken(TwitterConst.CALLBACK_URL);
			} catch (Exception e) {
				LLog.e(Globals.TAG, CLASSTAG + " OAuthTask.doInBackground: Exception getting requestToken", e); 	
			}
			return null;
		}
	}
	
	private class OAuthTokenTask extends AsyncTask<String, Void, String> {

		@Override
		protected void onPostExecute(String result) {
			if (result == null) {
				if (msg != null) new SendTask().execute(msg);
				msg = null;
			} else
				Toast.makeText(Globals.appContext, result, Toast.LENGTH_LONG).show(); 
			super.onPostExecute(result);
		}

		@Override
		protected String doInBackground(String... param) {
			if (param == null || param.length < 1) return "No verifier for access Token";
			try { 
				AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, param[0]); 
				PreferenceHelper.set(TwitterConst.PREF_KEY_TOKEN, accessToken.getToken()); 
				PreferenceHelper.set(TwitterConst.PREF_KEY_SECRET, accessToken.getTokenSecret()); 
				return null;
			} catch (Exception e) { 
				LLog.e(Globals.TAG, CLASSTAG + "OAuthTokenTask.doInBackground: Exception getting accessToken " + param[0], e); 
				return Globals.appContext.getString(R.string.twitter_access_token_exception, e.getMessage()); 
			}
		}
	}
	
	private class SendTask extends AsyncTask<String, Void, String> {

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				Toast.makeText(Globals.appContext, result, Toast.LENGTH_LONG).show(); 
				LLog.d(Globals.TAG,  CLASSTAG + ".SendTask.onPostExecute: Result = " + result);
			}
			super.onPostExecute(result);
		}

		@Override
		protected String doInBackground(String... param) {
			if (param == null || param.length < 1) return null;
			try { 
				String token = PreferenceHelper.get(TwitterConst.PREF_KEY_TOKEN, ""); //"109288447-d3vvXzWGwKV4LoOl2vTVOxyV6CyStHfS2FZIslVc");
				String secret = PreferenceHelper.get(TwitterConst.PREF_KEY_SECRET, ""); // "OFAJrn0gcaRbkj48rHmkKo5BuBMrqXlJrwv5jwdwRE");
				LLog.d(Globals.TAG,  CLASSTAG + ".SendTask.doInBackground: Token = " + token + ", secret = " + secret);
				AccessToken a = new AccessToken(token,secret);
				Twitter twitter = new TwitterFactory().getInstance();
				twitter.setOAuthConsumer(TwitterConst.CONSUMER_KEY, TwitterConst.CONSUMER_SECRET);
				twitter.setOAuthAccessToken(a);
		        twitter.updateStatus(param[0]);
		        return Globals.appContext.getString(R.string.twitter_sent, param[0]);
			} catch (Exception e) { 
				LLog.e(Globals.TAG, CLASSTAG + ".SendTask.doInBackground: Exception sending tweet", e); 
				return Globals.appContext.getString(R.string.twitter_send_exception, e.getMessage()); 
			}
		}
	}
	
	public void send(final String msg) {
		Tweet.msg = msg;
		if (isConnected()) {
			new SendTask().execute(msg);
			Tweet.msg = null;
		} else {
			askOAuth();
		}
	}

}
