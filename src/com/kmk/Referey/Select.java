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

import java.util.ArrayList;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;


public class Select extends TabActivity {
	private ListView tagsView;
	private ListView authorsView;
	private ListView journalsView;
	
	private NumberPicker yearStart;
	private NumberPicker yearEnd;
	
	private TabHost tabHost;
	ArrayList<String> tags_list = new ArrayList<String>();
	ArrayList<String> authors_list = new ArrayList<String>();
	ArrayList<String> journals_list = new ArrayList<String>();
	int minimum_year = 0;
	int maximum_year = 2100;	
	boolean[] tags_selection_array;
	ArrayList<String> tags_selected = new ArrayList<String>();
	ArrayList<String> authors_selected = new ArrayList<String>();
	int checked_position = ListView.INVALID_POSITION;
	ArrayList<String> journals_selected = new ArrayList<String>();
	int minimum_year_selected = -1;
	int maximum_year_selected = -1;
	
	static final int PICKER_SPEED = 5;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		boolean dark_theme = false;
		
		Intent myIntent = getIntent();
		Bundle extras =  myIntent.getExtras();
        if ( extras != null ) {
/*	        tags_list=extras.getStringArrayList("com.kmk.Referey.tags_list");
	        authors_list=extras.getStringArrayList("com.kmk.Referey.authors_list");
	        journals_list=extras.getStringArrayList("com.kmk.Referey.journals_list");*/
	        minimum_year = extras.getInt("com.kmk.Referey.minimum_year");
			maximum_year = extras.getInt("com.kmk.Referey.maximum_year");
	        
	        tags_selected=extras.getStringArrayList("com.kmk.Referey.tags_selected");
	        authors_selected=extras.getStringArrayList("com.kmk.Referey.authors_selected");
	        journals_selected=extras.getStringArrayList("com.kmk.Referey.journals_selected");
			minimum_year_selected = extras.getInt("com.kmk.Referey.minimum_year_selected");
			maximum_year_selected = extras.getInt("com.kmk.Referey.maximum_year_selected");
	        
        	dark_theme = extras.getBoolean("com.kmk.Referey.dark_theme");
		}
        
        //load large datasets from shared preferences
        SharedPreferences settings = getSharedPreferences("SELECT", 0);
                
        int size = settings.getInt("tags_list_size", 0);  
        for(int i=0;i<size;i++) 
        {
            tags_list.add(settings.getString("tags_list_" + i, null));  

        }
  
        size = settings.getInt("authors_list_size", 0);  
        for(int i=0;i<size;i++) 
        {
            authors_list.add(settings.getString("authors_list_" + i, null));  

        }
        
        size = settings.getInt("journals_list_size", 0);  
        for(int i=0;i<size;i++) 
        {
            journals_list.add(settings.getString("journals_list_" + i, null));  

        }

		if ( dark_theme ) {
    		setTheme(android.R.style.Theme_Black_NoTitleBar);
    	} else {
    		setTheme(android.R.style.Theme_Light_NoTitleBar);
    	}		
    	super.onCreate(savedInstanceState);
    	
		if (savedInstanceState != null) {
			minimum_year_selected = savedInstanceState.getInt("minimum_year_selected");
			maximum_year_selected = savedInstanceState.getInt("maximum_year_selected");
		}

		setContentView(R.layout.select_tabs);
  
        boolean[] tags_selection_array = new boolean[tags_list.size()];
		for ( int i = 0; i < tags_selection_array.length; i++ ) {
			tags_selection_array [i] = false;
			for ( int j=0; j < tags_selected.size(); j++) {
				if ( tags_list.get(i).equals(tags_selected.get(j)) ) {
					tags_selection_array [i] = true;
				}
			}
		}
        boolean[] authors_selection_array = new boolean[authors_list.size()];
		for ( int i = 0; i < authors_selection_array.length; i++ ) {
			authors_selection_array [i] = false;
			for ( int j=0; j < authors_selected.size(); j++) {
				if ( authors_list.get(i).equals(authors_selected.get(j)) ) {
					authors_selection_array [i] = true;
				}
			}
		}
        boolean[] journals_selection_array = new boolean[journals_list.size()];
		for ( int i = 0; i < journals_selection_array.length; i++ ) {
			journals_selection_array [i] = false;
			for ( int j=0; j < journals_selected.size(); j++) {
				if ( journals_list.get(i).equals(journals_selected.get(j)) ) {
					journals_selection_array [i] = true;
				}
			}
		}
		
		Resources res = getResources();
		tagsView = (ListView)findViewById(R.id.tagsview);
		tagsView.setAdapter(new ArrayAdapter<String>(this, R.layout.modified_list_item_multiple_choice, tags_list));
		tagsView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		authorsView = (ListView)findViewById(R.id.authorsview);
		authorsView.setAdapter(new ArrayAdapter<String>(this, R.layout.modified_list_item_multiple_choice, authors_list));
		authorsView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		journalsView = (ListView)findViewById(R.id.journalsview);
		journalsView.setAdapter(new ArrayAdapter<String>(this, R.layout.modified_list_item_single_choice, journals_list));
		journalsView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		
		yearStart = (NumberPicker)findViewById(R.id.yearstart);
		yearEnd = (NumberPicker)findViewById(R.id.yearend);
		
		if ( (minimum_year >= 0) && (maximum_year >= 0) && (maximum_year >= minimum_year) ) {
			yearStart.setRange( minimum_year, maximum_year);
			yearEnd.setRange( minimum_year, maximum_year);
		} else {
			yearStart.setRange( 0, 2100);
			yearEnd.setRange( 0, 2100);
		}
		
		if ( minimum_year_selected >= 0 ) {
			yearStart.setCurrent(minimum_year_selected);
		} else {
			yearStart.setCurrent(minimum_year);
		}
		if ( maximum_year_selected >= 0 ) {
			yearEnd.setCurrent(maximum_year_selected);
		} else{
			yearEnd.setCurrent(maximum_year);
		}
		
		yearStart.setSpeed(PICKER_SPEED);
		yearEnd.setSpeed(PICKER_SPEED);
		
		/** TabHost will have Tabs */
		tabHost = (TabHost)findViewById(android.R.id.tabhost);
		
		if ( !dark_theme ) {
			tabHost.getTabWidget().setBackgroundColor(android.graphics.Color.BLACK);
		}
		
		/** TabSpec used to create a new tab.
		* By using TabSpec only we can able to setContent to the tab.
		* By using TabSpec setIndicator() we can set name to tab. */

		/** tid1 is firstTabSpec Id. Its used to access outside. */
		TabSpec firstTabSpec = tabHost.newTabSpec("tags");
		TabSpec secondTabSpec = tabHost.newTabSpec("authors");
		TabSpec thirdTabSpec = tabHost.newTabSpec("publication");
		TabSpec fourthTabSpec = tabHost.newTabSpec("year");
		

		/** TabSpec setIndicator() is used to set name for the tab. */
		/** TabSpec setContent() is used to set content for a particular tab. */
		firstTabSpec.setIndicator("Tags", res.getDrawable(R.drawable.tab_tags)).setContent(R.id.tagsview);
		secondTabSpec.setIndicator("Authors", res.getDrawable(R.drawable.tab_authors)).setContent(R.id.authorsview);
		thirdTabSpec.setIndicator("Publication", res.getDrawable(R.drawable.tab_journals)).setContent(R.id.journalsview);
		fourthTabSpec.setIndicator("Year", res.getDrawable(R.drawable.tab_year)).setContent(R.id.yearview);

		/** Add tabSpec to the TabHost to display. */
		tabHost.addTab(firstTabSpec);
		tabHost.addTab(secondTabSpec);
		tabHost.addTab(thirdTabSpec);
		tabHost.addTab(fourthTabSpec);
		
		for (int i=0; i < tags_selection_array.length; i++) {
			tagsView.setItemChecked(i, tags_selection_array[i]);	
		}
		for (int i=0; i < authors_selection_array.length; i++) {
			authorsView.setItemChecked(i, authors_selection_array[i]);	
		}
		for (int i=0; i < journals_selection_array.length; i++) {
			if (journals_selection_array[i]) {
				journalsView.setItemChecked(i, true);
				break;
			}
		}
		
		checked_position = journalsView.getCheckedItemPosition();
		
		journalsView.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterview, View arg1, int position, long arg3) {
				if ( checked_position == position ) {
					journalsView.setItemChecked(position, false);
					checked_position = ListView.INVALID_POSITION;
				} else {
					checked_position = position;
				}
			}
		});

		//get last active tab and set
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String current_filter_tab = prefs.getString("current_filter_tab", "tags");
		tabHost.setCurrentTabByTag(current_filter_tab);
		
		Button FilterButton = (Button) findViewById(R.id.Button01);
		Button ResetButton = (Button) findViewById(R.id.Button02);
		Button ResetTabButton = (Button) findViewById(R.id.Button03);

		ResetButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				ResetTags();
				ResetAuthors();
				ResetJournals();
				ResetYears();
				
				Context context = getApplicationContext();
	    		Toast.makeText(context, "Reset all items", Toast.LENGTH_SHORT).show();
			}
		});
		
		ResetTabButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				String CurrentTab = tabHost.getCurrentTabTag();
				if ( CurrentTab.equals("tags") ) {
		    		ResetTags();
				}
				else if ( CurrentTab.equals("authors")) {
		    		ResetAuthors();
				}
				else if ( CurrentTab.equals("publication")) {
		    		ResetJournals();
				}
				else if ( CurrentTab.equals("year")) {
		    		ResetYears();
				}
				
				Context context = getApplicationContext();
	    		Toast.makeText(context, "Reset " + CurrentTab, Toast.LENGTH_SHORT).show();
			}
		});
				
				
		FilterButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View view) {
				SparseBooleanArray tags_selection = tagsView.getCheckedItemPositions();
				SparseBooleanArray authors_selection = authorsView.getCheckedItemPositions();
				SparseBooleanArray journals_selection = journalsView.getCheckedItemPositions();
				
				tags_selected.clear();
				for ( int i=0; i< tags_list.size(); i++ ) {
					if ( tags_selection.get(i) ) {
						tags_selected.add(tags_list.get(i));
					}
				}
				authors_selected.clear();
				for ( int i=0; i< authors_list.size(); i++ ) {
					if ( authors_selection.get(i) ) {
						authors_selected.add(authors_list.get(i));
					}
				}
				journals_selected.clear();
				for ( int i=0; i< journals_list.size(); i++ ) {
					if ( journals_selection.get(i) ) {
						journals_selected.add(journals_list.get(i));
					}
				}
				minimum_year_selected = yearStart.getCurrent();
				maximum_year_selected = yearEnd.getCurrent();
				
				if ( minimum_year_selected > maximum_year_selected ) {
					int i = minimum_year_selected;
					minimum_year_selected = maximum_year_selected;
					maximum_year_selected = i;
				}
				if ( (minimum_year_selected == minimum_year) && (maximum_year_selected == maximum_year) ) {
					minimum_year_selected = -1;
					maximum_year_selected = -1;
				}
				
				Intent select = new Intent();
    			select.putStringArrayListExtra("com.kmk.Referey.tags_selected", tags_selected);
    			select.putStringArrayListExtra("com.kmk.Referey.authors_selected", authors_selected);
    			select.putStringArrayListExtra("com.kmk.Referey.journals_selected", journals_selected);
    			select.putExtra("com.kmk.Referey.minimum_year_selected", minimum_year_selected);
    			select.putExtra("com.kmk.Referey.maximum_year_selected", maximum_year_selected);
	            setResult(RESULT_OK, select);
		    	finish();
			}
		});
	}
	
    protected void ResetTags() {
		for (int i=0; i < tags_list.size(); i++) {
			tagsView.setItemChecked(i, false);	
		}
      }
    
    protected void ResetAuthors() {
		for (int i=0; i < authors_list.size(); i++) {
			authorsView.setItemChecked(i, false);	
		}
      }
    
    protected void ResetJournals() {
		for (int i=0; i < journals_list.size(); i++) {
			journalsView.setItemChecked(i, false);	
		}
		checked_position = ListView.INVALID_POSITION;
      }
    
    protected void ResetYears() {
		yearStart.setCurrent(minimum_year);
		yearEnd.setCurrent(maximum_year);
      }
    
    @Override
    protected void onPause() {
      //save current tab
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
       
        editor.putString("current_filter_tab", tabHost.getCurrentTabTag());
        
        // Commit the edits!
    	editor.commit();
    	
    	//call super
    	super.onPause();
    }
    
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
      // Save UI state changes to the savedInstanceState.
      // This bundle will be passed to onCreate if the process is
      // killed and restarted.
		minimum_year_selected = yearStart.getCurrent();
		maximum_year_selected = yearEnd.getCurrent();
        savedInstanceState.putInt("minimum_year_selected", minimum_year_selected);
        savedInstanceState.putInt("maximum_year_selected", maximum_year_selected);
		
        super.onSaveInstanceState(savedInstanceState);
    }

}
