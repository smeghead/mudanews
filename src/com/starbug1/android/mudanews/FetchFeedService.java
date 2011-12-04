/**
 * 
 */
package com.starbug1.android.mudanews;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.util.Xml;

import com.starbug1.android.mudanews.data.DatabaseHelper;
import com.starbug1.android.mudanews.data.NewsListItem;

/**
 * @author smeghead
 *
 */
public class FetchFeedService extends Service {
	public static final String ACTION = "mudanews fetch feed Service";
	static final String TAG = "FetchFeedService";
	
	public void onStart(Intent intent, int startId) {
		Log.d("FetchFeedService", "onStart");
		// タイマの設定
		Timer timer = new Timer(true);
		final Handler handler = new Handler();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				Log.d("FetchFeedService", "fetch feeds start.....");
				handler.post(fetchFeeds_);
			}
		}, 1000, 1000 * 60 * 15);
		super.onStart(intent, startId);
	}

	private Runnable fetchFeeds_ = new Runnable() {
		public void run() {
			int count = updateFeeds();
			
			if (count > 0) {
				NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				Notification notification = new Notification(
						R.drawable.icon, 
						"ニュースです", 
						System.currentTimeMillis());
				notification.sound = Settings.System.DEFAULT_NOTIFICATION_URI;
				
				Intent intent = new Intent(FetchFeedService.this, MudanewsActivity.class);
				PendingIntent contentIntent = PendingIntent.getActivity(FetchFeedService.this, 0, intent, 0);
				notification.setLatestEventInfo(
						getApplicationContext(), 
						"無駄新聞", 
						String.valueOf(count) + "件のニュースが更新されました",
						contentIntent);
				manager.notify(R.string.app_name, notification);
				
			}
		}
	};
			
	public int updateFeeds() {
		int count = 0;
		try {
			count += registerNews(fetchImage(parseXml("らばQ", "http://labaq.com/index.rdf")));
			count += registerNews(fetchImage(parseXml("痛いニュース", "http://blog.livedoor.jp/dqnplus/index.rdf")));
		} catch(Exception e) {
			Log.e("NewsParserTask", e.toString());
			throw new NewsException("failed to background task.", e);
		}
		return count;
	}

	private List<NewsListItem> parseXml(String source, String urlString) {
		XmlPullParser parser = Xml.newPullParser();
		List<NewsListItem> list = new ArrayList<NewsListItem>(20);
		
		try {
			URL url = new URL(urlString);
			InputStream is = url.openConnection().getInputStream();

			parser.setInput(is, null);
			int eventType = parser.getEventType();
			NewsListItem currentItem = null;
			while (eventType != XmlPullParser.END_DOCUMENT) {
				String tag = null;
				switch (eventType) {
					case XmlPullParser.START_TAG:
						tag = parser.getName();
						if (tag.equals("item")) {
							currentItem = new NewsListItem();
							currentItem.setSource(source);
						} else if (currentItem != null) {
							if (tag.equals("title")) {
								currentItem.setTitle(parser.nextText());
							} else if (tag.equals("description")) {
								currentItem.setDescription(parser.nextText());
							} else if (tag.equals("link")) {
								currentItem.setLink(parser.nextText());
							} else if (tag.equals("subject")) {
								currentItem.setCategory(parser.nextText());
							} else if (tag.equals("encoded")) {
								String imageUrl = pickupUrl(parser.nextText());
								currentItem.setImageUrl(imageUrl);
							} else if (tag.equals("date")) {
								String dateExp = parser.nextText();
								currentItem.setPublishedAt(parseDate(dateExp));
							}
						}
						break;
					case XmlPullParser.END_TAG:
						tag = parser.getName();
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
	
	private String pickupUrl(String src) {
		Log.d("FetchFeedService", "src: " + src);
		Pattern p = Pattern.compile("<img.*src=\"([^\"]*)\"");
		Matcher m = p.matcher(src);
		if (!m.find()) {
			return "";
		}
		return m.group(1);
	}
	
	private long parseDate(String src) {
		Date d = null;
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			d = format.parse(src);
		} catch (Exception e) {
			Log.e("FetchFeedService", e.getMessage());
		}
		if (d != null) {
			return d.getTime();
		}
		try {
			SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
			d = format.parse(src);
		} catch (Exception e) {
			Log.e("FetchFeedService", e.getMessage());
		}
		if (d != null) {
			return d.getTime();
		}
		return 0;
	}

	private List<NewsListItem> fetchImage(List<NewsListItem> list) {
		for (NewsListItem item : list) {
			String imageUrl = item.getImageUrl();
			if (imageUrl == null || imageUrl.length() == 0) {
				continue;
			}
			URL url;
			try {
				url = new URL(imageUrl);
				InputStream is = url.openConnection().getInputStream();
				Bitmap image = BitmapFactory.decodeStream(is);
				
				int heightOrg = image.getHeight(), widthOrg = image.getWidth();
				int height = 0, width = 0;
				int x = 0, y = 0;

				height = width = Math.min(widthOrg, heightOrg);
				
				if (heightOrg > widthOrg) {
					// 縦長
					y = Math.abs(heightOrg - widthOrg) /  2;
				} else {
					//横長
					x = Math.abs(heightOrg - widthOrg) /  2;
				}
				
		        Matrix matrix = new Matrix(); 
		        matrix.postScale(160f / width, 160f / height);
		        Bitmap scaledBitmap = Bitmap.createBitmap(
		        		image, 
		        		x, 
		        		y, 
		        		width, 
		        		height, 
		        		matrix,
		        		true);
		        ByteArrayOutputStream stream = new ByteArrayOutputStream();
		        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
		        item.setImage(stream.toByteArray());
			} catch (MalformedURLException e) {
				Log.e("FetchFeedService", "failed to get image." + e.getMessage());
				continue;
			} catch (IOException e) {
				Log.e("FetchFeedService", "failed to get image." + e.getMessage());
				continue;
			} catch (Exception e) {
				Log.e("FetchFeedService", "failed to get image." + e.getMessage());
				continue;
			}
		}
		return list;
	}
	
	private int registerNews(List<NewsListItem> list) {
		int registerCount = 0;
		DatabaseHelper helper = new DatabaseHelper(this);
		final SQLiteDatabase db = helper.getWritableDatabase();
		Date now = new Date();
//		db.execSQL("delete from feeds");
//		db.execSQL("delete from images");
		
		for (NewsListItem item : list) {
			Cursor c = db.rawQuery("select id from feeds where title = ? and source = ?", new String[]{item.getTitle(), item.getSource()});
			int count = c.getCount();
			c.close();
			if (count > 0) continue; //同じソースで同じタイトルがあったら、取り込まない。
			
			ContentValues values = new ContentValues();
	        values.put("source", item.getSource());
	        values.put("title", item.getTitle());
	        values.put("link", item.getLink());
	        values.put("description", item.getDescription());
	        values.put("category", item.getCategory());
	        values.put("published_at", item.getPublishedAt());
	        values.put("created_at", now.getTime());
	        long id = db.insert("feeds", null, values);
	        Log.d("FetchFeedService", "feed_id:" + id);

	        registerCount++;

	        if (item.getImage() == null) {
	        	continue;
	        }
	        values = new ContentValues();
	        values.put("feed_id", id);
	        values.put("image", item.getImage());
	        values.put("created_at", now.getTime());
	        db.insert("images", null, values);
		}
		db.close();
		return registerCount;
	}

    public class FetchFeedServiceLocalBinder extends Binder {
        //サービスの取得
        FetchFeedService getService() {
            return FetchFeedService.this;
        }
    }
    //Binderの生成
    private final IBinder mBinder = new FetchFeedServiceLocalBinder();
 
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind" + ": " + intent);
        return mBinder;
    }
 
    @Override
    public void onRebind(Intent intent){
        Log.i(TAG, "onRebind" + ": " + intent);
    }
 
    @Override
    public boolean onUnbind(Intent intent){
        Log.i(TAG, "onUnbind" + ": " + intent);
 
        //onUnbindをreturn trueでoverrideすると次回バインド時にonRebildが呼ばれる
        return true;
    }
}
