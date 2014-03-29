package com.example.hough;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

public class ResetDialogPreference extends DialogPreference {

	protected Context context;
	
	public ResetDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	@Override  
    public void onClick(DialogInterface dialog, int which)   
    {  
        super.onClick(dialog, which);  
  
        if(which == DialogInterface.BUTTON_POSITIVE)  
        {  
            SharedPreferences.Editor preferencesEditor = PreferenceManager.getDefaultSharedPreferences(this.context).edit();  
            preferencesEditor.clear();  
            PreferenceManager.setDefaultValues(context, R.xml.settings, true);  
            preferencesEditor.commit();  
  
            getOnPreferenceChangeListener().onPreferenceChange(this, true);  
        }  
    }  
}
