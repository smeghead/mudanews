/**
 * 
 */
package com.starbug1.android.nudanews;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.xmlpull.v1.XmlPullParser;

import com.starbug1.android.nudanews.data.DatabaseHelper;
import com.starbug1.android.nudanews.data.NewsListItem;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.text.method.DateTimeKeyListener;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

/**
 * @author smeghead
 *
 */
public class FetchFeedService extends Service {
	public static final String ACTION = "mudanews fetch feed Service";
	private static final String FEED = "http://labaq.com/index.rdf";
	
	public void onStart(Intent intent, int startId) {
		// タイマの設定
		Timer timer = new Timer(true);
		final Handler handler = new Handler();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					public void run() {
						int count = 0;
						try {
							URL url = new URL(FEED);
							InputStream is = url.openConnection().getInputStream();
							List<NewsListItem> list = parseXml(is);
							count = registerNews(list);
						} catch(Exception e) {
							Log.e("NewsParserTask", e.getMessage());
							throw new NewsException("failed to background task.", e);
						}
						
						if (count > 0) {
//							Toast.makeText(FetchFeedService.this, "無駄新聞の記事を更新しました",
//									Toast.LENGTH_SHORT).show();
							NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
							Notification notification = new Notification(
									android.R.drawable.btn_default, 
									"ニュースです", 
									System.currentTimeMillis());
							Intent intent = new Intent(getApplicationContext(), MudanewsActivity.class);
							PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
							notification.setLatestEventInfo(
									getApplicationContext(), 
									"無駄新聞", 
									String.valueOf(count) + "件のニュースが更新されました",
									contentIntent);
							manager.notify(R.string.app_name, notification);
						}
					}

				});
			}
		}, 1000, 1000 * 60 * 15);
		super.onStart(intent, startId);
	}

	 /* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	private List<NewsListItem> parseXml(InputStream is) {
		XmlPullParser parser = Xml.newPullParser();
		List<NewsListItem> list = new ArrayList<NewsListItem>(20);
		
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
							currentItem.setSource("らばQ");
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
								list.add(currentItem);
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
		return list;
	}

	private int registerNews(List<NewsListItem> list) {
		int registerCount = 0;
		DatabaseHelper helper = new DatabaseHelper(this);
		final SQLiteDatabase db = helper.getWritableDatabase();
		Date now = new Date();
		for (NewsListItem item : list) {
			Cursor c = db.rawQuery("select id from feeds where title = ? and source = ?", new String[]{item.getTitle(), item.getSource()});
			int count = c.getCount();
			c.close();
			if (count > 0) continue; //同じソースで同じタイトルがあったら、取り込まない。
			
			ContentValues values = new ContentValues();
	        values.put("source", "らばQ");
	        values.put("title", item.getTitle());
	        values.put("link", item.getLink());
	        values.put("description", item.getDescription());
	        values.put("category", item.getCategory());
	        values.put("published_at", item.getPublishedAt());
	        values.put("created_at", now.getTime());
	        db.insert("feeds", null, values);
	        registerCount++;
		}
		return registerCount;
	}

}
