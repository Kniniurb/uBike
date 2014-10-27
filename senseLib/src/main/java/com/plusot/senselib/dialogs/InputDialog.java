package com.plusot.senselib.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.plusot.senselib.R;

public class InputDialog extends Dialog {

	public interface Listener {
		public void onClose(int id, String temp);
	}


	private final int id;
	private final int title;
	private final int description;
	private String value;
	private final int hint;
	private final Listener listener;


	public InputDialog(Context context, final int id, final int title, final int description, final String value, final int hint, final Listener listener) {
		super(context);
		this.id = id;
		this.listener = listener;
		this.value = value;
		this.hint = hint;
		this.description = description;
		this.title = title;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.input_dialog);

		setTitle(getContext().getResources().getString(title));

		TextView descr = (TextView) findViewById(R.id.dialog_text);
		EditText input = (EditText) findViewById(R.id.dialog_input);

		descr.setText(getContext().getResources().getString(description));
		input.setText(value);
		input.setHint(hint);

		Button buttonOK = (Button) findViewById(R.id.dialog_button);
		buttonOK.setOnClickListener(new OKListener());     
	}

	@Override 
	protected void onStart() {
		super.onStart();
		EditText input = (EditText) findViewById(R.id.dialog_input);
		input.setText(String.valueOf(value));
		input.selectAll();
	}

	public int getId() {
		return id;
	}



	@Override
	public void onBackPressed() {
		EditText input = (EditText) findViewById(R.id.dialog_input);
		String temp = input.getText().toString();         
		listener.onClose(id, temp);  
		super.onBackPressed();
	}

	private class OKListener implements android.view.View.OnClickListener {  

		public void onClick(View v) {
			EditText input = (EditText) findViewById(R.id.dialog_input);
			String temp = input.getText().toString();         
			listener.onClose(id, temp);  
			dismiss();
		}
	}
}
