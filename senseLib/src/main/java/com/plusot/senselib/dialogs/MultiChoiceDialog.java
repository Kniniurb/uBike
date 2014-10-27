package com.plusot.senselib.dialogs;

import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;
import com.plusot.senselib.R;
import com.plusot.senselib.util.Util;

public class MultiChoiceDialog extends DialogFragment {
	private static final String CLASSTAG = MultiChoiceDialog.class.getSimpleName();
	
	private static final String VIEWID = "VIEWID";
	private static final String TITLEID = "TITLEID";
	private static final String DIALOGID = "DIALOGID";
	private static final String ITEMS = "ITEMS";
	private static final String SELECTED = "SELECTED";
	
	public interface Listener {
		public void onMultiChoiceDialogResult(int dialogId, int viewId, Map<String, Boolean> map);
	}
	
	public static void showPopupDialog(FragmentManager mgr, String title, int dialogId, int viewId, Map<String, Boolean> items) {
		MultiChoiceDialog frag = new MultiChoiceDialog();
		Bundle args = new Bundle();
		args.putString(TITLEID, title);
		args.putInt(DIALOGID, dialogId);
		args.putInt(VIEWID, viewId);
		args.putStringArray(ITEMS, items.keySet().toArray(new String[0]));
		args.putBooleanArray(SELECTED, Util.toBooleanArray(items.values())); 
		frag.setArguments(args);
		LLog.d(Globals.TAG, "PopupDialog.showPopupDialog called with dialogId: " + dialogId + " and viewId: " + viewId);
		frag.show(mgr, "dialog");
	}

	@Override
	public Dialog onCreateDialog(Bundle bundle) {
		Dialog dialog = null;
		final int dialogId = getArguments().getInt(DIALOGID);
		final int viewId = getArguments().getInt(VIEWID);
		final String title = getArguments().getString(TITLEID);
		final String[] items = getArguments().getStringArray(ITEMS);
		final boolean[] selected = getArguments().getBooleanArray(SELECTED);
		final Map<String, Boolean> selectedMap = new HashMap<String, Boolean>();
		LLog.d(Globals.TAG, CLASSTAG + ".onCreateDialog " + dialogId);

		dialog = new AlertDialog.Builder(getActivity())
		//.setIcon()
		.setTitle(title)
		.setMultiChoiceItems(items, selected, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				selectedMap.put(items[which], isChecked);
			}
		})
		.setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton)  {
				((Listener)getActivity()).onMultiChoiceDialogResult(dialogId, viewId, selectedMap);
			}
		})
		.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				if (dialog != null) dialog.dismiss();
			}
		})
		.create();
		return dialog;
	}
}

