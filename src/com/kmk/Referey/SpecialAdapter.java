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
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class SpecialAdapter extends SimpleAdapter {
	private int unread_color;
	private int favourite_color;
	private int normal_color;
	private int favourite_unread_color;
	private int font_size;
	private List<HashMap<String, String>> mItems;

	public SpecialAdapter(Context context, List<HashMap<String, String>> items, int resource, String[] from, int[] to, boolean dark_theme, int font_size_selection) {
		super(context, items, resource, from, to);
		mItems =  new ArrayList<HashMap<String, String>>();
		
		for (int i=0; i < items.size(); i++) {
			String read = items.get(i).get("read");
			String favourite = items.get(i).get("favourite");			
       		HashMap<String, String> map = new HashMap<String, String>();
			map.put("favourite", favourite);
	   		map.put("read", read);
       		mItems.add(map);
		}
		
   		if ( dark_theme ) {
			unread_color = 0xff8080ff;
			favourite_color = 0xffff8000;
			normal_color = 0xffffffff;
			favourite_unread_color = 0xffff00ff;
		} else {
			unread_color = 0xff0000ff;
			favourite_color = 0xffff8000;
			normal_color = 0xff000000;
			favourite_unread_color = 0xffff00ff;
		}
		font_size = font_size_selection;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	  View view = super.getView(position, convertView, parent);
	  String read = mItems.get(position).get("read");
	  String favourite = mItems.get(position).get("favourite");
	  TextView text1 = (TextView) view.findViewById(R.id.item1);
	  TextView text2 = (TextView) view.findViewById(R.id.item2);
	  TextView text3 = (TextView) view.findViewById(R.id.item3);
	  text1.setTextSize((float) (font_size*0.9));
	  text2.setTextSize(font_size);
	  text3.setTextSize((float) (font_size*0.75));
	  
	  if ( !read.equals("true") && favourite.equals("true") ) {
		  text1.setTextColor(favourite_unread_color);
		  text2.setTextColor(favourite_unread_color);
		  text3.setTextColor(favourite_unread_color);
	  } else if ( !read.equals("true") ) {
		  text1.setTextColor(unread_color);
		  text2.setTextColor(unread_color);
		  text3.setTextColor(unread_color);
	  } else if ( favourite.equals("true") ) {
		  text1.setTextColor(favourite_color);
		  text2.setTextColor(favourite_color);
		  text3.setTextColor(favourite_color);
	  } else {
		  text1.setTextColor(normal_color);
		  text2.setTextColor(normal_color);
		  text3.setTextColor(normal_color);
	  }
	  return view;
	}
}
