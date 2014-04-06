package com.jiahaoliuliu.ebookexplorer;

import java.io.IOException;
import java.io.InputStream;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxException.Unauthorized;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

	private ImageView bookCoverImageView;
	private TextView noCoverImageTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Lock the screen
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		setContentView(R.layout.ebook_details);
		
	    mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), APP_KEY, APP_SECRET);
	    context = this;

	    // Link the content
	    bookCoverImageView = (ImageView)findViewById(R.id.bookCoverImageView);
	    noCoverImageTextView = (TextView)findViewById(R.id.noCoverImageTextView);

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
	    
	    if (startedIntent.hasExtra(EBOOK_PATH_INTENT_KEY)) {
	        DbxPath dbxPath = new DbxPath(startedIntent.getStringExtra(EBOOK_PATH_INTENT_KEY));
	        openEbook(dbxPath);
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

	private void openEbook(DbxPath dbxPath) {
		// Check the account manager
		if (!mDbxAcctMgr.hasLinkedAccount()) {
			Log.e(LOG_TAG, "Trying to open a ebook which the user has not linked its account");
			Toast.makeText(
					context,
					getResources().getString(R.string.error_message_account_not_linked),
					Toast.LENGTH_LONG).show();
			return;
		}

		// Open the file
		try {
			DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
			DbxFile fileOpened = dbxFs.open(dbxPath);
			
			Book book = (new EpubReader()).readEpub(fileOpened.getReadStream());

			// Log the book's authors
		    Log.i(LOG_TAG, "author(s): " + book.getMetadata().getAuthors());

		    // Log the book's title
		    Log.i(LOG_TAG, "title: " + book.getTitle());

		    // If the book has cover image
		    if (book.getCoverImage() != null) {
		    	noCoverImageTextView.setVisibility(View.GONE);
		    	InputStream inputStream = book.getCoverImage().getInputStream();
			    Bitmap coverImage =
			    		BitmapFactory.decodeStream(inputStream);
			    bookCoverImageView.setVisibility(View.VISIBLE);
			    bookCoverImageView.setImageBitmap(coverImage);
		    }

			fileOpened.close();
		} catch (Unauthorized e) {
			Log.e(LOG_TAG, "Error. The user is not autorithed to get the file manager", e);
		} catch (DbxException dbxEception) {
			Log.e(LOG_TAG, "Error opening the file", dbxEception);
		} catch (IOException ioException) {
			Log.e(LOG_TAG, "Error opening the file", ioException);
		}
	}
}
