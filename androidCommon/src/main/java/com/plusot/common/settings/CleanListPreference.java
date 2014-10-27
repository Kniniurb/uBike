package com.plusot.common.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class CleanListPreference extends ListPreference {

		public CleanListPreference(Context context, AttributeSet attrs) {
		    super(context, attrs);
		}

		@Override
		protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		    super.onPrepareDialogBuilder(builder);    //To change body of overridden methods use File | Settings | File Templates.
		    builder.setNegativeButton(null,null);
		    //builder.setTitle(null);
		}

}
