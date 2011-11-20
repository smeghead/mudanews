/**
 * 
 */
package com.starbug1.android.nudanews;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.starbug1.android.nudanews.data.NewsListItem;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

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
			URL url = new URL(params[0]);
			InputStream is = url.openConnection().getInputStream();
			result = parseXml(is);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	protected void onPostExecute(NewsListAdapter result) {
		progresDialog_.dismiss();
		activity_.setListAdapter(result);
	}

	private NewsListAdapter parseXml(InputStream is) throws IOException, XmlPullParserException {
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
							}
						}
						break;
					case XmlPullParser.END_TAG:
						tag = parser.getName();
						Log.d("NewsParserTask", "<" + tag);
						if (tag.equals("item")) {
							adapter_.add(currentItem);
						}
						break;
				}
				eventType = parser.next();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return adapter_;
	}

}
