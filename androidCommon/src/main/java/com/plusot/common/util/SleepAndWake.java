package com.plusot.common.util;

import android.os.Handler;
import android.os.Looper;

public class SleepAndWake {
	private Object object = null;

	public interface Listener {
		public void onWake();
	};

	public void release() {
		object = null;
	}

	public SleepAndWake(final Listener listener, final long delayInMillis) {

		final Handler handler = new Handler(Looper.getMainLooper());
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				listener.onWake();
			}
		}, delayInMillis);
	}

	public SleepAndWake(final Object object, final Listener listener, final long delayInMillis) {
		this.object = object;

		final Handler handler = new Handler(Looper.getMainLooper());
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				listener.onWake();
			}
		}, delayInMillis);
	}

	public SleepAndWake(final Runnable runnable, final long delayInMillis) {
		new Handler(Looper.getMainLooper()).postDelayed(runnable, delayInMillis);
	}

	public SleepAndWake(final Runnable runnable) {
		new Handler(Looper.getMainLooper()).post(runnable);
	}

	public Object getObject() {
		return object;
	}

}
