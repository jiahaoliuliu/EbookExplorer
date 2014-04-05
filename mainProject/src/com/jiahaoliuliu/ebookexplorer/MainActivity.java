package com.jiahaoliuliu.ebookexplorer;

import com.dropbox.sync.android.DbxAccountManager;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.os.Build;

public class MainActivity extends Activity {

	private static final String LOG_TAG = MainActivity.class.getSimpleName();
	private static final int REQUEST_LINK_TO_DBX = 1000;
	private static final String APP_KEY = "znc9n35hujd5e7y";
	private static final String APP_SECRET = "9j5xc567qroisd7";

	private Button linkAccountButton;
	private DbxAccountManager mDbxAcctMgr;
	private Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
	    mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), APP_KEY, APP_SECRET);
	    context = this;

		linkAccountButton = (Button)findViewById(R.id.linkAccountButton);
		linkAccountButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mDbxAcctMgr.startLink((Activity)MainActivity.this, REQUEST_LINK_TO_DBX);
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == REQUEST_LINK_TO_DBX) {
	        if (resultCode == Activity.RESULT_OK) {
	        	Log.v(LOG_TAG, "DropBox account linked correctly");
	        } else {
	        	Log.w(LOG_TAG, "Error linking DropBox Account. The result is " + resultCode);
	        	Toast.makeText(
	        			context,
	        			getResources().getString(R.string.error_message_account_not_linked),
	        			Toast.LENGTH_LONG).show();
	        }
	    } else {
	        super.onActivityResult(requestCode, resultCode, data);
	    }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
