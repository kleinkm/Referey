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
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

public class HelpActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

   		setTheme(android.R.style.Theme_Light_NoTitleBar);
    	super.onCreate(savedInstanceState);

		setContentView(R.layout.help);
		
		// creating references to the UI Commands
		WebView webview = (WebView) findViewById(R.id.WebView01);
		Button OKButton = (Button) findViewById(R.id.Button01);
		
		webview.loadUrl("file:///android_asset/help.html"); 
		
		OKButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
		    	setResult(RESULT_OK);
		    	finish();
			}
		});
	}
}
