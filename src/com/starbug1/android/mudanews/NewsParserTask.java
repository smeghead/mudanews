/**
 * 
 */
package com.starbug1.android.mudanews;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.GridView;

import com.starbug1.android.mudanews.data.DatabaseHelper;
import com.starbug1.android.mudanews.data.More;
import com.starbug1.android.mudanews.data.NewsListItem;

/**
 * @author smeghead
 *
 */
public class NewsParserTask extends AsyncTask<String, Integer, List<NewsListItem>> {
	private final MudanewsActivity activity_;
	private final NewsListAdapter adapter_;
	private ProgressDialog progresDialog_;
	private int page_;

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
	protected List<NewsListItem> doInBackground(String... params) {
		List<NewsListItem> result = new ArrayList<NewsListItem>(30);
		page_ = Integer.parseInt(params[0]);
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			DatabaseHelper helper = new DatabaseHelper(activity_);
			db = helper.getWritableDatabase();

			c = db.rawQuery(
					"select id, title, description, link, source " +
					"from feeds " + 
					"order by published_at desc " + 
					"limit ? " +
					"offset ?", new String[]{String.valueOf(30), String.valueOf(page_ * 30)});
			c.moveToFirst();
			for (int i = 0, len = c.getCount(); i < len; i++) {
				NewsListItem item = new NewsListItem();
				item.setId(Integer.parseInt(c.getString(0)));
				item.setTitle(c.getString(1));
				item.setDescription(c.getString(2));
				item.setLink(c.getString(3));
				item.setSource(c.getString(4));
				result.add(item);
				c.moveToNext();
			}
			for (NewsListItem item : result) {
				Cursor cu = null;
				try {
					cu = db.rawQuery("select image from images where feed_id = ?", new String[]{String.valueOf(item.getId())});
					cu.moveToFirst();
					if (cu.getCount() != 1) {
						Log.w("NewsParserTask", "no image.");
						continue;
					}
					item.setImage(cu.getBlob(0));
				} finally {
					if (cu != null) cu.close();
				}
			}
			//more
			result.add(new More());
			return result;
		} catch(Exception e) {
			Log.e("NewsParserTask", e.getMessage());
			throw new NewsException("failed to background task.", e);
		} finally {
			if (c != null) c.close();
			if (db != null) db.close();
		}
	}

	@Override
	protected void onPostExecute(List<NewsListItem> result) {
		if (progresDialog_.isShowing()) {
			progresDialog_.dismiss();
		}
		for (NewsListItem item : result) {
			adapter_.add(item);
		}
		if (page_ == 0) {
			GridView view = (GridView) activity_.findViewById(R.id.grid);
			view.setAdapter(adapter_);
//			activity_.setListAdapter(adapter_);
		}
	}
}
