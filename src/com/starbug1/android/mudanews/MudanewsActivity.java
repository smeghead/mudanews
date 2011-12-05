package com.starbug1.android.mudanews;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.starbug1.android.mudanews.data.More;
import com.starbug1.android.mudanews.data.NewsListItem;

public class MudanewsActivity extends Activity {
	private List<NewsListItem> items_;
	private NewsListAdapter adapter_;
	private int page_ = 0;
	
//	private AdView adView_;

	// private final ServiceReceiver receiver_ = new ServiceReceiver();
	private FetchFeedService fetchFeedService_;
	private boolean isBound_;
	final Handler handler_ = new Handler();
	private ProgressDialog progresDialog_;

	private ServiceConnection connection_ = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// サービスにはIBinder経由で#getService()してダイレクトにアクセス可能
			fetchFeedService_ = ((FetchFeedService.FetchFeedServiceLocalBinder) service)
					.getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			fetchFeedService_ = null;
			// Toast.makeText(MudanewsActivity.this,
			// "Activity:onServiceDisconnected",
			// Toast.LENGTH_SHORT).show();
		}
	};

	void doBindService() {
		bindService(new Intent(MudanewsActivity.this, FetchFeedService.class),
				connection_, Context.BIND_AUTO_CREATE);
		isBound_ = true;
	}

	void doUnbindService() {
		if (isBound_) {
			unbindService(connection_);
			isBound_ = false;
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		if (items_ != null) {
			updateList(true);
			return;
		}
		Log.d("MudanewsActivity", "start service.");
		new Thread() {
			@Override
			public void run() {
				Log.d("MudanewsActivity", "thread start service.");
				MudanewsActivity.this.startService(new Intent(MudanewsActivity.this, FetchFeedService.class));
				Log.d("MudanewsActivity", "thread started service.");
			}
		}.start();
		Log.d("MudanewsActivity", "started service.");

		Log.d("MudanewsActivity", "bind service.");
		doBindService();
		Log.d("MudanewsActivity", "bound service.");

		page_ = 0;
		items_ = new ArrayList<NewsListItem>();
		adapter_ = new NewsListAdapter(this, items_);
		GridView view = (GridView) this.findViewById(R.id.grid);
		view.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> adapter, View view,
					int position, long id) {
				NewsListItem item = items_.get(position);
				if (item instanceof More) {
					// read more
					items_.remove(position);
					if (items_.size() > 0) {
						page_++;
					}

					updateList(false);
				} else {
					Intent intent = new Intent(MudanewsActivity.this,
							NewsDetailActivity.class);
					intent.putExtra("link", item.getLink());
					startActivity(intent);
				}
			}
		});
		Log.d("MudanewsActivity", "updateList start.");
		updateList(true);
		Log.d("MudanewsActivity", "updateList end.");

		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		manager.cancelAll();
	}

	private void updateList(boolean initAdd) {
		WindowManager w = getWindowManager();
		Display d = w.getDefaultDisplay();
		int width = d.getWidth();
		int column_count = width / 160;
		GridView grid = (GridView) this.findViewById(R.id.grid);
		grid.setNumColumns(column_count);

		NewsParserTask task = new NewsParserTask(this, adapter_);
		task.execute(String.valueOf(page_));
		
		if (!initAdd) {
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.update_feeds:
			fetchFeeds();
		}
		return super.onOptionsItemSelected(item);
	}

	private void fetchFeeds() {
		items_.clear();
		progresDialog_ = new ProgressDialog(MudanewsActivity.this);
		progresDialog_.setMessage("読み込み中...");
		progresDialog_.show();
		new Thread() {
			@Override
			public void run() {
				fetchFeedService_.updateFeeds();
				handler_.post(new Runnable() {
					public void run() {
						progresDialog_.dismiss();
						page_ = 0;
						items_.clear();
						updateList(false);
					}
				});
			}
		}.start();
	}
	@Override
	protected void onDestroy() {
//		adView_.destroy();
		super.onDestroy();
		doUnbindService();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
	}

}