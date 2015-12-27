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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ReferenceDetails extends Activity {
	String id="";
	boolean dark_theme = true;
	String fileName = "";
	String path_file = "";
	String current_search = "";
	int preserve_path = 1;
	boolean alternative_path;
	int remove_path = 0;
	int font_size = 15;
	List<String> file_list = new ArrayList<String>();
	static final int OPEN_FILE = 0;
	static final int SEND_FILE = 1;
	int task;
	String authors="";
	String title="";
	String reference="";
	AlertDialog FileDialog = null;	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		SQLiteDatabase db = null;
		
		Bundle extras = getIntent().getExtras();
		if(extras != null)
		{
			id = extras.getString("com.kmk.Referey.id");
			dark_theme = extras.getBoolean("com.kmk.Referey.dark_theme");
			fileName = extras.getString("com.kmk.Referey.fileName");
			path_file = extras.getString("com.kmk.Referey.path_file");
			current_search = extras.getString("com.kmk.Referey.current_search");
			preserve_path = extras.getInt("com.kmk.Referey.preserve_path");
			alternative_path = extras.getBoolean("com.kmk.Referey.alternative_path");
			remove_path = extras.getInt("com.kmk.Referey.remove_path");
			font_size = extras.getInt("com.kmk.Referey.font_size");
		} else {
			setResult(RESULT_CANCELED);
          	finish();
		}
		
	    if ( dark_theme ) {
    		setTheme(android.R.style.Theme_Black_NoTitleBar);
    	} else {
    		setTheme(android.R.style.Theme_Light_NoTitleBar);
    	}
	    super.onCreate(savedInstanceState);

		String[] column_excluded = {"title", "authors", "lastname", "firstnames", "publication", "year", "volume", "issue", "pages", "abstract", "note", "id", "confirmed", "deletionpending", "onlyreference", "uuid", "modified", "importer", "privacy", "contribution", "documentid", "citationkey", "added"};

		
		setContentView(R.layout.reference_details);
		
		SpannableStringBuilder details = new SpannableStringBuilder();
		
		try {
			db = SQLiteDatabase.openDatabase(fileName, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
	        Cursor c = db.rawQuery("select *, group_concat(firstNames ||' '|| lastName, ', ') as authors from Documents left outer join DocumentContributors on Documents.id = DocumentContributors.documentid where Documents.id=" + id + " and (DocumentContributors.contribution='DocumentAuthor' or DocumentContributors.contribution is NULL) group by Documents.id", null);
	        Context context = getApplicationContext();    	
	        
	    	/* Check if our result was valid. */
	        if (c != null) {
	        	startManagingCursor(c);
	        	c.moveToFirst();
	        	if (!c.isAfterLast()) {
		        	int last_length = 0;
		        	reference = "";
		          	String publication = getString(c, "publication");
		        	if ( publication != null ) {
		        		reference = publication;
		          	}
		        	String year = getString(c, "year");
		        	if ( year != null ) {
		        		reference = reference + " " + year;
		          	}
		        	String volume = getString(c, "volume");
		        	if ( volume != null ) {
		        		reference = reference + ";" + volume;
		          	}
		        	String issue = getString(c, "issue");
		        	if ( issue != null ) {
		        		reference = reference + "(" + issue + ")";
		          	}
		        	String pages = getString(c, "pages");
		        	if ( pages != null ) {
		        		reference = reference + ":" + pages;
		          	}
		        	details = add_italic (details, reference);
		        	details = add_normal(details, "\n\n");
		            details.setSpan(new RelativeSizeSpan((float) 1.2), 0, details.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		            last_length = details.length();
		            
		            title = getString(c, "title");
		        	if ( title != null ) {
		        		details = add_bold (details, title+"\n\n");
		        		details.setSpan(new RelativeSizeSpan((float) 1.5), last_length, details.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        		last_length = details.length();
		        	}
		        	
		        	authors = getString(c, "authors");
		        	if ( authors != null ) {
		        		details = add_normal (details, authors +"\n\n");
		        		details.setSpan(new RelativeSizeSpan((float) 1.2), last_length, details.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        		last_length = details.length();
		        	}
		
		            String _abstract = getString(c, "abstract");
		        	if ( _abstract != null ) {
		        		details = add_normal (details, _abstract + "\n\n");
		        		last_length = details.length();
		          	}
		            
		        	String note = getString(c, "note");
		        	if ( note != null ) {
		        		details = add_bold (details, "Notes:\n");
		        		
		        		//format note string
		        		note = note.replaceAll ("<m:note>", "");
		        		note = note.replaceAll ("</m:note>", "");
		        		note = note.replaceAll ("<m:bold>", "<b>");
		        		note = note.replaceAll ("</m:bold>", "</b>");
		        		note = note.replaceAll ("<m:italic>", "<i>");
		        		note = note.replaceAll ("</m:italic>", "</i>");
		        		note = note.replaceAll ("<m:underline>", "<u>");
		        		note = note.replaceAll ("</m:underline>", "</u>");
		        		note = note.replaceAll ("<m:linebreak/>", "<br />");
		        		note = note.replaceAll ("<m:center>", "<div align=\"center\">");
		        		note = note.replaceAll ("</m:center>", "</div>");
		        		note = note.replaceAll ("<m:left>", "<div align=\"left\">");
		        		note = note.replaceAll ("</m:left>", "</div>");
		        		note = note.replaceAll ("<m:right>", "<div align=\"right\">");
		        		note = note.replaceAll ("</m:right>", "</div>");
		        		
		        		details.append (Html.fromHtml(note));
		        		details = add_normal (details, "\n\n");
		        		last_length = details.length();
		          	}
		            
		            Cursor d = db.rawQuery("select *, group_concat(tag, ', ') as tags from Documents left outer join DocumentTags on Documents.id = DocumentTags.documentid where Documents.id=" + id + " group by Documents.id", null);
		            if (d != null) {
		            	startManagingCursor(d);
		            	d.moveToFirst();
		            	if (!d.isAfterLast()) {
			            	String tags = getString(d, "tags");
			            	if ( tags != null ) {
			            		details = add_bold (details, "Tags:\n");
			            		details = add_normal (details, tags + "\n\n");
			            		last_length = details.length();
			              	}
		            	}
		            	stopManagingCursor(d);
			           	d.close();
		            } else {
		            	Toast.makeText(context, "Error accessing database", Toast.LENGTH_SHORT).show();
		            }
		
		            d = db.rawQuery("select *, group_concat(keyword, ', ') as keywords from Documents left outer join DocumentKeywords on Documents.id = DocumentKeywords.documentid where Documents.id=" + id + " group by Documents.id", null);
		            if (d != null) {
		            	startManagingCursor(d);
		            	d.moveToFirst();
		            	if (!d.isAfterLast()) {
			            	String keywords = getString(d, "keywords");
			            	if ( keywords != null ) {
			            		details = add_bold (details, "Keywords:\n");
			            		details = add_normal (details, keywords + "\n\n");
			            		last_length = details.length();
			            		
			              	}
		            	}
		            	stopManagingCursor(d);
			           	d.close();
		            } else {
		            	Toast.makeText(context, "Error accessing database", Toast.LENGTH_SHORT).show();
		            }
		
		            d = db.rawQuery("select *, group_concat(FileNotes.note, '\n') as notes from Documents left outer join FileNotes on Documents.id = FileNotes.documentid where Documents.id=" + id + " group by Documents.id", null);
		            if (d != null) {
		            	startManagingCursor(d);
		            	d.moveToFirst();
		            	if (!d.isAfterLast()) {
			            	String notes = getString(d, "notes");
			            	if ( notes != null ) {
			            		details = add_bold (details, "Annotations:\n");
			            		details = add_normal (details, notes + "\n\n");
			            		last_length = details.length();
			              	}
		            	}
		            	stopManagingCursor(d);
			           	d.close();
		            } else {
		            	Toast.makeText(context, "Error accessing database", Toast.LENGTH_SHORT).show();
		            }
		            
		            d = db.rawQuery("select group_concat(firstNames ||' '|| lastName, ', ') as editors from Documents left outer join DocumentContributors on Documents.id = DocumentContributors.documentid where Documents.id=" + id + " and DocumentContributors.contribution='DocumentEditor' group by Documents.id", null);
		            if (d != null) {
		            	startManagingCursor(d);
		            	d.moveToFirst();
		            	if (!d.isAfterLast()) {
			            	String editors = getString(d, "editors");
			            	if ( editors != null ) {
			            		details = add_bold (details, "Editors:\n");
			            		details = add_normal (details, editors + "\n\n");
			            		last_length = details.length();
			              	}
		            	}
		            	stopManagingCursor(d);
			           	d.close();
		            } else {
		            	Toast.makeText(context, "Error accessing database", Toast.LENGTH_SHORT).show();
		            }
		            
		            d = db.rawQuery("select url from DocumentUrls where documentid=" + id, null);
		            if (d != null) {
		            	startManagingCursor(d);
		            	d.moveToFirst();
		            	if (!d.isAfterLast()) {
			            	String url = getString(d, "url");
			            	if ( url != null ) {
			            		details = add_bold (details, "URL:\n");
			            		last_length = details.length();
			            		details = add_normal (details, url + "\n\n");
			            		details.setSpan(new URLSpan(url), last_length, details.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			            		last_length = details.length();
			              	}
		            	}
		            	stopManagingCursor(d);
			           	d.close();
		            } else {
		            	Toast.makeText(context, "Error accessing database", Toast.LENGTH_SHORT).show();
		            }
		            String added = getString(c, "added");
		        	if ( added != null ) {
		        		try {
		        			Long added_ms = Long.parseLong(added);
		        			details = add_bold (details, "Added:\n");
			        		final Calendar cal = Calendar.getInstance();
			        		cal.setTimeInMillis(added_ms*1000);
			        		SimpleDateFormat dateform = new SimpleDateFormat();
			        		details = add_normal (details, dateform.format(cal.getTime()) + "\n\n");
			        		last_length = details.length();
		        		} catch (NumberFormatException e) {
		        			;
		        		}
		          	}
	
		            boolean exclude;
		            for ( int i=0; i< c.getColumnCount(); i++) {
			    		exclude=false;
		            	String ColumnName = c.getColumnName(i);
			    		for (int j=0; j < column_excluded.length; j++) {
			    			if (ColumnName.toLowerCase().equals(column_excluded[j])) {
			    				exclude = true;
			    			}
			    		}
		            	String value = c.getString(i);
			    		if ((!exclude) && (value != null) && (value != "")) {
			    			ColumnName=Character.toUpperCase(ColumnName.charAt(0)) + (ColumnName.length() > 1 ? ColumnName.substring(1) : "");
			        		details = add_bold (details, ColumnName + ":\n");
			     	      	details = add_normal (details, value + "\n\n");
			    		}
			     
			        }
	
		            if ( !current_search.equals("") ) {
		            	//identify search text in string
		            	String[] search = TextUtils.split(current_search, " ");
		            	String details_text = details.toString().toLowerCase();
		            	for (int i = 0; i < search.length; i++) {
	                		if ( !search[i].equals("") ) {
	            	            int position = 0;
			 	            	if ( details_text != null ) {
					            	String [] details_text_split = TextUtils.split(details_text, search[i].toLowerCase());
					            	if ( details_text_split[0].length() != details_text.length()) {
					            		for (int j = 0; j < details_text_split.length - 1; j++) {
					            			position = position + details_text_split[j].length();
					            			details.setSpan(new BackgroundColorSpan(0xFFFF0000), position, position + search[i].length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					            			position = position + search[i].length();
					            		}
					            	}
				            	}
	                   		}
	                	}
		            }
		            
			    	TextView vw = (TextView)findViewById(R.id.reference);
			    	vw.setTextSize(font_size);
					vw.setText(details, TextView.BufferType.SPANNABLE);
					vw.setMovementMethod(LinkMovementMethod.getInstance());
					Button cancel = (Button) findViewById(R.id.Button01);
				        cancel.setOnClickListener(new View.OnClickListener() {
				            public void onClick(View view) {
				        		setResult(RESULT_CANCELED);
				              	finish();
				            }
			        });
			        Button open_file = (Button) findViewById(R.id.Button02);
			        open_file.setOnClickListener(new View.OnClickListener() {
			            public void onClick(View view) {
			        		Openfile (id, OPEN_FILE);
			            }
			        });
			        Button send_file = (Button) findViewById(R.id.Button03);
			        send_file.setOnClickListener(new View.OnClickListener() {
			            public void onClick(View view) {
			        		Openfile (id, SEND_FILE);
			            }
			        });
				}
            	stopManagingCursor(c);
	           	c.close();
	        } else {
	        	Toast.makeText(context, "Error accessing database", Toast.LENGTH_SHORT).show();
	        	setResult(RESULT_CANCELED);
	        	finish();
	        }
		} catch ( SQLiteException e ) {
			Context context = getApplicationContext();  	
			Toast.makeText(context, "Error accessing database", Toast.LENGTH_SHORT).show();
			setResult(RESULT_CANCELED);
			finish();
		}
    	if ( db != null ) {
    		db.close();
    	}
	}	
	
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if ( FileDialog != null && FileDialog.isShowing() ) {
        	FileDialog.dismiss();
        }
        
        super.onSaveInstanceState(savedInstanceState);
    }
		
	protected SpannableStringBuilder add_bold(SpannableStringBuilder sp, String st) {
		int sp_length = sp.length();
		int st_length = st.length();
		sp.append(st);
		sp.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), sp_length, sp_length+st_length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return sp;
	}
	
	protected SpannableStringBuilder add_italic(SpannableStringBuilder sp, String st) {
		int sp_length = sp.length();
		int st_length = st.length();
		sp.append(st);
		sp.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), sp_length, sp_length+st_length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return sp;
	}
	
	protected SpannableStringBuilder add_normal(SpannableStringBuilder sp, String st) {
		sp.append(st);
		return sp;
	}
	
	protected String getString(Cursor cursor, String column) {
		try {
			int column_index= cursor.getColumnIndex(column);
			if ( column_index != -1 ) {
				String value = cursor.getString(column_index); 
				if ( (value != null) && (!value.equals("")) ) {
					return value;
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
		catch ( SQLiteException e ) {
			return null;
		}
	}
	
	public void Openfile (String id, int type) {
		task = type;
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
					
					if (task == SEND_FILE ) {
						Toast.makeText(context, "Cannot send Dropbox files directly.\nDropbox started. Please send file from there.",Toast.LENGTH_LONG).show();
					}
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
				switch (task) {
					case OPEN_FILE:
						intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(path, mime);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						break;
					default:
						intent = new Intent(Intent.ACTION_SEND);
						intent.setType(mime);
		                intent.putExtra(Intent.EXTRA_SUBJECT, title);
		                intent.putExtra(Intent.EXTRA_STREAM, path);
		                intent.putExtra(Intent.EXTRA_TEXT, "The following document is attached:\n\n" + authors + "\n\n" + title + "\n\n" + reference );
		                intent = Intent.createChooser(intent, "Email:");
						break;
				}
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
}
