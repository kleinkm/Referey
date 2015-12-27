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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;

public class Sort extends Activity {
	private RadioGroup mRadioGroup;
	private CheckBox mCheckBox;
	private String current_sort_by = "authors";
	private String current_reverse = "";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		boolean dark_theme = false;
		
		Intent myIntent = getIntent();
		Bundle extras =  myIntent.getExtras();
        if ( extras != null ) {
        	dark_theme = extras.getBoolean("com.kmk.Referey.dark_theme");
        	current_sort_by = extras.getString("com.kmk.Referey.current_sort_by");
        	current_reverse = extras.getString("com.kmk.Referey.current_reverse");
		}
		
		if ( dark_theme ) {
    		setTheme(android.R.style.Theme_Black);
    	} else {
    		setTheme(android.R.style.Theme_Light);
    	}		
    	super.onCreate(savedInstanceState);

		setContentView(R.layout.sort_form);
		// creating references to the UI Commands
		mRadioGroup = (RadioGroup) findViewById(R.id.RadioGroup01);
		mCheckBox = (CheckBox) findViewById(R.id.CheckBox01);

		Button SortButton = (Button) findViewById(R.id.Button02);
		Button CancelButton = (Button) findViewById(R.id.Button01);
		
		//set RadioGroup to current_sort_by 
		int RadioGroup_preselect;
	   	if ( current_sort_by.equals("authors") ) {
	   		RadioGroup_preselect = R.id.RadioButton01;
	   	} else if ( current_sort_by.equals("title") ) {
	   		RadioGroup_preselect = R.id.RadioButton02;
	   	} else if ( current_sort_by.equals("year") ) {
	   		RadioGroup_preselect = R.id.RadioButton03;
	   	} else if ( current_sort_by.equals("publication") ) {
	   		RadioGroup_preselect = R.id.RadioButton04;
	   	} else if ( current_sort_by.equals("added") ) {
	   		RadioGroup_preselect = R.id.RadioButton05;
	   	} else if ( current_sort_by.equals("favourite") ) {
	   		RadioGroup_preselect = R.id.RadioButton06;
	   	} else {
	   		RadioGroup_preselect = R.id.RadioButton07;
	   	}
		mRadioGroup.check(RadioGroup_preselect);
		
		//set CheckBox to current_reverse
		if ( current_reverse.equals("desc") ) {
			mCheckBox.setChecked(true);
		} else {
			mCheckBox.setChecked(false);
		}
		
		CancelButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
		    	setResult(RESULT_CANCELED);
		    	finish();
			}
		});
				
		SortButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				switch ( mRadioGroup.getCheckedRadioButtonId() ) 
		    	{ 
		    	  case R.id.RadioButton01: 
		    		  current_sort_by="authors";
		    	      break; 
		    	  case R.id.RadioButton02: 
		    		  current_sort_by="title";
		      	      break; 
		    	  case R.id.RadioButton03: 
		    		  current_sort_by="year";
		      	      break; 
		    	  case R.id.RadioButton04: 
		    		  current_sort_by="publication";
		    		  break; 
		    	  case R.id.RadioButton05: 
		    		  current_sort_by="added";
		      	      break; 
		    	  case R.id.RadioButton06: 
		    		  current_sort_by="favourite";
		      	      break; 
		    	  case R.id.RadioButton07: 
		    		  current_sort_by="read";
		    	      break; 
		    	}

				if (mCheckBox.isChecked()) {
		            	current_reverse ="desc";
				} else {
	            	current_reverse ="";
				}
				
				Intent sort = new Intent();
	            sort.putExtra("com.kmk.Referey.current_sort_by", current_sort_by); 
	            sort.putExtra("com.kmk.Referey.current_reverse", current_reverse);
	            setResult(RESULT_OK, sort);
		    	finish();
			}
		});
	}
}
