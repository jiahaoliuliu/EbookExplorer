
package com.jiahaoliuliu.ebookexplorer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.jiahaoliuliu.ebookexplorer.util.FolderListComparator;
import com.jiahaoliuliu.ebookexplorer.util.FolderLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends SherlockFragmentActivity implements
        LoaderCallbacks<List<DbxFileInfo>>, ActionBar.OnNavigationListener {

    private enum SortBy {
        NAME, DATE;

        // The constructor if it comes from String
        public static SortBy toSortyBy(String sortBy) {
            try {
                return valueOf(sortBy);
            } catch (Exception ex) {
                return NAME;
            }
        }
    }

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_LINK_TO_DBX = 1000;

    // TODO: Copy and paste your own API key below
    private static final String APP_KEY = "";
    private static final String APP_SECRET = "";

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
    private FolderAdapter folderAdapter;

    // The list of ePubs received
    private List<DbxFileInfo> listContentFiltered;

    // To sort the list
    // The list will be sort by name first
    private SortBy sortBy = SortBy.NAME;
    private Comparator<DbxFileInfo> mSortComparator = FolderListComparator.getNameFirst(true);

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

        linkAccountButton = (Button) findViewById(R.id.linkAccountButton);
        linkAccountButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mDbxAcctMgr.startLink((Activity) MainActivity.this, REQUEST_LINK_TO_DBX);
            }
        });

        // List with double tap actions
        ebooksListView = (ListView) findViewById(R.id.ebooksListView);
        final GestureDetector gestureDectector = new GestureDetector(this, new GestureListener());
        ebooksListView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDectector.onTouchEvent(event);
                return true;
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

            // Set the list navigation mode if it is not
            if (getSupportActionBar().getNavigationMode() != ActionBar.NAVIGATION_MODE_LIST) {
                // List navigation for the action bar
                ArrayAdapter<SortBy> sortByArrayAdapter = new ArrayAdapter<SortBy>(this,
                        R.layout.sherlock_spinner_item, SortBy.values());
                sortByArrayAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);

                getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                getSupportActionBar().setListNavigationCallbacks(sortByArrayAdapter, this);
            }
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
                Toast.makeText(context,
                        getResources().getString(R.string.error_message_account_not_linked),
                        Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Retrieve the list of ebooks (with epub extension) PreCondition: The account must be linked
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

        // Check if the data exists
        if (folderAdapter == null || listContentFiltered == null) {
            return false;
        }

        Log.v(LOG_TAG, "The user has selected the position " + itemPosition);

        // Check if the new sortBy is the same as the old one
        SortBy newSortBy = SortBy.values()[itemPosition];
        if (newSortBy.equals(sortBy)) {
            return true;
        }

        sortBy = newSortBy;
        switch (sortBy) {
            case NAME:
                mSortComparator = FolderListComparator.getNameFirst(true);
                break;

            case DATE:
                mSortComparator = FolderListComparator.getDateFirst(true);
                break;
        }

        Collections.sort(listContentFiltered, mSortComparator);
        folderAdapter.notifyDataSetChanged();

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
        listContentFiltered = new ArrayList<DbxFileInfo>();
        for (DbxFileInfo fileInfo : data) {
            if (!fileInfo.isFolder) {
                String fileName = fileInfo.path.getName();
                if (fileName.endsWith(EPUB_EXTENSION)) {
                    listContentFiltered.add(fileInfo);
                }
            }
        }

        // Sort the content
        Collections.sort(listContentFiltered, mSortComparator);

        Log.v(LOG_TAG, "Data arrived " + listContentFiltered.toString());
        folderAdapter = new FolderAdapter(this, listContentFiltered);
        ebooksListView.setAdapter(folderAdapter);
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
            int position = ebooksListView.pointToPosition((int) e.getX(), (int) e.getY());
            Log.d(LOG_TAG, "The user has clicked on the position " + position);

            // If the user has clicked on a wrong position
            if (position == -1 || position == AdapterView.INVALID_ROW_ID) {
                return false;
            }

            DbxFileInfo info = (DbxFileInfo) ebooksListView.getAdapter().getItem(position);
            Log.v(LOG_TAG, "The user has clicked on the file " + info.path.getName()
                    + " with the path " + info.path);

            Intent startEbookDetailsActivityIntent = new Intent(MainActivity.this,
                    EbookDetailsActivity.class);
            startEbookDetailsActivityIntent.putExtra(EbookDetailsActivity.EBOOK_NAME_INTENT_KEY,
                    info.path.getName());
            startEbookDetailsActivityIntent.putExtra(EbookDetailsActivity.EBOOK_PATH_INTENT_KEY,
                    info.path.toString());

            startActivity(startEbookDetailsActivityIntent);
            return true;
        }
    }
}
