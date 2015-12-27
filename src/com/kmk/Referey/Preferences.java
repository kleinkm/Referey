// Copyright 2011-2015 Karl Martin Klein

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.kmk.Referey;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;


public class Preferences extends PreferenceActivity {
	 Preference preserve_path;
	 Preference remove_path;
	 CheckBoxPreference alternative_path;
	 
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		boolean dark_theme = false;
		
		Bundle extras = getIntent().getExtras();
		if(extras != null)
		{
			dark_theme = extras.getBoolean("com.kmk.Referey.dark_theme");

		}
		
	    if ( dark_theme ) {
    		setTheme(android.R.style.Theme_Black);
    	} else {
    		setTheme(android.R.style.Theme_Light);
    	}
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.preferences);

	    alternative_path = (CheckBoxPreference) findPreference("alternative_path");
	    preserve_path = findPreference("preserve_path");
	    remove_path = findPreference("remove_path");
	    
    	if ( alternative_path.isChecked() == true ) {
    		preserve_path.setEnabled(false);
    		remove_path.setEnabled(true);
    	} else {
    		preserve_path.setEnabled(true);
    		remove_path.setEnabled(false);
    	}
	    
	    alternative_path.setOnPreferenceClickListener(new OnPreferenceClickListener() {
	        public boolean onPreferenceClick(Preference preference) {
	            if ( preference.getKey().contains("alternative_path") ) {
	            	if ( alternative_path.isChecked() == true ) {
	            		preserve_path.setEnabled(false);
	            		remove_path.setEnabled(true);
	            	} else {
	            		preserve_path.setEnabled(true);
	            		remove_path.setEnabled(false);	            	}
	            	return true;
	            } else {
	            	return false;
	            }
	        }
	    });

	}
}
	

