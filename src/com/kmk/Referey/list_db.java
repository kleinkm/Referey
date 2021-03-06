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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class list_db extends Activity {
    //preferences
	String fileName="/sdcard/database.sqlite";
    String path_file="/sdcard/referey";
    int preserve_path = 1;
    boolean alternative_path = false;
    int remove_path = 0;
    int font_size = 14;
    //other variables
    List<HashMap<String, String>> current_reference_list;
	List<HashMap<String, String>> collections_list = new ArrayList<HashMap<String, String>>();
	List<String> file_list = new ArrayList<String>();
    String current_collection = "All documents";
    String old_collection="";
    String current_group = "0";
    String old_group = "";
    String current_sort_by = "authors";
    String old_sort_by="";
	String current_reverse = "";
	String old_reverse="";
	ListView lv;
	int lv_top=0;
	int lv_index=0;
	ArrayList<String> tags_list = null;
	ArrayList<String> authors_list = null;
	ArrayList<String> journals_list = null;
	AlertDialog FileDialog = null;
	ArrayList<String> tags_selected = new ArrayList<String>();
	ArrayList<String> old_tags_selected = new ArrayList<String>();
	ArrayList<String> authors_selected = new ArrayList<String>();
	ArrayList<String> old_authors_selected = new ArrayList<String>();
	ArrayList<String> journals_selected = new ArrayList<String>();
	ArrayList<String> old_journals_selected = new ArrayList<String>();
	int minimum_year = -1;
	int maximum_year = -1;
	int minimum_year_selected = -1;
	int maximum_year_selected = -1;
	int old_minimum_year_selected = -1;
	int old_maximum_year_selected = -1;
	
	String current_search = "";
	String old_search = "";
	boolean dark_theme = true;
	boolean onlyReference = true;
	LinearLayout searchbar;
	LinearLayout lLayout;
	list_db ACTIVITY;
	PendingIntent RESTART_INTENT;
	StateButton sort;
    StateButton tags;
	public ProgressDialog pdia;
	AnimationDrawable rotate;
    
    QueryDatabaseTask backgroundtask = null;
    GetFilterLists filterlisttask = null;
    
    static final int GET_SORT_OPTIONS = 0;
    static final int SHOW_REFERENCE_DETAILS = 1;
    static final int GET_TAGS_SELECTED = 2;
    static final String LEVEL_SIGN = ">";

			
    public class QueryDatabaseTask extends AsyncTask<Void, Void, Void> {
    	private list_db activity;
    	private boolean completed;
    	
     	private boolean perform_query=true;
    	private boolean perform_sort=true;
    	List<HashMap<String, String>> references = new ArrayList<HashMap<String, String>>();
    	
    	private QueryDatabaseTask(list_db activity) {
            this.activity = activity;
    	}
    	
    	public List<HashMap<String, String>> getReferences() {
    		return references;
    	}

        @Override
        protected Void doInBackground(Void... params) {
	    	Cursor c = null;
	    	SQLiteDatabase db = null;
	    		    	
			try {
				db = SQLiteDatabase.openDatabase(activity.fileName, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READWRITE);
			}
			catch (SQLiteException e) {
    			db = null;
			}
			
	    	if (db != null) {
		    	if ( perform_query ) {
		    		//prepare author query
			    	SQLiteStatement author_query = db.compileStatement("select group_concat(lastName, ', ') as authors from DocumentContributors where documentid=? and (contribution='DocumentAuthor' or contribution is NULL) group by documentid order by id");
			    	//generate SQLite query
		    		String query = "select Documents.id as id, favourite, added, read, title, publication, year, volume, issue, pages";
		    		query = query + " from Documents where (";
		    		if ( onlyReference ) {
		    			query = query + "(onlyReference='false' or onlyReference is null) and ";
		    		}
		    		
		    		query = query + "(deletionPending='false' or deletionPending is null)";
		    		
		    		query = query + " and Documents.id in (select documentid from RemoteDocuments where groupid=" + activity.current_group + ")";
		    		if (activity.current_collection.equals("Unsorted")) {
		    			query = query + "and Documents.id not in (select documentid from DocumentFolders)";
		    		} else if (!activity.current_collection.equals("All Documents")) {
		    			query = query + " and Documents.id in (select documentid from DocumentFolders where folderId in (" + activity.current_collection +"))";
		    		}

		    		if ( activity.tags_selected.size() > 0 ) {
		    			for (int i=0; i < activity.tags_selected.size(); i++ ) {
		    				String tag_selected = activity.tags_selected.get(i).replace("'", "''");
		    				query = query + " and Documents.id in (select Documentid from DocumentTags where tag='" + tag_selected +"' collate nocase)"; 
		    			}
		    		}
		    		
		    		if ( activity.authors_selected.size() > 0 ) {
		    			for (int i=0; i < activity.authors_selected.size(); i++ ) {
		    				String author_selected = activity.authors_selected.get(i).replace("'", "''");
		    				query = query + " and Documents.id in (select Documentid from DocumentContributors where lastname ||', '|| firstnames='" + author_selected +"' collate nocase)"; 
		    			}
		    		}
		    		
		    		if ( activity.journals_selected.size() > 0 ) {
		    			for (int i=0; i < activity.journals_selected.size(); i++ ) {
		    				String journal_selected = activity.journals_selected.get(i).replace("'", "''");
		    				query = query + " and publication='" + journal_selected +"' collate nocase"; 
		    			}
		    		}
		    		
		    		if ( (minimum_year_selected != -1) && (maximum_year_selected != -1) ) {
		    			    query = query + " and year between " + activity.minimum_year_selected + " and " + activity.maximum_year_selected; 
		    		}
		    		if ( !activity.current_search.equals("") ) {
		    			String[] search = TextUtils.split(activity.current_search, " ");
		    			for (int i = 0; i < search.length; i++) {
	                		if ( search[i].equals("") == false ) {
	                			query = query + " and (title like '%" + search[i] + "%'";
	        	    			query = query + " or publication like '%" + search[i] + "%'";
	        	    			query = query + " or abstract like '%" + search[i] + "%'";
	        	    			query = query + " or year like '%" + search[i] + "%'";
	        	    			query = query + " or note like '%" + search[i] + "%'";
	        	    			query = query + " or Documents.id in (select documentId from DocumentContributors where lastName like '%" + search[i] + "%' or firstNames like '%" +search[i] + "%')";
	        	    			query = query + " or Documents.id in (select documentId from DocumentTags where tag like '%" + search[i] + "%')";
	        	    			query = query + " or Documents.id in (select documentId from FileNotes where note like '%" + search[i] + "%')";
	        	    			query = query + " or Documents.id in (select documentId from DocumentKeywords where keyword like '%" + search[i] + "%'))";
	                		}
	                	}
		    		}
		    		query = query + ")";
	
	    		 			
		    		/* Query for results */
		    		try {
		    			c = db.rawQuery(query, null);
			    		/* Check if our result was valid. */
			    		if (c != null) {
			    			////startManagingCursor(c);
			    			/* Get the indices of the Columns we will need */
			                int idColumn = c.getColumnIndex("id");
			            	int titleColumn = c.getColumnIndex("title");
			            	int publicationColumn = c.getColumnIndex("publication");
			            	int yearColumn = c.getColumnIndex("year");
			            	int volumeColumn = c.getColumnIndex("volume");
			            	int issueColumn = c.getColumnIndex("issue");
			            	int pagesColumn = c.getColumnIndex("pages");
			            	int favouriteColumn = c.getColumnIndex("favourite");
			            	int readColumn = c.getColumnIndex("read");
			            	int addedColumn = c.getColumnIndex("added");
			            	String citation = "";
			           		
			              	c.moveToFirst();
			               	/* Loop through all Results */
			               	while ( !c.isAfterLast() ) {
			               		/* Retrieve the values of the Entry
			               		/* the Cursor is pointing to. */
			               		String id = c.getString(idColumn);
			               		String title = c.getString(titleColumn);
			               		String publication = c.getString(publicationColumn);
			               		String year = c.getString(yearColumn);
			               		String volume = c.getString(volumeColumn);
			               		String issue = c.getString(issueColumn);
			               		String pages = c.getString(pagesColumn);
			               		String favourite = c.getString(favouriteColumn);
			               		String read = c.getString(readColumn);
			               		String added = c.getString(addedColumn);
			               		String authors = "";

			               		//get authors
			               		author_query.bindString(1, id);
			               		try {
			               			authors = author_query.simpleQueryForString();
			               		} catch ( SQLiteDoneException e ) {
			               			authors = "";
			               		} catch ( SQLiteException e ) {
			               			authors = "";
			               		}
			               		author_query.clearBindings();
			               		
			               		// generate citation string
			                    citation = "";
			                    
			                    if ( publication != null)
			                    	citation = citation + publication;
			                    else
			                    	publication = "";
			                    
			                    if ( year != null)
			                    	citation = citation + " " + year;
			                    else
			                    	year = "";
			                    
			                    if ( volume != null)
			                    	citation = citation + ";" + volume;
			                    if ( issue != null)
			                    	citation = citation + "(" + issue + ")";
			                    if ( pages != null)
			                    	citation = citation + ":" + pages;
			                    
			                    if ( title == null)
			                    	title= "";
			                    if ( added == null)
			                    	added= "";
			                    if ( favourite == null || !favourite.equals("true") )
			                    	favourite= "false";
			                    if ( read == null || !read.equals("true") )
			                    	read = "false";
			                     
			               		/* Add current Entry to results. */
			                    HashMap<String, String> map = new HashMap<String, String>();
			               		map.put("id", id);
			               		map.put("authors", authors);
			               		map.put("title", title);
			               		map.put("citation", citation);
			               		map.put("favourite", favourite);
			               		map.put("read", read);
			               		map.put("publication", publication);
			               		map.put("added", added);
			               		map.put("year", year);

			               		references.add(map);
			           	      	c.moveToNext();
			               	}
			               	c.close();
			            }
			           
		    		}
		    		catch ( SQLiteException e) {
		    			references = null;
		    			if ( c != null ) {
		    				c.close();
		    			}
		        		db.close();
		    			return null;
		    		}
		    	} else {
		    		references = activity.current_reference_list;
		    	}
		    	if ( perform_sort == true ) {
		            //sort references
		            Comparator<HashMap<String, String>> comperator = new Comparator<HashMap<String, String>>() {
		                	Collator myCollator = Collator.getInstance();
		                	@Override
			            	public int compare(HashMap<String, String> object1, HashMap<String, String> object2) {
			            		int reverse = 1;
			                    if ( !activity.current_reverse.equals("") ) {
			                    	reverse = -1;
			                    }
			                    return myCollator.compare(object1.get(activity.current_sort_by), object2.get(activity.current_sort_by)) * reverse;
			            	}
		            };
		            Collections.sort(references, comperator);
		    	}
	    		db.close();
	    	} else {
	    		references = null;
	    	}
	    	return null;
        }
		
	    @Override
	    protected void onPreExecute() {

			if ( !activity.current_search.equals("") ) {
				activity.lLayout.removeView(activity.lv);
				activity.lLayout.removeView(activity.searchbar);
				activity.lLayout.addView(activity.searchbar);
				activity.lLayout.addView(activity.lv);
				TextView search_text = (TextView) findViewById(R.id.SearchText);
				search_text.setText("Search: " + activity.current_search);
			}
			if ( !activity.old_collection.equals(activity.current_collection) || !activity.old_group.equals(activity.current_group) || !activity.tags_selected.equals(activity.old_tags_selected) || !activity.authors_selected.equals(activity.old_authors_selected) || !activity.journals_selected.equals(activity.old_journals_selected) || !activity.current_search.equals(activity.old_search) || (old_minimum_year_selected != minimum_year_selected) || (old_maximum_year_selected != maximum_year_selected) ) {
				perform_query = true;
				perform_sort = true;
				activity.lv_index=0;
				activity.lv_top=0;
			} else if ( !activity.old_sort_by.equals(activity.current_sort_by) || !activity.old_reverse.equals(activity.current_reverse) ) {
	    		perform_query = false;
	    		perform_sort = true;
	    		activity.lv_index=0;
	    		activity.lv_top=0;
			} else if (activity.current_reference_list == null) {
				perform_query = true;
				perform_sort = true;
			} else {
				perform_query = false;
				perform_sort = false;
			}
	   	    if ( activity.pdia == null || !activity.pdia.isShowing() ) {
	   	    	activity.pdia = ProgressDialog.show(activity, "", "Updating references...", true, false);
	   	    	activity.pdia.setOwnerActivity(activity);
	   	    }
        }

	    @Override
	    protected void onPostExecute(Void unused) {
    		completed = true;
            notifyActivityTaskCompleted();
		}
		
	    private void setActivity(list_db activity) {
               this.activity = activity;
               if ( completed ) {
               	notifyActivityTaskCompleted();
               }
        }

	    private void notifyActivityTaskCompleted() {
               if ( null != activity ) {
               	activity.onTaskCompleted();
               }
        }
   }

    
    public class GetFilterLists extends AsyncTask<Void, Void, Void> {
    	private list_db activity;
    	private boolean completed;
    	
    	private ArrayList<String> async_tags_list = new ArrayList<String>();
    	private ArrayList<String> async_authors_list = new ArrayList<String>();
    	private ArrayList<String> async_journals_list = new ArrayList<String>();
    	int async_minimum_year;
    	int async_maximum_year;

    	private GetFilterLists(list_db activity) {
            this.activity = activity;
    	}
    	
    	public ArrayList<String> get_tags_list() {
    		return async_tags_list;
    	}
    	
    	public ArrayList<String> get_authors_list() {
    		return async_authors_list;
    	}
    	
    	public ArrayList<String> get_journals_list() {
    		return async_journals_list;
    	}
    	
    	public int get_minimum_year() {
    		return async_minimum_year;
    	}
    	
    	public int get_maximum_year() {
    		return async_maximum_year;
    	}
        @Override
        protected Void doInBackground(Void... params) {
	    	Cursor c = null;
	    	SQLiteDatabase db = null;
	    		    	
			try {
				db = SQLiteDatabase.openDatabase(activity.fileName, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READWRITE);
			}
			catch (SQLiteException e) {
    			db = null;
			}
			
	    	if (db != null) {
	    		try {
			        //get tags
					c = db.rawQuery("select tag from DocumentTags group by lower(tag)", null);
					
			        /* Check if our result was valid. */
			        if (c != null) {
			            /* Get the indices of the Columns we will need */
			            int tagColumn = c.getColumnIndex("tag");
			        	
			          	c.moveToFirst();
			           	/* Loop through all Results */
			           	while (c.isAfterLast() == false) {
			           		/* Retrieve the values of the Entry
			           		/* the Cursor is pointing to. */
			           		String tag = c.getString(tagColumn);
			           		/* Add current Entry to results. */
			           		if (tag != null ) {
			           			async_tags_list.add (tag);
			           		}
			           		c.moveToNext();
			           	}
			           	c.close();
			        }
	
			        //get authors
			        
			        if ( onlyReference ) {
			        	c = db.rawQuery("select lastname ||', '|| firstnames as authors from Documentcontributors where documentid in (select id from documents where ((onlyReference='false' or onlyReference is null) and (deletionPending='false' or deletionPending is null))) group by lower(authors)", null);
			        } else {
			        	c = db.rawQuery("select lastname ||', '|| firstnames as authors from Documentcontributors where documentid in (select id from documents where ((deletionPending='false' or deletionPending is null))) group by lower(authors)", null);
			        }
					
					
			        /* Check if our result was valid. */
			        if (c != null) {
			            /* Get the indices of the Columns we will need */
			            int authorsColumn = c.getColumnIndex("authors");
			        	
			          	c.moveToFirst();
			           	/* Loop through all Results */
			           	while (c.isAfterLast() == false) {
			           		/* Retrieve the values of the Entry
			           		/* the Cursor is pointing to. */
			           		String author = c.getString(authorsColumn);
			           		/* Add current Entry to results. */
			           		if (author != null ) {
			           			async_authors_list.add (author);
			           		}
			           		c.moveToNext();
			           	}
			           	c.close();
			        }
	
			        //get journals
			        if ( onlyReference ) {
			        	c = db.rawQuery("select publication from Documents where ((onlyReference='false' or onlyReference is null) and (deletionPending='false' or deletionPending is null)) group by lower(publication)", null);
			        } else {
			        	c = db.rawQuery("select publication from Documents where ((deletionPending='false' or deletionPending is null)) group by lower(publication)", null);
	    			}
	    		
			        /* Check if our result was valid. */
			        if (c != null) {
			            /* Get the indices of the Columns we will need */
			            int journalsColumn = c.getColumnIndex("publication");
			        	
			          	c.moveToFirst();
			           	/* Loop through all Results */
			           	while (c.isAfterLast() == false) {
			           		/* Retrieve the values of the Entry
			           		/* the Cursor is pointing to. */
			           		String journal = c.getString(journalsColumn);
			           		/* Add current Entry to results. */
			           		if ( journal != null ) {
			           			async_journals_list.add (journal);
			           		}
			           		c.moveToNext();
			           	}
			           	c.close();
			        }
			        
			        //get first and last year
			        if ( onlyReference ) {
			        	c = db.rawQuery("select min(year) as minyear, max(year) as maxyear from Documents where ((onlyReference='false' or onlyReference is null) and (deletionPending='false' or deletionPending is null))", null);
			        } else {
			        c = db.rawQuery("select min(year) as minyear, max(year) as maxyear from Documents where ((deletionPending='false' or deletionPending is null))", null);
			        }
			        
			        /* Check if our result was valid. */
			        if (c != null) {
			            /* Get the indices of the Columns we will need */
			            int minyearColumn = c.getColumnIndex("minyear");
			            int maxyearColumn = c.getColumnIndex("maxyear");
			        	
			          	c.moveToFirst();
			           	/* Loop through all Results */
			           	if ( !c.isAfterLast() ) {
			           		/* Retrieve the values of the Entry
			           		/* the Cursor is pointing to. */
			           		try {
			           			async_minimum_year = c.getInt(minyearColumn);
			           		} catch ( NumberFormatException e) {
			           			async_minimum_year = 0;
			           		}
			           		try {
			           			async_maximum_year = c.getInt(maxyearColumn);
			           		} catch ( NumberFormatException e) {
			           			async_maximum_year = 2100;
			           		}
			           	}
			           	c.close();
			        }
			        
			        //define comparator
		            Comparator<String> comparator = new Comparator<String>() {
		            	Collator myCollator = Collator.getInstance();
		            	@Override
		            	public int compare(String object1, String object2) {
		            		return myCollator.compare(object1, object2);
		            	}
		            };
		        	//sort
		            Collections.sort(async_tags_list, comparator);
		            Collections.sort(async_authors_list, comparator);
		            Collections.sort(async_journals_list, comparator);

		    		// save large datasets in shared preferences
		    		SharedPreferences settings = getSharedPreferences("SELECT", 0);
		    		SharedPreferences.Editor editor = settings.edit();
		    		editor.clear();
		    		
		    		int list_size=async_tags_list.size();
		    		editor.putInt("tags_list_size",list_size);
		    		for(int i=0;i<list_size;i++)  
		    		{
		    			//editor.remove("tags_list_" + i);
		    			editor.putString("tags_list_" + i, async_tags_list.get(i));  
		    		}
		    		
		    		list_size=async_authors_list.size();
		    		editor.putInt("authors_list_size",list_size);
		    		for(int i=0;i<list_size;i++)  
		    		{
		    			//editor.remove("authors_list_" + i);
		    			editor.putString("authors_list_" + i, async_authors_list.get(i));  
		    		}
		        
		    		list_size=async_journals_list.size();
		    		editor.putInt("journals_list_size",list_size);
		    		for(int i=0;i<list_size;i++)  
		    		{
		    			//editor.remove("journals_list_" + i);
		    			editor.putString("journals_list_" + i, async_journals_list.get(i));  
		    		}
		    		editor.commit();

		            
			    } catch ( SQLiteException e) {
	    			if ( c != null ) {
			           	c.close();
	    			}
	        		db.close();
	    			return null;
	    		}
		    	db.close();
	    	} 
	    	return null;
        }
		
	    @Override
	    protected void onPreExecute() {

        }

	    @Override
	    protected void onPostExecute(Void unused) {
    		completed = true;
            notifyActivityTaskCompleted();
		}
		
	    private void setActivity(list_db activity) {
               this.activity = activity;
               if ( completed ) {
               	notifyActivityTaskCompleted();
               }
        }

	    private void notifyActivityTaskCompleted() {
               if ( null != activity ) {
               	activity.onGetFilterListsCompleted();
               }
        }
   }
    
    

	   /** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) {
		GetPreferences();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);		
		current_sort_by = prefs.getString("current_sort_by", "authors");
        current_collection = prefs.getString("current_collection", "All Documents");
		current_group = prefs.getString("current_group", "0");
		current_reverse = prefs.getString("current_reverse", "");      
		old_sort_by = prefs.getString("old_sort_by", "");
		old_collection = prefs.getString("old_collection", "");
		old_group = prefs.getString("old_group", "");
		old_reverse = prefs.getString("old_reverse", "");
		
				
        if ( dark_theme ) {
    		setTheme(android.R.style.Theme_Black_NoTitleBar);
    	} else {
    		setTheme(android.R.style.Theme_Light_NoTitleBar);
    	}
    	
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null) {
	       		lv_index = savedInstanceState.getInt("lv_index");
			    lv_top = savedInstanceState.getInt("lv_top");
			    tags_selected = savedInstanceState.getStringArrayList("tags_selected");
			    old_tags_selected = savedInstanceState.getStringArrayList("old_tags_selected");
			    authors_selected = savedInstanceState.getStringArrayList("authors_selected");
			    old_authors_selected = savedInstanceState.getStringArrayList("old_authors_selected");
			    journals_selected = savedInstanceState.getStringArrayList("journals_selected");
			    old_journals_selected = savedInstanceState.getStringArrayList("old_journals_selected");
			    current_search = savedInstanceState.getString("current_search");
			    old_search = savedInstanceState.getString("old_search");
				current_sort_by = savedInstanceState.getString("current_sort_by");
		        current_collection = savedInstanceState.getString("current_collection");
				current_group = savedInstanceState.getString("current_group");
				current_reverse = savedInstanceState.getString("current_reverse");      
				old_sort_by = savedInstanceState.getString("old_sort_by");
				old_collection = savedInstanceState.getString("old_collection");
				old_group = savedInstanceState.getString("old_group");
				old_reverse = savedInstanceState.getString("old_reverse");
				minimum_year_selected = savedInstanceState.getInt("minimum_year_selected");
				maximum_year_selected = savedInstanceState.getInt("maximum_year_selected");
				old_minimum_year_selected = savedInstanceState.getInt("old_minimum_year_selected");
				old_maximum_year_selected = savedInstanceState.getInt("old_maximum_year_selected");
		}
		
		RotateData configuration = (RotateData) getLastNonConfigurationInstance();
		if ( configuration != null ) {
			if ( configuration.backgroundtask != null ) {
				backgroundtask = configuration.backgroundtask;
				backgroundtask.setActivity(this);
			} else {
				current_reference_list = configuration.current_reference_list;
			}
			if ( configuration.filterlisttask != null ) {
				filterlisttask = configuration.filterlisttask;
				filterlisttask.setActivity(this);
			} else {
				tags_list = configuration.tags_list;
				authors_list = configuration.authors_list;
				journals_list = configuration.journals_list;
				minimum_year = configuration.minimum_year;
				maximum_year = configuration.maximum_year;
			}
		}
		
        ACTIVITY = this;
        RESTART_INTENT = PendingIntent.getActivity(this.getBaseContext(), 0, new Intent(getIntent()), getIntent().getFlags());
        
		PrepareApp();
		return;
	}

    private void PrepareApp() {
    	
    	//show UI elements
    	setContentView(R.layout.main);
    	
    	//assign views
    	lv= (ListView)findViewById(R.id.listview);
	    final LayoutInflater  inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		lLayout = (LinearLayout)findViewById(R.id.main);
		searchbar = (LinearLayout)inflater.inflate(R.layout.search, null);
	    Spinner s = (Spinner) findViewById(R.id.spinner);
	    sort = (StateButton) findViewById(R.id.StateButton01);
	    tags = (StateButton) findViewById(R.id.StateButton02);
		
		ArrayAdapter<String> collections = new ArrayAdapter<String>(this, R.layout.custom_spinner_text);
    	collections_list.clear();
    	List<HashMap<String, String>> group_list = new ArrayList<HashMap<String, String>>();

	    SQLiteDatabase db = null;

		File file = new File(fileName);
	    if(!file.exists()) { 
	    	Context context = getApplicationContext();  	
			Toast.makeText(context, "Database not found.\n\nPlease enter the correct location in Preferences.\n\nFor setup information press menu key and choose 'Help'.", Toast.LENGTH_LONG).show();
			return;
	    }

		try {
			
			db = SQLiteDatabase.openDatabase(fileName, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READWRITE);
			Cursor c = db.rawQuery("select * from Folders limit 1", null);
	    	if (c != null) {
	    		c.close();
	    	}
			if ( db != null ) {
	    		db.close();
	    	}
		} catch ( SQLiteException e ) {
			
	    	if ( db != null ) {
	    		db.close();
	    	}
	    	
			try {
				RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
				raf.seek(18);
				int format_write = raf.read();
				int format_read = raf.read();
				
				if (format_write == 2 || format_read == 2) {
					raf.seek(18);
					raf.write(1);
					raf.write(1);
					
					Context context = getApplicationContext();  	
					Toast.makeText(context, "Database journal mode changed to ensure compatibility.", Toast.LENGTH_LONG).show();
				}
				raf.close();

			} catch (FileNotFoundException f) {
				Context context = getApplicationContext();  	
				Toast.makeText(context, "Database not found.\n\nPlease enter the correct location in Preferences.\n\nFor setup information press menu key and choose 'Help'.", Toast.LENGTH_LONG).show();
				return;
			} catch (IOException f) {
				Context context = getApplicationContext();  	
				Toast.makeText(context, "Error accessing database.\n\nPlease enter the correct location in Preferences.\n\nFor setup information press menu key and choose 'Help'.", Toast.LENGTH_LONG).show();
				return;
			}
			
		}

		try {
			
			onlyReference=true;
			
			db = SQLiteDatabase.openDatabase(fileName, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READWRITE);
			
			//check presence of onlyReference column
			Cursor c = db.rawQuery("select Documents.id as id from Documents where (onlyReference is null)", null);
		
	    	if (c != null) {
	    		c.close();
	    	}
			if ( db != null ) {
	    		db.close();
	    	}
		} catch ( SQLiteException e ) {
			
	    	if ( db != null ) {
	    		db.close();
	    	}
	    	
			onlyReference = false;
			
		}		
		
		try {

			db = SQLiteDatabase.openDatabase(fileName, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READWRITE);
			
			//get collections
			Cursor c = db.rawQuery("select * from Folders where id in (select folderId from RemoteFolders where groupId=0) and ( parentid=-1 or parentid=0 ) order by lower(name)", null);
	        
	        /* Check if our result was valid. */
	        if (c != null) {
	        	startManagingCursor(c);
	        	
	            /* Get the indices of the Columns we will need */
	            int idColumn = c.getColumnIndex("id");
	        	int nameColumn = c.getColumnIndex("name");
	        	int parentidColumn = c.getColumnIndex("parentId");
 	        	// add entry for all collections and unsorted
	        	collections.add ("All Documents");
 	       		collections.add ("Unsorted");
	        	
	          	c.moveToFirst();
	           	/* Loop through all Results */
	           	while (c.isAfterLast() == false) {
	           		/* Retrieve the values of the Entry
	           		/* the Cursor is pointing to. */
	           		String id = c.getString(idColumn);
	           		String name = c.getString(nameColumn);
	           		String parentid = c.getString(parentidColumn);
	                                              
	           		/* Add current Entry to results. */
	           		HashMap<String, String> map = new HashMap<String, String>();
	           		map.put("id", id);
	           		map.put("name", name);
	           		map.put("group", "0");
	           		map.put("parentid", parentid);
	           		map.put("level", "0");
	           		collections_list.add(map);
	       	      	c.moveToNext();
	           	}
	           	stopManagingCursor(c);
	           	c.close();
	        }

			//get subfolders
			c = db.rawQuery("select * from Folders where id in (select folderId from RemoteFolders where groupId=0) and not parentId=-1 and not parentId=0 order by lower(name)", null);
	        
	        /* Check if our result was valid. */
	        if (c != null) {
	        	startManagingCursor(c);
	           	List<HashMap<String, String>> folder_list = new ArrayList<HashMap<String, String>>();
	            /* Get the indices of the Columns we will need */
	            int idColumn = c.getColumnIndex("id");
	        	int nameColumn = c.getColumnIndex("name");
	        	int parentidColumn = c.getColumnIndex("parentId");

	        	
	          	c.moveToFirst();
	           	/* Loop through all Results */
	           	while (c.isAfterLast() == false) {
	           		/* Retrieve the values of the Entry
	           		/* the Cursor is pointing to. */
	           		String id = c.getString(idColumn);
	           		String name = c.getString(nameColumn);
	           		String parentid = c.getString(parentidColumn);
	                                              
	           		/* Add current Entry to results. */
	           		HashMap<String, String> map = new HashMap<String, String>();
	           		map.put("id", id);
	           		map.put("name", name);
	           		map.put("group", "0");
	           		map.put("parentid", parentid);
	           		folder_list.add(map);
	       	      	c.moveToNext();
	           	}
	           	stopManagingCursor(c);
	           	c.close();
	           	
	           	//sort subfolders to collections
	           	int level = 0;
	           	boolean stall = false; 
	           	while ( folder_list.size() > 0 && stall == false ) {
	           		level++;
	           		stall = true;
	           		int i=0;
	           		while ( i < collections_list.size()) {
	           			String parent = collections_list.get(i).get("id");
	           			int j=0;
	           			while ( j < folder_list.size() ) {
	           				HashMap<String, String> map = folder_list.get(j);
	           				if ( map.get("parentid").equals(parent)) {
	           					//put subfolder in collections_list
	           					stall = false;
	           					i++;
	           					map.put("level", Integer.toString(level));
	           					String name = "";
	           					for (int k=0; k < level; k++) {
	           						name = name + LEVEL_SIGN;
	           					}
	           					name = name + " " + map.get("name");
	           					map.put("name", name);
	           					collections_list.add(i, map);
	           					folder_list.remove(j);
	           					
	           				} else {
	           					j++;
	           				}
	           			}
	           			i++;
	           		}
	           	}

	        	//add subfolders to parents id
	           	for (int l=0; l<=level; l++) {
	           		for (int i=0; i < collections_list.size(); i++) {
	           			if (collections_list.get(i).get("level").equals(Integer.toString(l))) {
	           				int current_level = 0;
	           				int j=i+1;
	           				boolean next_item_reached = false;
	           				while (!next_item_reached && (j < collections_list.size())) {
		           				try {
		           					current_level = Integer.parseInt(collections_list.get(j).get("level"));
		           				} catch(NumberFormatException nfe) {
		           					current_level = 0;
		           				}
		           				if (current_level > l) {
		           					HashMap<String, String> map = collections_list.get(i);
		           					String id = map.get("id");
		           					id = id + "," + collections_list.get(j).get("id");
		           					map.put("id", id);
		           					collections_list.set(i, map);
		           				} else {
		           					next_item_reached = true;
		           				}
		           				j++;
	           				}
	           			}	

	           		}
	           	}
      	
	        }
        	
 	        //get groups
			c = db.rawQuery("select * from Groups where id != 0 order by lower(name)", null);
	        
	        /* Check if our result was valid. */
	        if (c != null) {
	        	startManagingCursor(c);
	        	group_list.clear();
	            /* Get the indices of the Columns we will need */
	            int idColumn = c.getColumnIndex("id");
	        	int nameColumn = c.getColumnIndex("name");
	       			        	
	          	c.moveToFirst();
	           	/* Loop through all Results */
	           	while (c.isAfterLast() == false) {
	           		/* Retrieve the values of the Entry
	           		/* the Cursor is pointing to. */
	           		String id = c.getString(idColumn);
	           		String name = c.getString(nameColumn);
	                                              
	           		/* Add current Entry to results. */
	           		HashMap<String, String> map = new HashMap<String, String>();
	           		map.put("id", "All Documents");
	           		map.put("name", "Group: " + name);
	           		map.put("group", id);
	           		map.put("parentid", "none");
	           		map.put("level", "0");
	           		group_list.add(map);
	       	      	c.moveToNext();
	           	}
	           	stopManagingCursor(c);
	           	c.close();
	        }



	        //run through all groups
	        for (int current_group=0; current_group < group_list.size(); current_group++) {
	           	List<HashMap<String, String>> group_folder_list = new ArrayList<HashMap<String, String>>();
	           	
				//get top folders of current group
				c = db.rawQuery("select * from Folders where id in (select folderId from RemoteFolders where groupId=" + group_list.get(current_group).get("group") + ") and parentid<1 order by lower(name)", null);
		        
		        /* Check if our result was valid. */
		        if (c != null) {
		        	startManagingCursor(c);
		        	
		            /* Get the indices of the Columns we will need */
		            int idColumn = c.getColumnIndex("id");
		        	int nameColumn = c.getColumnIndex("name");
		        	int parentidColumn = c.getColumnIndex("parentId");
		        	
		          	c.moveToFirst();
		           	/* Loop through all Results */
		           	while (c.isAfterLast() == false) {
		           		/* Retrieve the values of the Entry
		           		/* the Cursor is pointing to. */
		           		String id = c.getString(idColumn);
		           		String name = c.getString(nameColumn);
		           		String parentid = c.getString(parentidColumn);
		                                              
		           		/* Add current Entry to results. */
		           		HashMap<String, String> map = new HashMap<String, String>();
		           		map.put("id", id);
		           		map.put("name", LEVEL_SIGN + " " + name);
		           		map.put("group", group_list.get(current_group).get("group"));
		           		map.put("parentid", parentid);
		           		map.put("level", "1");
		           		group_folder_list.add(map);
		       	      	c.moveToNext();
		           	}
		           	stopManagingCursor(c);
		           	c.close();
		        }
		        
	        	//get subfolders of current group
	        	c = db.rawQuery("select * from Folders where id in (select folderId from RemoteFolders where groupId=" + group_list.get(current_group).get("group") + ") and not parentId=-1 order by lower(name)", null);
		        
		        /* Check if our result was valid. */
		        if (c != null) {
		        	startManagingCursor(c);
		           	List<HashMap<String, String>> folder_list = new ArrayList<HashMap<String, String>>();
		            /* Get the indices of the Columns we will need */
		            int idColumn = c.getColumnIndex("id");
		        	int nameColumn = c.getColumnIndex("name");
		        	int parentidColumn = c.getColumnIndex("parentId");

		        	
		          	c.moveToFirst();
		           	/* Loop through all Results */
		           	while (c.isAfterLast() == false) {
		           		/* Retrieve the values of the Entry
		           		/* the Cursor is pointing to. */
		           		String id = c.getString(idColumn);
		           		String name = c.getString(nameColumn);
		           		String parentid = c.getString(parentidColumn);
		                                              
		           		/* Add current Entry to results. */
		           		HashMap<String, String> map = new HashMap<String, String>();
		           		map.put("id", id);
		           		map.put("name", name);
		           		map.put("group", group_list.get(current_group).get("group"));
		           		map.put("parentid", parentid);
		           		folder_list.add(map);
		       	      	c.moveToNext();
		           	}
		           	stopManagingCursor(c);
		           	c.close();
		           	
		           	//sort subfolders to groupfolders
		           	int level = 1;
		           	boolean stall = false; 
		           	while ( folder_list.size() > 0 && stall == false ) {
		           		level++;
		           		stall = true;
		           		int i=0;
		           		while ( i < group_folder_list.size()) {
		           			String parent = group_folder_list.get(i).get("id");
		           			int j=0;
		           			while ( j < folder_list.size() ) {
		           				HashMap<String, String> map = folder_list.get(j);
		           				if ( map.get("parentid").equals(parent)) {
		           					//put subfolder in collections_list
		           					stall = false;
		           					i++;
		           					map.put("level", Integer.toString(level));
		           					String name = "";
		           					for (int k=0; k < level; k++) {
		           						name = name + LEVEL_SIGN;
		           					}
		           					name = name + " " + map.get("name");
		           					map.put("name", name);
		           					group_folder_list.add(i, map);
		           					folder_list.remove(j);
		           					
		           				} else {
		           					j++;
		           				}
		           			}
		           			i++;
		           		}
		           	}

		        	//add subfolders to parents id
		           	for (int l=1; l<=level; l++) {
		           		for (int i=0; i < group_folder_list.size(); i++) {
		           			if (group_folder_list.get(i).get("level").equals(Integer.toString(l))) {
		           				int current_level = 0;
		           				int j=i+1;
		           				boolean next_item_reached = false;
		           				while (!next_item_reached && (j < group_folder_list.size())) {
			           				try {
			           					current_level = Integer.parseInt(group_folder_list.get(j).get("level"));
			           				} catch(NumberFormatException nfe) {
			           					current_level = 0;
			           				}
			           				if (current_level > l) {
			           					HashMap<String, String> map = group_folder_list.get(i);
			           					String id = map.get("id");
			           					id = id + "," + group_folder_list.get(j).get("id");
			           					map.put("id", id);
			           					group_folder_list.set(i, map);
			           				} else {
			           					next_item_reached = true;
			           				}
			           				j++;
		           				}
		           			}	

		           		}
		           	}
	      	
		        }
		        //add current group to collections_list
		        collections_list.add(group_list.get(current_group));
		        collections_list.addAll(group_folder_list);

	        }
	        group_list.clear();
	        //transfer group+collection names to collections
	        
       		for (int i=0; i < collections_list.size(); i++) {
       			collections.add (collections_list.get(i).get("name"));
       		}

	        
	        collections.setDropDownViewResource(android.R.layout.simple_spinner_item);
		    s.setAdapter(collections);

			for ( int i=0; i < collections_list.size(); i++ ) {
	       		HashMap<String, String> map = new HashMap<String, String>();
	       		map = collections_list.get(i); 
	       		if ( current_collection.equals(map.get("id")) && current_group.equals(map.get("group")) ) {
	       			int position = i;
	       		    s.setSelection(position+2);
	       		}
			}
	        
	    	s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
	    		@Override
			    public void onItemSelected(AdapterView<?> a, View arg1, int position, long arg3) {
	    			if (position == 0) {
	    				current_collection = "All Documents";
	    				current_group = "0";
		   			} else if (position == 1) {
	    				current_collection = "Unsorted";
	    				current_group = "0";
		   			} else {
		   				current_collection = collections_list.get(position-2).get("id");
		   				current_group = collections_list.get(position-2).get("group");
		   			}
	       	    	/*Context context = getApplicationContext();
	    			Toast.makeText(context, "selected " + current_collection, Toast.LENGTH_SHORT).show();*/
	    			StartBackgroundTask();
			    }
			    public void onNothingSelected(AdapterView<?> parent) {
			    }
	    	});
	    	
	    	s.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View view) {
                	if ( current_reference_list != null) {
                		ShowToast();
                	}
					return true;
                }
	    	});

	    	setSortButtonState();
	    	
	    	sort.setOnClickListener(new View.OnClickListener() {
	                 public void onClick(View view) {
	                 	Intent myIntent = new Intent(view.getContext(), Sort.class);
	                 	myIntent.putExtra("com.kmk.Referey.dark_theme", dark_theme);
	                 	myIntent.putExtra("com.kmk.Referey.current_sort_by", current_sort_by);
	                 	myIntent.putExtra("com.kmk.Referey.current_reverse", current_reverse);
	                    startActivityForResult(myIntent, GET_SORT_OPTIONS);
	                 }
	        });
	    	
	    	sort.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View view) {
                	if ( current_reference_list != null) {
                		ShowToast();
                	}
					return true;
                }
	    	});
   	
	    	//set correct state of tag button
	    	setSelectButtonState();
	    	
	    	//check here if tag button should be disabled or not
	    	tags.setEnabled(false);
   	    		    	
	    	searchbar.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View view) {
	            	lLayout.removeView(searchbar);
	            	current_search="";
	            	StartBackgroundTask();
	            }
	    	});
		}
		catch ( SQLiteException e ) {
			Context context = getApplicationContext();  	
			Toast.makeText(context, "Error accessing database.\n\nPlease enter the correct location in Preferences.\n\nFor setup information press menu key and choose 'Help'.", Toast.LENGTH_LONG).show();
			// Launch Preference activity
			//Intent myIntent = new Intent(context, Preferences.class);
         	//myIntent.putExtra("com.kmk.Referey.dark_theme", dark_theme);
			//startActivity(myIntent);
		}
    	if ( db != null ) {
    		db.close();
    	}
		return;
    }
    
    @Override
	public void onStart() {
		super.onStart();
		String old_fileName = fileName;
		String old_pathfile = path_file;
		int old_preserve_path = preserve_path;
		boolean old_alternative_path = alternative_path;
		int old_remove_path = remove_path;
		int old_font_size = font_size;
		boolean old_dark_theme = dark_theme;
		GetPreferences();
		
		if ( old_dark_theme != dark_theme ) {
			AlarmManager mgr = (AlarmManager)ACTIVITY.getSystemService(Context.ALARM_SERVICE);
			mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, RESTART_INTENT);
			System.exit(2);
		} else if (!fileName.equals(old_fileName) || !path_file.equals(old_pathfile) || (old_preserve_path != preserve_path) || (old_alternative_path != alternative_path) || (old_remove_path != remove_path) || (old_font_size != font_size) ) {
			current_reference_list=null;
		    current_collection = "All Documents";
		    old_collection="";
		    current_group = "0";
		    old_group = "";
		    current_sort_by = "authors";
		    old_sort_by="";
			current_reverse = "";
			old_reverse="";
			lv_top=0;
			lv_index=0;
			tags_selected.clear();
			old_tags_selected.clear();
			authors_selected.clear();
			old_authors_selected.clear();
			journals_selected.clear();
			old_journals_selected.clear();
			tags_list = null;
			journals_list = null;
			authors_list = null;
			minimum_year = -1;
			maximum_year = -1;
			current_search = "";
			old_search = "";
			minimum_year_selected = -1;
			maximum_year_selected = -1;
			old_minimum_year_selected = -1;
			old_maximum_year_selected = -1;

			PrepareApp();
		} 
    }
    
    
    private void GetPreferences() {
    	Context context = getApplicationContext();  	
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		fileName = prefs.getString("sqlfilename", "/sdcard/database.sqlite");
		path_file = prefs.getString("pdf_folder", "/sdcard/referey");
		String preserve_path_string = prefs.getString("preserve_path", "1");
		alternative_path = prefs.getBoolean("alternative_path", false);
		String remove_path_string = prefs.getString("remove_path", "0");
		String font_size_string = prefs.getString("font_size", "14");
		dark_theme = prefs.getBoolean("theme", false);
		
		try {
			preserve_path = Integer.parseInt (preserve_path_string);
			if ( preserve_path < 1 ) {
				preserve_path = 1;
				Toast.makeText(context, "Invalid value for 'Preserve PDF path' entered in preferences. Using default value: 'file.pdf'", Toast.LENGTH_LONG).show();
			}
		}
		catch ( NumberFormatException e) {
			preserve_path = 1;
			Toast.makeText(context, "Invalid value for 'Preserve PDF path' entered in preferences. Using default value: 'file.pdf'", Toast.LENGTH_LONG).show();
			
		}
		
		try {
			remove_path = Integer.parseInt (remove_path_string);
			if ( remove_path < 0 ) {
				remove_path = 0;
				Toast.makeText(context, "Invalid value for 'Remove PDF path' entered in preferences. Using default value: 0", Toast.LENGTH_LONG).show();
			}

		}
		catch ( NumberFormatException e) {
			remove_path = 0;
			Toast.makeText(context, "Invalid value for 'Remove PDF path' entered in preferences. Using default value: 0", Toast.LENGTH_LONG).show();
		}
		
		try {
			font_size = Integer.parseInt (font_size_string);
		}
		catch ( NumberFormatException e) {
			font_size = 14;
		}
    }
    
    private void setSortButtonState() {
		//set correct state of sort button
		String sort_by_button = "";
		if ( current_sort_by.equals("authors") ) {
			sort_by_button = "Auth";
		} else if ( current_sort_by.equals("title") ) {
			sort_by_button = "Title";
		} else if ( current_sort_by.equals("year") ) {
			sort_by_button = "Year";
		} else if ( current_sort_by.equals("publication") ) {
			sort_by_button = "Pub";
		} else if ( current_sort_by.equals("added") ) {
			sort_by_button = "Add";
		} else if ( current_sort_by.equals("favourite") ) {
			sort_by_button = "Fav";
		} else if ( current_sort_by.equals("read") ) {
			sort_by_button = "Read";
		}
		sort.setTextOff(sort_by_button);
		sort.setTextOn(sort_by_button);
	
		if ( current_reverse.equals("desc") ) {
			sort.setChecked(false);
		} else { 
			sort.setChecked(true);
		}
    }
    
    private void setSelectButtonState() {
		//set correct state of select button
		if ( (tags_selected.size() != 0) || (authors_selected.size() != 0) || (journals_selected.size() != 0) || ( (minimum_year_selected != -1) && (maximum_year_selected != -1) ) ) {
			tags.setChecked(true);
		} else { 
			tags.setChecked(false);
		}
    }


    
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			current_search = intent.getStringExtra(SearchManager.QUERY);
			StartBackgroundTask();
		}
    }
    
    @Override
    protected void onPause() {
      //save current_parameters
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
       
        editor.putString("current_sort_by", current_sort_by);
        editor.putString("current_collection", current_collection);
        editor.putString("current_group", current_group);
        editor.putString("current_reverse", current_reverse);      
        editor.putString("old_sort_by", old_sort_by);
        editor.putString("old_collection", old_collection);
        editor.putString("old_group", old_group);
        editor.putString("old_reverse", old_reverse);

        // Commit the edits!
    	editor.commit();
    	
    	if ( pdia != null && pdia.isShowing() ) {
	    	pdia.dismiss();
    	}
    	
        //call super
    	super.onPause();
    }

    @Override
    protected void onResume() {    
        //call super
    	super.onResume();

    	if (backgroundtask != null && backgroundtask.getStatus() != QueryDatabaseTask.Status.FINISHED) {
    	    if ( pdia == null || !pdia.isShowing() ) {
    	    	pdia = ProgressDialog.show(this, "", "Updating references...", true, false);
        		pdia.setOwnerActivity(this);
    		}
    	}
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
      // Save UI state changes to the savedInstanceState.
      // This bundle will be passed to onCreate if the process is
      // killed and restarted.
    	
        lv_index = lv.getFirstVisiblePosition();
        View v = lv.getChildAt(0);
        lv_top = (v == null) ? 0 : v.getTop();
        savedInstanceState.putInt("lv_index", lv_index);
        savedInstanceState.putInt("lv_top", lv_top);
 		        
        if ( FileDialog != null && FileDialog.isShowing() ) {
        	FileDialog.dismiss();
        }
        
        savedInstanceState.putStringArrayList("tags_selected", tags_selected);
        savedInstanceState.putStringArrayList("old_tags_selected", old_tags_selected);
        savedInstanceState.putStringArrayList("authors_selected", authors_selected);
        savedInstanceState.putStringArrayList("old_authors_selected", old_authors_selected);
        savedInstanceState.putStringArrayList("journals_selected", journals_selected);
        savedInstanceState.putStringArrayList("old_journals_selected", old_journals_selected);
        savedInstanceState.putString("current_search", current_search);  
        savedInstanceState.putString("old_search", old_search);  
        savedInstanceState.putString("current_sort_by", current_sort_by);
        savedInstanceState.putString("current_collection", current_collection);
        savedInstanceState.putString("current_group", current_group);
        savedInstanceState.putString("current_reverse", current_reverse);      
        savedInstanceState.putString("old_sort_by", old_sort_by);
        savedInstanceState.putString("old_collection", old_collection);
        savedInstanceState.putString("old_group", old_group);
        savedInstanceState.putString("old_reverse", old_reverse);   
        savedInstanceState.putInt("minimum_year_selected", minimum_year_selected);
        savedInstanceState.putInt("maximum_year_selected", maximum_year_selected);
        savedInstanceState.putInt("old_minimum_year_selected", old_minimum_year_selected);
        savedInstanceState.putInt("old_maximum_year_selected", old_maximum_year_selected);
		
        super.onSaveInstanceState(savedInstanceState);
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	RotateData configuration = new RotateData();
    	if (backgroundtask != null && backgroundtask.getStatus() != QueryDatabaseTask.Status.FINISHED) {
            backgroundtask.setActivity(null);
            configuration.backgroundtask = backgroundtask;
            configuration.current_reference_list = null;
		} else {
			configuration.backgroundtask = null;
			configuration.current_reference_list = current_reference_list;
		}
    	if (filterlisttask != null && filterlisttask.getStatus() != GetFilterLists.Status.FINISHED) {
            filterlisttask.setActivity(null);
            configuration.filterlisttask = filterlisttask;
            configuration.tags_list = null;
            configuration.authors_list = null;
            configuration.journals_list = null;
            configuration.minimum_year = -1;
            configuration.maximum_year = -1;
		} else {
	        configuration.filterlisttask = null;
	        configuration.tags_list = tags_list;
	        configuration.authors_list = authors_list;
	        configuration.journals_list = journals_list;
	        configuration.minimum_year = minimum_year;
	        configuration.maximum_year = maximum_year;
		}
   	   	return configuration;
    }

	public void Openfile (String id) {
    	String localUrl = "";
    	String android_file="";
    	ArrayAdapter<String> file_list_short = new ArrayAdapter<String>(this, R.layout.menu_items);
    	file_list.clear();
    	Context context = getApplicationContext();    
    	SQLiteDatabase db = null;
    	
    	try {
	    	db = SQLiteDatabase.openDatabase(fileName, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READWRITE);
	        Cursor c = db.rawQuery("select localUrl from Files where hash in (select hash from DocumentFiles where documentid=" + id + ")", null);
	        
	    	/* Check if our result was valid. */
	        if (c != null) {
	        	startManagingCursor(c);
	            /* Get the indices of the Columns we will need */
	            int localUrlColumn = c.getColumnIndex("localUrl");
	            if (localUrlColumn >= 0) {
	            	c.moveToFirst();
	           		/* Loop through all Results */
	            	while (c.isAfterLast() == false) {
	            		/* Retrieve the values of the Entry
	           			/* the Cursor is pointing to. */
	            		localUrl = c.getString(localUrlColumn);
	                	if (localUrl != "") {
	                		// remove initial "file:///"
	                		localUrl = localUrl.replaceFirst("file:///", "");
	                		// remove path from file location
	                    	String[] list = TextUtils.split(localUrl, "/");
	
	                    	if ( alternative_path == false ) {
	                    		// original method
		                    	if ( preserve_path > list.length ) {
		                    		Toast.makeText(context, "ERROR: File path has fewer levels than requested to preserve", Toast.LENGTH_SHORT).show();
		                    		return;
		                    	}
		                    	//create URI
		                    	if ( path_file.startsWith("dropbox://") ) {
		                        	android_file = path_file;
		                    		android_file = android_file.replaceFirst("dropbox://", "content://com.dropbox.android.Dropbox/metadata/");
		                    	} else {
			                    	android_file = path_file;
		                    	}
		                    	if ( (android_file.length() != 0) && (android_file.charAt(android_file.length()-1) == '/') ) {
		                    		android_file = (String) android_file.subSequence(0, android_file.length()-2);
		                    	}
		                    	for (int i = list.length - preserve_path; i < list.length; i++) {
		                    		android_file = android_file + "/" + list[i];
		                    	}
		                    	android_file = Uri.decode(android_file);
		                		file_list.add(android_file);
		                    	file_list_short.add(Uri.decode(list[list.length-1]));
	                    	} else {
	                    		//alternative method
	                    		int remove_path_use = remove_path;
	                    		
	                    		if (list[0].matches("[a-zA-Z]:") ) {
	                    			remove_path_use++;
	                    		}
	                    		if ( remove_path_use >= list.length ) {
		                    		Toast.makeText(context, "ERROR: File path has fewer levels than requested to remove", Toast.LENGTH_SHORT).show();
		                    		return;
		                    	}
		                    	//create URI
		                    	if ( path_file.startsWith("dropbox://") ) {
		                        	android_file = path_file;
		                    		android_file = android_file.replaceFirst("dropbox://", "content://com.dropbox.android.Dropbox/metadata/");
		                    	} else {
			                    	android_file = path_file;
		                    	}
		                    	if ( (android_file.length() != 0) && (android_file.charAt(android_file.length()-1) == '/') ) {
		                    		android_file = (String) android_file.subSequence(0, android_file.length()-2);
		                    	}
		                    	for (int i = remove_path_use; i < list.length; i++) {
		                    		android_file = android_file + "/" + list[i];
		                    	}
		                    	android_file = Uri.decode(android_file);
		                		file_list.add(android_file);
		                    	file_list_short.add(Uri.decode(list[list.length-1]));
	                    	}
	                	}
	            		c.moveToNext();
	            	}
		            stopManagingCursor(c);
		           	c.close();
		           	
	            	if ( file_list.size() == 1 ) { 
	            		OpenFileIntent(file_list.get(0));
	            	} else if ( file_list.size() > 1 ) {
	            		FileDialog=new AlertDialog.Builder(this) 
	            		.setTitle("Chose file") 
	            		.setSingleChoiceItems(file_list_short, 0, new DialogInterface.OnClickListener() 
	            		{ 
	            	        public void onClick(DialogInterface dlg, int which) { 
	            	        	OpenFileIntent(file_list.get(which));
	            	        } 
	            		}).show(); 
	            		FileDialog.setOwnerActivity(this);
	            	} else {
	            		Toast.makeText(context, "No file associated", Toast.LENGTH_SHORT).show();
	            	}
	            } else {
	            		Toast.makeText(context, "No file associated", Toast.LENGTH_SHORT).show();
	            }
	        } else {
	        	Toast.makeText(context, "Error accessing database", Toast.LENGTH_SHORT).show();
	        }
    	} catch (SQLiteException e ) {
        	Toast.makeText(context, "Error accessing database", Toast.LENGTH_SHORT).show();
    	}
    	if ( db != null ) {
    		db.close();
    	}
    	return;
    }
 	
	public void OpenFileIntent(String android_file) {
		Context context = getApplicationContext();    	
		
		if ( android_file.startsWith("content://com.dropbox.android.Dropbox/metadata/") ) {
			int cur_size = 0;
			boolean no_dropbox = false;
			Cursor cur = null;
			try {
				cur = managedQuery(Uri.parse(android_file), null, null, null, null);
				if ( cur != null ) {
					cur_size = cur.getCount();
					stopManagingCursor(cur);
					cur.close();
				} else {
					no_dropbox = true;
				}
			} catch (SecurityException e) {
				//Toast.makeText(context, "Cannot access Dropbox provider. Try to open file anyway...",Toast.LENGTH_SHORT).show();
				no_dropbox = false;
				cur_size = 1;
			}
			if ( no_dropbox == false ) {
		        if (cur_size == 0 ) {
		        	Toast.makeText(context, "Dropbox file not found: " + android_file + "\n\nYou may need to start the Dropbox app and refresh its file list.", Toast.LENGTH_SHORT).show();
		        } else {
					Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(android_file));
					intent.setClassName("com.dropbox.android","com.dropbox.android.activity.DropboxBrowser");
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					try {
						startActivity(intent);
					} 
					catch (ActivityNotFoundException e) {
						Toast.makeText(context, "Cannot open " + android_file + " with Dropbox",Toast.LENGTH_SHORT).show();
					}
		        }
			} else {
				Toast.makeText(context, "Cannot open Dropbox application",Toast.LENGTH_SHORT).show();
			}
		} else {
			//get mime type
			String[] list_extension = TextUtils.split(android_file, "\\.");
			String extension = list_extension[list_extension.length-1];
			
		   	MimeTypeMap tMimeType = MimeTypeMap.getSingleton();
			String mime= tMimeType.getMimeTypeFromExtension(extension);
			
			File file = new File(android_file);
		
			if (file.exists()) {
				Intent intent;
				Uri path = Uri.fromFile(file);
				intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(path, mime);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				try {
					startActivity(intent);
				} 
				catch (ActivityNotFoundException e) {
					Toast.makeText(context, "No application available to view file " + android_file,Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(context, "File not found: " + android_file, Toast.LENGTH_SHORT).show();
			}
		}
	}

    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
   		if (resultCode == RESULT_OK) {
   			Bundle extras =  data.getExtras();
	   				
			switch ( requestCode ) {
	    	case GET_SORT_OPTIONS:
	    		if ( extras != null ) {
	            	current_sort_by = extras.getString("com.kmk.Referey.current_sort_by");
	            	current_reverse = extras.getString("com.kmk.Referey.current_reverse");
	            	setSortButtonState();
	            	//update reference list
	            	StartBackgroundTask();		
				}
	    		break;
	    	case GET_TAGS_SELECTED:
	    		if ( extras != null ) {
	    				tags_selected = extras.getStringArrayList("com.kmk.Referey.tags_selected");
	    				authors_selected = extras.getStringArrayList("com.kmk.Referey.authors_selected");
	    				journals_selected = extras.getStringArrayList("com.kmk.Referey.journals_selected");
	    				minimum_year_selected = extras.getInt("com.kmk.Referey.minimum_year_selected");
	    				maximum_year_selected = extras.getInt("com.kmk.Referey.maximum_year_selected");
	    				setSelectButtonState();
		            	StartBackgroundTask();
	    		}
	    		break;
	     	}
   		}
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	// This method is called once the menu is selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.preferences:
			// Launch Preference activity
			Intent myIntent = new Intent(this, Preferences.class);
			myIntent.putExtra("com.kmk.Referey.dark_theme", dark_theme);
			startActivity(myIntent);
			break;
		case R.id.search:
			onSearchRequested();
			break;
		case R.id.help:
			// Launch help activity
			Intent intent = new Intent(this, HelpActivity.class);
			startActivity(intent);
			break;
		}
		return true;
	}
	
	protected void StartBackgroundTask() {
		if (backgroundtask == null || backgroundtask.getStatus() == QueryDatabaseTask.Status.FINISHED) {
			backgroundtask = new QueryDatabaseTask(this);                 
	        backgroundtask.execute();
		}
	}

	private void onTaskCompleted() {
	    String[] from = new String[] {"authors", "title", "citation"};
		int[] to = new int[] { R.id.item1, R.id.item2, R.id.item3 };
	
	    current_reference_list = backgroundtask.getReferences();
	
		Context context = getApplicationContext();
		if ( current_reference_list == null) {
			Toast.makeText(context, "Error accessing database", Toast.LENGTH_SHORT).show();
			SpecialAdapter adapter = new SpecialAdapter(context, new ArrayList<HashMap<String, String>>(), R.layout.grid_item, from, to, dark_theme, font_size);
			lv.setAdapter(adapter);
		} else {
			SpecialAdapter adapter = new SpecialAdapter(context, current_reference_list, R.layout.grid_item, from, to, dark_theme, font_size);
				lv.setAdapter(adapter);
				lv.setSelectionFromTop(lv_index, lv_top);
				
			old_collection = current_collection;
			old_group = current_group;
			old_sort_by = current_sort_by;
			old_reverse = current_reverse;
			old_tags_selected = new ArrayList<String>(tags_selected);
			old_authors_selected = new ArrayList<String>(authors_selected);
			old_journals_selected = new ArrayList<String>(journals_selected);
			old_search = current_search;
			old_minimum_year_selected = minimum_year_selected;
			old_maximum_year_selected = maximum_year_selected;
			
			ShowToast();
				
			lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View arg1, int position, long arg3) { 
				String id = current_reference_list.get(position).get("id");
				Intent myIntent = new Intent(arg1.getContext(), ReferenceDetails.class);
				myIntent.putExtra("com.kmk.Referey.id", id);
		     	myIntent.putExtra("com.kmk.Referey.dark_theme", dark_theme);
		     	myIntent.putExtra("com.kmk.Referey.fileName", fileName);
		     	myIntent.putExtra("com.kmk.Referey.path_file", path_file);
		     	myIntent.putExtra("com.kmk.Referey.current_search", current_search);
		     	myIntent.putExtra("com.kmk.Referey.preserve_path", preserve_path);
				myIntent.putExtra("com.kmk.Referey.alternative_path", alternative_path);
				myIntent.putExtra("com.kmk.Referey.remove_path", remove_path);
		     	myIntent.putExtra("com.kmk.Referey.font_size", font_size);
		        startActivityForResult(myIntent, SHOW_REFERENCE_DETAILS);
			}	
			});
			lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> a, View arg1, int position, long arg3) { 
				String id = current_reference_list.get(position).get("id");
				Openfile (id);
				return true;
			}
			});
			
			SetupTagsButton();
		}

	    if ( pdia != null && pdia.isShowing() ) {
	    	pdia.dismiss();
		}
	
	}
	
	private void onGetFilterListsCompleted() {
		tags_list = filterlisttask.get_tags_list();
		authors_list = filterlisttask.get_authors_list();
		journals_list = filterlisttask.get_journals_list();
		minimum_year = filterlisttask.get_minimum_year();
		maximum_year = filterlisttask.get_maximum_year();
		
		/*Context context = getApplicationContext();
		Toast.makeText(context, "Filter lists completed\nminimum year: " + minimum_year + ", maximum year: " + maximum_year, Toast.LENGTH_SHORT).show();*/
		
		SetupTagsButton();
	}
	
	private void SetupTagsButton() {
		if (tags_list == null || authors_list == null || journals_list == null ) {
			if (filterlisttask == null || filterlisttask.getStatus() == GetFilterLists.Status.FINISHED) {
				filterlisttask = new GetFilterLists(this);                 
		        filterlisttask.execute();
			} 
    	    tags.setEnabled(false);
	        tags.setBackgroundResource(R.anim.toggle_tag_animation);
	        rotate = (AnimationDrawable) tags.getBackground();
	        rotate.start();
    	} else {
    		if ( rotate != null ) {
	    		rotate.stop();
	    	}
	    	tags.setBackgroundResource(R.drawable.toggletag);
			tags.setEnabled(true);
			tags.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View view) {

            		Intent intent = new Intent(view.getContext(), Select.class);
/*	    			intent.putStringArrayListExtra("com.kmk.Referey.tags_list", tags_list);
    				intent.putStringArrayListExtra("com.kmk.Referey.authors_list", authors_list);
    				intent.putStringArrayListExtra("com.kmk.Referey.journals_list", journals_list);*/
            		intent.putStringArrayListExtra("com.kmk.Referey.tags_selected", tags_selected);
            		intent.putStringArrayListExtra("com.kmk.Referey.authors_selected", authors_selected);
            		intent.putStringArrayListExtra("com.kmk.Referey.journals_selected", journals_selected);
            		intent.putExtra("com.kmk.Referey.minimum_year", minimum_year);
            		intent.putExtra("com.kmk.Referey.maximum_year", maximum_year);
            		intent.putExtra("com.kmk.Referey.minimum_year_selected", minimum_year_selected);
            		intent.putExtra("com.kmk.Referey.maximum_year_selected", maximum_year_selected);
            		intent.putExtra("com.kmk.Referey.dark_theme", dark_theme);
	    	        	    	        	    			
            		startActivityForResult(intent, GET_TAGS_SELECTED);
	            }
	    	});
			
	    	tags.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View view) {
                	if ( current_reference_list != null) {
                		ShowToast();
                	}
					return true;
                }
	    	});
    	}
	}
	
	private void ShowToast() {
		String showing;  
		Context context = getApplicationContext();
		
		if (current_reference_list.size() == 1){
			showing = "Showing 1 reference";
		} else {
			showing = "Showing " + current_reference_list.size() + " references";
		}
		
		if ( tags_selected.size() > 0 ) {
			showing = showing + "\n\ntagged ";
			int i;
			for (i=0; i < tags_selected.size()-1; i++ ) {
				showing = showing + tags_selected.get(i) +"; "; 
			}
			showing = showing + tags_selected.get(i);
		}
		
		if ( authors_selected.size() > 0 ) {
			showing = showing + "\n\nauthored by ";
			int i;
			for (i=0; i < authors_selected.size()-1; i++ ) {
				showing = showing + authors_selected.get(i) +"; "; 
			}
			showing = showing + authors_selected.get(i);
		}
		
   		
		if ( journals_selected.size() > 0 ) {
			showing = showing + "\n\npublished in ";
			int i;
			for (i=0; i < journals_selected.size()-1; i++ ) {
				showing = showing + journals_selected.get(i) +"; "; 
			}
			showing = showing + journals_selected.get(i);
		}

		if ( (minimum_year_selected != -1) && (maximum_year_selected != -1) ) {
			showing = showing + "\n\nbetween " + minimum_year_selected + " and " + maximum_year_selected;
		}

		Toast.makeText(context, showing, Toast.LENGTH_LONG).show();
	}

}
