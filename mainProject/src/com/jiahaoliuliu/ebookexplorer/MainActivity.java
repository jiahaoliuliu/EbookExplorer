package com.jiahaoliuliu.ebookexplorer;

import java.util.ArrayList;
import java.util.List;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxFileSystem;
import com.jiahaoliuliu.ebookexplorer.util.FolderLoader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements LoaderCallbacks<List<DbxFileInfo>> {

	private static final String LOG_TAG = MainActivity.class.getSimpleName();
	private static final int REQUEST_LINK_TO_DBX = 1000;
	
	private static final String APP_KEY = "znc9n35hujd5e7y";
	private static final String APP_SECRET = "9j5xc567qroisd7";
	
	private static final String EPUB_EXTENSION = ".epub";
	
	private LoaderCallbacks<List<DbxFileInfo>> mCallbacks;

	// The dropbox account manager
	private DbxAccountManager mDbxAcctMgr;

	// The dropbox file system
	private DbxFileSystem dbxFs;
	private Context context;

	// Layout
	private Button linkAccountButton;
	private ListView ebooksListView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Request the progress bar
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.activity_main);

	    mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), APP_KEY, APP_SECRET);
	    context = this;
	    mCallbacks = this;

		linkAccountButton = (Button)findViewById(R.id.linkAccountButton);
		linkAccountButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mDbxAcctMgr.startLink((Activity)MainActivity.this, REQUEST_LINK_TO_DBX);
			}
		});

		ebooksListView = (ListView)findViewById(R.id.ebooksListView);
		ebooksListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position,
					long id) {
				DbxFileInfo info = (DbxFileInfo)ebooksListView.getAdapter().getItem(position);
				Log.v(LOG_TAG, "The user has clicked on the file " + info.path.getName() +
						" with the path " + info.path);
				
				Intent startEbookDetailsActivityIntent = new Intent(MainActivity.this, EbookDetailsActivity.class);
				startEbookDetailsActivityIntent.putExtra(EbookDetailsActivity.EBOOK_NAME_INTENT_KEY, info.path.getName());
				startEbookDetailsActivityIntent.putExtra(EbookDetailsActivity.EBOOK_PATH_INTENT_KEY, info.path.toString());

				startActivity(startEbookDetailsActivityIntent);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		// Check if there is any account linked
		if (mDbxAcctMgr.hasLinkedAccount()) {
			Log.v(LOG_TAG, "The app started with an account already linked");
			
			// It shouldn't restart the view. The view will be restarted when 
			// the user linked the activity
			boolean restartView = false;
			retrieveEbooks(restartView);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == REQUEST_LINK_TO_DBX) {
	        if (resultCode == Activity.RESULT_OK && mDbxAcctMgr.hasLinkedAccount()) {
	        	Log.v(LOG_TAG, "DropBox account linked correctly");

	        	// Restart the view because there could be new data
	        	boolean restartView = true;
	        	retrieveEbooks(restartView);
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
	
	/**
	 * Retrieve the list of ebooks (with epub extension)
	 * PreCondition: The account must be linked
	 */
	private void retrieveEbooks(boolean shouldRestartView) {
		// Precondition
		if ((mDbxAcctMgr == null) || (!mDbxAcctMgr.hasLinkedAccount())) {
			return;
		}

		// Show the list view
		linkAccountButton.setVisibility(View.GONE);
		ebooksListView.setVisibility(View.VISIBLE);

		// Load the files
		if (shouldRestartView) {
			getSupportLoaderManager().restartLoader(0, null, mCallbacks);
		} else {
			getSupportLoaderManager().initLoader(0, null, mCallbacks);
		}
	}

	// ==================================== Menu ==========================================
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

	// ==================================== list loader ==========================================
	@Override
	public Loader<List<DbxFileInfo>> onCreateLoader(int id, Bundle args) {
		setProgressBarIndeterminateVisibility(true);
        return new FolderLoader(this, mDbxAcctMgr, DbxPath.ROOT);
	}

    @Override
    public void onLoadFinished(Loader<List<DbxFileInfo>> loader, List<DbxFileInfo> data) {

    	setProgressBarIndeterminateVisibility(false);

    	// Filter the content
    	List<DbxFileInfo> filteredData = new ArrayList<DbxFileInfo>();
    	for (DbxFileInfo fileInfo: data) {
    		if (!fileInfo.isFolder) {
    			String fileName = fileInfo.path.getName();
    			if (fileName.endsWith(EPUB_EXTENSION)) {
    				filteredData.add(fileInfo);
    			}
    		}
    	}

        Log.v(LOG_TAG, "Data arrived " + data.toString());
        ebooksListView.setAdapter(new FolderAdapter(this, filteredData));
    }

    @Override
    public void onLoaderReset(Loader<List<DbxFileInfo>> loader) {
        // Do nothing.
    }
}
