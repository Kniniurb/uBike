package com.plusot.senselib.ant;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;

public class AntPlusService extends Service {
	private static final String CLASSTAG = AntPlusService.class.getSimpleName();
	private static final boolean DEBUG = false;

	public class LocalBinder extends Binder {
		public AntPlusManager getManager() {
			return mManager;
		}
	}

	private final LocalBinder mBinder = new LocalBinder();

	private AntPlusManager mManager;

	public static final int NOTIFICATION_ID = 1;

	@Override
	public IBinder onBind(Intent intent)
	{
		LLog.i(Globals.TAG, CLASSTAG + " First Client bound.");
		return mBinder;
	}

	@Override
	public void onRebind(Intent intent)
	{
		LLog.i(Globals.TAG, CLASSTAG + " Client rebound");
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		LLog.i(Globals.TAG, CLASSTAG + " All clients unbound.");
		// TODO Auto-generated method stub
		return super.onUnbind(intent);
	}

	@Override
	public void onCreate()
	{
		LLog.i(Globals.TAG, CLASSTAG + " Service created.");
		super.onCreate();
		mManager = new AntPlusManager();
		mManager.start(this);
	}	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (DEBUG) Log.d(Globals.TAG, CLASSTAG + ".onStartCommand");
		//ToastHelper.showToastLong(R.string.ant_service_started);
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy()
	{
		mManager.setCallbacks(null);
		mManager.shutDown();
		mManager = null;
		super.onDestroy();
		LLog.i(Globals.TAG, CLASSTAG + " Service destroyed.");
	}

}
