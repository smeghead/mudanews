/**
 * 
 */
package com.starbug1.android.nudanews;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.starbug1.android.nudanews.data.DatabaseHelper;
import com.starbug1.android.nudanews.data.NewsListItem;

/**
 * @author smeghead
 *
 */
public class NewsParserTask extends AsyncTask<String, Integer, NewsListAdapter> {
	private final MudanewsActivity activity_;
	private final NewsListAdapter adapter_;
	private ProgressDialog progresDialog_;

	public NewsParserTask(MudanewsActivity activity, NewsListAdapter adapter) {
		activity_ = activity;
		adapter_ = adapter;
	}
	
	@Override
	protected void onPreExecute() {
		progresDialog_ = new ProgressDialog(activity_);
		progresDialog_.setMessage("読み込み中...");
		progresDialog_.show();
	}
	
	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected NewsListAdapter doInBackground(String... params) {
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			DatabaseHelper helper = new DatabaseHelper(activity_);
			db = helper.getWritableDatabase();

			c = db.rawQuery("select title, description, link, source from feeds order by published_at limit 30", null);
			c.moveToFirst();
			for (int i = 0, len = c.getCount(); i < len; i++) {
				NewsListItem item = new NewsListItem();
				item.setTitle(c.getString(0));
				item.setDescription(c.getString(1));
				item.setLink(c.getString(2));
				item.setSource(c.getString(3));
				adapter_.add(item);
				c.moveToNext();
			}
			return adapter_;
		} catch(Exception e) {
			Log.e("NewsParserTask", e.getMessage());
			throw new NewsException("failed to background task.", e);
		} finally {
			if (c != null) c.close();
			if (db != null) db.close();
		}
	}

	@Override
	protected void onPostExecute(NewsListAdapter result) {
		progresDialog_.dismiss();
		activity_.setListAdapter(result);
	}
}
