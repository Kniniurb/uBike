package com.plusot.senselib;

import java.util.Locale;
import java.util.Set;

import android.app.Activity;
import android.app.Application;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.share.LogMail;
import com.plusot.common.share.SimpleLog;
import com.plusot.common.util.ElapsedTime;
import com.plusot.common.util.Watchdog;
import com.plusot.senselib.http.HttpSender;
import com.plusot.senselib.settings.PreferenceKey;
import com.plusot.senselib.store.DataLog;
import com.plusot.senselib.store.FitLog;
import com.plusot.senselib.store.GpxLog;
import com.plusot.senselib.store.PwxLog;
import com.plusot.senselib.store.SRMLog;
import com.plusot.senselib.values.DeviceType;
import com.plusot.senselib.values.Value;
import com.plusot.senselib.values.ValueType;
import com.plusot.senselib.widget.BaseGraph;

public class SenseApp extends Application implements Thread.UncaughtExceptionHandler, Value.ValueListener, TextToSpeech.OnInitListener, LogMail.Listener {
	private static final String CLASSTAG = SenseApp.class.getSimpleName();
	public static boolean firstActivity = true;
	private TextToSpeech tts;
	private Set<ValueType> voiceValues = null;

	@Override
	public void onCreate() {
		super.onCreate(); 
		SenseGlobals.ln = System.getProperty("line.separator"); 
		Thread.setDefaultUncaughtExceptionHandler(this);
		Globals.init(this.getApplicationContext(), this);
		BaseGraph.labels = DeviceType.getLabels(getApplicationContext());
		SenseGlobals.mainClass = SenseMain.class;
		tts = new TextToSpeech(Globals.appContext, this);
		voiceValues = PreferenceKey.getVoiceValues();

		ElapsedTime.setTimePartAsker(new ElapsedTime.TimePartAsker() {

			@Override
			public String getDay() {
				return Globals.appContext.getString(R.string.day);
			}

			@Override
			public String getSeconds() {
				return Globals.appContext.getString(R.string.seconds);
			}

			@Override
			public String getHours() {
				return Globals.appContext.getString(R.string.hours);
			}

			@Override
			public String getMinutes() {
				return Globals.appContext.getString(R.string.minutes);
			}

		});
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	public void activityCreated(Activity activity) {
		if (Globals.runMode.isFinished() && firstActivity == false) {
			LLog.d(Globals.TAG, CLASSTAG + ".activityCreate: Re-creating application while parts still running !!!!!!!!");
			firstActivity = true;
		}
		Globals.runMode = Globals.RunMode.RUN;
		if (tts == null) tts = new TextToSpeech(Globals.appContext, this);
		Value.addGlobalListener(this);
	}

	public void finish(String caller) {
		if (!Globals.runMode.isRun()) return;
		Globals.runMode = Globals.RunMode.FINISHING;
		LLog.d(Globals.TAG, CLASSTAG + ".finish called by " + caller); 
		Value.saveTotals();
		Value.stopInstance();
		HttpSender.stopInstance();
		//DbData.stopInstance();

		Watchdog.addBeforeKillListener(new Watchdog.BeforeKill() {

			@Override
			public void onBeforeKill() {
				DataLog.stopInstance();
				FitLog.stopInstance();
				SRMLog.stopInstance(CLASSTAG + "->WatchDog");
				GpxLog.stopInstance(CLASSTAG + "->WatchDog", true);
				PwxLog.stopInstance(CLASSTAG + "->WatchDog", true);
				SimpleLog.stopInstances();	

			}
		});
		//		AppPreferences.stopInstance();
		if (tts != null) {
			tts.stop();
			tts.shutdown();
			tts = null;
		}

		LLog.d(Globals.TAG, CLASSTAG + ".finish closing Watchdog"); 	
		Watchdog.stopInstance();
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		LLog.e(Globals.TAG, "uncaughtException", ex);
		//new LogMail(null, "Exception in " + Globals.TAG + ": " + ex.getMessage(), "Sending", "No mail to send", this);
	}

	@Override
	public void onValueChanged(Value value, boolean isLive) {
		if (!isLive) return;
		if (!SenseGlobals.activity.equals(SenseGlobals.ActivityMode.RUN)) {
			//			LLog.d(Globals.TAG, CLASSTAG + ".onValueChanged: Stop");
			return;
		}
		if (!voiceValues.contains(value.getValueType())) {
			//			LLog.d(Globals.TAG, CLASSTAG + ".onValueChanged: " + value.getValueType() + " Not in container");
			return;
		}
		if (!value.hasRegistrations()) {
			//			LLog.d(Globals.TAG, CLASSTAG + ".onValueChanged: No registrations");
			return;
		}
		//		LLog.d(Globals.TAG, CLASSTAG + ".onValueChanged: " + value.getValueType());
		if (value.isChangeToNotify(PreferenceKey.VOICEINTERVAL.getInt())) {
			String unit = value.getUnitSpeechLabel(Globals.appContext);
			if (unit.length() > 0) unit = " " + unit;
			//			LLog.d(Globals.TAG, CLASSTAG + ".onValueChanged: Change to notify: " + 
			//					value.getValueType(Globals.appContext) + " " + value.speak() + unit);
			say(value.getValueType(Globals.appContext));
			say(value.speak() + unit); 
		}
	}

	public void say(String text) {
		if (!Globals.runMode.isRun()) {
			LLog.d(Globals.TAG, CLASSTAG + ".say: not in run mode");
			return;
		}
		if (!PreferenceKey.VOICEON.isTrue())  {
			//LLog.d(Globals.TAG, CLASSTAG + ".say: voice = off");
			return;
		}
		if (tts == null) {
			LLog.d(Globals.TAG, CLASSTAG + ".say: tts == null");
			tts = new TextToSpeech(Globals.appContext, this);
		} else {	
			//			LLog.d(Globals.TAG, CLASSTAG + ".say: " + text);

			tts.speak(text, TextToSpeech.QUEUE_ADD, null);
		}
		//LLog.d(Globals.TAG, CLASSTAG + ".say: " + text);

	}

	public void addVoiceValues(ValueType valueType) {
		voiceValues.add(valueType);
		PreferenceKey.setVoiceValues(voiceValues);
	}

	public void removeVoiceValues(ValueType valueType) {
		voiceValues.remove(valueType);
		PreferenceKey.setVoiceValues(voiceValues);
	}

	@Override
	public void onInit(int status) {
		Locale speechLocale = null;

		if (status == TextToSpeech.SUCCESS && tts != null) {
			int result = tts.isLanguageAvailable(Locale.getDefault());
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				speechLocale = tts.getLanguage();
			} else {
				result = tts.setLanguage(Locale.getDefault());
				speechLocale = Locale.getDefault();
				if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
					LLog.e(Globals.TAG, CLASSTAG + ".onInit: Requested language is not available.");
					//Toast.makeText(Globals.appContext, "Requested language not available", Toast.LENGTH_SHORT).show();
				} 
			}
			if (speechLocale != null) LLog.d(Globals.TAG, CLASSTAG + ".onInit: Current speech locale: " + speechLocale.getDisplayCountry() + ", " + speechLocale);				
		} else {
			// Initialization failed.
			LLog.e(Globals.TAG, CLASSTAG + ".onInit: Could not initialize TextToSpeech.");
			Toast.makeText(Globals.appContext, "Could not initialize TextToSpeech.", Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	public void onMailComplete() {
		Watchdog.killIt();
	}

}
