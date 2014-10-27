package com.plusot.senselib.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.common.util.SleepAndWake;

import java.util.HashMap;
import java.util.Map;

public class PopupDialog extends DialogFragment {
	private static final String CLASSTAG = PopupDialog.class.getSimpleName();
	
	private static final String VIEWID = "VIEWID";
	private static final String TITLEID = "TITLEID";
	private static final String DIALOGID = "DIALOGID";
    private static final String ITEMS = "ITEMS";
    private static final String TIMEOUT = "TIMEOUT";
    private static final String DEFAULTCHOICE = "DEFAULTCHOICE";
    private static final String CANCELABLE = "CANCELABLE";
	private static final String MAP = "MAP_";
	private Map<String, String> map = new HashMap<String, String>();
	//private static final String TAG = "TAG";
	
	public interface Listener {
		public void onPopupResult(int dialogId, int viewId, int iWhich, int itemsSize, String sWhich, String sTag); //, int tag);

	}
	
	public static boolean showPopupDialog(final FragmentManager mgr, final String title, final int dialogId, final int viewId, final String[] items, final Map<String, String> lookUp, final boolean cancelable, final int timeOut, final int defaultChoice) { //, int tag) { //String[] ids) {
		final PopupDialog frag = new PopupDialog();
		Bundle args = new Bundle();
		args.putString(TITLEID, title);
		args.putInt(DIALOGID, dialogId);
        args.putInt(VIEWID, viewId);
        args.putInt(TIMEOUT, timeOut);
        args.putInt(DEFAULTCHOICE, defaultChoice);
        args.putStringArray(ITEMS, items);
		args.putBoolean(CANCELABLE, cancelable);
		String value;
		if (lookUp != null) for (String item : items) {
			if ((value = lookUp.get(item)) != null) args.putString(MAP + item, value);
		}
		frag.setArguments(args);
		LLog.d(Globals.TAG, "PopupDialog.showPopupDialog called with dialogId: " + dialogId + " and viewId: " + viewId);
		try {
			frag.setCancelable(cancelable);
			frag.show(mgr, "dialog");
			return true;
		} catch (Exception e) {
			LLog.e(Globals.TAG, CLASSTAG + ".showPopupDialog: Exception", e);
			return false;
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle bundleOrg) {
		//Dialog dialog = null;
		Bundle bundle = getArguments();
		final int dialogId = bundle.getInt(DIALOGID);
		final int viewId = bundle.getInt(VIEWID);
		final String title = bundle.getString(TITLEID);
		final String[] items = bundle.getStringArray(ITEMS);
        final int timeOut = bundle.getInt(TIMEOUT);
        final int defaultChoice = bundle.getInt(DEFAULTCHOICE);
        for (String item: items) {
			String value;
			if ((value = bundle.getString(MAP + item)) != null) map.put(item, value);
		}
		//final int tag = getArguments().getInt(TAG);
		LLog.d(Globals.TAG, "Create dialog " + dialogId);

		final Dialog dialog = new AlertDialog.Builder(getActivity())
		.setTitle(Html.fromHtml(title))
		.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				((Listener)getActivity()).onPopupResult(dialogId, viewId, which, items.length, items[which], map.get(items[which])); //, tag);

			}
		})
		/*.setPositiveButton("Test", new OnClickListener () {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				LLog.d(Globals.TAG, CLASSTAG + ".onPositive button called.");
				
				
			}})
		.setCancelable(cancelable)
		.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				LLog.d(Globals.TAG, CLASSTAG + ".onCancel called.");
				((Listener)getActivity()).onPopupResult(dialogId, viewId, -1, items.length, ""); //, tag);	
				
			}
		})*/
		.create();
        if (timeOut > 0) new SleepAndWake(new SleepAndWake.Listener() {
            @Override
            public void onWake() {
                Log.d(Globals.TAG, CLASSTAG + ".onWake");
                Listener listener = (Listener) getActivity();
                if (listener != null) listener.onPopupResult(dialogId, viewId, defaultChoice, items.length, items[defaultChoice], map.get(items[defaultChoice]));
                dialog.dismiss();
            }
        }, timeOut);


		return dialog;
	}
}

