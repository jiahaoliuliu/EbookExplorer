package com.jiahaoliuliu.ebookexplorer;

import java.util.ArrayList;
import java.util.List;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxFileSystem;
import com.jiahaoliuliu.ebookexplorer.util.FolderLoader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

public class MainActivity extends SherlockFragmentActivity
	implements LoaderCallbacks<List<DbxFileInfo>>, ActionBar.OnNavigationListener {

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
	
	// Others
	private String[] sortBy;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Request the progress bar
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// Lock the screen
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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

		// List with double tap actions
		ebooksListView = (ListView)findViewById(R.id.ebooksListView);
		final GestureDetector gestureDectector = new GestureDetector(this, new GestureListener());        
	    ebooksListView.setOnTouchListener(new OnTouchListener() {

		        @Override
		        public boolean onTouch(View v, MotionEvent event) {
		            gestureDectector.onTouchEvent(event);
		            return true;
		        }
		    }
	    );

	    // List navigation for the action bar
	    sortBy = getResources().getStringArray(R.array.sort_by);
	    Context context = getSupportActionBar().getThemedContext();
	    ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(context, R.array.sort_by, R.layout.sherlock_spinner_item);
	    list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);

	    getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
	    getSupportActionBar().setListNavigationCallbacks(list, this);

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
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
    	Log.v(LOG_TAG, "You have selected the position " + itemPosition +
    			" which is " + sortBy[itemPosition]);
        return true;
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
    
    public class GestureListener extends GestureDetector.SimpleOnGestureListener {

        public boolean onDown(MotionEvent e) {
            return true;
        }

        public boolean onDoubleTap(MotionEvent e) {
            Log.d(LOG_TAG, "Yes, Clicked " + e.getX() + ", " + e.getY());
            int position = ebooksListView.pointToPosition((int)e.getX(), (int)e.getY());
            Log.d(LOG_TAG, "The user has clicked on the position " + position);

            // If the user has clicked on a wrong position
            if (position == -1 || position == AdapterView.INVALID_ROW_ID) {
            	return false;
            }

			DbxFileInfo info = (DbxFileInfo)ebooksListView.getAdapter().getItem(position);
			Log.v(LOG_TAG, "The user has clicked on the file " + info.path.getName() +
					" with the path " + info.path);

			Intent startEbookDetailsActivityIntent = new Intent(MainActivity.this, EbookDetailsActivity.class);
			startEbookDetailsActivityIntent.putExtra(EbookDetailsActivity.EBOOK_NAME_INTENT_KEY, info.path.getName());
			startEbookDetailsActivityIntent.putExtra(EbookDetailsActivity.EBOOK_PATH_INTENT_KEY, info.path.toString());

			startActivity(startEbookDetailsActivityIntent);
            return true;
        }
    }
}