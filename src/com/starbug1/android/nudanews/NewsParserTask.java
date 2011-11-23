/**
 * 
 */
package com.starbug1.android.nudanews;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

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
		progresDialog_.setMessage("Now Loading...");
		progresDialog_.show();
	}
	
	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected NewsListAdapter doInBackground(String... params) {
		NewsListAdapter result = null;
		try {
			DatabaseHelper helper = new DatabaseHelper(activity_);
			final SQLiteDatabase db = helper.getWritableDatabase();
			Cursor c = db.rawQuery("select title, description, link, source from feeds order by published_at limit 20", null);
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
			c.close();
			return adapter_;
		} catch(Exception e) {
			Log.e("NewsParserTask", e.getMessage());
			throw new NewsException("failed to background task.", e);
		}
	}

	@Override
	protected void onPostExecute(NewsListAdapter result) {
		progresDialog_.dismiss();
		activity_.setListAdapter(result);
	}

	private NewsListAdapter parseXml(InputStream is) {
		XmlPullParser parser = Xml.newPullParser();
		try {
			parser.setInput(is, null);
			int eventType = parser.getEventType();
			NewsListItem currentItem = null;
			while (eventType != XmlPullParser.END_DOCUMENT) {
				String tag = null;
				switch (eventType) {
					case XmlPullParser.START_TAG:
						tag = parser.getName();
						Log.d("NewsParserTask", ">" + tag);
						if (tag.equals("item")) {
							currentItem = new NewsListItem();
						} else if (currentItem != null) {
							if (tag.equals("title")) {
								currentItem.setTitle(parser.nextText());
							} else if (tag.equals("description")) {
								currentItem.setDescription(parser.nextText());
							} else if (tag.equals("link")) {
								currentItem.setLink(parser.nextText());
							} else if (tag.equals("dc:subject")) {
								currentItem.setCategory(parser.nextText());
							} else if (tag.equals("dc:date")) {
								currentItem.setPublishedAt(parser.nextText());
							}
						}
						break;
					case XmlPullParser.END_TAG:
						tag = parser.getName();
						Log.d("NewsParserTask", "<" + tag);
						if (tag.equals("item")) {
							Log.d("NewsParserTask", "title:" + currentItem.getTitle());
							if (currentItem.getTitle().toString().indexOf("［PR］") == -1) {
								adapter_.add(currentItem);
							}
						}
						break;
				}
				eventType = parser.next();
			}
		} catch (Exception e) {
			Log.e("NewsParserTask", e.getMessage());
			throw new NewsException("failed to retrieve rss feed.", e);
		}
		return adapter_;
	}
}
