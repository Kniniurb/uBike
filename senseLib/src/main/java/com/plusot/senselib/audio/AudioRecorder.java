package com.plusot.senselib.audio;

import java.io.File;
import java.io.IOException;

import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.SoundPool;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.javacommon.util.TimeUtil;
import com.plusot.senselib.SenseGlobals;

public class AudioRecorder implements SoundPool.OnLoadCompleteListener  {
	private static final String CLASSTAG = AudioRecorder.class.getSimpleName();
	public static final String EXT = ".3gp";
	public static final String SPEC = "dicta";


	private MediaRecorder recorder = new MediaRecorder();
	private String path = null;
	private SoundPool pool = null;
	int soundId = 0;


	/**
	 * Creates a new audio recording at the given path (relative to root of SD card).
	 */
	public AudioRecorder() {
	}


	/**
	 * Starts a new recording.
	 */
	public boolean start()  {
		path = SenseGlobals.getAudioPath() + SPEC + SenseGlobals.DELIM_DETAIL + TimeUtil.formatTime(System.currentTimeMillis(), "yyyyMMdd-HHmmss") + EXT;

		// make sure the directory we plan to store the recording in exists
		File directory = new File(path).getParentFile();
		if (!directory.exists() && !directory.mkdirs()) {
			LLog.e(Globals.TAG, CLASSTAG + ".start: Path to file could not be created.");
		}

		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT); ///.AMR_NB);
		recorder.setOutputFile(path);
		recorder.setAudioChannels(1);
		try {
			recorder.prepare();
			recorder.start();
			return true;
		} catch (IllegalStateException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".start: State not OK (" + e.getMessage() + ")");
		} catch (IOException e) {
			LLog.e(Globals.TAG, CLASSTAG + ".start: IOException (" + e.getMessage() + ")");
		}
		return false;

	}

	public void playBack() {
		if (path == null) return; 
		if (recorder != null) stop();

		if (pool == null) {
			pool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		} else {
			pool.unload(soundId);
		}
		soundId = pool.load(path, 1);
		pool.setOnLoadCompleteListener(this);
	}


	@Override
	public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
		LLog.d(Globals.TAG, CLASSTAG + ".onLoadComplete");
		soundPool.play(sampleId, 1.0f, 1.0f, 1, 0, 1.0f);
	}

	public void stop()  {
		if (recorder == null) return;
		recorder.stop();
		recorder.release();
		recorder = null;
	}

	public void close()  {
		stop();
		if (pool != null) {
			pool.release();
			pool = null;
		}
	}
}
