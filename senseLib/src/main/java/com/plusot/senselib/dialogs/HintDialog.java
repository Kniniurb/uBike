package com.plusot.senselib.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import com.plusot.senselib.R;
import com.plusot.senselib.util.Util;

public class HintDialog extends Dialog {
	private Context context;
	private int iHint = 0;
	private final DispatchKeyListener listener;

	public HintDialog(Context context, final DispatchKeyListener listener) {
		//super(context, android.R.style.Theme_Translucent_NoTitleBar);
		super(context, R.style.Theme_TranslucentDialog);
		this.context = context;
		this.listener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hint);

		final String[] hints = context.getResources().getStringArray(R.array.hint_items);
		iHint = (int) (Math.random() * (hints.length - 1));
		Util.setText(this, R.id.hint_title, getContext().getString(R.string.hint_title, iHint + 1, hints.length));
		Util.setText(this, R.id.hint_view,hints[iHint]);

		Button hintButton = (Button) this.findViewById(R.id.hint_button);

		hintButton.setOnClickListener(new android.view.View.OnClickListener() {

			@Override
			public void onClick(View v) {				
				iHint++;
				iHint %= hints.length;
				Util.setText(HintDialog.this, R.id.hint_title, getContext().getString(R.string.hint_title, iHint + 1, hints.length));
				Util.setText(HintDialog.this, R.id.hint_view,hints[iHint]);
			}
		});
	}

	@Override 
	protected void onStart() {
		super.onStart();
	}

	/*@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		final int keyCode = event.getKeyCode();
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			dismiss();
			return true;
		}
		return super.dispatchKeyEvent(event);
	}*/

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (listener != null) {
			final int keyCode = event.getKeyCode();
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
				return listener.onDispatchKeyEvent(event);
		}
		return super.dispatchKeyEvent(event);
	}

}
