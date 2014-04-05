package com.jiahaoliuliu.ebookexplorer;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFileSystem;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class EbookDetailsActivity extends SherlockActivity {

	public static final String EBOOK_NAME_INTENT_KEY = "com.jiahaoliuliu.ebookexplorer.ebooksdetails.name";
	public static final String EBOOK_PATH_INTENT_KEY = "com.jiahaoliuliu.ebookexplorer.ebooksdetails.path";

	private static final String LOG_TAG = EbookDetailsActivity.class.getSimpleName();
	private static final String APP_KEY = "znc9n35hujd5e7y";
	private static final String APP_SECRET = "9j5xc567qroisd7";

	// The dropbox account manager
	private DbxAccountManager mDbxAcctMgr;

	// The dropbox file system
	private DbxFileSystem dbxFs;
	private Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ebook_details);
		
	    mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), APP_KEY, APP_SECRET);
	    context = this;

	    // Set home up button
	    getSupportActionBar().setHomeButtonEnabled(true);
	    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

	    Intent startedIntent = getIntent();
	    if (startedIntent.hasExtra(EBOOK_NAME_INTENT_KEY)) {
	    	// Set the name of the activity
	    	getSupportActionBar().setTitle(startedIntent.getStringExtra(EBOOK_NAME_INTENT_KEY));
	    	Log.v(LOG_TAG, "The app has the name of the ebook with it, which is " +
	    		startedIntent.getStringExtra(EBOOK_NAME_INTENT_KEY));
	    }
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			// Back to the previous activity
			onBackPressed();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

}
